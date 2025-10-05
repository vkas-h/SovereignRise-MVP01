import { NextResponse } from 'next/server';
import pool from '@/lib/db';
import { verifyFirebaseToken } from '@/lib/auth';
import { generateText } from '@/lib/gemini';

/**
 * GET /api/analytics
 * Get comprehensive analytics data with AI-generated insights
 */
export async function GET(request: Request) {
  try {
    // Verify Firebase token
    const authHeader = request.headers.get('Authorization');
    if (!authHeader?.startsWith('Bearer ')) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const token = authHeader.substring(7);
    const decodedToken = await verifyFirebaseToken(token);
    
    if (!decodedToken) {
      return NextResponse.json({ error: 'Invalid token' }, { status: 401 });
    }
    
    const firebase_uid = decodedToken.uid;

    // Parse query parameters
    const url = new URL(request.url);
    const period = parseInt(url.searchParams.get('period') || '30');

    const client = await pool.connect();

    try {
      // Get user
      const userResult = await client.query(
        'SELECT * FROM users WHERE firebase_uid = $1',
        [firebase_uid]
      );

      if (userResult.rows.length === 0) {
        return NextResponse.json({ error: 'User not found' }, { status: 404 });
      }

      const user = userResult.rows[0];
      const now = Date.now();
      const startTime = now - period * 24 * 60 * 60 * 1000;

      // 1. Get streak history
      const streakHistory = [{
        date: now,
        streakDays: user.current_streak || 0,
      }];

      // 2. Get task completion rate by day
      // Calculate today's midnight for proper "today" filtering when period=1
      const todayMidnight = new Date(now);
      todayMidnight.setHours(0, 0, 0, 0);
      const todayTimestamp = todayMidnight.getTime();
      
      // For period=1 (today), use today's midnight timestamp
      // For other periods, use the calculated startTime
      const taskStartTime = period === 1 ? todayTimestamp : startTime;
      
      const taskCompletionResult = await client.query(
        `SELECT 
          DATE(TO_TIMESTAMP(created_at::float / 1000)) as date,
          COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed,
          COUNT(*) FILTER (WHERE status = 'PENDING') as pending,
          COUNT(*) FILTER (WHERE status = 'FAILED') as failed,
          AVG(CASE WHEN status = 'COMPLETED' AND completed_at IS NOT NULL 
              THEN (completed_at - created_at) / (1000.0 * 60 * 60) ELSE NULL END) as avg_completion_hours
        FROM tasks
        WHERE user_id = $1 AND created_at >= $2
        GROUP BY DATE(TO_TIMESTAMP(created_at::float / 1000))
        ORDER BY date ASC`,
        [user.id, taskStartTime]
      );

      const taskCompletionRate = taskCompletionResult.rows.map((row) => {
        const total = parseInt(row.completed) + parseInt(row.pending) + parseInt(row.failed);
        const completionRate = total > 0 ? parseInt(row.completed) / total : 0;
        return {
          date: new Date(row.date).getTime(),
          tasksCompleted: parseInt(row.completed),
          tasksPending: parseInt(row.pending),
          tasksFailed: parseInt(row.failed),
          completionRate,
          avgCompletionHours: row.avg_completion_hours ? parseFloat(row.avg_completion_hours) : 0
        };
      });

      // 3. Get phone usage data from usage_stats table
      const phoneUsageResult = await client.query(
        `SELECT 
          DATE(TO_TIMESTAMP(timestamp::float / 1000)) as day,
          -- Use MAX to get the latest value for each day (handles any duplicates)
          MAX(total_screen_time_minutes) as total_screen_time_minutes,
          MAX(unlock_count) as unlock_count,
          MAX(distracting_app_time_minutes) as distracting_app_time_minutes,
          MAX(productive_app_time_minutes) as productive_app_time_minutes,
          MAX(peak_usage_hour) as peak_usage_hour
        FROM usage_stats
        WHERE user_id = $1 AND timestamp >= $2
        GROUP BY DATE(TO_TIMESTAMP(timestamp::float / 1000))
        ORDER BY day ASC`,
        [user.id, startTime]
      );

      const phoneUsage = phoneUsageResult.rows.map((row) => ({
        date: new Date(row.day).getTime(),
        screenTimeMinutes: parseInt(row.total_screen_time_minutes || 0),
        unlockCount: parseInt(row.unlock_count || 0),
        distractingTime: parseInt(row.distracting_app_time_minutes || 0),
        productiveTime: parseInt(row.productive_app_time_minutes || 0),
        peakHour: row.peak_usage_hour
      }));

      // Calculate today's stats (from midnight to now)
      // Note: usage_stats table has one row per day with timestamp at midnight
      // todayMidnight and todayTimestamp already calculated above for task filtering
      
      const todayStatsResult = await client.query(
        `SELECT 
          COALESCE(MAX(total_screen_time_minutes), 0) as screen_time,
          COALESCE(MAX(unlock_count), 0) as unlocks
        FROM usage_stats
        WHERE user_id = $1 
          AND DATE(TO_TIMESTAMP(timestamp::float / 1000)) = DATE(TO_TIMESTAMP($2::float / 1000))`,
        [user.id, todayTimestamp]
      );

      const todayStats = {
        screenTimeMinutes: parseInt(todayStatsResult.rows[0]?.screen_time || 0),
        unlockCount: parseInt(todayStatsResult.rows[0]?.unlocks || 0)
      };

      // Calculate last 7 days stats (including today)
      const sevenDaysAgo = new Date(todayMidnight);
      sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6); // -6 to include today = 7 days total
      const sevenDaysTimestamp = sevenDaysAgo.getTime();
      
      const weekStatsResult = await client.query(
        `SELECT 
          COALESCE(SUM(daily_screen_time), 0) as screen_time,
          COALESCE(SUM(daily_unlocks), 0) as unlocks
        FROM (
          SELECT 
            DATE(TO_TIMESTAMP(timestamp::float / 1000)) as day,
            MAX(total_screen_time_minutes) as daily_screen_time,
            MAX(unlock_count) as daily_unlocks
          FROM usage_stats
          WHERE user_id = $1 
            AND DATE(TO_TIMESTAMP(timestamp::float / 1000)) >= DATE(TO_TIMESTAMP($2::float / 1000))
            AND DATE(TO_TIMESTAMP(timestamp::float / 1000)) <= DATE(TO_TIMESTAMP($3::float / 1000))
          GROUP BY DATE(TO_TIMESTAMP(timestamp::float / 1000))
        ) daily_stats`,
        [user.id, sevenDaysTimestamp, todayTimestamp]
      );
      
      const weekStats = {
        screenTimeMinutes: parseInt(weekStatsResult.rows[0]?.screen_time || 0),
        unlockCount: parseInt(weekStatsResult.rows[0]?.unlocks || 0)
      };

      // 4. Get app usage breakdown
      // For period=1 (today), use today's midnight timestamp to match screen time calculation
      // For other periods, use the calculated startTime
      const appUsageStartTime = period === 1 ? todayTimestamp : startTime;
      
      const appUsageResult = await client.query(
        `SELECT 
          package_name,
          app_name,
          -- Sum usage across all timestamps for the same day (handles duplicates)
          SUM(usage_time_minutes) as total_minutes,
          category,
          is_productive,
          -- Group by date to see how many days this app was used
          COUNT(DISTINCT DATE(TO_TIMESTAMP(timestamp::float / 1000))) as days_used
        FROM app_usage
        WHERE user_id = $1 
          AND DATE(TO_TIMESTAMP(timestamp::float / 1000)) >= DATE(TO_TIMESTAMP($2::float / 1000))
        GROUP BY package_name, app_name, category, is_productive
        ORDER BY total_minutes DESC
        LIMIT 20`,
        [user.id, appUsageStartTime]
      );

      const topApps = appUsageResult.rows.map((row) => ({
        packageName: row.package_name,
        appName: row.app_name || row.package_name,
        totalMinutes: parseInt(row.total_minutes || 0),
        category: row.category,
        isProductive: row.is_productive,
        daysUsed: parseInt(row.days_used || 0)
      }));

      // 5. Get habit completion data
      // Use the same approach: todayTimestamp for period=1, startTime for others
      const habitStartTime = period === 1 ? todayTimestamp : startTime;
      
      const habitLogsResult = await client.query(
        `SELECT 
          DATE(TO_TIMESTAMP(completed_at::float / 1000)) as date,
          COUNT(*) as completions,
          AVG(streak_at_completion) as avg_streak
        FROM habit_logs
        WHERE user_id = $1 AND completed_at >= $2
        GROUP BY DATE(TO_TIMESTAMP(completed_at::float / 1000))
        ORDER BY date ASC`,
        [user.id, habitStartTime]
      );

      const habitCompletions = habitLogsResult.rows.map((row) => ({
        date: new Date(row.date).getTime(),
        completions: parseInt(row.completions),
        avgStreak: row.avg_streak ? parseFloat(row.avg_streak) : 0
      }));

      // 6. Calculate summary statistics
      const totalTasks = taskCompletionRate.reduce((sum, day) => 
        sum + day.tasksCompleted + day.tasksPending + day.tasksFailed, 0);
      const totalCompleted = taskCompletionRate.reduce((sum, day) => 
        sum + day.tasksCompleted, 0);
      const overallCompletionRate = totalTasks > 0 ? totalCompleted / totalTasks : 0;
      
      const totalScreenTime = phoneUsage.reduce((sum, day) => sum + day.screenTimeMinutes, 0);
      const avgDailyScreenTime = phoneUsage.length > 0 ? totalScreenTime / phoneUsage.length : 0;
      
      const totalUnlocks = phoneUsage.reduce((sum, day) => sum + day.unlockCount, 0);
      const avgDailyUnlocks = phoneUsage.length > 0 ? totalUnlocks / phoneUsage.length : 0;
      
      const totalHabitCompletions = habitCompletions.reduce((sum, day) => sum + day.completions, 0);
      
      const totalDistractingTime = phoneUsage.reduce((sum, day) => sum + day.distractingTime, 0);
      const totalProductiveTime = phoneUsage.reduce((sum, day) => sum + day.productiveTime, 0);

      // Add logging for debugging
      console.log('📊 Analytics query results:', {
        todayScreenTime: todayStats.screenTimeMinutes,
        todayUnlocks: todayStats.unlockCount,
        weekScreenTime: weekStats.screenTimeMinutes,
        weekUnlocks: weekStats.unlockCount,
        topAppsCount: topApps.length,
        phoneUsageDataPoints: phoneUsage.length
      });

      // 7. Generate AI insights using Gemini
      let aiInsights = null;
      try {
        const insightPrompt = `You are a productivity and wellness AI analyst. Analyze the following user data and provide detailed, actionable insights.

**User Statistics (Last ${period} days):**
- Current Streak: ${user.current_streak} days
- Task Completion Rate: ${(overallCompletionRate * 100).toFixed(1)}%
- Total Tasks Completed: ${totalCompleted}/${totalTasks}
- Average Daily Screen Time: ${Math.round(avgDailyScreenTime)} minutes (${(avgDailyScreenTime / 60).toFixed(1)} hours)
- Average Daily Phone Unlocks: ${Math.round(avgDailyUnlocks)}
- Total Habit Completions: ${totalHabitCompletions}
- Distracting App Time: ${Math.round(totalDistractingTime)} minutes
- Productive App Time: ${Math.round(totalProductiveTime)} minutes

**Top Apps Used:**
${topApps.slice(0, 5).map((app, i) => `${i + 1}. ${app.appName}: ${app.totalMinutes} minutes (${app.isProductive ? 'Productive' : 'Distracting'})`).join('\n')}

**Task Completion Trend:**
${taskCompletionRate.slice(-7).map(day => `- ${new Date(day.date).toLocaleDateString()}: ${day.tasksCompleted}/${day.tasksCompleted + day.tasksPending + day.tasksFailed} completed (${(day.completionRate * 100).toFixed(0)}%)`).join('\n')}

Provide a comprehensive analysis in the following structured format:

## 📊 Performance Overview
[Brief 2-3 sentence summary of overall productivity and wellbeing]

## 🎯 Key Strengths
[List 2-3 positive patterns or achievements]

## ⚠️ Areas for Improvement
[List 2-3 concerning patterns or areas needing attention]

## 📱 Phone Usage Insights
[Analysis of screen time, app usage patterns, and focus levels]

## ✅ Task & Habit Patterns
[Analysis of completion rates, consistency, and time management]

## 💡 Actionable Recommendations
[Provide 3-5 specific, actionable recommendations to improve productivity and reduce phone distractions]

Keep the tone supportive and motivating. Be specific with insights and recommendations.`;

        const insightText = await generateText(insightPrompt);
        
        // Save insights to database
        await client.query(
          `INSERT INTO analytics_insights 
          (user_id, generated_at, period_days, insight_text, metrics, recommendations)
          VALUES ($1, $2, $3, $4, $5, $6)`,
          [
            user.id,
            now,
            period,
            insightText,
            JSON.stringify({
              completionRate: overallCompletionRate,
              avgScreenTime: avgDailyScreenTime,
              avgUnlocks: avgDailyUnlocks,
              totalTasks,
              totalCompleted
            }),
            []
          ]
        );

        aiInsights = insightText;
      } catch (error) {
        console.error('Error generating AI insights:', error);
        // Don't fail the entire request if insights generation fails
        aiInsights = 'Insights temporarily unavailable. Your analytics data is still being tracked.';
      }

      // 8. Return comprehensive analytics
      return NextResponse.json({
        period,
        summary: {
          currentStreak: user.current_streak,
          longestStreak: user.longest_streak,
          totalTasksCompleted: totalCompleted,
          totalHabitCompletions,
          overallCompletionRate: parseFloat((overallCompletionRate * 100).toFixed(1)),
          avgDailyScreenTime: Math.round(avgDailyScreenTime),
          avgDailyUnlocks: Math.round(avgDailyUnlocks),
          totalDistractingTime: Math.round(totalDistractingTime),
          totalProductiveTime: Math.round(totalProductiveTime),
          // Today's stats (resets at midnight)
          todayScreenTime: todayStats.screenTimeMinutes,
          todayUnlocks: todayStats.unlockCount,
          // Last 7 days stats
          weekScreenTime: weekStats.screenTimeMinutes,
          weekUnlocks: weekStats.unlockCount,
        },
        streakHistory,
        taskCompletionRate,
        phoneUsage,
        topApps,
        habitCompletions,
        aiInsights,
        generatedAt: now
      });
    } finally {
      client.release();
    }
  } catch (error) {
    console.error('Error fetching analytics:', error);
    return NextResponse.json(
      { error: 'Failed to fetch analytics', message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}
