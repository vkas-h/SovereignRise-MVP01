import { auth } from './firebase-admin';

/**
 * Verify Firebase ID token and return decoded token
 * @param idToken - Firebase ID token from client
 * @returns Decoded token or null if verification fails
 */
export async function verifyFirebaseToken(idToken: string) {
  try {
    const decodedToken = await auth.verifyIdToken(idToken);
    return decodedToken;
  } catch (error) {
    console.error('Token verification error:', error);
    return null;
  }
}

