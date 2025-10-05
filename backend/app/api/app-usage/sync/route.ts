import { NextRequest, NextResponse } from 'next/server';
import { verifyFirebaseToken } from '@/lib/auth';
import pool from '@/lib/db';

/**
 * POST /api/app-usage/sync
 * Sync app usage data from Android device
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
    const { appUsageData, dailySummary } = await request.json();

    console.log(`📊 Usage sync request from user ${userId}:`, {
      appCount: appUsageData?.length || 0,
      hasSummary: !!dailySummary,
      summary: dailySummary ? {
        timestamp: new Date(dailySummary.timestamp).toISOString(),
        screenTime: dailySummary.totalScreenTimeMinutes,
        unlocks: dailySummary.unlockCount,
        distractingTime: dailySummary.distractingAppTimeMinutes,
        productiveTime: dailySummary.productiveAppTimeMinutes
      } : null,
      sampleApp: appUsageData?.[0] ? {
        name: appUsageData[0].appName,
        timestamp: new Date(appUsageData[0].timestamp).toISOString(),
        minutes: appUsageData[0].usageTimeMinutes
      } : null
    });

    if (!appUsageData || !Array.isArray(appUsageData)) {
      return NextResponse.json({ error: 'Invalid app usage data' }, { status: 400 });
    }

    const client = await pool.connect();
    try {
      // Validate that all timestamps are normalized to midnight
      for (const app of appUsageData) {
        const date = new Date(app.timestamp);
        if (date.getHours() !== 0 || date.getMinutes() !== 0 || date.getSeconds() !== 0) {
          console.warn(`⚠️ Non-midnight timestamp detected for ${app.packageName}: ${date.toISOString()}`);
          // Normalize it server-side as a safety measure
          const midnight = new Date(date);
          midnight.setHours(0, 0, 0, 0);
          app.timestamp = midnight.getTime();
        }
      }

      // Delete existing data for today, then insert fresh data
      // This ensures we always have the latest cumulative stats for the day
      let syncedApps = 0;
      for (const app of appUsageData) {
        // Delete any existing records for this user/app/day using DATE matching
        // This handles cases where timestamp might be slightly different
        await client.query(
          `DELETE FROM app_usage 
           WHERE user_id = $1 
             AND package_name = $2 
             AND DATE(TO_TIMESTAMP(timestamp::float / 1000)) = DATE(TO_TIMESTAMP($3::float / 1000))`,
          [userId, app.packageName, app.timestamp]
        );
        
        // Insert fresh record with normalized midnight timestamp
        await client.query(
          `INSERT INTO app_usage 
          (user_id, timestamp, package_name, app_name, usage_time_minutes, category, is_productive)
          VALUES ($1, $2, $3, $4, $5, $6, $7)`,
          [
            userId,
            app.timestamp, // Already normalized to midnight by Android code
            app.packageName,
            app.appName,
            app.usageTimeMinutes,
            app.category || 'Other',
            app.isProductive || false
          ]
        );
        syncedApps++;
      }

      // Insert or update daily summary in usage_stats
      if (dailySummary) {
        console.log(`💾 Saving usage_stats: ${dailySummary.totalScreenTimeMinutes} min, ${dailySummary.unlockCount} unlocks`);
        await client.query(
          `INSERT INTO usage_stats 
          (user_id, timestamp, total_screen_time_minutes, unlock_count, 
           distracting_app_time_minutes, productive_app_time_minutes, peak_usage_hour)
          VALUES ($1, $2, $3, $4, $5, $6, $7)
          ON CONFLICT (user_id, timestamp)
          DO UPDATE SET
            total_screen_time_minutes = EXCLUDED.total_screen_time_minutes,
            unlock_count = EXCLUDED.unlock_count,
            distracting_app_time_minutes = EXCLUDED.distracting_app_time_minutes,
            productive_app_time_minutes = EXCLUDED.productive_app_time_minutes,
            peak_usage_hour = EXCLUDED.peak_usage_hour`,
          [
            userId,
            dailySummary.timestamp,
            dailySummary.totalScreenTimeMinutes,
            dailySummary.unlockCount,
            dailySummary.distractingAppTimeMinutes || 0,
            dailySummary.productiveAppTimeMinutes || 0,
            dailySummary.peakUsageHour || null
          ]
        );
      }

      return NextResponse.json({
        success: true,
        syncedApps,
        message: `Successfully synced ${syncedApps} app usage entries`
      });
    } finally {
      client.release();
    }
  } catch (error) {
    console.error('Error syncing app usage:', error);
    return NextResponse.json(
      { error: 'Failed to sync app usage', message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

