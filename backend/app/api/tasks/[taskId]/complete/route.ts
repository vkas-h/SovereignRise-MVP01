import { NextRequest, NextResponse } from 'next/server';
import { auth } from '@/lib/firebase-admin';
import { query, queryOne } from '@/lib/db';
import pool from '@/lib/db';

/**
 * POST /api/tasks/[taskId]/complete - Mark task as completed
 */
export async function POST(
  request: NextRequest,
  { params }: { params: { taskId: string } }
) {
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

    // Get task
    const taskResult = await client.query(
      'SELECT * FROM tasks WHERE id = $1 AND user_id = $2',
      [params.taskId, user.id]
    );
    const task = taskResult.rows[0];

    if (!task) {
      await client.query('ROLLBACK');
      return NextResponse.json(
        { error: 'Task not found' },
        { status: 404 }
      );
    }

    if (task.status === 'COMPLETED') {
      await client.query('ROLLBACK');
      return NextResponse.json(
        { error: 'Task is already completed' },
        { status: 400 }
      );
    }

    if (task.status === 'FAILED') {
      await client.query('ROLLBACK');
      return NextResponse.json(
        { error: 'Cannot complete a failed task' },
        { status: 400 }
      );
    }

    // Update task to completed
    const now = Date.now();
    const updatedTaskResult = await client.query(
      `UPDATE tasks SET status = 'COMPLETED', completed_at = $1 
       WHERE id = $2 RETURNING *`,
      [now, params.taskId]
    );
    const updatedTask = updatedTaskResult.rows[0];

    // Calculate streak updates
    // Get today's start timestamp (midnight UTC)
    const todayStart = now - (now % 86400000);
    const yesterdayStart = todayStart - 86400000;

    // Check if user has completed any task or habit today (before this one)
    const todayCompletionResult = await client.query(
      `SELECT COUNT(*) as count FROM (
        SELECT 1 FROM tasks 
        WHERE user_id = $1 
        AND status = 'COMPLETED'
        AND completed_at >= $2 
        AND completed_at < $3
        AND id != $4
        UNION ALL
        SELECT 1 FROM habits 
        WHERE user_id = $1 
        AND last_checked_at >= $2 
        AND last_checked_at < $3
      ) as today_completions`,
      [user.id, todayStart, now, params.taskId]
    );

    const alreadyCompletedToday = parseInt(todayCompletionResult.rows[0].count) > 0;

    console.log(`[Streak Debug] User ${user.id}:`);
    console.log(`  - Current streak: ${user.current_streak}`);
    console.log(`  - Already completed today: ${alreadyCompletedToday}`);
    console.log(`  - Today start: ${new Date(todayStart).toISOString()}`);
    console.log(`  - Yesterday start: ${new Date(yesterdayStart).toISOString()}`);

    let newUserStreak: number;
    let newLongestUserStreak: number;

    if (!alreadyCompletedToday) {
      // This is the first task/habit completed today
      // Check if user completed any task or habit yesterday
      const yesterdayCompletionResult = await client.query(
        `SELECT COUNT(*) as count FROM (
          SELECT 1 FROM tasks 
          WHERE user_id = $1 
          AND status = 'COMPLETED'
          AND completed_at >= $2 
          AND completed_at < $3
          UNION ALL
          SELECT 1 FROM habits 
          WHERE user_id = $1 
          AND last_checked_at >= $2 
          AND last_checked_at < $3
        ) as yesterday_completions`,
        [user.id, yesterdayStart, todayStart]
      );

      const completedYesterday = parseInt(yesterdayCompletionResult.rows[0].count) > 0;
      console.log(`  - Completed yesterday: ${completedYesterday}`);

      if (completedYesterday) {
        // Continue streak
        newUserStreak = (user.current_streak || 0) + 1;
        console.log(`  - Continuing streak: ${user.current_streak} -> ${newUserStreak}`);
      } else {
        // Start new streak (missed yesterday or first task ever)
        newUserStreak = 1;
        console.log(`  - Starting new streak: ${newUserStreak}`);
      }

      newLongestUserStreak = Math.max(user.longest_streak || 0, newUserStreak);
    } else {
      // Already completed a task/habit today, keep the same streak
      // But ensure it's at least 1 if they completed something today
      newUserStreak = Math.max(user.current_streak || 0, 1);
      newLongestUserStreak = Math.max(user.longest_streak || 0, 1);
      console.log(`  - Keeping existing streak: ${newUserStreak} (was ${user.current_streak})`);
    }

    console.log(`[Streak Update] New streak: ${newUserStreak}, Longest: ${newLongestUserStreak}`);

    // Update user stats with streak
    await client.query(
      `UPDATE users 
       SET total_tasks_completed = total_tasks_completed + 1,
           current_streak = $2,
           longest_streak = $3
       WHERE id = $1`,
      [user.id, newUserStreak, newLongestUserStreak]
    );

    // Commit transaction
    await client.query('COMMIT');

    return NextResponse.json({
      task: {
        id: updatedTask.id,
        user_id: updatedTask.user_id,
        title: updatedTask.title,
        description: updatedTask.description,
        status: updatedTask.status,
        difficulty: updatedTask.difficulty,
        reminder_time: updatedTask.reminder_time,
        created_at: updatedTask.created_at,
        completed_at: updatedTask.completed_at,
        is_missed: updatedTask.is_missed,
        updated_at: updatedTask.updated_at || updatedTask.created_at
      }
    });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Complete task error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  } finally {
    client.release();
  }
}

