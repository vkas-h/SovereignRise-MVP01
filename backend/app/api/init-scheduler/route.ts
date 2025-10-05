import { NextRequest, NextResponse } from 'next/server';
import { initializeScheduler, triggerManualCleanup } from '@/lib/scheduler';

/**
 * POST /api/init-scheduler - Initialize the cleanup scheduler or trigger manual cleanup
 * Query param: ?manual=true to trigger cleanup immediately
 */
export async function POST(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const isManual = searchParams.get('manual') === 'true';
    
    if (isManual) {
      console.log('🧪 Manual cleanup triggered via API');
      await triggerManualCleanup();
      
      return NextResponse.json({
        success: true,
        message: 'Manual cleanup triggered successfully',
        timestamp: new Date().toISOString()
      }, { status: 200 });
    }
    
    // Initialize scheduler
    initializeScheduler();
    
    return NextResponse.json({
      success: true,
      message: 'Scheduler initialized successfully',
      timestamp: new Date().toISOString()
    }, { status: 200 });
    
  } catch (error) {
    console.error('Scheduler initialization error:', error);
    return NextResponse.json(
      { 
        success: false,
        error: 'Failed to initialize scheduler',
        message: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    );
  }
}

/**
 * GET /api/init-scheduler - Get information about the scheduler
 */
export async function GET(request: NextRequest) {
  return NextResponse.json({
    message: 'Cleanup scheduler endpoint',
    usage: {
      init: 'POST /api/init-scheduler - Initialize scheduler',
      manual: 'POST /api/init-scheduler?manual=true - Trigger cleanup immediately'
    },
    timestamp: new Date().toISOString()
  }, { status: 200 });
}

