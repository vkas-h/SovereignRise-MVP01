import { NextRequest, NextResponse } from 'next/server';
import { verifyToken } from '../../../../lib/firebase-admin';
import { query } from '../../../../lib/db';

/**
 * GET /api/habits/[habitId] - Get specific habit
 */
export async function GET(
  request: NextRequest,
  { params }: { params: { habitId: string } }
) {
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

    // Get user ID
    const userResult = await query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [decodedToken.uid]
    );

    if (userResult.rows.length === 0) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const userId = userResult.rows[0].id;

    // Get habit
    const habitResult = await query(
      'SELECT * FROM habits WHERE id = $1 AND user_id = $2',
      [habitId, userId]
    );

    if (habitResult.rows.length === 0) {
      return NextResponse.json({ error: 'Habit not found' }, { status: 404 });
    }

    const habit = habitResult.rows[0];

    // Transform to camelCase
    const response = {
      id: habit.id,
      userId: habit.user_id,
      name: habit.name,
      description: habit.description,
      type: habit.type,
      intervalDays: habit.interval_days,
      streakDays: habit.streak_days,
      longestStreak: habit.longest_streak,
      lastCheckedAt: habit.last_checked_at,
      createdAt: habit.created_at,
      isActive: habit.is_active,
      totalCompletions: habit.total_completions,
      milestonesAchieved: habit.milestones_achieved || []
    };

    return NextResponse.json(response, { status: 200 });
  } catch (error) {
    console.error('Error fetching habit:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

/**
 * PUT /api/habits/[habitId] - Update habit
 */
export async function PUT(
  request: NextRequest,
  { params }: { params: { habitId: string } }
) {
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

    // Get user ID
    const userResult = await query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [decodedToken.uid]
    );

    if (userResult.rows.length === 0) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const userId = userResult.rows[0].id;

    // Verify habit exists and belongs to user
    const habitResult = await query(
      'SELECT * FROM habits WHERE id = $1 AND user_id = $2',
      [habitId, userId]
    );

    if (habitResult.rows.length === 0) {
      return NextResponse.json({ error: 'Habit not found' }, { status: 404 });
    }

    // Parse request body
    const body = await request.json();
    const { name, description, type, intervalDays, isActive } = body;

    // Validate
    if (name !== undefined && (name.length < 3 || name.length > 50)) {
      return NextResponse.json({ error: 'Name must be 3-50 characters' }, { status: 400 });
    }
    if (description !== undefined && description !== null && description.length > 200) {
      return NextResponse.json({ error: 'Description must be at most 200 characters' }, { status: 400 });
    }
    if (type !== undefined && !['DAILY', 'WEEKLY', 'CUSTOM_INTERVAL'].includes(type)) {
      return NextResponse.json({ error: 'Invalid type' }, { status: 400 });
    }
    if (intervalDays !== undefined && intervalDays < 1) {
      return NextResponse.json({ error: 'Custom interval must be at least 1 day' }, { status: 400 });
    }

    // Build update query dynamically
    const updates: string[] = [];
    const values: any[] = [];
    let paramCount = 1;

    if (name !== undefined) {
      updates.push(`name = $${paramCount++}`);
      values.push(name);
    }
    if (description !== undefined) {
      updates.push(`description = $${paramCount++}`);
      values.push(description);
    }
    if (type !== undefined) {
      updates.push(`type = $${paramCount++}`);
      values.push(type);
    }
    if (intervalDays !== undefined) {
      updates.push(`interval_days = $${paramCount++}`);
      values.push(intervalDays);
    }
    if (isActive !== undefined) {
      updates.push(`is_active = $${paramCount++}`);
      values.push(isActive);
    }

    if (updates.length === 0) {
      return NextResponse.json({ error: 'No fields to update' }, { status: 400 });
    }

    values.push(habitId, userId);

    const updateResult = await query(
      `UPDATE habits SET ${updates.join(', ')} WHERE id = $${paramCount++} AND user_id = $${paramCount++} RETURNING *`,
      values
    );

    const updatedHabit = updateResult.rows[0];

    // Transform to camelCase
    const response = {
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
    };

    return NextResponse.json(response, { status: 200 });
  } catch (error) {
    console.error('Error updating habit:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

/**
 * DELETE /api/habits/[habitId] - Delete habit
 */
export async function DELETE(
  request: NextRequest,
  { params }: { params: { habitId: string } }
) {
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

    // Get user ID
    const userResult = await query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [decodedToken.uid]
    );

    if (userResult.rows.length === 0) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const userId = userResult.rows[0].id;

    // Delete habit
    const deleteResult = await query(
      'DELETE FROM habits WHERE id = $1 AND user_id = $2',
      [habitId, userId]
    );

    if (deleteResult.rowCount === 0) {
      return NextResponse.json({ error: 'Habit not found' }, { status: 404 });
    }

    return new NextResponse(null, { status: 204 });
  } catch (error) {
    console.error('Error deleting habit:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

