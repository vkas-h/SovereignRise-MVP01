import { NextRequest, NextResponse } from 'next/server';
import { auth } from '@/lib/firebase-admin';
import { query, queryOne } from '@/lib/db';

export async function GET(request: NextRequest) {
  try {
    // Get the Authorization header
    const authHeader = request.headers.get('Authorization');
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json(
        { error: 'Missing or invalid authorization header' },
        { status: 401 }
      );
    }

    const idToken = authHeader.substring(7); // Remove 'Bearer ' prefix

    // Verify the Firebase ID token
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
      'SELECT * FROM users WHERE firebase_uid = $1',
      [uid]
    );

    if (!user) {
      return NextResponse.json(
        { error: 'User not found' },
        { status: 404 }
      );
    }

    // Return user data
    return NextResponse.json({
      id: user.id,
      firebaseUid: user.firebase_uid,
      email: user.email,
      username: user.username,
      displayName: user.display_name,
      photoUrl: user.photo_url,
      currentStreak: user.current_streak,
      longestStreak: user.longest_streak,
      totalTasksCompleted: user.total_tasks_completed,
      totalHabitsCompleted: user.total_habits_completed,
      isGuest: user.is_guest,
      createdAt: user.created_at,
      lastLogin: user.last_login
    });
  } catch (error) {
    console.error('Get profile error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

export async function PUT(request: NextRequest) {
  try {
    // Get the Authorization header
    const authHeader = request.headers.get('Authorization');
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json(
        { error: 'Missing or invalid authorization header' },
        { status: 401 }
      );
    }

    const idToken = authHeader.substring(7); // Remove 'Bearer ' prefix

    // Verify the Firebase ID token
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

    // Parse request body
    const body = await request.json();
    const { username, photoUrl } = body;

    // Validate username if provided
    if (username !== undefined) {
      if (typeof username !== 'string' || username.trim().length < 3 || username.trim().length > 20) {
        return NextResponse.json(
          { error: 'Username must be between 3 and 20 characters' },
          { status: 400 }
        );
      }

      // Check if username is already taken by another user
      const existingUser = await queryOne(
        'SELECT id FROM users WHERE username = $1 AND firebase_uid != $2',
        [username.trim(), uid]
      );

      if (existingUser) {
        return NextResponse.json(
          { error: 'Username is already taken' },
          { status: 409 }
        );
      }
    }

    // Build update query dynamically based on provided fields
    const updates: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    if (username !== undefined) {
      updates.push(`username = $${paramIndex}`);
      values.push(username.trim());
      paramIndex++;
    }

    if (photoUrl !== undefined) {
      updates.push(`photo_url = $${paramIndex}`);
      values.push(photoUrl);
      paramIndex++;
    }

    if (updates.length === 0) {
      return NextResponse.json(
        { error: 'No fields to update' },
        { status: 400 }
      );
    }

    // Add updated_at timestamp
    updates.push(`updated_at = NOW()`);

    // Add WHERE clause parameter
    values.push(uid);

    // Execute update query
    const result = await query(
      `UPDATE users 
       SET ${updates.join(', ')}
       WHERE firebase_uid = $${paramIndex}
       RETURNING *`,
      values
    );

    const user = result.rows[0];

    if (!user) {
      return NextResponse.json(
        { error: 'User not found' },
        { status: 404 }
      );
    }

    // Return updated user data
    return NextResponse.json({
      success: true,
      user: {
        id: user.id,
        firebaseUid: user.firebase_uid,
        email: user.email,
        username: user.username,
        displayName: user.display_name,
        photoUrl: user.photo_url,
        currentStreak: user.current_streak,
        longestStreak: user.longest_streak,
        totalTasksCompleted: user.total_tasks_completed,
        totalHabitsCompleted: user.total_habits_completed,
        isGuest: user.is_guest,
        createdAt: user.created_at,
        lastLogin: user.last_login
      }
    });
  } catch (error) {
    console.error('Update profile error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

