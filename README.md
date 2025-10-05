# 🚀 Sovereign Rise

**Master yourself, rise above.**

Sovereign Rise is a comprehensive personal development and productivity application that combines task management, habit tracking, and AI-powered insights to help you achieve your goals and maintain a healthy work-life balance.

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white" alt="Next.js">
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase">
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL">
</p>

---

## ✨ Features

### 📋 Task Management
- Create, edit, and manage daily tasks with difficulty levels (Easy, Medium, Hard, Very Hard)
- Task reminders with exact alarm scheduling
- Daily task reset at midnight (configurable timezone)
- Task completion tracking with partial completion support
- Yesterday's task summary for daily reflection

### 🎯 Habit Tracking
- Build and maintain daily, weekly, or custom interval habits
- Streak tracking with milestone achievements (7, 30, 100+ days)
- Visual progress indicators and charts
- Habit completion history and analytics

### 🤖 AI-Powered Features
- **Burnout Detection**: AI monitors your completion rates, missed tasks, and usage patterns to detect burnout signs
- **Smart Affirmations**: Context-aware motivational messages powered by Google Gemini AI
- **Phone Usage Analytics**: Track screen time, app usage, and unlock counts
- **Personalized Insights**: AI-generated recommendations based on your behavior patterns
- **Recovery Mode**: Automatic difficulty adjustment when burnout is detected

### 📊 Analytics & Insights
- Comprehensive dashboard with visual charts (using Vico Charts)
- Task completion rates and trends
- Habit streak analytics
- Phone usage patterns and productivity metrics
- Weekly and monthly progress reports

### 🔐 Authentication & User Management
- Firebase Authentication integration
- Google Sign-In with One Tap
- Guest mode for trying out the app
- Secure profile management with photo upload support

### 🌐 Offline Support
- Full offline functionality with local Room database
- Automatic background sync when connection is restored
- Sync queue management for pending changes
- Conflict resolution strategies

### 💎 Additional Features
- Beautiful modern UI with Material Design 3
- Dark mode support (system-aware)
- Pull-to-refresh on all screens
- Haptic feedback for better UX
- Smooth animations with Lottie
- Real-time sync status indicators
- Push notifications for reminders
- Error tracking with Sentry and Firebase Crashlytics

---

## 🛠️ Tech Stack

### Android App
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: Clean Architecture + MVVM
- **Dependency Injection**: Manual DI (AppModule)
- **Local Database**: Room
- **Networking**: Retrofit + OkHttp
- **Authentication**: Firebase Auth + Credential Manager
- **Background Tasks**: WorkManager
- **Image Loading**: Coil
- **Charts**: Vico Compose
- **Animations**: Lottie
- **Error Tracking**: Sentry + Firebase Crashlytics

### Backend API
- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Database**: PostgreSQL
- **Authentication**: Firebase Admin SDK
- **AI Integration**: Google Gemini AI
- **Scheduling**: Node-cron for automated tasks
- **Testing**: Jest

### Infrastructure
- **Authentication**: Firebase Authentication
- **Database**: PostgreSQL (CockroachDB or similar)
- **Deployment**: Vercel/Railway (Backend), Google Play Store (Android)

---

## 📋 Prerequisites

### For Android Development
- Android Studio Ladybug or later
- JDK 11 or higher
- Android SDK (API 24-36)
- Gradle 8.x

### For Backend Development
- Node.js 20.x or higher
- npm or yarn
- PostgreSQL database
- Firebase project with Admin SDK

---

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/vkas-h/SovereignRise.git
cd SovereignRise
```

### 2. Backend Setup

#### Install Dependencies
```bash
cd backend
npm install
```

#### Configure Environment Variables
Create a `.env` file in the `backend` directory:

```env
# Database
DATABASE_URL=postgresql://username:password@host:port/database

# Firebase Admin SDK
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY="your-private-key"
FIREBASE_CLIENT_EMAIL=your-client-email@project.iam.gserviceaccount.com

# Google Gemini AI
GEMINI_API_KEY=your-gemini-api-key

# Server Configuration
PORT=3000
NODE_ENV=development
```

#### Firebase Admin Configuration
Add your Firebase Admin SDK key to `backend/config/firebase-admin-key.json`:
```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "your-private-key-id",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "your-service-account@project.iam.gserviceaccount.com",
  "client_id": "your-client-id",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "your-cert-url"
}
```

#### Initialize Database
The database schema is automatically initialized on first API call, or you can manually run:
```bash
npm run dev
# Visit http://localhost:3000 to trigger initialization
```

#### Run Backend Server
```bash
npm run dev        # Development mode
npm run build      # Build for production
npm start          # Production mode
```

The backend will be available at `http://localhost:3000`

### 3. Android App Setup

#### Configure Firebase
1. Download `google-services.json` from your Firebase project
2. Place it in `app/google-services.json` and `app/src/google-services.json`

#### Update Backend URL
Edit `app/src/main/java/com/sovereign_rise/app/data/remote/NetworkModule.kt`:
```kotlin
private const val BASE_URL = "http://your-backend-url:3000/" // Update this
```

For local development, use your computer's local IP:
```kotlin
private const val BASE_URL = "http://192.168.x.x:3000/"
```

#### Build and Run
1. Open the project in Android Studio
2. Sync Gradle files
3. Connect an Android device or start an emulator (API 24+)
4. Run the app (Shift + F10)

---

## 📁 Project Structure

```
SovereignRise/
├── app/                          # Android application
│   ├── src/main/
│   │   ├── java/com/sovereign_rise/app/
│   │   │   ├── data/            # Data layer
│   │   │   │   ├── local/       # Room database, DAOs, entities
│   │   │   │   ├── remote/      # API services, DTOs
│   │   │   │   ├── repository/  # Repository implementations
│   │   │   │   └── sync/        # Sync manager
│   │   │   ├── di/              # Dependency injection
│   │   │   ├── domain/          # Domain layer
│   │   │   │   ├── model/       # Domain models
│   │   │   │   ├── repository/  # Repository interfaces
│   │   │   │   └── usecase/     # Use cases (business logic)
│   │   │   ├── presentation/    # UI layer
│   │   │   │   ├── auth/        # Authentication screens
│   │   │   │   ├── home/        # Home screen
│   │   │   │   ├── task/        # Task management
│   │   │   │   ├── habit/       # Habit tracking
│   │   │   │   ├── analytics/   # Analytics dashboard
│   │   │   │   ├── profile/     # User profile
│   │   │   │   ├── components/  # Reusable UI components
│   │   │   │   └── viewmodel/   # ViewModels
│   │   │   ├── receiver/        # Broadcast receivers
│   │   │   ├── worker/          # Background workers
│   │   │   ├── ui/theme/        # App theme, colors, typography
│   │   │   └── util/            # Utility classes
│   │   └── res/                 # Resources (layouts, drawables, etc.)
│   └── build.gradle.kts         # App-level Gradle config
│
├── backend/                      # Next.js backend API
│   ├── app/
│   │   ├── api/                 # API routes
│   │   │   ├── auth/            # Authentication endpoints
│   │   │   ├── tasks/           # Task management
│   │   │   ├── habits/          # Habit tracking
│   │   │   ├── analytics/       # Analytics data
│   │   │   ├── ai/              # AI features
│   │   │   └── user/            # User management
│   │   ├── layout.tsx           # Root layout
│   │   └── page.tsx             # Home page
│   ├── lib/                     # Shared libraries
│   │   ├── db.ts                # Database connection
│   │   ├── auth.ts              # Auth helpers
│   │   ├── firebase-admin.ts   # Firebase Admin SDK
│   │   ├── gemini.ts            # Google Gemini AI
│   │   ├── scheduler.ts         # Cron jobs
│   │   └── init-db.ts           # Database initialization
│   ├── config/                  # Configuration files
│   ├── package.json             # Dependencies
│   └── tsconfig.json            # TypeScript config
│
├── gradle/                       # Gradle wrapper files
├── build.gradle.kts              # Root Gradle config
├── settings.gradle.kts           # Gradle settings
└── README.md                     # This file
```

---

## 🔑 Key Configuration

### Android App Constants
Edit `app/src/main/java/com/sovereign_rise/app/util/Constants.kt`:
- Backend URL
- Firebase configuration
- App-wide constants

### Backend Configuration
- Database connection: `backend/lib/db.ts`
- Firebase Admin: `backend/lib/firebase-admin.ts`
- AI configuration: `backend/lib/gemini.ts`
- Scheduler jobs: `backend/lib/scheduler.ts`

---

## 📡 API Documentation

### Base URL
```
http://your-backend-url:3000/api
```

### Endpoints

#### Authentication
- `POST /api/auth/verify` - Verify Firebase ID token and create/update user
- `POST /api/auth/logout` - Logout user

#### User Profile
- `GET /api/user/profile` - Get user profile
- `PUT /api/user/profile` - Update user profile
- `POST /api/user/sync` - Sync user data

#### Tasks
- `GET /api/tasks` - Get all tasks for user
- `POST /api/tasks` - Create new task
- `PUT /api/tasks/:id` - Update task
- `DELETE /api/tasks/:id` - Delete task
- `GET /api/tasks/yesterday-summary` - Get yesterday's task summary

#### Habits
- `GET /api/habits` - Get all habits for user
- `POST /api/habits` - Create new habit
- `PUT /api/habits/:id` - Update habit
- `DELETE /api/habits/:id` - Delete habit
- `POST /api/habits/:id/check` - Mark habit as complete

#### Analytics
- `POST /api/analytics/usage-stats` - Submit phone usage statistics
- `GET /api/analytics/usage-stats` - Get usage statistics
- `GET /api/analytics/burnout` - Check burnout metrics

#### AI Features
- `POST /api/ai/affirmation` - Get AI-generated affirmation
- `GET /api/ai/insights` - Get personalized insights
- `POST /api/ai/recovery-mode` - Activate recovery mode

### Authentication
All API endpoints (except `/api/auth/verify`) require Firebase ID token in the Authorization header:
```
Authorization: Bearer <firebase-id-token>
```

---

## 🧪 Testing

### Backend Tests
```bash
cd backend
npm test              # Run all tests
npm run test:watch    # Watch mode
```

### Android Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

---

## 🚢 Deployment

### Backend Deployment (Vercel)
1. Install Vercel CLI: `npm i -g vercel`
2. Configure environment variables in Vercel dashboard
3. Deploy:
```bash
cd backend
vercel --prod
```

### Android App Deployment
1. Generate signed APK/Bundle in Android Studio
2. Upload to Google Play Console
3. Follow Google Play's review process

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Code Style
- **Kotlin**: Follow official Kotlin coding conventions
- **TypeScript**: Use ESLint configuration provided
- Write meaningful commit messages
- Add comments for complex logic
- Update documentation as needed

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Authors

- **Your Name** - *Initial work* - [YourGitHub](https://github.com/vkas-h)

---

## 🙏 Acknowledgments

- Firebase for authentication and backend services
- Google Gemini AI for intelligent features
- Jetpack Compose team for the amazing UI toolkit
- Next.js team for the excellent backend framework
- All open-source contributors whose libraries made this possible

---

## 📧 Support

For support, email bikashp9861@gmail.com or open an issue on GitHub.

---

## 🗺️ Roadmap

- [ ] Social features (accountability partners, guilds)
- [ ] Marketplace for custom themes and features
- [ ] iOS version
- [ ] Web dashboard
- [ ] Advanced AI coaching
- [ ] Integration with fitness trackers
- [ ] Gamification elements (achievements, rewards)

---

<p align="center">Made with ❤️ by the Sovereign Rise Team</p>
<p align="center">⭐ Star us on GitHub if you find this project useful!</p>

