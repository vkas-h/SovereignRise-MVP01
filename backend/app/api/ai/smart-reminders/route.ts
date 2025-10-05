import { NextRequest, NextResponse } from 'next/server';
import { verifyFirebaseToken } from '@/lib/auth';
import pool from '@/lib/db';
import { generateSmartReminderSuggestion } from '@/lib/gemini';

/**
 * POST /api/ai/smart-reminders - Generate smart reminder suggestion for a task
 */
export async function POST(request: NextRequest) {
  try {
    // Verify Firebase token
    const authHeader = request.headers.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const token = authHeader.split('Bearer ')[1];
    const decodedToken = await verifyFirebaseToken(token);
    
    if (!decodedToken) {
      return NextResponse.json({ error: 'Invalid token' }, { status: 401 });
    }

    // Get user from database
    const userResult = await pool.query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [decodedToken.uid]
    );

    if (userResult.rows.length === 0) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const userId = userResult.rows[0].id;

    // Parse request body
    const { taskTitle, taskDescription } = await request.json();

    if (!taskTitle) {
      return NextResponse.json({ error: 'Task title is required' }, { status: 400 });
    }

    // Get user's completion patterns from recent tasks
    const patternsResult = await pool.query(`
      SELECT 
        EXTRACT(HOUR FROM TO_TIMESTAMP(completed_at / 1000)) as hour,
        EXTRACT(MINUTE FROM TO_TIMESTAMP(completed_at / 1000)) as minute
      FROM tasks
      WHERE user_id = $1 AND completed_at IS NOT NULL
      ORDER BY completed_at DESC
      LIMIT 20
    `, [userId]);

    let averageHour = 17; // Default 5 PM
    let averageMinute = 0;
    let sampleSize = patternsResult.rows.length;

    if (sampleSize > 0) {
      const totalHours = patternsResult.rows.reduce((sum: number, row: any) => sum + parseFloat(row.hour), 0);
      const totalMinutes = patternsResult.rows.reduce((sum: number, row: any) => sum + parseFloat(row.minute), 0);
      averageHour = Math.round(totalHours / sampleSize);
      averageMinute = Math.round(totalMinutes / sampleSize);
    }

    // Generate AI-powered suggestion
    let aiSuggestion;
    try {
      aiSuggestion = await generateSmartReminderSuggestion(
        taskTitle,
        taskDescription || '',
        {
          averageHour,
          averageMinute,
          sampleSize
        }
      );
    } catch (error) {
      console.error('AI suggestion failed:', error);
      aiSuggestion = {
        suggestedTime: `${averageHour.toString().padStart(2, '0')}:${averageMinute.toString().padStart(2, '0')}`,
        reasoning: 'Based on your typical completion pattern'
      };
    }

    // Parse the suggested time and create a timestamp for today/tomorrow
    const [hours, minutes] = aiSuggestion.suggestedTime.split(':').map(Number);
    const now = new Date();
    const suggestedDate = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hours, minutes, 0, 0);
    
    // If the time has passed today, suggest for tomorrow
    if (suggestedDate < now) {
      suggestedDate.setDate(suggestedDate.getDate() + 1);
    }

    return NextResponse.json({
      suggestedTime: suggestedDate.getTime(),
      confidence: sampleSize >= 5 ? 0.8 : 0.5,
      reason: aiSuggestion.reasoning,
      basedOn: 'HISTORICAL_COMPLETION_TIME',
      alternativeTimes: [
        suggestedDate.getTime() - (30 * 60 * 1000), // 30 min earlier
        suggestedDate.getTime() + (30 * 60 * 1000), // 30 min later
        suggestedDate.getTime() + (60 * 60 * 1000)  // 1 hour later
      ]
    });
  } catch (error) {
    console.error('Error generating smart reminder:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

