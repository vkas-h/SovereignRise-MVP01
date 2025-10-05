'use client';

import { useEffect, useState } from 'react';

export default function Home() {
  const [schedulerStatus, setSchedulerStatus] = useState('Initializing...');

  useEffect(() => {
    // Initialize the scheduler when the app loads
    fetch('/api/init-scheduler', { method: 'POST' })
      .then(res => res.json())
      .then(data => {
        setSchedulerStatus(data.message || 'Initialized');
      })
      .catch(err => {
        console.error('Failed to initialize scheduler:', err);
        setSchedulerStatus('Failed to initialize');
      });
  }, []);

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <div className="text-center">
        <h1 className="text-4xl font-bold mb-4">Sovereign Rise API</h1>
        <p className="text-gray-600 mb-4">Backend server is running</p>
        <p className="text-sm text-blue-600 mb-8">Scheduler: {schedulerStatus}</p>
        <div className="bg-gray-100 p-6 rounded-lg text-left">
          <h2 className="font-semibold mb-2">Available Endpoints:</h2>
          <ul className="space-y-1 text-sm">
            <li>• POST /api/auth/verify</li>
            <li>• GET /api/user/profile</li>
            <li>• POST /api/auth/logout</li>
            <li>• POST /api/cleanup - Run cleanup</li>
            <li>• GET /api/cleanup - Preview cleanup</li>
          </ul>
        </div>
      </div>
    </main>
  );
}

