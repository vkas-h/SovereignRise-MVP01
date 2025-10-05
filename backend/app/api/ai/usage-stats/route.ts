import { NextRequest, NextResponse } from 'next/server';
import { verifyFirebaseToken } from '@/lib/auth';
import pool from '@/lib/db';

/**
 * POST /api/ai/usage-stats - Sync usage stats from device
 */
export async function POST(request: NextRequest) {
  try {
    // Verify Firebase token
    const authHeader = request.headers.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const token = authHeader.split('Bearer ')[1];
    const decodedToken = await verifyFirebaseToken(token);
    
    if (!decodedToken) {
      return NextResponse.json({ error: 'Invalid token' }, { status: 401 });
    }

    // Get user from database
    const userResult = await pool.query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [decodedToken.uid]
    );

    if (userResult.rows.length === 0) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const userId = userResult.rows[0].id;

    // Parse request body
    const { stats } = await request.json();

    if (!stats || !Array.isArray(stats) || stats.length === 0) {
      return NextResponse.json({ error: 'Invalid stats data' }, { status: 400 });
    }

    // Insert or update usage stats
    let syncedCount = 0;
    for (const stat of stats) {
      await pool.query(`
        INSERT INTO usage_stats (
          user_id, timestamp, total_screen_time_minutes, unlock_count,
          distracting_app_time_minutes, productive_app_time_minutes, peak_usage_hour
        )
        VALUES ($1, $2, $3, $4, $5, $6, $7)
        ON CONFLICT (user_id, timestamp) DO UPDATE SET
          total_screen_time_minutes = EXCLUDED.total_screen_time_minutes,
          unlock_count = EXCLUDED.unlock_count,
          distracting_app_time_minutes = EXCLUDED.distracting_app_time_minutes,
          productive_app_time_minutes = EXCLUDED.productive_app_time_minutes,
          peak_usage_hour = EXCLUDED.peak_usage_hour
      `, [
        userId,
        stat.timestamp,
        stat.totalScreenTimeMinutes || 0,
        stat.unlockCount || 0,
        stat.distractingAppTimeMinutes || 0,
        stat.productiveAppTimeMinutes || 0,
        stat.peakUsageHour || 0
      ]);
      syncedCount++;
    }

    return NextResponse.json({ synced: syncedCount });
  } catch (error) {
    console.error('Error syncing usage stats:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

/**
 * GET /api/ai/usage-stats - Get usage stats for a time range
 */
export async function GET(request: NextRequest) {
  try {
    // Verify Firebase token
    const authHeader = request.headers.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const token = authHeader.split('Bearer ')[1];
    const decodedToken = await verifyFirebaseToken(token);
    
    if (!decodedToken) {
      return NextResponse.json({ error: 'Invalid token' }, { status: 401 });
    }

    // Get user from database
    const userResult = await pool.query(
      'SELECT id FROM users WHERE firebase_uid = $1',
      [decodedToken.uid]
    );

    if (userResult.rows.length === 0) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const userId = userResult.rows[0].id;

    // Get query parameters
    const { searchParams } = new URL(request.url);
    const startTime = searchParams.get('startTime');
    const endTime = searchParams.get('endTime');

    if (!startTime || !endTime) {
      return NextResponse.json({ error: 'Missing time range parameters' }, { status: 400 });
    }

    // Query usage stats
    const result = await pool.query(`
      SELECT * FROM usage_stats
      WHERE user_id = $1 AND timestamp >= $2 AND timestamp <= $3
      ORDER BY timestamp DESC
    `, [userId, startTime, endTime]);

    return NextResponse.json({ stats: result.rows });
  } catch (error) {
    console.error('Error fetching usage stats:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

