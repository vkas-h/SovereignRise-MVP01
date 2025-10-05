/**
 * Unit tests for habit tick API route
 * Tests rapid retick prevention and cadence guard behavior
 */

import { POST } from '../../../app/api/habits/[habitId]/tick/route';
import { NextRequest } from 'next/server';
import { GRACE_PERIOD_MS } from '../../../lib/constants';

// Mock dependencies
jest.mock('../../../lib/firebase-admin', () => ({
  verifyToken: jest.fn(),
}));

jest.mock('../../../lib/db', () => ({
  default: {
    connect: jest.fn(),
  },
  query: jest.fn(),
}));

const { verifyToken } = require('../../../lib/firebase-admin');
const dbModule = require('../../../lib/db');

describe('POST /api/habits/[habitId]/tick', () => {
  let mockClient: any;
  const mockUserId = 'test-user-123';
  const mockHabitId = 'habit-456';
  const oneDayMs = 24 * 60 * 60 * 1000;
  const oneHourMs = 60 * 60 * 1000;

  beforeEach(() => {
    jest.clearAllMocks();

    // Setup mock client
    mockClient = {
      query: jest.fn(),
      release: jest.fn(),
    };

    dbModule.default.connect.mockResolvedValue(mockClient);

    // Setup mock token verification
    verifyToken.mockResolvedValue({ uid: mockUserId });
  });

  const createRequest = (habitId: string, token: string = 'valid-token') => {
    return new NextRequest(`http://localhost/api/habits/${habitId}/tick`, {
      method: 'POST',
      headers: {
        authorization: `Bearer ${token}`,
      },
    });
  };

  describe('Cadence Guard - Rapid Retick Prevention', () => {
    it('should reject instant retick attempt (0ms elapsed)', async () => {
      const now = Date.now();
      const lastCheckedAt = now; // Same timestamp

      // Mock user query
      mockClient.query
        .mockResolvedValueOnce({ rows: [{ id: 1, firebase_uid: mockUserId }] }) // BEGIN
        .mockResolvedValueOnce({ rows: [] }) // user query - need to add this
        .mockResolvedValueOnce({ rows: [{ id: 1, firebase_uid: mockUserId }] }) // user query (actual)
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              type: 'DAILY',
              streak_days: 5,
              longest_streak: 10,
              last_checked_at: lastCheckedAt,
              total_completions: 5,
              milestones_achieved: [],
            },
          ],
        }); // habit query

      const request = createRequest(mockHabitId);
      const response = await POST(request, { params: { habitId: mockHabitId } });
      const data = await response.json();

      expect(response.status).toBe(400);
      expect(data.error).toBe('Habit already checked within its cadence period');
      expect(mockClient.query).toHaveBeenCalledWith('ROLLBACK');
    });

    it('should reject retick attempt after 30 minutes (below 1 hour minimum)', async () => {
      const now = Date.now();
      const thirtyMinutesMs = 30 * 60 * 1000;
      const lastCheckedAt = now - thirtyMinutesMs;

      mockClient.query
        .mockResolvedValueOnce({ rows: [] }) // BEGIN
        .mockResolvedValueOnce({ rows: [{ id: 1, firebase_uid: mockUserId }] }) // user query
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              type: 'DAILY',
              streak_days: 5,
              longest_streak: 10,
              last_checked_at: lastCheckedAt,
              total_completions: 5,
              milestones_achieved: [],
            },
          ],
        }); // habit query

      const request = createRequest(mockHabitId);
      const response = await POST(request, { params: { habitId: mockHabitId } });
      const data = await response.json();

      expect(response.status).toBe(400);
      expect(data.error).toBe('Habit already checked within its cadence period');
      expect(mockClient.query).toHaveBeenCalledWith('ROLLBACK');
    });

    it('should reject retick attempt after 59 minutes (just below 1 hour minimum)', async () => {
      const now = Date.now();
      const fiftyNineMinutesMs = 59 * 60 * 1000;
      const lastCheckedAt = now - fiftyNineMinutesMs;

      mockClient.query
        .mockResolvedValueOnce({ rows: [] }) // BEGIN
        .mockResolvedValueOnce({ rows: [{ id: 1, firebase_uid: mockUserId }] }) // user query
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              type: 'DAILY',
              streak_days: 5,
              longest_streak: 10,
              last_checked_at: lastCheckedAt,
              total_completions: 5,
              milestones_achieved: [],
            },
          ],
        }); // habit query

      const request = createRequest(mockHabitId);
      const response = await POST(request, { params: { habitId: mockHabitId } });
      const data = await response.json();

      expect(response.status).toBe(400);
      expect(data.error).toBe('Habit already checked within its cadence period');
      expect(mockClient.query).toHaveBeenCalledWith('ROLLBACK');
    });

    it('should allow tick after 1 hour for daily habit (minimum gap enforced)', async () => {
      const now = Date.now();
      const lastCheckedAt = now - oneHourMs; // Exactly 1 hour ago

      mockClient.query
        .mockResolvedValueOnce({ rows: [] }) // BEGIN
        .mockResolvedValueOnce({ rows: [{ id: 1, firebase_uid: mockUserId, xp: 100, aether_balance: 50 }] }) // user query
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              type: 'DAILY',
              streak_days: 5,
              longest_streak: 10,
              last_checked_at: lastCheckedAt,
              total_completions: 5,
              milestones_achieved: [],
            },
          ],
        }) // habit query
        .mockResolvedValueOnce({ rows: [] }) // UPDATE habits
        .mockResolvedValueOnce({ rows: [] }) // UPDATE users
        .mockResolvedValueOnce({ rows: [] }) // COMMIT
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              name: 'Test Habit',
              description: 'Test',
              type: 'DAILY',
              interval_days: null,
              streak_days: 6,
              longest_streak: 10,
              last_checked_at: now,
              created_at: now - 10000000,
              is_active: true,
              total_completions: 6,
              milestones_achieved: [],
            },
          ],
        }); // SELECT updated habit

      const request = createRequest(mockHabitId);
      const response = await POST(request, { params: { habitId: mockHabitId } });
      const data = await response.json();

      expect(response.status).toBe(200);
      expect(data.habit.streakDays).toBe(6);
      expect(mockClient.query).not.toHaveBeenCalledWith('ROLLBACK');
    });
  });

  describe('Cadence Guard - Grace Period Behavior', () => {
    it('should allow daily habit tick within grace period (23 hours elapsed)', async () => {
      const now = Date.now();
      const twentyThreeHoursMs = 23 * 60 * 60 * 1000;
      const lastCheckedAt = now - twentyThreeHoursMs;

      mockClient.query
        .mockResolvedValueOnce({ rows: [] }) // BEGIN
        .mockResolvedValueOnce({ rows: [{ id: 1, firebase_uid: mockUserId, xp: 100, aether_balance: 50 }] }) // user query
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              type: 'DAILY',
              streak_days: 5,
              longest_streak: 10,
              last_checked_at: lastCheckedAt,
              total_completions: 5,
              milestones_achieved: [],
            },
          ],
        }) // habit query
        .mockResolvedValueOnce({ rows: [] }) // UPDATE habits
        .mockResolvedValueOnce({ rows: [] }) // UPDATE users
        .mockResolvedValueOnce({ rows: [] }) // COMMIT
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              name: 'Test Habit',
              description: 'Test',
              type: 'DAILY',
              interval_days: null,
              streak_days: 6,
              longest_streak: 10,
              last_checked_at: now,
              created_at: now - 10000000,
              is_active: true,
              total_completions: 6,
              milestones_achieved: [],
            },
          ],
        }); // SELECT updated habit

      const request = createRequest(mockHabitId);
      const response = await POST(request, { params: { habitId: mockHabitId } });
      const data = await response.json();

      expect(response.status).toBe(200);
      expect(data.habit.streakDays).toBe(6);
      expect(mockClient.query).not.toHaveBeenCalledWith('ROLLBACK');
    });

    it('should enforce minimum gap even for weekly habits', async () => {
      const now = Date.now();
      const thirtyMinutesMs = 30 * 60 * 1000;
      const lastCheckedAt = now - thirtyMinutesMs;

      mockClient.query
        .mockResolvedValueOnce({ rows: [] }) // BEGIN
        .mockResolvedValueOnce({ rows: [{ id: 1, firebase_uid: mockUserId }] }) // user query
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              type: 'WEEKLY',
              streak_days: 5,
              longest_streak: 10,
              last_checked_at: lastCheckedAt,
              total_completions: 5,
              milestones_achieved: [],
            },
          ],
        }); // habit query

      const request = createRequest(mockHabitId);
      const response = await POST(request, { params: { habitId: mockHabitId } });
      const data = await response.json();

      expect(response.status).toBe(400);
      expect(data.error).toBe('Habit already checked within its cadence period');
      expect(mockClient.query).toHaveBeenCalledWith('ROLLBACK');
    });

    it('should allow weekly habit tick within grace period (6 days + 1 hour elapsed)', async () => {
      const now = Date.now();
      const sixDaysOneHourMs = (6 * oneDayMs) + oneHourMs;
      const lastCheckedAt = now - sixDaysOneHourMs;

      mockClient.query
        .mockResolvedValueOnce({ rows: [] }) // BEGIN
        .mockResolvedValueOnce({ rows: [{ id: 1, firebase_uid: mockUserId, xp: 100, aether_balance: 50 }] }) // user query
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              type: 'WEEKLY',
              streak_days: 5,
              longest_streak: 10,
              last_checked_at: lastCheckedAt,
              total_completions: 5,
              milestones_achieved: [],
            },
          ],
        }) // habit query
        .mockResolvedValueOnce({ rows: [] }) // UPDATE habits
        .mockResolvedValueOnce({ rows: [] }) // UPDATE users
        .mockResolvedValueOnce({ rows: [] }) // COMMIT
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              name: 'Test Weekly Habit',
              description: 'Test',
              type: 'WEEKLY',
              interval_days: null,
              streak_days: 6,
              longest_streak: 10,
              last_checked_at: now,
              created_at: now - 10000000,
              is_active: true,
              total_completions: 6,
              milestones_achieved: [],
            },
          ],
        }); // SELECT updated habit

      const request = createRequest(mockHabitId);
      const response = await POST(request, { params: { habitId: mockHabitId } });
      const data = await response.json();

      expect(response.status).toBe(200);
      expect(data.habit.streakDays).toBe(6);
      expect(mockClient.query).not.toHaveBeenCalledWith('ROLLBACK');
    });
  });

  describe('Cadence Guard - Edge Cases', () => {
    it('should allow tick when last_checked_at is null (first tick)', async () => {
      const now = Date.now();

      mockClient.query
        .mockResolvedValueOnce({ rows: [] }) // BEGIN
        .mockResolvedValueOnce({ rows: [{ id: 1, firebase_uid: mockUserId, xp: 100, aether_balance: 50 }] }) // user query
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              type: 'DAILY',
              streak_days: 0,
              longest_streak: 0,
              last_checked_at: null, // First tick
              total_completions: 0,
              milestones_achieved: [],
            },
          ],
        }) // habit query
        .mockResolvedValueOnce({ rows: [] }) // UPDATE habits
        .mockResolvedValueOnce({ rows: [] }) // UPDATE users
        .mockResolvedValueOnce({ rows: [] }) // COMMIT
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              name: 'Test Habit',
              description: 'Test',
              type: 'DAILY',
              interval_days: null,
              streak_days: 1,
              longest_streak: 1,
              last_checked_at: now,
              created_at: now - 10000000,
              is_active: true,
              total_completions: 1,
              milestones_achieved: [],
            },
          ],
        }); // SELECT updated habit

      const request = createRequest(mockHabitId);
      const response = await POST(request, { params: { habitId: mockHabitId } });
      const data = await response.json();

      expect(response.status).toBe(200);
      expect(data.habit.streakDays).toBe(1);
    });

    it('should respect custom interval cadence with minimum gap', async () => {
      const now = Date.now();
      const intervalDays = 3;
      const threeHoursMs = 3 * 60 * 60 * 1000;
      const lastCheckedAt = now - threeHoursMs;

      mockClient.query
        .mockResolvedValueOnce({ rows: [] }) // BEGIN
        .mockResolvedValueOnce({ rows: [{ id: 1, firebase_uid: mockUserId }] }) // user query
        .mockResolvedValueOnce({
          rows: [
            {
              id: mockHabitId,
              user_id: 1,
              type: 'CUSTOM_INTERVAL',
              interval_days: intervalDays,
              streak_days: 5,
              longest_streak: 10,
              last_checked_at: lastCheckedAt,
              total_completions: 5,
              milestones_achieved: [],
            },
          ],
        }); // habit query

      const request = createRequest(mockHabitId);
      const response = await POST(request, { params: { habitId: mockHabitId } });
      const data = await response.json();

      // Should reject because 3 hours < (3 days - 24 hour grace) = 48 hours
      expect(response.status).toBe(400);
      expect(data.error).toBe('Habit already checked within its cadence period');
    });
  });
});

