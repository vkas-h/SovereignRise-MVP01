import { NextRequest, NextResponse } from 'next/server';
import { auth } from '@/lib/firebase-admin';
import { query, queryOne } from '@/lib/db';

/**
 * GET /api/tasks/[taskId] - Get specific task
 */
export async function GET(
  request: NextRequest,
  { params }: { params: { taskId: string } }
) {
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

    // Get task
    const task = await queryOne(
      'SELECT * FROM tasks WHERE id = $1 AND user_id = $2',
      [params.taskId, user.id]
    );

    if (!task) {
      return NextResponse.json(
        { error: 'Task not found' },
        { status: 404 }
      );
    }

    return NextResponse.json({
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
    });
  } catch (error) {
    console.error('Get task error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

/**
 * PUT /api/tasks/[taskId] - Update task
 */
export async function PUT(
  request: NextRequest,
  { params }: { params: { taskId: string } }
) {
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

    // Get existing task
    const existingTask = await queryOne(
      'SELECT * FROM tasks WHERE id = $1 AND user_id = $2',
      [params.taskId, user.id]
    );

    if (!existingTask) {
      return NextResponse.json(
        { error: 'Task not found' },
        { status: 404 }
      );
    }

    // Parse request body
    const body = await request.json();
    const { title, description, difficulty, reminder_time, status } = body;

    // Validate inputs
    if (title !== undefined) {
      if (title.trim().length < 3) {
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
    }

    if (description !== undefined && description !== null && description.length > 500) {
      return NextResponse.json(
        { error: 'Description must be at most 500 characters' },
        { status: 400 }
      );
    }

    if (difficulty !== undefined) {
      const validDifficulties = ['EASY', 'MEDIUM', 'HARD', 'VERY_HARD'];
      if (!validDifficulties.includes(difficulty)) {
        return NextResponse.json(
          { error: 'Invalid difficulty' },
          { status: 400 }
        );
      }
    }

    if (reminder_time !== undefined && reminder_time !== null && reminder_time <= Date.now()) {
      return NextResponse.json(
        { error: 'Reminder time must be in the future' },
        { status: 400 }
      );
    }

    if (status !== undefined && status === 'COMPLETED') {
      return NextResponse.json(
        { error: 'Use the complete endpoint to mark task as completed' },
        { status: 400 }
      );
    }

    // Build update query dynamically
    const updates: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    if (title !== undefined) {
      updates.push(`title = $${paramIndex++}`);
      values.push(title.trim());
    }

    if (description !== undefined) {
      updates.push(`description = $${paramIndex++}`);
      values.push(description);
    }

    if (status !== undefined) {
      updates.push(`status = $${paramIndex++}`);
      values.push(status);
    }

    if (reminder_time !== undefined) {
      updates.push(`reminder_time = $${paramIndex++}`);
      values.push(reminder_time);
    }

    if (difficulty !== undefined) {
      updates.push(`difficulty = $${paramIndex++}`);
      values.push(difficulty);
    }

    if (updates.length === 0) {
      return NextResponse.json(existingTask);
    }

    // Add task ID and user ID to values
    values.push(params.taskId);
    values.push(user.id);

    // Update task
    const result = await queryOne(
      `UPDATE tasks SET ${updates.join(', ')} 
       WHERE id = $${paramIndex++} AND user_id = $${paramIndex++}
       RETURNING *`,
      values
    );

    return NextResponse.json({
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
    });
  } catch (error) {
    console.error('Update task error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

/**
 * DELETE /api/tasks/[taskId] - Delete task
 */
export async function DELETE(
  request: NextRequest,
  { params }: { params: { taskId: string } }
) {
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

    // Check if task exists and belongs to user
    const task = await queryOne(
      'SELECT id FROM tasks WHERE id = $1 AND user_id = $2',
      [params.taskId, user.id]
    );

    if (!task) {
      return NextResponse.json(
        { error: 'Task not found' },
        { status: 404 }
      );
    }

    // Delete task
    await query(
      'DELETE FROM tasks WHERE id = $1 AND user_id = $2',
      [params.taskId, user.id]
    );

    return new NextResponse(null, { status: 204 });
  } catch (error) {
    console.error('Delete task error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

