import { NextRequest, NextResponse } from 'next/server';
import { auth } from '@/lib/firebase-admin';
import { query, queryOne } from '@/lib/db';

/**
 * GET /api/tasks - Get all tasks for authenticated user
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
    const user = await queryOne(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [uid]
    );

    if (!user) {
      return NextResponse.json(
        { error: 'User not found' },
        { status: 404 }
      );
    }

    // Get all tasks for user
    const result = await query(
      'SELECT * FROM tasks WHERE user_id = $1 ORDER BY created_at DESC',
      [user.id]
    );

    const tasks = result.rows.map((task: any) => ({
      id: task.id,
      user_id: task.user_id,
      title: task.title,
      description: task.description,
      status: task.status,
      difficulty: task.difficulty,
      reminder_time: task.reminder_time,
      created_at: task.created_at,
      completed_at: task.completed_at,
      is_missed: task.is_missed,
      updated_at: task.updated_at || task.created_at
    }));

    // Calculate totals
    const totalPending = tasks.filter((t: any) => t.status === 'PENDING').length;
    const totalCompleted = tasks.filter((t: any) => t.status === 'COMPLETED').length;
    const totalFailed = tasks.filter((t: any) => t.status === 'FAILED').length;

    // Calculate next reset time (next midnight UTC)
    const now = new Date();
    const nextMidnight = new Date(Date.UTC(
      now.getUTCFullYear(),
      now.getUTCMonth(),
      now.getUTCDate() + 1,
      0, 0, 0, 0
    ));

    return NextResponse.json({
      tasks,
      total_pending: totalPending,
      total_completed: totalCompleted,
      total_failed: totalFailed,
      next_reset_time: nextMidnight.getTime()
    });
  } catch (error) {
    console.error('Get tasks error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

/**
 * POST /api/tasks - Create new task
 */
export async function POST(request: NextRequest) {
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
    const user = await queryOne(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [uid]
    );

    if (!user) {
      return NextResponse.json(
        { error: 'User not found' },
        { status: 404 }
      );
    }

    // Parse request body
    const body = await request.json();
    const { title, description, difficulty, reminder_time } = body;

    // Validate inputs
    if (!title || title.trim().length < 3) {
      return NextResponse.json(
        { error: 'Title must be at least 3 characters' },
        { status: 400 }
      );
    }

    if (title.length > 100) {
      return NextResponse.json(
        { error: 'Title must be at most 100 characters' },
        { status: 400 }
      );
    }

    if (description && description.length > 500) {
      return NextResponse.json(
        { error: 'Description must be at most 500 characters' },
        { status: 400 }
      );
    }

    const validDifficulties = ['EASY', 'MEDIUM', 'HARD', 'VERY_HARD'];
    if (!difficulty || !validDifficulties.includes(difficulty)) {
      return NextResponse.json(
        { error: 'Invalid difficulty. Must be EASY, MEDIUM, HARD, or VERY_HARD' },
        { status: 400 }
      );
    }

    if (reminder_time && reminder_time <= Date.now()) {
      return NextResponse.json(
        { error: 'Reminder time must be in the future' },
        { status: 400 }
      );
    }

    // Insert task into database
    const result = await queryOne(
      `INSERT INTO tasks (user_id, title, description, status, difficulty, reminder_time, created_at, is_missed)
       VALUES ($1, $2, $3, 'PENDING', $4, $5, $6, false)
       RETURNING *`,
      [user.id, title.trim(), description || null, difficulty, reminder_time || null, Date.now()]
    );

    const task = {
      id: result.id,
      user_id: result.user_id,
      title: result.title,
      description: result.description,
      status: result.status,
      difficulty: result.difficulty,
      reminder_time: result.reminder_time,
      created_at: result.created_at,
      completed_at: result.completed_at,
      is_missed: result.is_missed,
      updated_at: result.updated_at || result.created_at
    };

    return NextResponse.json(task, { status: 201 });
  } catch (error) {
    console.error('Create task error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

