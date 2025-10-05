/**
 * Scheduler for background tasks
 * Runs cleanup jobs at midnight to remove expired tasks and habits
 */

let isSchedulerInitialized = false;

/**
 * Initialize the cleanup scheduler
 * This runs daily at 00:00 UTC to clean up expired tasks and habits
 */
export function initializeScheduler() {
  if (isSchedulerInitialized) {
    console.log('⏰ Scheduler already initialized');
    return;
  }

  console.log('⏰ Initializing cleanup scheduler...');

  // For production deployment, you would use a proper cron service
  // For now, we'll use a simple interval-based approach
  
  // Calculate milliseconds until next midnight (00:00 UTC)
  function getMillisecondsUntilMidnight() {
    const now = new Date();
    const midnight = new Date(Date.UTC(
      now.getUTCFullYear(),
      now.getUTCMonth(),
      now.getUTCDate() + 1,
      0, 0, 0, 0
    ));
    
    // If we've passed 00:00 UTC today, set to tomorrow
    if (now.getTime() > midnight.getTime()) {
      midnight.setUTCDate(midnight.getUTCDate() + 1);
    }
    
    const msUntilMidnight = midnight.getTime() - now.getTime();
    
    console.log('⏰ Scheduler timing (UTC):');
    console.log('  Current time:', now.toISOString());
    console.log('  Next run at:', midnight.toISOString());
    console.log('  Time until run:', Math.round(msUntilMidnight / 1000 / 60), 'minutes');
    
    return msUntilMidnight;
  }

  // Schedule the first cleanup
  function scheduleNextCleanup() {
    const msUntilMidnight = getMillisecondsUntilMidnight();
    const nextRun = new Date(Date.now() + msUntilMidnight);
    const localTime = nextRun.toLocaleString('en-US', { timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone });
    
    console.log(`⏰ Next cleanup scheduled for:`);
    console.log(`  UTC: ${nextRun.toISOString()}`);
    console.log(`  Local: ${localTime}`);
    console.log(`  In ${Math.round(msUntilMidnight / 1000 / 60)} minutes`);
    
    setTimeout(async () => {
      console.log('⏰ Cleanup timer triggered at:', new Date().toISOString());
      await runCleanup();
      // Schedule the next cleanup (runs every 24 hours)
      console.log('⏰ Rescheduling next cleanup...');
      scheduleNextCleanup();
    }, msUntilMidnight);
  }

  // Start the scheduler
  scheduleNextCleanup();
  
  isSchedulerInitialized = true;
  console.log('✅ Scheduler initialized successfully');
}

/**
 * Run the cleanup job
 */
async function runCleanup() {
  try {
    console.log('🧹 Running scheduled cleanup at:', new Date().toISOString());
    
    // Call the cleanup endpoint internally
    const baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3000';
    const response = await fetch(`${baseUrl}/api/cleanup`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    if (response.ok) {
      const result = await response.json();
      console.log('✅ Cleanup completed:', result);
    } else {
      console.error('❌ Cleanup failed with status:', response.status);
    }
  } catch (error) {
    console.error('❌ Cleanup error:', error);
  }
}

/**
 * Manually trigger cleanup (for testing)
 */
export async function triggerManualCleanup() {
  console.log('🧹 Manual cleanup triggered');
  await runCleanup();
}

