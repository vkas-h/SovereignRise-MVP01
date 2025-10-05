import { NextRequest, NextResponse } from 'next/server';
import { auth } from '@/lib/firebase-admin';
import { query, queryOne } from '@/lib/db';
import pool from '@/lib/db';

/**
 * POST /api/tasks/reset - Trigger daily reset
 */
export async function POST(request: NextRequest) {
  const client = await pool.connect();
  
  try {
    // Start transaction
    await client.query('BEGIN');

    // Verify authorization
    const authHeader = request.headers.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      await client.query('ROLLBACK');
      return NextResponse.json(
        { error: 'Missing or invalid authorization header' },
        { status: 401 }
      );
    }

    const idToken = authHeader.substring(7);
    let decodedToken;
    try {
      decodedToken = await auth.verifyIdToken(idToken);
    } catch (error) {
      await client.query('ROLLBACK');
      console.error('Token verification failed:', error);
      return NextResponse.json(
        { error: 'Invalid or expired token' },
        { status: 401 }
      );
    }

    const { uid } = decodedToken;

    // Get user from database
    const userResult = await client.query(
      'SELECT * FROM users WHERE firebase_uid = $1',
      [uid]
    );
    const user = userResult.rows[0];

    if (!user) {
      await client.query('ROLLBACK');
      return NextResponse.json(
        { error: 'User not found' },
        { status: 404 }
      );
    }

    // Check if reset is needed (last reset was more than 24 hours ago)
    const lastResetTime = user.last_task_reset || 0;
    const now = Date.now();
    const twentyFourHours = 24 * 60 * 60 * 1000;
    
    let resetApplied = false;
    let missedTaskCount = 0;

    if (now - lastResetTime >= twentyFourHours) {
      // Find tasks that should be marked as failed
      // Include 15-minute grace period
      const gracePeriod = 15 * 60 * 1000;
      // Use the more recent of lastResetTime or (now - 24h) to handle long gaps
      const resetBase = Math.max(lastResetTime ?? 0, now - twentyFourHours);
      // Calculate cutoff and ensure it doesn't exceed current time
      const cutoffTime = Math.min(resetBase - gracePeriod, now);

      console.log('Daily reset calculation:', {
        lastResetTime: new Date(lastResetTime).toISOString(),
        now: new Date(now).toISOString(),
        resetBase: new Date(resetBase).toISOString(),
        cutoffTime: new Date(cutoffTime).toISOString(),
        timeSinceLastReset: (now - lastResetTime) / (60 * 60 * 1000) + ' hours'
      });

      const missedTasksResult = await client.query(
        `SELECT * FROM tasks 
         WHERE user_id = $1 
         AND status = 'PENDING' 
         AND created_at < $2`,
        [user.id, cutoffTime]
      );

      const missedTasks = missedTasksResult.rows;
      missedTaskCount = missedTasks.length;

      if (missedTaskCount > 0) {
        // Mark tasks as failed
        await client.query(
          `UPDATE tasks 
           SET status = 'FAILED', is_missed = true 
           WHERE user_id = $1 
           AND status = 'PENDING' 
           AND created_at < $2`,
          [user.id, cutoffTime]
        );

        // No penalties applied - just mark as failed
      }

      // Update last reset timestamp
      await client.query(
        'UPDATE users SET last_task_reset = $1 WHERE id = $2',
        [now, user.id]
      );

      resetApplied = true;
    }

    // Get all tasks for user
    const tasksResult = await client.query(
      'SELECT * FROM tasks WHERE user_id = $1 ORDER BY created_at DESC',
      [user.id]
    );

    const tasks = tasksResult.rows.map((task: any) => ({
      id: task.id,
      user_id: task.user_id,
      title: task.title,
      description: task.description,
      status: task.status,
      difficulty: task.difficulty,
      reminder_time: task.reminder_time,
      created_at: task.created_at,
      completed_at: task.completed_at,
      is_missed: task.is_missed
    }));

    // Calculate totals
    const totalPending = tasks.filter((t: any) => t.status === 'PENDING').length;
    const totalCompleted = tasks.filter((t: any) => t.status === 'COMPLETED').length;
    const totalFailed = tasks.filter((t: any) => t.status === 'FAILED').length;

    // Calculate next reset time (next midnight UTC)
    const nextMidnight = new Date();
    nextMidnight.setUTCHours(24, 0, 0, 0);

    // Commit transaction
    await client.query('COMMIT');

    return NextResponse.json({
      tasks,
      total_pending: totalPending,
      total_completed: totalCompleted,
      total_failed: totalFailed,
      next_reset_time: nextMidnight.getTime(),
      reset_applied: resetApplied,
      missed_task_count: missedTaskCount
    });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Daily reset error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  } finally {
    client.release();
  }
}

