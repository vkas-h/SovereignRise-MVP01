# 🎯 Railway UI Navigation Guide - Where to Find Settings

## Understanding Railway's UI

Railway's interface has changed over time. Here's where to find everything:

---

## 📍 Step-by-Step: Finding Service Settings

### If You Haven't Created a Project Yet

**1. From Railway Dashboard (railway.app):**
```
Dashboard Home
    ↓
Click "New Project" button (top right)
    ↓
Select "Deploy from GitHub repo"
    ↓
Choose "SovereignRise" repository
    ↓
Railway creates a project with ONE service inside
```

### If You Already Created a Project

**2. After Creating Project:**

You'll see a screen that looks like this:

```
┌─────────────────────────────────────────┐
│  Project: SovereignRise                 │
│  ┌───────────────────────────────────┐  │
│  │  [Purple Box]                     │  │ ← This is your SERVICE
│  │  sovereign-rise                   │  │
│  │  Building...                      │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

**3. Click on that purple/blue box** (the service card)

This opens the service view with tabs at the top:

```
┌─────────────────────────────────────────┐
│  [<] sovereign-rise                     │
│  ───────────────────────────────────    │
│  Deployments  Metrics  Settings  Variables │ ← Click "Settings" tab
└─────────────────────────────────────────┘
```

**4. In the Settings tab, scroll down to find:**

- **Source** section (where your GitHub repo is)
- **Root Directory** field ← THIS IS WHAT YOU NEED!
- **Build settings**
- **Deploy settings**

---

## 🎯 Exact Location of "Root Directory"

### New Railway UI (2024-2025)

1. Click on your **project card** (the main dashboard)
2. Click on the **service** (the colored box with your repo name)
3. Click **"Settings"** tab at the top
4. Scroll down to **"Source"** or **"Build"** section
5. Look for field labeled **"Root Directory"** or **"Watch Paths"**
6. Enter: `backend`
7. Click **Save** or the changes won't apply!

### Alternative: Using the Three Dots Menu

```
Service Card
    ↓
Click the ⋮ (three dots) on the service
    ↓
Select "Settings"
    ↓
Find "Root Directory"
```

---

## 🔍 Can't Find Root Directory Field?

### Option 1: Use Railway CLI (Easier!)

This is actually easier than using the UI:

```bash
# Install Railway CLI
npm install -g @railway/cli

# Login to Railway
railway login

# Link to your project
cd backend
railway link

# This will ask you to select your project and service

# Now Railway knows to use the backend directory!
```

### Option 2: Use railway.toml in Your Repo

Create `railway.toml` in your **PROJECT ROOT** (not in backend):

```toml
[build]
builder = "NIXPACKS"
buildCommand = "cd backend && npm ci && npm run build"

[deploy]
startCommand = "cd backend && npm start"
restartPolicyType = "ON_FAILURE"
```

Then commit and push:
```bash
git add railway.toml
git commit -m "Add Railway build configuration"
git push origin main
```

---

## 🚨 If Railway Already Started Building (Wrong Build)

If Railway is currently building the Android app:

### Method 1: Stop and Reconfigure
1. Click on the service
2. Go to **Deployments** tab
3. Click on the current deployment
4. Click **"Cancel Deployment"** (if still building)
5. Go back to **Settings**
6. Set Root Directory
7. Click **"Redeploy"** button

### Method 2: Delete and Start Fresh (Recommended)
1. Click on the service
2. Click **Settings** tab
3. Scroll to bottom
4. Click **"Delete Service"** (red button)
5. Go back to project
6. Click **"New Service"**
7. Select **"GitHub Repo"**
8. Choose your repo
9. **IMMEDIATELY** set Root Directory to `backend`
10. Add environment variables
11. Deploy

---

## 📸 What You Should See

### Before Setting Root Directory:
```
Build Output:
↳ Detected Java
↳ Using Gradle
./gradlew clean build
❌ ERROR: SDK location not found
```

### After Setting Root Directory:
```
Build Output:
↳ Detected Node.js
↳ Using npm
npm ci
npm run build
✅ Build succeeded
```

---

## 🎯 Alternative: Use Monorepo Detection

Railway has automatic monorepo detection. Another way:

1. Keep the files I created (`backend/nixpacks.toml`)
2. In Railway settings, instead of Root Directory, set:
   - **Watch Paths**: `backend/**`
   - Railway will only rebuild when backend files change

---

## 🆘 Still Can't Find It?

### Check Railway Documentation
Railway's UI changes frequently. Check their latest docs:
- https://docs.railway.app/deploy/deployments
- https://docs.railway.app/develop/services

### Use Railway CLI Method (Recommended)

This is honestly easier:

```bash
# From your project root
npm install -g @railway/cli
railway login
cd backend
railway init
railway up
```

Railway CLI will:
- Create the project
- Automatically detect it's Next.js
- Use the backend directory
- Deploy correctly

### Contact Railway Support

If you're still stuck:
1. Railway Discord: https://discord.gg/railway
2. Railway Docs: https://docs.railway.app
3. Post a screenshot in Discord #help channel

---

## 📝 Current Railway UI Terms

Railway uses these terms:
- **Project** = Your entire application (SovereignRise)
- **Service** = One part of your project (your backend)
- **Deployment** = One build/deploy attempt
- **Environment** = Production/staging/development

You need to:
1. Find your **Project**
2. Click on the **Service** (the backend)
3. Go to **Settings**
4. Set **Root Directory**

---

## ✅ Simplest Solution: Use Railway CLI

Since the UI is confusing, here's the easiest way:

```bash
# Install
npm install -g @railway/cli

# Login (opens browser)
railway login

# Go to backend
cd backend

# Deploy (Railway auto-detects Next.js)
railway up

# Add environment variables
railway variables set DATABASE_URL="your_database_url"
railway variables set FIREBASE_PROJECT_ID="sovereign-rise-1e8dd"
# ... etc
```

The CLI is much simpler and doesn't require finding settings in the UI!

---

**Need more help?** Take a screenshot of what you see in Railway and I can guide you specifically!

