# 🔧 Quick Fix for Railway Deployment Issue

## ❌ The Problem

Railway tried to build your **Android app** instead of your **Next.js backend**, resulting in this error:

```
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable
```

## ✅ The Solution

### Step 1: Commit the Configuration Files

I've created two important files that tell Railway what to build:

```bash
# From your project root directory
git add backend/nixpacks.toml
git add .railwayignore
git commit -m "Add Railway configuration to build only backend"
git push origin main
```

**What these files do:**
- `backend/nixpacks.toml` - Tells Railway to use Node.js and build your Next.js app
- `.railwayignore` - Tells Railway to ignore your Android app files

### Step 2: Configure Railway Settings

1. Go to your **Railway dashboard**
2. Click on your service
3. Go to **Settings** tab
4. Find **Service Settings** section
5. Set **Root Directory** to: `backend`
6. Click **Save Changes**

### Step 3: Redeploy

**Option A: Delete and recreate the service (recommended)**
1. Delete the current service in Railway
2. Create a new service from your GitHub repo
3. **IMMEDIATELY set Root Directory to `backend`** before first deployment
4. Add all environment variables
5. Deploy!

**Option B: Trigger new deployment**
1. After saving the Root Directory setting
2. Go to **Deployments** tab
3. Click **Deploy** button
4. Watch the build logs

### Step 4: Verify Success

After redeployment, check the logs. You should see:

✅ **Good signs:**
```
↳ Detected Node.js
↳ Using npm
next build
npm start
```

❌ **Bad signs (means root directory not set):**
```
↳ Detected Java
↳ Using Gradle
./gradlew clean build
```

## 📝 Environment Variables to Set

Don't forget to add these in Railway's **Variables** tab:

```env
DATABASE_URL=postgresql://bikash:5kbg25VI4mLfLhvXnPjmNg@quiet-dragon-16573.j77.aws-ap-south-1.cockroachlabs.cloud:26257/defaultdb?sslmode=verify-full

FIREBASE_PROJECT_ID=sovereign-rise-1e8dd
FIREBASE_PRIVATE_KEY=<paste your actual private key with line breaks>
FIREBASE_CLIENT_EMAIL=<your firebase admin email>

GEMINI_API_KEY=<your gemini api key>

NODE_ENV=production
```

## 🎯 After Successful Deployment

Once Railway shows a successful build:

1. **Get your Railway URL** from the Settings → Domains section
2. **Update your Android app** to use this URL:

```kotlin:app/src/main/java/com/sovereign_rise/app/data/remote/NetworkModule.kt
private const val BASE_URL = "https://your-railway-url.up.railway.app/"
```

3. **Test the API**:
```bash
# Health check
curl https://your-railway-url.up.railway.app/api/user/profile

# Or open in browser
https://your-railway-url.up.railway.app/api/user/profile
```

## 🆘 Still Having Issues?

1. Check Railway build logs for specific errors
2. Verify Root Directory is set to `backend`
3. Ensure the configuration files are committed and pushed
4. Try deleting and recreating the service
5. Check the full guide: `RAILWAY_DEPLOYMENT_GUIDE.md`

---

## 📁 Files Created

These new files help Railway understand your project structure:

### `backend/nixpacks.toml`
```toml
[phases.setup]
nixPkgs = ['nodejs_20']

[phases.install]
cmds = ['npm ci']

[phases.build]
cmds = ['npm run build']

[start]
cmd = 'npm start'
```

### `.railwayignore`
```
# Ignore Android app - only deploy backend
app/
gradle/
build/
*.gradle
*.gradle.kts
gradlew
gradlew.bat
settings.gradle.kts
local.properties
```

---

**Next Steps:**
1. ✅ Commit and push the config files
2. ✅ Set Root Directory in Railway to `backend`
3. ✅ Add environment variables
4. ✅ Redeploy
5. ✅ Update Android app with Railway URL

Good luck! 🚀

