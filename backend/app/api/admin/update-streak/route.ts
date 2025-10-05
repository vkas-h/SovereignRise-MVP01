import { NextRequest, NextResponse } from 'next/server';
import { auth } from '@/lib/firebase-admin';
import pool from '@/lib/db';

/**
 * POST /api/admin/update-streak - Manually update a user's streak (for testing)
 * Body: { email: string, streak: number }
 */
export async function POST(request: NextRequest) {
  try {
    // Verify authorization
    const authHeader = request.headers.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json(
        { error: 'Missing authorization header' },
        { status: 401 }
      );
    }

    const idToken = authHeader.substring(7);
    let decodedToken;
    try {
      decodedToken = await auth.verifyIdToken(idToken);
    } catch (error) {
      return NextResponse.json(
        { error: 'Invalid token' },
        { status: 401 }
      );
    }

    const body = await request.json();
    const { email, streak } = body;

    if (!email || streak === undefined) {
      return NextResponse.json(
        { error: 'Email and streak are required' },
        { status: 400 }
      );
    }

    const client = await pool.connect();

    try {
      // Find user by email
      const userResult = await client.query(
        'SELECT * FROM users WHERE email = $1',
        [email]
      );

      if (userResult.rows.length === 0) {
        return NextResponse.json(
          { error: 'User not found' },
          { status: 404 }
        );
      }

      const user = userResult.rows[0];

      // Update streak
      await client.query(
        `UPDATE users 
         SET current_streak = $1,
             longest_streak = GREATEST(longest_streak, $1)
         WHERE id = $2`,
        [streak, user.id]
      );

      console.log(`✅ Updated streak for ${email}: ${user.current_streak} → ${streak}`);

      return NextResponse.json({
        success: true,
        message: `Streak updated to ${streak}`,
        user: {
          email: user.email,
          username: user.username,
          previousStreak: user.current_streak,
          newStreak: streak
        }
      });
    } finally {
      client.release();
    }
  } catch (error) {
    console.error('Update streak error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}

