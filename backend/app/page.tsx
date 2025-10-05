export default function Home() {
  return (
    <main style={{ display: 'flex', minHeight: '100vh', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '24px' }}>
      <div style={{ textAlign: 'center' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: 'bold', marginBottom: '1rem' }}>Sovereign Rise API</h1>
        <p style={{ color: '#666', marginBottom: '2rem' }}>Backend server is running ✅</p>
        <div style={{ backgroundColor: '#f5f5f5', padding: '1.5rem', borderRadius: '8px', textAlign: 'left' }}>
          <h2 style={{ fontWeight: '600', marginBottom: '0.5rem' }}>Available Endpoints:</h2>
          <ul style={{ listStyle: 'none', padding: 0 }}>
            <li>• POST /api/auth/verify</li>
            <li>• GET /api/user/profile</li>
            <li>• POST /api/auth/logout</li>
            <li>• GET /api/tasks</li>
            <li>• POST /api/tasks</li>
            <li>• GET /api/habits</li>
            <li>• POST /api/ai/affirmations</li>
            <li>• POST /api/ai/burnout</li>
            <li>• GET /api/ai/insights</li>
          </ul>
        </div>
      </div>
    </main>
  );
}

