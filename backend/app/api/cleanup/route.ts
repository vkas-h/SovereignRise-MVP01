import { NextRequest, NextResponse } from 'next/server';
import { query } from '@/lib/db';

/**
 * POST /api/cleanup - Clean up expired tasks and habits
 * This endpoint should be called daily at midnight (00:00 UTC)
 */
export async function POST(request: NextRequest) {
  try {
    const now = Date.now();
    
    // Get the start of today (midnight) in UTC
    const today = new Date(now);
    today.setUTCHours(0, 0, 0, 0);
    const todayStart = today.getTime();
    
    // Get the start of tomorrow (midnight) in UTC for task cleanup
    const tomorrow = new Date(now);
    tomorrow.setUTCDate(tomorrow.getUTCDate() + 1);
    tomorrow.setUTCHours(0, 0, 0, 0);
    const tomorrowStart = tomorrow.getTime();
    
    console.log('🧹 Starting cleanup process...');
    console.log('Current time (UTC):', new Date(now).toISOString());
    console.log('Today start (UTC):', new Date(todayStart).toISOString());
    console.log('Tomorrow start (UTC):', new Date(tomorrowStart).toISOString());
    console.log('Timezone offset (minutes):', new Date().getTimezoneOffset());
    
    // ===== SAVE TASK SUMMARY BEFORE CLEANUP =====
    // Get yesterday's date (for summary) in UTC
    const yesterday = new Date(now);
    yesterday.setUTCDate(yesterday.getUTCDate() - 1);
    yesterday.setUTCHours(0, 0, 0, 0);
    const yesterdayStart = yesterday.getTime();
    
    console.log('Yesterday start (UTC):', new Date(yesterdayStart).toISOString());
    
    // Get all tasks from yesterday only (created >= yesterdayStart AND < todayStart)
    console.log('📊 Fetching tasks for summary (created_at >= yesterdayStart AND created_at < todayStart)...');
    const yesterdayTasksResult = await query(
      `SELECT user_id, id, title, description, status, difficulty, created_at, completed_at
       FROM tasks 
       WHERE created_at >= $1 AND created_at < $2`,
      [yesterdayStart, todayStart]
    );
    
    console.log(`Found ${yesterdayTasksResult.rows.length} tasks to summarize`);
    
    // Group tasks by user and calculate statistics
    const userSummaries = new Map();
    
    for (const task of yesterdayTasksResult.rows) {
      const userId = task.user_id;
      
      if (!userSummaries.has(userId)) {
        userSummaries.set(userId, {
          userId,
          totalTasks: 0,
          completedTasks: 0,
          failedTasks: 0,
          tasks: []
        });
      }
      
      const summary = userSummaries.get(userId);
      summary.totalTasks++;
      
      if (task.status === 'COMPLETED') {
        summary.completedTasks++;
      } else if (task.status === 'FAILED' || task.status === 'PENDING') {
        summary.failedTasks++;
      }
      
      summary.tasks.push({
        id: task.id,
        title: task.title,
        description: task.description,
        status: task.status,
        difficulty: task.difficulty,
        created_at: parseInt(task.created_at),
        completed_at: task.completed_at ? parseInt(task.completed_at) : null
      });
    }
    
    // Save summaries to database
    for (const [userId, summary] of userSummaries) {
      const completionRate = summary.totalTasks > 0 
        ? ((summary.completedTasks / summary.totalTasks) * 100).toFixed(2)
        : 0;
      
      try {
        // Insert or update summary for yesterday
        await query(
          `INSERT INTO daily_task_summary 
           (user_id, date, total_tasks, completed_tasks, failed_tasks, completion_rate, tasks_data, created_at)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
           ON CONFLICT (user_id, date) 
           DO UPDATE SET
             total_tasks = EXCLUDED.total_tasks,
             completed_tasks = EXCLUDED.completed_tasks,
             failed_tasks = EXCLUDED.failed_tasks,
             completion_rate = EXCLUDED.completion_rate,
             tasks_data = EXCLUDED.tasks_data`,
          [
            userId,
            yesterdayStart,
            summary.totalTasks,
            summary.completedTasks,
            summary.failedTasks,
            completionRate,
            JSON.stringify(summary.tasks),
            now
          ]
        );
        
        console.log(`💾 Saved task summary for user ${userId}:`, {
          date: new Date(yesterdayStart).toISOString().split('T')[0],
          total: summary.totalTasks,
          completed: summary.completedTasks,
          failed: summary.failedTasks,
          rate: completionRate + '%'
        });
      } catch (error) {
        console.error(`❌ Failed to save summary for user ${userId}:`, error);
        // Don't throw - continue with cleanup even if summary save fails for one user
      }
    }
    
    console.log(`✅ Saved summaries for ${userSummaries.size} user(s)`);
    
    // ===== CLEANUP EXPIRED TASKS =====
    // Delete tasks created before tomorrow (previous days)
    // Tasks should only exist for the day they were created
    console.log('🗑️  Deleting tasks created before tomorrow...');
    const tasksResult = await query(
      `DELETE FROM tasks 
       WHERE created_at < $1 
       RETURNING id, title, user_id, created_at`,
      [tomorrowStart]
    );
    
    const deletedTasksCount = tasksResult.rows.length;
    console.log(`✅ Deleted ${deletedTasksCount} expired task(s)`);
    
    if (deletedTasksCount > 0) {
      console.log('Sample deleted tasks (first 5):');
      tasksResult.rows.slice(0, 5).forEach(t => {
        const createdDate = new Date(parseInt(t.created_at));
        console.log(`  - "${t.title}" (created: ${createdDate.toISOString()}, user: ${t.user_id})`);
      });
      if (deletedTasksCount > 5) {
        console.log(`  ... and ${deletedTasksCount - 5} more`);
      }
    }
    
    // ===== CLEANUP EXPIRED HABITS =====
    // For habits, we need to check if they've reached their duration
    // A habit expires at the end of its last day at 11:59 PM UTC
    // Since cleanup runs at 00:00 UTC (start of next day), expired habits are removed
    // Example: Created Oct 3, 7-day habit → expires Oct 10 at 11:59 PM UTC → removed Oct 11 at 00:00 UTC
    
    console.log('🔍 Checking for expired habits...');
    
    // Get all habits to check expiration
    const habitsToCheckResult = await query(
      `SELECT id, name, user_id, created_at, interval_days, type 
       FROM habits 
       WHERE is_active = true`
    );
    
    console.log(`Found ${habitsToCheckResult.rows.length} active habit(s) to check`);
    
    const expiredHabitIds: string[] = [];
    
    for (const habit of habitsToCheckResult.rows) {
      const createdAt = parseInt(habit.created_at);
      const intervalDays = parseInt(habit.interval_days);
      
      // Calculate expiration time using UTC to avoid timezone bugs
      // Expiration = created date + interval_days at 11:59:59 PM UTC
      const createdDate = new Date(createdAt);
      const expirationDate = new Date(Date.UTC(
        createdDate.getUTCFullYear(),
        createdDate.getUTCMonth(),
        createdDate.getUTCDate() + intervalDays,
        23, 59, 59, 999
      ));
      const expirationTime = expirationDate.getTime();
      
      const isExpired = now > expirationTime;
      
      console.log(`  Habit: "${habit.name}" (${habit.type})`);
      console.log(`    Created: ${createdDate.toISOString()}`);
      console.log(`    Interval: ${intervalDays} day(s)`);
      console.log(`    Expires: ${expirationDate.toISOString()}`);
      console.log(`    Current: ${new Date(now).toISOString()}`);
      console.log(`    Status: ${isExpired ? '❌ EXPIRED' : '✅ ACTIVE'}`);
      
      // If current time is past expiration time, mark for deletion
      if (isExpired) {
        expiredHabitIds.push(habit.id);
      }
    }
    
    // Delete expired habits
    let deletedHabitsCount = 0;
    let deletedHabits: any[] = [];
    
    if (expiredHabitIds.length > 0) {
      console.log(`🗑️  Deleting ${expiredHabitIds.length} expired habit(s)...`);
      const habitsDeleteResult = await query(
        `DELETE FROM habits 
         WHERE id = ANY($1)
         RETURNING id, name, user_id, created_at, interval_days, type`,
        [expiredHabitIds]
      );
      
      deletedHabitsCount = habitsDeleteResult.rows.length;
      deletedHabits = habitsDeleteResult.rows;
      
      console.log(`✅ Deleted ${deletedHabitsCount} expired habit(s)`);
      
      deletedHabits.forEach(h => {
        const createdDate = new Date(parseInt(h.created_at));
        const expirationDate = new Date(createdDate);
        expirationDate.setUTCDate(expirationDate.getUTCDate() + parseInt(h.interval_days));
        console.log(`  - "${h.name}" (${h.type}, ${h.interval_days} days, expired: ${expirationDate.toISOString().split('T')[0]})`);
      });
    } else {
      console.log('✅ No expired habits to delete');
    }
    
    const response = {
      success: true,
      timestamp: new Date(now).toISOString(),
      deleted: {
        tasks: deletedTasksCount,
        habits: deletedHabitsCount
      },
      summaries_saved: userSummaries.size,
      details: {
        tasks: tasksResult.rows.map(t => ({
          id: t.id,
          title: t.title,
          created_at: new Date(parseInt(t.created_at)).toISOString()
        })),
        habits: deletedHabits.map(h => ({
          id: h.id,
          name: h.name,
          created_at: new Date(parseInt(h.created_at)).toISOString(),
          interval_days: h.interval_days
        }))
      }
    };
    
    console.log('🎉 Cleanup completed successfully');
    console.log('Summary:', {
      summaries_saved: userSummaries.size,
      tasks_deleted: deletedTasksCount,
      habits_deleted: deletedHabitsCount,
      timestamp: response.timestamp
    });
    
    return NextResponse.json(response, { status: 200 });
    
  } catch (error) {
    console.error('❌ Cleanup error:', error);
    return NextResponse.json(
      { 
        success: false,
        error: 'Internal server error during cleanup',
        message: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    );
  }
}

/**
 * GET /api/cleanup - Get cleanup status and preview what would be deleted
 */
export async function GET(request: NextRequest) {
  try {
    const now = Date.now();
    
    // Get the start of today (midnight) in UTC
    const today = new Date(now);
    today.setUTCHours(0, 0, 0, 0);
    const todayStart = today.getTime();
    
    // Get the start of tomorrow (midnight) in UTC for task cleanup
    const tomorrow = new Date(now);
    tomorrow.setUTCDate(tomorrow.getUTCDate() + 1);
    tomorrow.setUTCHours(0, 0, 0, 0);
    const tomorrowStart = tomorrow.getTime();
    
    // Count tasks that would be deleted
    const tasksCountResult = await query(
      `SELECT COUNT(*) as count FROM tasks WHERE created_at < $1`,
      [tomorrowStart]
    );
    
    // Get tasks preview
    const tasksPreviewResult = await query(
      `SELECT id, title, created_at FROM tasks 
       WHERE created_at < $1 
       LIMIT 10`,
      [tomorrowStart]
    );
    
    // Get habits that would be deleted
    const habitsToCheckResult = await query(
      `SELECT id, name, created_at, interval_days 
       FROM habits 
       WHERE is_active = true`
    );
    
    const expiredHabits: any[] = [];
    
    for (const habit of habitsToCheckResult.rows) {
      const createdAt = parseInt(habit.created_at);
      const intervalDays = parseInt(habit.interval_days);
      
      // Use UTC for expiration calculation
      const createdDate = new Date(createdAt);
      const expirationDate = new Date(Date.UTC(
        createdDate.getUTCFullYear(),
        createdDate.getUTCMonth(),
        createdDate.getUTCDate() + intervalDays,
        23, 59, 59, 999
      ));
      const expirationTime = expirationDate.getTime();
      
      if (now > expirationTime) {
        expiredHabits.push({
          id: habit.id,
          name: habit.name,
          created_at: new Date(createdAt).toISOString(),
          expires_at: expirationDate.toISOString(),
          interval_days: intervalDays
        });
      }
    }
    
    return NextResponse.json({
      current_time: new Date(now).toISOString(),
      today_start: new Date(todayStart).toISOString(),
      tomorrow_start: new Date(tomorrowStart).toISOString(),
      preview: {
        tasks: {
          count: parseInt(tasksCountResult.rows[0].count),
          sample: tasksPreviewResult.rows.map(t => ({
            id: t.id,
            title: t.title,
            created_at: new Date(parseInt(t.created_at)).toISOString()
          }))
        },
        habits: {
          count: expiredHabits.length,
          items: expiredHabits
        }
      }
    }, { status: 200 });
    
  } catch (error) {
    console.error('Cleanup preview error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

