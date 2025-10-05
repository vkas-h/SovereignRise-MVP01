import { NextRequest, NextResponse } from 'next/server';
import { auth } from '@/lib/firebase-admin';
import { queryOne } from '@/lib/db';

/**
 * GET /api/tasks/summary/yesterday - Get yesterday's task summary
 */
export async function GET(request: NextRequest) {
  try {
    // Verify authorization
    const authHeader = request.headers.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
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
      console.error('Token verification failed:', error);
      return NextResponse.json(
        { error: 'Invalid or expired token' },
        { status: 401 }
      );
    }

    const { uid } = decodedToken;

    // Get user from database with timezone
    const userResult = await queryOne(
      'SELECT id, timezone FROM users WHERE firebase_uid = $1',
      [uid]
    );

    if (!userResult) {
      return NextResponse.json(
        { error: 'User not found' },
        { status: 404 }
      );
    }

    const userId = userResult.id;
    const userTimezone = userResult.timezone || 'UTC';
    
    // Calculate yesterday's date based on USER'S TIMEZONE, not UTC
    const now = Date.now();
    
    // Get current date in user's timezone
    const formatter = new Intl.DateTimeFormat('en-US', {
      timeZone: userTimezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
    
    const parts = formatter.formatToParts(new Date(now));
    const year = parseInt(parts.find(p => p.type === 'year')!.value);
    const month = parseInt(parts.find(p => p.type === 'month')!.value);
    const day = parseInt(parts.find(p => p.type === 'day')!.value);
    
    // Calculate yesterday's date in user's timezone
    const todayDate = new Date(Date.UTC(year, month - 1, day));
    const yesterdayDate = new Date(todayDate);
    yesterdayDate.setUTCDate(yesterdayDate.getUTCDate() - 1);
    
    // Get yesterday as YYYY-MM-DD string
    const yesterdayDateString = yesterdayDate.toISOString().split('T')[0];
    
    // Use yesterday's date at UTC midnight as the lookup key
    // This matches how cleanup saves summaries (UTC day boundaries)
    const yesterdayStart = yesterdayDate.getTime();
    
    console.log('📊 Fetching yesterday summary (user timezone-aware):', {
      userTimezone,
      nowUTC: new Date(now).toISOString(),
      nowInUserTZ: `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`,
      yesterdayInUserTZ: yesterdayDateString,
      yesterdayUTCMidnight: yesterdayDate.toISOString(),
      yesterdayStart,
      userId
    });

    // Get yesterday's summary
    const summaryResult = await queryOne(
      `SELECT 
        id,
        date,
        total_tasks,
        completed_tasks,
        failed_tasks,
        completion_rate,
        tasks_data,
        created_at
       FROM daily_task_summary 
       WHERE user_id = $1 AND date = $2`,
      [userId, yesterdayStart]
    );

    if (!summaryResult) {
      // No tasks yesterday
      console.log('ℹ️ No summary found for yesterday');
      return NextResponse.json({
        hasSummary: false,
        date: yesterdayStart,
        dateString: yesterdayDate.toISOString().split('T')[0],
        message: 'No tasks from yesterday'
      }, { status: 200 });
    }
    
    console.log('✅ Found summary:', {
      totalTasks: summaryResult.total_tasks,
      completedTasks: summaryResult.completed_tasks,
      failedTasks: summaryResult.failed_tasks,
      completionRate: summaryResult.completion_rate
    });

    // Parse tasks data with error handling
    let tasksData;
    try {
      tasksData = typeof summaryResult.tasks_data === 'string' 
        ? JSON.parse(summaryResult.tasks_data)
        : summaryResult.tasks_data;
    } catch (parseError) {
      console.error('❌ Failed to parse tasks_data:', parseError);
      tasksData = [];
    }

    const response = {
      hasSummary: true,
      date: parseInt(summaryResult.date),
      dateString: new Date(parseInt(summaryResult.date)).toISOString().split('T')[0],
      totalTasks: summaryResult.total_tasks,
      completedTasks: summaryResult.completed_tasks,
      failedTasks: summaryResult.failed_tasks,
      completionRate: parseFloat(summaryResult.completion_rate),
      tasks: tasksData.map((task: any) => ({
        id: task.id,
        title: task.title,
        description: task.description,
        status: task.status,
        difficulty: task.difficulty,
        createdAt: task.created_at,
        completedAt: task.completed_at
      })),
      createdAt: parseInt(summaryResult.created_at)
    };

    return NextResponse.json(response, { status: 200 });

  } catch (error) {
    console.error('❌ Error fetching yesterday\'s summary:', error);
    return NextResponse.json(
      { 
        error: 'Internal server error',
        message: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    );
  }
}

