/**
 * Next.js Instrumentation
 * This file runs when the server starts (both dev and production)
 * Used to initialize background services like the cleanup scheduler
 */

export async function register() {
  // Only run on the server (not during build)
  if (process.env.NEXT_RUNTIME === 'nodejs') {
    console.log('🚀 Server starting - initializing services...');
    
    // Dynamically import to avoid running during build
    const { initializeScheduler } = await import('./lib/scheduler');
    
    // Initialize the cleanup scheduler
    initializeScheduler();
    
    console.log('✅ Services initialized successfully');
  }
}

