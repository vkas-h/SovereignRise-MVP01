import { NextRequest, NextResponse } from 'next/server';
import { auth } from '@/lib/firebase-admin';
import { query, queryOne } from '@/lib/db';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { idToken } = body;

    if (!idToken) {
      return NextResponse.json(
        { error: 'ID token is required' },
        { status: 400 }
      );
    }

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

    const { uid, email, name, picture } = decodedToken;

    // Check if user exists in database
    let user = await queryOne(
      'SELECT * FROM users WHERE firebase_uid = $1',
      [uid]
    );

    if (!user) {
      // Create new user
      const result = await query(
        `INSERT INTO users (
          firebase_uid, email, username, display_name, photo_url, is_guest
        ) VALUES ($1, $2, $3, $4, $5, $6)
        RETURNING *`,
        [
          uid,
          email || `guest_${uid.substring(0, 8)}@sovereignrise.com`,
          email ? email.split('@')[0] : `guest_${uid.substring(0, 8)}`,
          name || 'Guest User',
          picture || null,
          !email // is_guest if no email
        ]
      );
      user = result.rows[0];
      console.log('Created new user:', user.id);
    } else {
      // Update last login
      const result = await query(
        `UPDATE users 
         SET last_login = NOW(), 
             updated_at = NOW(),
             display_name = COALESCE($2, display_name),
             photo_url = COALESCE($3, photo_url)
         WHERE firebase_uid = $1
         RETURNING *`,
        [uid, name, picture]
      );
      user = result.rows[0];
      console.log('Updated existing user:', user.id);
    }

    // Return user data
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
    console.error('Auth verify error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

