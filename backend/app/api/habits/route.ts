import { NextRequest, NextResponse } from 'next/server';
import { verifyToken } from '../../../lib/firebase-admin';
import { query } from '../../../lib/db';

/**
 * GET /api/habits - List all habits for authenticated user
 */
export async function GET(request: NextRequest) {
  try {
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

    // Get user ID from firebase_uid
    const userResult = await query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [decodedToken.uid]
    );

    if (userResult.rows.length === 0) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const userId = userResult.rows[0].id;

    // Get habits (both active and inactive)
    const habitsResult = await query(
      `SELECT * FROM habits 
       WHERE user_id = $1 
       ORDER BY streak_days DESC, name ASC`,
      [userId]
    );

    // Transform to camelCase
    const habits = habitsResult.rows.map(row => ({
      id: row.id,
      userId: row.user_id,
      name: row.name,
      description: row.description,
      type: row.type,
      intervalDays: row.interval_days,
      streakDays: row.streak_days,
      longestStreak: row.longest_streak,
      lastCheckedAt: row.last_checked_at,
      createdAt: row.created_at,
      isActive: row.is_active,
      totalCompletions: row.total_completions,
      milestonesAchieved: row.milestones_achieved || [],
      updated_at: row.updated_at || row.created_at
    }));

    return NextResponse.json(habits, { status: 200 });
  } catch (error) {
    console.error('Error fetching habits:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

/**
 * POST /api/habits - Create new habit
 */
export async function POST(request: NextRequest) {
  try {
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

    // Parse request body
    const body = await request.json();
    const { name, description, type, intervalDays } = body;

    // Validate
    if (!name || name.length < 3 || name.length > 50) {
      return NextResponse.json({ error: 'Name must be 3-50 characters' }, { status: 400 });
    }
    if (description && description.length > 200) {
      return NextResponse.json({ error: 'Description must be at most 200 characters' }, { status: 400 });
    }
    if (!['DAILY', 'WEEKLY', 'CUSTOM_INTERVAL'].includes(type)) {
      return NextResponse.json({ error: 'Invalid type' }, { status: 400 });
    }
    if ((type === 'CUSTOM_INTERVAL' && (!intervalDays || intervalDays < 1))) {
      return NextResponse.json({ error: 'Custom interval must be at least 1 day' }, { status: 400 });
    }

    // Check MAX_ACTIVE_HABITS limit
    const countResult = await query(
      'SELECT COUNT(*) as count FROM habits WHERE user_id = $1 AND is_active = true',
      [userId]
    );
    const activeCount = parseInt(countResult.rows[0].count);
    if (activeCount >= 10) {
      return NextResponse.json({ error: 'Maximum of 10 active habits reached' }, { status: 400 });
    }

    // Insert habit
    const createdAt = Date.now();
    const finalIntervalDays = intervalDays || 1;

    const insertResult = await query(
      `INSERT INTO habits (user_id, name, description, type, interval_days, streak_days, longest_streak, created_at, is_active, total_completions, milestones_achieved)
       VALUES ($1, $2, $3, $4, $5, 0, 0, $6, true, 0, ARRAY[]::integer[])
       RETURNING *`,
      [userId, name, description || null, type, finalIntervalDays, createdAt]
    );

    const habit = insertResult.rows[0];

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
      milestonesAchieved: habit.milestones_achieved || [],
      updated_at: habit.updated_at || habit.created_at
    };

    return NextResponse.json(response, { status: 201 });
  } catch (error) {
    console.error('Error creating habit:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

