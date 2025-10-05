import { NextRequest, NextResponse } from 'next/server';
import { verifyFirebaseToken } from '@/lib/auth';
import pool from '@/lib/db';
import { generateBurnoutInsights } from '@/lib/gemini';

/**
 * POST /api/ai/burnout/detect - Detect burnout for user
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

    // Get user from database with profile
    const userResult = await pool.query(
      'SELECT id, username FROM users WHERE firebase_uid = $1',
      [decodedToken.uid]
    );

    if (userResult.rows.length === 0) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const user = userResult.rows[0];
    const userId = user.id;

    // Calculate burnout metrics from recent tasks
    const sevenDaysAgo = Date.now() - (7 * 24 * 60 * 60 * 1000);
    
    const tasksResult = await pool.query(`
      SELECT 
        COUNT(*) as total_tasks,
        COUNT(CASE WHEN completed_at IS NOT NULL THEN 1 END) as completed_tasks,
        COUNT(CASE WHEN due_date < $1 AND completed_at IS NULL THEN 1 END) as missed_tasks
      FROM tasks
      WHERE user_id = $2 AND created_at >= $3
    `, [Date.now(), userId, sevenDaysAgo]);

    const taskData = tasksResult.rows[0];
    const totalTasks = parseInt(taskData.total_tasks) || 1;
    const completedTasks = parseInt(taskData.completed_tasks) || 0;
    const missedTasks = parseInt(taskData.missed_tasks) || 0;
    const completionRate = completedTasks / totalTasks;

    // Get usage stats for late-night activity
    const usageResult = await pool.query(`
      SELECT COALESCE(SUM(total_screen_time_minutes), 0) as late_night_minutes
      FROM usage_stats
      WHERE user_id = $1 AND timestamp >= $2 AND peak_usage_hour >= 23
    `, [userId, sevenDaysAgo]);

    const lateNightMinutes = parseInt(usageResult.rows[0]?.late_night_minutes) || 0;

    const metrics = {
      completionRate,
      missedTaskCount: missedTasks,
      lateNightActivityMinutes: lateNightMinutes,
      streakBreaks: 0, // TODO: Calculate from habits
      snoozeCount: 0 // TODO: Track snoozes
    };

    // Calculate burnout score
    let burnoutScore = 0;
    if (completionRate < 0.5) burnoutScore += 0.3;
    if (missedTasks > 5) burnoutScore += 0.2;
    if (lateNightMinutes > 120) burnoutScore += 0.2;
    burnoutScore = Math.min(burnoutScore, 1.0);

    const level = burnoutScore < 0.3 ? 'HEALTHY' : 
                  burnoutScore < 0.5 ? 'MILD' :
                  burnoutScore < 0.7 ? 'MODERATE' : 'SEVERE';

    // Generate AI insights if burnout detected
    let aiInsights = { message: '', recommendations: [] as string[] };
    if (level !== 'HEALTHY') {
      try {
        aiInsights = await generateBurnoutInsights(metrics, {
          username: user.username || 'there'
        });
      } catch (error) {
        console.error('AI insights generation failed:', error);
        aiInsights = {
          message: "You're showing signs of burnout. Let's adjust your approach.",
          recommendations: [
            "Reduce your daily task load by 30%",
            "Set a strict evening cutoff time",
            "Take a full recovery day"
          ]
        };
      }
    }

    return NextResponse.json({
      level,
      score: burnoutScore,
      metrics: {
        ...metrics,
        burnoutScore
      },
      message: aiInsights.message,
      recommendations: aiInsights.recommendations,
      shouldActivateRecoveryMode: level === 'SEVERE'
    });
  } catch (error) {
    console.error('Error detecting burnout:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

