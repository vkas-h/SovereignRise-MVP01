import pool from '../lib/db';

/**
 * Script to check and optionally reset a user's streak
 * Usage: npx tsx scripts/check-streak.ts <email> [--reset]
 */
async function checkStreak() {
  const args = process.argv.slice(2);
  const email = args[0];
  const shouldReset = args.includes('--reset');

  if (!email) {
    console.error('Usage: npx tsx scripts/check-streak.ts <email> [--reset]');
    process.exit(1);
  }

  const client = await pool.connect();

  try {
    // Get user
    const userResult = await client.query(
      'SELECT * FROM users WHERE email = $1',
      [email]
    );

    if (userResult.rows.length === 0) {
      console.error(`User not found: ${email}`);
      process.exit(1);
    }

    const user = userResult.rows[0];

    console.log('\n=== User Info ===');
    console.log(`Email: ${user.email}`);
    console.log(`Username: ${user.username}`);
    console.log(`ID: ${user.id}`);
    console.log(`Firebase UID: ${user.firebase_uid}`);
    console.log('\n=== Streak Info ===');
    console.log(`Current Streak: ${user.current_streak}`);
    console.log(`Longest Streak: ${user.longest_streak}`);
    console.log(`Total Tasks Completed: ${user.total_tasks_completed}`);
    console.log(`Total Habits Completed: ${user.total_habits_completed}`);

    // Check recent completions
    const now = Date.now();
    const todayStart = now - (now % 86400000);
    const yesterdayStart = todayStart - 86400000;

    const todayTasks = await client.query(
      `SELECT COUNT(*) as count FROM tasks 
       WHERE user_id = $1 AND status = 'COMPLETED' 
       AND completed_at >= $2`,
      [user.id, todayStart]
    );

    const yesterdayTasks = await client.query(
      `SELECT COUNT(*) as count FROM tasks 
       WHERE user_id = $1 AND status = 'COMPLETED' 
       AND completed_at >= $2 AND completed_at < $3`,
      [user.id, yesterdayStart, todayStart]
    );

    const todayHabits = await client.query(
      `SELECT COUNT(*) as count FROM habits 
       WHERE user_id = $1 AND last_checked_at >= $2`,
      [user.id, todayStart]
    );

    const yesterdayHabits = await client.query(
      `SELECT COUNT(*) as count FROM habits 
       WHERE user_id = $1 AND last_checked_at >= $2 AND last_checked_at < $3`,
      [user.id, yesterdayStart, todayStart]
    );

    console.log('\n=== Recent Activity ===');
    console.log(`Tasks completed today: ${todayTasks.rows[0].count}`);
    console.log(`Tasks completed yesterday: ${yesterdayTasks.rows[0].count}`);
    console.log(`Habits checked today: ${todayHabits.rows[0].count}`);
    console.log(`Habits checked yesterday: ${yesterdayHabits.rows[0].count}`);

    if (shouldReset) {
      console.log('\n=== Resetting Streak ===');
      await client.query(
        'UPDATE users SET current_streak = 0, longest_streak = 0 WHERE id = $1',
        [user.id]
      );
      console.log('✓ Streak reset to 0');
    } else {
      console.log('\n💡 Tip: Add --reset flag to reset the streak to 0');
    }

  } catch (error) {
    console.error('Error:', error);
  } finally {
    client.release();
    await pool.end();
  }
}

checkStreak();

