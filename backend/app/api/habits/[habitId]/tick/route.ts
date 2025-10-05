import { NextRequest, NextResponse } from 'next/server';
import { verifyToken } from '../../../../../lib/firebase-admin';
import { query } from '../../../../../lib/db';
import pool from '../../../../../lib/db';
import { GRACE_PERIOD_MS } from '../../../../../lib/constants';

/**
 * POST /api/habits/[habitId]/tick - Mark habit as completed
 */
export async function POST(
  request: NextRequest,
  { params }: { params: { habitId: string } }
) {
  const client = await pool.connect();
  
  try {
    const { habitId } = params;

    // Extract and verify token
    const authHeader = request.headers.get('authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json({ error: 'Missing or invalid authorization header' }, { status: 401 });
    }

    const token = authHeader.substring(7);
    const decodedToken = await verifyToken(token);
    if (!decodedToken) {
      return NextResponse.json({ error: 'Invalid token' }, { status: 401 });
    }

    // Get user
    const userResult = await client.query(
      'SELECT * FROM users WHERE firebase_uid = $1',
      [decodedToken.uid]
    );

    if (userResult.rows.length === 0) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const user = userResult.rows[0];

    // Start transaction
    await client.query('BEGIN');

    // Get habit
    const habitResult = await client.query(
      'SELECT * FROM habits WHERE id = $1 AND user_id = $2',
      [habitId, user.id]
    );

    if (habitResult.rows.length === 0) {
      await client.query('ROLLBACK');
      return NextResponse.json({ error: 'Habit not found' }, { status: 404 });
    }

    const habit = habitResult.rows[0];

    // Check if already checked within the habit's cadence period
    const now = Date.now();
    const oneDayMs = 24 * 60 * 60 * 1000;
    
    // Calculate cadence based on habit type
    let cadenceMs: number;
    if (habit.type === 'DAILY') {
      cadenceMs = oneDayMs; // 24 hours
    } else if (habit.type === 'WEEKLY') {
      cadenceMs = 7 * oneDayMs; // 7 days
    } else if (habit.type === 'CUSTOM_INTERVAL') {
      cadenceMs = habit.interval_days * oneDayMs; // interval_days * 24 hours
    } else {
      cadenceMs = oneDayMs; // Default to daily
    }
    
    if (habit.last_checked_at) {
      const elapsed = now - habit.last_checked_at;
      
      // Enforce a positive minimum gap to prevent back-to-back rewards
      // Set minimum to 1 hour to prevent rapid retick attempts
      const minimumGapMs = 1 * 60 * 60 * 1000; // 1 hour
      
      // Allow tick when next due boundary (last_checked_at + cadenceMs) is within GRACE_PERIOD_MS
      // This means: elapsed >= (cadenceMs - GRACE_PERIOD_MS)
      // But always enforce the minimum gap as the lower bound
      const requiredInterval = Math.max(minimumGapMs, cadenceMs - GRACE_PERIOD_MS);
      
      if (elapsed < requiredInterval) {
        await client.query('ROLLBACK');
        return NextResponse.json({ error: 'Habit already checked within its cadence period' }, { status: 400 });
      }
    }

    // Calculate user's streak BEFORE updating the habit
    // This ensures we check yesterday's completions with the old data
    const todayStart = now - (now % oneDayMs); // Start of today (midnight)
    const yesterdayStart = todayStart - oneDayMs - GRACE_PERIOD_MS;
    const yesterdayEnd = todayStart + GRACE_PERIOD_MS; // Allow grace period into today
    
    // Check if user already completed any habit today (before this tick)
    const todayCompletionResult = await client.query(
      `SELECT COUNT(*) as count FROM habits 
       WHERE user_id = $1 
       AND last_checked_at >= $2`,
      [user.id, todayStart]
    );
    
    const alreadyCompletedToday = parseInt(todayCompletionResult.rows[0].count) > 0;
    
    let newUserStreak: number;
    let newLongestUserStreak: number;
    
    if (!alreadyCompletedToday) {
      // This is the first habit completed today
      // Check if user completed any habit yesterday
      const yesterdayCompletionResult = await client.query(
        `SELECT COUNT(*) as count FROM habits 
         WHERE user_id = $1 
         AND last_checked_at >= $2 
         AND last_checked_at < $3`,
        [user.id, yesterdayStart, todayStart]
      );
      
      const completedYesterday = parseInt(yesterdayCompletionResult.rows[0].count) > 0;
      
      if (completedYesterday) {
        // Continue streak
        newUserStreak = (user.current_streak || 0) + 1;
      } else {
        // Start new streak (missed yesterday or first habit ever)
        newUserStreak = 1;
      }
      
      newLongestUserStreak = Math.max(user.longest_streak || 0, newUserStreak);
    } else {
      // Already completed a habit today, keep the same streak
      newUserStreak = user.current_streak || 1;
      newLongestUserStreak = user.longest_streak || 1;
    }

    // Calculate habit streak
    const newStreakDays = habit.streak_days + 1;
    const newLongestStreak = Math.max(habit.longest_streak, newStreakDays);

    // Update habit
    await client.query(
      `UPDATE habits 
       SET streak_days = $1, 
           longest_streak = $2,
           total_completions = total_completions + 1,
           last_checked_at = $3
       WHERE id = $4`,
      [newStreakDays, newLongestStreak, now, habitId]
    );

    // Update user stats
    await client.query(
      `UPDATE users 
       SET total_habits_completed = total_habits_completed + 1,
           current_streak = $2,
           longest_streak = $3
       WHERE id = $1`,
      [user.id, newUserStreak, newLongestUserStreak]
    );

    // Check for milestone achievements
    let milestoneAchieved: any = null;
    const milestones = [7, 30, 100];
    
    for (let i = 0; i < milestones.length; i++) {
      const milestone = milestones[i];
      if (newStreakDays === milestone && !habit.milestones_achieved.includes(milestone)) {
        // Add milestone to achieved list
        await client.query(
          `UPDATE habits 
           SET milestones_achieved = array_append(milestones_achieved, $1)
           WHERE id = $2`,
          [milestone, habitId]
        );

        milestoneAchieved = {
          milestoneDays: milestone,
          message: `${milestone}-day streak achieved!`
        };

        break;
      }
    }

    // Commit transaction
    await client.query('COMMIT');

    // Get updated habit
    const updatedHabitResult = await client.query(
      'SELECT * FROM habits WHERE id = $1',
      [habitId]
    );
    const updatedHabit = updatedHabitResult.rows[0];

    // Transform to camelCase
    const response = {
      habit: {
        id: updatedHabit.id,
        userId: updatedHabit.user_id,
        name: updatedHabit.name,
        description: updatedHabit.description,
        type: updatedHabit.type,
        intervalDays: updatedHabit.interval_days,
        streakDays: updatedHabit.streak_days,
        longestStreak: updatedHabit.longest_streak,
        lastCheckedAt: updatedHabit.last_checked_at,
        createdAt: updatedHabit.created_at,
        isActive: updatedHabit.is_active,
        totalCompletions: updatedHabit.total_completions,
        milestonesAchieved: updatedHabit.milestones_achieved || []
      },
      newStreakDays,
      milestoneAchieved
    };

    return NextResponse.json(response, { status: 200 });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Error ticking habit:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  } finally {
    client.release();
  }
}

