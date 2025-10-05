import { NextRequest, NextResponse } from 'next/server';
import { auth } from '@/lib/firebase-admin';
import { query, queryOne } from '@/lib/db';
import { generateAIAffirmation } from '@/lib/gemini';

/**
 * POST /api/ai/affirmations - Generate AI-powered affirmation
 */
export async function POST(request: NextRequest) {
  try {
    // Verify Firebase token
    const authHeader = request.headers.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const idToken = authHeader.substring(7);
    let decodedToken;
    try {
      decodedToken = await auth.verifyIdToken(idToken);
    } catch (error) {
      console.error('Token verification failed:', error);
      return NextResponse.json({ error: 'Invalid or expired token' }, { status: 401 });
    }

    const { uid } = decodedToken;

    // Get user from database with profile data
    const user = await queryOne(
      'SELECT id, username, display_name, current_streak FROM users WHERE firebase_uid = $1',
      [uid]
    );

    if (!user) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    // Parse request body
    const { context, tone, variables } = await request.json();

    if (!context || !tone) {
      return NextResponse.json({ error: 'Missing context or tone' }, { status: 400 });
    }

    // Check daily limit (100 per day for testing, will reduce to 3 for production)
    const todayStart = new Date().setHours(0, 0, 0, 0);
    const countResult = await queryOne(`
      SELECT COUNT(*) as count FROM affirmation_deliveries
      WHERE user_id = $1 AND delivered_at >= $2
    `, [user.id, todayStart]);

    const count = parseInt(countResult?.count || '0');
    if (count >= 100) {
      return NextResponse.json({ error: 'Daily affirmation limit reached' }, { status: 429 });
    }

    // Generate AI-powered affirmation using Gemini
    let affirmationMessage;
    try {
      affirmationMessage = await generateAIAffirmation(
        context,
        tone,
        {
          username: user.username || user.display_name || 'Champion',
          level: user.level || 1,
          xp: user.xp || 0,
          streak: user.current_streak || 0
        }
      );
    } catch (error) {
      console.error('AI generation failed, using fallback:', error);
      affirmationMessage = "You're crushing it! Keep that momentum going! 💪";
    }

    const affirmation = {
      id: Math.random().toString(36).substring(7),
      message: affirmationMessage,
      tone,
      context
    };

    // Record delivery
    await query(`
      INSERT INTO affirmation_deliveries (user_id, affirmation_id, context, delivered_at)
      VALUES ($1, $2, $3, $4)
    `, [user.id, affirmation.id, context, Date.now()]);

    return NextResponse.json(affirmation);
  } catch (error) {
    console.error('Error generating affirmation:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

