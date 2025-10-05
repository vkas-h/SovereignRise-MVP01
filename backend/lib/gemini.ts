import { GoogleGenAI } from '@google/genai';

// Initialize Gemini AI
const ai = new GoogleGenAI({
  apiKey: 'AIzaSyAc2WTPz3fVg7LS8CtL_WGs5jqYDK6v8Rc'
});

/**
 * Generate a personalized affirmation using Gemini AI
 */
export async function generateAIAffirmation(
  context: string,
  tone: string,
  userProfile: {
    username: string;
    streak: number;
  }
): Promise<string> {
  const toneGuides = {
    MOTIVATIONAL: 'energetic, powerful, and action-oriented',
    PHILOSOPHICAL: 'thoughtful, reflective, and wisdom-focused',
    PRACTICAL: 'straightforward, factual, results-oriented',
    HUMOROUS: 'light, playful, witty'
  };

  const contextDescriptions = {
    TASK_COMPLETED: `just completed a task`,
    HABIT_CHECKED: `just maintained their daily habit streak of ${userProfile.streak} days`,
    PERFECT_DAY: `completed ALL their tasks today`,
    STREAK_MILESTONE: `reached a ${userProfile.streak}-day streak milestone`,
    COMEBACK: `came back after a break and is getting back on track`
  };

  // Add randomness to ensure variety
  const randomSeed = Math.random();
  const styleVariations = [
    'Be bold and direct',
    'Use a metaphor or analogy',
    'Reference their specific stats',
    'Make it feel epic and legendary',
    'Keep it simple but powerful'
  ];
  const randomStyle = styleVariations[Math.floor(randomSeed * styleVariations.length)];

  const prompt = `You are a motivational coach in a productivity gamification app called Sovereign Rise.

User Profile:
- Username: ${userProfile.username}
- Streak: ${userProfile.streak} days

Context: The user ${contextDescriptions[context as keyof typeof contextDescriptions] || context}.

Generate a UNIQUE, SHORT (max 15 words), ${toneGuides[tone as keyof typeof toneGuides] || 'motivational'} affirmation message.

Rules:
1. Be personal - use "you" or reference their progress
2. Make it punchy and memorable
3. NO generic platitudes - be SPECIFIC and UNIQUE
4. Reference specific achievements when relevant ( ${userProfile.streak}-day streak)
5. Match the ${tone} tone exactly
6. Keep it under 15 words
7. Style: ${randomStyle}
8. NEVER repeat the same message - be creative and fresh EVERY TIME

Generate ONLY the affirmation message, nothing else:`;

  const response = await ai.models.generateContent({
    model: 'gemini-2.0-flash-lite',
    contents: prompt
  });
  const text = response.text?.trim() || '';

  // Remove quotes if present
  return text.replace(/^["']|["']$/g, '') || "You're doing great! Keep going!";
}

/**
 * Generate burnout insights and recommendations using Gemini AI
 */
export async function generateBurnoutInsights(
  metrics: {
    completionRate: number;
    missedTaskCount: number;
    lateNightActivityMinutes: number;
    streakBreaks: number;
    snoozeCount: number;
  },
  userProfile: {
    username: string;
  }
): Promise<{
  message: string;
  recommendations: string[];
}> {
  const prompt = `You are a wellness coach analyzing productivity patterns for ${userProfile.username}.

Current Metrics:
- Task completion rate: ${(metrics.completionRate * 100).toFixed(0)}%
- Missed tasks: ${metrics.missedTaskCount}
- Late-night activity: ${metrics.lateNightActivityMinutes} minutes (after 11 PM)
- Streak breaks: ${metrics.streakBreaks}
- Task snoozes: ${metrics.snoozeCount}

Generate:
1. A supportive, empathetic message (2-3 sentences) about their current state
2. Exactly 3 specific, actionable recommendations

Format your response as JSON:
{
  "message": "your supportive message here",
  "recommendations": ["rec 1", "rec 2", "rec 3"]
}

Be caring but honest. Focus on sustainable productivity, not guilt.`;

  const response = await ai.models.generateContent({
    model: 'gemini-2.0-flash-exp',
    contents: prompt
  });
  const text = response.text?.trim() || '';

  try {
    // Extract JSON from response (handle markdown code blocks)
    const jsonMatch = text.match(/\{[\s\S]*\}/);
    if (jsonMatch) {
      return JSON.parse(jsonMatch[0]);
    }
  } catch (e) {
    console.error('Failed to parse Gemini response:', e);
  }

  // Fallback
  return {
    message: "You're going through a challenging phase. Remember, productivity isn't about perfection—it's about progress.",
    recommendations: [
      "Reduce your daily task load by 30%",
      "Set a strict 10 PM cutoff for work",
      "Take a full rest day this weekend"
    ]
  };
}

/**
 * Generate smart reminder suggestions using AI
 */
export async function generateSmartReminderSuggestion(
  taskTitle: string,
  taskDescription: string,
  completionPatterns: {
    averageHour: number;
    averageMinute: number;
    sampleSize: number;
  }
): Promise<{
  suggestedTime: string;
  reasoning: string;
}> {
  const prompt = `You are an AI productivity assistant helping schedule a task reminder.

Task: "${taskTitle}"
Description: ${taskDescription || 'No description'}

User's historical pattern:
- Usually completes similar tasks around ${completionPatterns.averageHour}:${completionPatterns.averageMinute.toString().padStart(2, '0')}
- Based on ${completionPatterns.sampleSize} previous completions

Suggest the optimal reminder time (in 24-hour format HH:MM) and explain WHY in one short sentence.

Format as JSON:
{
  "suggestedTime": "HH:MM",
  "reasoning": "one sentence explanation"
}`;

  const response = await ai.models.generateContent({
    model: 'gemini-2.0-flash-exp',
    contents: prompt
  });
  const text = response.text?.trim() || '';

  try {
    const jsonMatch = text.match(/\{[\s\S]*\}/);
    if (jsonMatch) {
      return JSON.parse(jsonMatch[0]);
    }
  } catch (e) {
    console.error('Failed to parse Gemini response:', e);
  }

  // Fallback
  return {
    suggestedTime: `${completionPatterns.averageHour.toString().padStart(2, '0')}:${completionPatterns.averageMinute.toString().padStart(2, '0')}`,
    reasoning: "Based on your typical completion pattern"
  };
}

/**
 * Generate personalized productivity insights
 */
export async function generateProductivityInsights(
  weeklyStats: {
    tasksCompleted: number;
    habitsCompleted: number;
    perfectDays: number;
    currentStreak: number;
  },
  userProfile: {
    username: string;
  }
): Promise<string> {
  const prompt = `You are a productivity coach analyzing ${userProfile.username}'s weekly performance.

This Week:
- Tasks completed: ${weeklyStats.tasksCompleted}
- Habits maintained: ${weeklyStats.habitsCompleted}
- Perfect days: ${weeklyStats.perfectDays}
- Current streak: ${weeklyStats.currentStreak} days

Generate a SHORT (2-3 sentences) personalized insight that:
1. Celebrates specific wins
2. Identifies one pattern (good or concerning)
3. Gives one actionable tip for next week

Be specific, not generic. Use numbers. Make it feel personal.

Generate ONLY the insight message:`;

  const response = await ai.models.generateContent({
    model: 'gemini-2.0-flash-exp',
    contents: prompt
  });
  return response.text?.trim() || ''.replace(/^["']|["']$/g, '');
}

/**
 * Generate a motivational push notification message
 */
export async function generateNudgeMessage(
  distractingApp: string,
  minutesSpent: number,
  uncompletedTasks: number
): Promise<string> {
  const prompt = `You are a gentle productivity assistant sending a nudge notification.

Situation: User has spent ${minutesSpent} minutes on ${distractingApp} and has ${uncompletedTasks} tasks waiting.

Generate a SHORT (max 12 words), friendly nudge that:
1. Acknowledges the app without judgment
2. Redirects to their tasks
3. Feels motivating, not nagging

Examples of good tone:
- "Brain break over? Those tasks won't tackle themselves 💪"
- "${uncompletedTasks} tasks ready for you. Let's go!"
- "Recharged? Time to turn that energy into progress!"

Generate ONLY the nudge message:`;

  const response = await ai.models.generateContent({
    model: 'gemini-2.0-flash-exp',
    contents: prompt
  });
  return response.text?.trim() || ''.replace(/^["']|["']$/g, '');
}

/**
 * Generate a personalized task difficulty suggestion
 */
export async function suggestTaskDifficulty(
  taskTitle: string,
  taskDescription: string
): Promise<{
  difficulty: 'EASY' | 'MEDIUM' | 'HARD' | 'VERY_HARD';
  reasoning: string;
}> {
  const prompt = `Analyze this task and suggest an appropriate difficulty level.

Task: "${taskTitle}"
Description: ${taskDescription || 'No description'}

Consider:
- Time commitment implied
- Mental effort required
- Complexity and scope
- Dependencies and unknowns

Suggest difficulty (EASY/MEDIUM/HARD/VERY_HARD) and explain in one sentence.

Format as JSON:
{
  "difficulty": "MEDIUM",
  "reasoning": "one sentence explanation"
}`;

  const response = await ai.models.generateContent({
    model: 'gemini-2.0-flash-exp',
    contents: prompt
  });
  const text = response.text?.trim() || '';

  try {
    const jsonMatch = text.match(/\{[\s\S]*\}/);
    if (jsonMatch) {
      return JSON.parse(jsonMatch[0]);
    }
  } catch (e) {
    console.error('Failed to parse Gemini response:', e);
  }

  // Fallback
  return {
    difficulty: 'MEDIUM',
    reasoning: 'Based on the task description'
  };
}

/**
 * Generate text using Gemini AI
 * Generic function for any text generation task
 */
export async function generateText(prompt: string): Promise<string> {
  const response = await ai.models.generateContent({
    model: 'gemini-2.0-flash-exp',
    contents: prompt
  });
  return response.text?.trim() || '';
}