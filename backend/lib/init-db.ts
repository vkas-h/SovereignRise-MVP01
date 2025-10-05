import pool from './db';

/**
 * Initialize database schema
 * Creates necessary tables if they don't exist
 */
export async function initDatabase() {
  const client = await pool.connect();
  
  try {
    console.log('Initializing database schema...');
    
    // Create users table
    await client.query(`
      CREATE TABLE IF NOT EXISTS users (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        firebase_uid VARCHAR(255) UNIQUE NOT NULL,
        email VARCHAR(255) UNIQUE NOT NULL,
        username VARCHAR(50) UNIQUE,
        display_name VARCHAR(100),
        photo_url TEXT,
        current_streak INTEGER DEFAULT 0,
        longest_streak INTEGER DEFAULT 0,
        total_tasks_completed INTEGER DEFAULT 0,
        total_habits_completed INTEGER DEFAULT 0,
        is_guest BOOLEAN DEFAULT false,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW(),
        last_login TIMESTAMP DEFAULT NOW()
      );
    `);
    
    // Create index on firebase_uid for faster lookups
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users(firebase_uid);
    `);
    
    // Create index on email
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
    `);
    
    // Add last_task_reset column to users table if it doesn't exist
    await client.query(`
      ALTER TABLE users 
      ADD COLUMN IF NOT EXISTS last_task_reset BIGINT DEFAULT 0
    `);
    
    // Create tasks table
    await client.query(`
      CREATE TABLE IF NOT EXISTS tasks (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        title VARCHAR(100) NOT NULL,
        description TEXT,
        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
        difficulty VARCHAR(20) NOT NULL,
        reminder_time BIGINT,
        created_at BIGINT NOT NULL,
        completed_at BIGINT,
        is_missed BOOLEAN NOT NULL DEFAULT false,
        CONSTRAINT valid_status CHECK (status IN ('PENDING', 'COMPLETED', 'PARTIAL', 'FAILED')),
        CONSTRAINT valid_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD', 'VERY_HARD'))
      )
    `);
    
    // Create index on user_id for faster queries
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks(user_id)
    `);
    
    // Create index on status for filtering
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status)
    `);
    
    // Create index on created_at for sorting and date-based queries
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_tasks_created_at ON tasks(created_at)
    `);
    
    // Add updated_at column to tasks table for sync support
    await client.query(`
      ALTER TABLE tasks
      ADD COLUMN IF NOT EXISTS updated_at BIGINT DEFAULT created_at
    `);
    
    // Create function to auto-update updated_at on tasks
    await client.query(`
      CREATE OR REPLACE FUNCTION update_tasks_updated_at()
      RETURNS TRIGGER AS $$
      BEGIN
        NEW.updated_at = EXTRACT(EPOCH FROM NOW()) * 1000;
        RETURN NEW;
      END;
      $$ LANGUAGE plpgsql;
    `);
    
    // Create trigger to auto-update updated_at on task modifications
    await client.query(`
      DROP TRIGGER IF EXISTS tasks_updated_at_trigger ON tasks;
      CREATE TRIGGER tasks_updated_at_trigger
      BEFORE UPDATE ON tasks
      FOR EACH ROW
      EXECUTE FUNCTION update_tasks_updated_at();
    `);
    
    // Create habits table
    await client.query(`
      CREATE TABLE IF NOT EXISTS habits (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        name VARCHAR(50) NOT NULL,
        description TEXT,
        type VARCHAR(20) NOT NULL,
        interval_days INT NOT NULL DEFAULT 1,
        streak_days INT NOT NULL DEFAULT 0,
        longest_streak INT NOT NULL DEFAULT 0,
        last_checked_at BIGINT,
        created_at BIGINT NOT NULL,
        is_active BOOLEAN NOT NULL DEFAULT true,
        total_completions INT NOT NULL DEFAULT 0,
        milestones_achieved INT[] NOT NULL DEFAULT ARRAY[]::INT[],
        CONSTRAINT valid_type CHECK (type IN ('DAILY', 'WEEKLY', 'CUSTOM_INTERVAL')),
        CONSTRAINT valid_interval CHECK (interval_days > 0)
      )
    `);
    
    // Create indexes on habits table
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_habits_user_id ON habits(user_id)
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_habits_is_active ON habits(is_active)
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_habits_streak_days ON habits(streak_days)
    `);
    
    // Add updated_at column to habits table for sync support
    await client.query(`
      ALTER TABLE habits
      ADD COLUMN IF NOT EXISTS updated_at BIGINT DEFAULT created_at
    `);
    
    // Create daily_task_summary table for storing yesterday's task stats
    await client.query(`
      CREATE TABLE IF NOT EXISTS daily_task_summary (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        date BIGINT NOT NULL,
        total_tasks INT NOT NULL DEFAULT 0,
        completed_tasks INT NOT NULL DEFAULT 0,
        failed_tasks INT NOT NULL DEFAULT 0,
        completion_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
        tasks_data JSONB NOT NULL DEFAULT '[]'::jsonb,
        created_at BIGINT NOT NULL,
        CONSTRAINT unique_user_date UNIQUE (user_id, date)
      )
    `);
    
    // Create indexes on daily_task_summary table
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_daily_summary_user_id ON daily_task_summary(user_id)
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_daily_summary_date ON daily_task_summary(date)
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_daily_summary_user_date ON daily_task_summary(user_id, date)
    `);
    
    // Create function to auto-update updated_at on habits
    await client.query(`
      CREATE OR REPLACE FUNCTION update_habits_updated_at()
      RETURNS TRIGGER AS $$
      BEGIN
        NEW.updated_at = EXTRACT(EPOCH FROM NOW()) * 1000;
        RETURN NEW;
      END;
      $$ LANGUAGE plpgsql;
    `);
    
    // Create trigger to auto-update updated_at on habit modifications
    await client.query(`
      DROP TRIGGER IF EXISTS habits_updated_at_trigger ON habits;
      CREATE TRIGGER habits_updated_at_trigger
      BEFORE UPDATE ON habits
      FOR EACH ROW
      EXECUTE FUNCTION update_habits_updated_at();
    `);
    
    
    // Create AI Features tables
    
    // Usage stats table
    await client.query(`
      CREATE TABLE IF NOT EXISTS usage_stats (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        timestamp BIGINT NOT NULL,
        total_screen_time_minutes INT NOT NULL,
        unlock_count INT NOT NULL,
        distracting_app_time_minutes INT NOT NULL DEFAULT 0,
        productive_app_time_minutes INT NOT NULL DEFAULT 0,
        peak_usage_hour INT,
        UNIQUE(user_id, timestamp)
      )
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_usage_stats_user_id ON usage_stats(user_id)
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_usage_stats_timestamp ON usage_stats(timestamp)
    `);
    
    // Burnout metrics table
    await client.query(`
      CREATE TABLE IF NOT EXISTS burnout_metrics (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        date BIGINT NOT NULL,
        completion_rate FLOAT NOT NULL,
        snooze_count INT NOT NULL DEFAULT 0,
        missed_task_count INT NOT NULL DEFAULT 0,
        late_night_activity_minutes INT NOT NULL DEFAULT 0,
        streak_breaks INT NOT NULL DEFAULT 0,
        burnout_score FLOAT NOT NULL,
        UNIQUE(user_id, date)
      )
    `);
    
    // Burnout alerts table
    await client.query(`
      CREATE TABLE IF NOT EXISTS burnout_alerts (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        level VARCHAR(20) NOT NULL,
        detected_at BIGINT NOT NULL,
        burnout_score FLOAT NOT NULL,
        recommendations TEXT[],
        CONSTRAINT valid_burnout_level CHECK (level IN ('HEALTHY', 'MILD', 'MODERATE', 'SEVERE'))
      )
    `);
    
    // Recovery modes table
    await client.query(`
      CREATE TABLE IF NOT EXISTS recovery_modes (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        started_at BIGINT NOT NULL,
        ends_at BIGINT NOT NULL,
        penalty_multiplier FLOAT NOT NULL DEFAULT 0.5,
        is_active BOOLEAN NOT NULL DEFAULT true
      )
    `);
    
    // Affirmation deliveries table
    await client.query(`
      CREATE TABLE IF NOT EXISTS affirmation_deliveries (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        affirmation_id VARCHAR(100),
        context VARCHAR(50) NOT NULL,
        delivered_at BIGINT NOT NULL
      )
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_affirmation_deliveries_user_id ON affirmation_deliveries(user_id)
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_affirmation_deliveries_delivered_at ON affirmation_deliveries(delivered_at)
    `);
    
    // Add AI features columns to users table
    await client.query(`
      ALTER TABLE users
      ADD COLUMN IF NOT EXISTS recovery_mode_active BOOLEAN DEFAULT false,
      ADD COLUMN IF NOT EXISTS recovery_mode_ends_at BIGINT
    `);
    
    // Add login tracking columns to users table
    await client.query(`
      ALTER TABLE users
      ADD COLUMN IF NOT EXISTS login_streak INT DEFAULT 0,
      ADD COLUMN IF NOT EXISTS last_login_date BIGINT,
      ADD COLUMN IF NOT EXISTS consecutive_login_days INT DEFAULT 0
    `);
    
    // Create habit_logs table for tracking habit completions
    await client.query(`
      CREATE TABLE IF NOT EXISTS habit_logs (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        habit_id UUID NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        completed_at BIGINT NOT NULL,
        streak_at_completion INT NOT NULL DEFAULT 0,
        notes TEXT
      )
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_habit_logs_habit_id ON habit_logs(habit_id)
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_habit_logs_user_id ON habit_logs(user_id)
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_habit_logs_completed_at ON habit_logs(completed_at)
    `);
    
    // Create app_usage table for tracking which apps are used
    await client.query(`
      CREATE TABLE IF NOT EXISTS app_usage (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        timestamp BIGINT NOT NULL,
        package_name VARCHAR(255) NOT NULL,
        app_name VARCHAR(255),
        usage_time_minutes INT NOT NULL,
        category VARCHAR(50),
        is_productive BOOLEAN DEFAULT false,
        UNIQUE(user_id, package_name, timestamp)
      )
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_app_usage_user_id ON app_usage(user_id)
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_app_usage_timestamp ON app_usage(timestamp)
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_app_usage_package_name ON app_usage(package_name)
    `);
    
    // Create analytics_insights table for storing AI-generated insights
    await client.query(`
      CREATE TABLE IF NOT EXISTS analytics_insights (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        generated_at BIGINT NOT NULL,
        period_days INT NOT NULL,
        insight_text TEXT NOT NULL,
        metrics JSONB,
        recommendations TEXT[]
      )
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_analytics_insights_user_id ON analytics_insights(user_id)
    `);
    
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_analytics_insights_generated_at ON analytics_insights(generated_at)
    `);
    
    
    console.log('Database schema initialized successfully');
  } catch (error) {
    console.error('Error initializing database:', error);
    throw error;
  } finally {
    client.release();
  }
}

// Run initialization if this file is executed directly
if (require.main === module) {
  initDatabase()
    .then(() => {
      console.log('Database initialization complete');
      process.exit(0);
    })
    .catch((error) => {
      console.error('Database initialization failed:', error);
      process.exit(1);
    });
}

