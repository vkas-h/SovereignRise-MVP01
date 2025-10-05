import { NextRequest, NextResponse } from 'next/server';
import { verifyFirebaseToken } from '@/lib/auth';
import pool from '@/lib/db';
import { generateProductivityInsights } from '@/lib/gemini';

/**
 * GET /api/ai/insights - Get personalized productivity insights
 */
export async function GET(request: NextRequest) {
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
      'SELECT id, username, current_streak FROM users WHERE firebase_uid = $1',
      [decodedToken.uid]
    );

    if (userResult.rows.length === 0) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const user = userResult.rows[0];
    const userId = user.id;

    // Get weekly stats (last 7 days)
    const sevenDaysAgo = Date.now() - (7 * 24 * 60 * 60 * 1000);
    
    // Tasks completed this week
    const tasksResult = await pool.query(`
      SELECT COUNT(*) as tasks_completed
      FROM tasks
      WHERE user_id = $1 AND completed_at >= $2 AND status = 'COMPLETED'
    `, [userId, sevenDaysAgo]);

    // Habits completed this week
    const habitsResult = await pool.query(`
      SELECT COUNT(*) as habits_completed
      FROM habit_logs
      WHERE user_id = $1 AND completed_at >= $2
    `, [userId, sevenDaysAgo]);

    // Perfect days (all tasks completed)
    const perfectDaysResult = await pool.query(`
      SELECT COUNT(DISTINCT DATE(TO_TIMESTAMP(completed_at / 1000))) as perfect_days
      FROM tasks
      WHERE user_id = $1 
        AND completed_at >= $2
        AND NOT EXISTS (
          SELECT 1 FROM tasks t2 
          WHERE t2.user_id = $1 
            AND DATE(TO_TIMESTAMP(t2.created_at / 1000)) = DATE(TO_TIMESTAMP(tasks.completed_at / 1000))
            AND t2.completed_at IS NULL
        )
    `, [userId, sevenDaysAgo]);

    const weeklyStats = {
      tasksCompleted: parseInt(tasksResult.rows[0].tasks_completed) || 0,
      habitsCompleted: parseInt(habitsResult.rows[0]?.habits_completed) || 0,
      perfectDays: parseInt(perfectDaysResult.rows[0]?.perfect_days) || 0,
      currentStreak: user.current_streak || 0
    };

    // Generate AI insights
    let insight;
    try {
      insight = await generateProductivityInsights(weeklyStats, {
        username: user.username || 'there'
      });
    } catch (error) {
      console.error('AI insights generation failed:', error);
      insight = `This week: ${weeklyStats.tasksCompleted} tasks completed. Keep building that momentum!`;
    }

    return NextResponse.json({
      insight,
      stats: weeklyStats,
      generatedAt: Date.now()
    });
  } catch (error) {
    console.error('Error generating insights:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

