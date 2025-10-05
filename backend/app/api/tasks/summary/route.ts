import { NextRequest, NextResponse } from 'next/server';
import { auth } from '@/lib/firebase-admin';
import { query } from '@/lib/db';

/**
 * GET /api/tasks/summary - Get task summaries for a date range
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

    // Get user from database
    const userResult = await query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [uid]
    );

    if (userResult.rows.length === 0) {
      return NextResponse.json(
        { error: 'User not found' },
        { status: 404 }
      );
    }

    const userId = userResult.rows[0].id;
    
    // Get query parameters
    const { searchParams } = new URL(request.url);
    const limit = parseInt(searchParams.get('limit') || '7'); // Default: last 7 days
    
    // Get summaries ordered by date (most recent first)
    const summariesResult = await query(
      `SELECT 
        id,
        date,
        total_tasks,
        completed_tasks,
        failed_tasks,
        completion_rate,
        created_at
       FROM daily_task_summary 
       WHERE user_id = $1
       ORDER BY date DESC
       LIMIT $2`,
      [userId, limit]
    );

    const summaries = summariesResult.rows.map((row: any) => ({
      id: row.id,
      date: parseInt(row.date),
      dateString: new Date(parseInt(row.date)).toISOString().split('T')[0],
      totalTasks: row.total_tasks,
      completedTasks: row.completed_tasks,
      failedTasks: row.failed_tasks,
      completionRate: parseFloat(row.completion_rate),
      createdAt: parseInt(row.created_at)
    }));

    return NextResponse.json({
      summaries,
      count: summaries.length
    }, { status: 200 });

  } catch (error) {
    console.error('Error fetching summaries:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

