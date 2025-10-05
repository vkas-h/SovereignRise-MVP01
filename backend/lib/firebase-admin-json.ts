import * as admin from 'firebase-admin';
import * as path from 'path';
import * as fs from 'fs';

// Initialize Firebase Admin SDK using JSON file
if (!admin.apps.length) {
  try {
    // Path to your Firebase Admin JSON file
    const serviceAccountPath = path.join(process.cwd(), 'config', 'firebase-admin-key.json');
    
    // Check if file exists
    if (!fs.existsSync(serviceAccountPath)) {
      throw new Error(`Firebase Admin key file not found at: ${serviceAccountPath}`);
    }
    
    const serviceAccount = require(serviceAccountPath);
    
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
    });
    
    console.log('Firebase Admin initialized successfully from JSON file');
  } catch (error) {
    console.error('Firebase admin initialization error:', error);
    throw error;
  }
}

export const auth = admin.auth();
export default admin;

/**
 * Helper function to verify Firebase ID token
 */
export async function verifyToken(idToken: string) {
  try {
    const decodedToken = await auth.verifyIdToken(idToken);
    return decodedToken;
  } catch (error) {
    console.error('Token verification error:', error);
    return null;
  }
}

