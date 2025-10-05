# 🚂 Railway Deployment Guide - Sovereign Rise Backend

This guide will help you deploy your Next.js backend to Railway, making it production-ready and accessible 24/7 without running it on your computer.

## 📊 Before vs After

| Aspect | Before (Local) | After (Railway) |
|--------|---------------|-----------------|
| **Availability** | Only when computer is on | 24/7 uptime |
| **Access** | Local network only | Accessible anywhere |
| **Startup** | Manual `npm run dev` | Automatic |
| **Deployment** | N/A | Auto-deploy on git push |
| **SSL/HTTPS** | HTTP only | HTTPS by default |
| **Scaling** | Limited to your machine | Auto-scaling available |

---

## 🚀 Step-by-Step Deployment

### Step 1: Create Railway Account

1. Go to **[railway.app](https://railway.app)**
2. Click **"Sign up"**
3. Sign up with **GitHub** (recommended) or email
4. Verify your account via email if needed

### Step 2: Create New Project

1. Click **"New Project"** button on Railway dashboard
2. Select **"Deploy from GitHub repo"**
3. If not connected, authorize Railway to access your GitHub
4. Search and select your **`SovereignRise`** repository
5. Railway will auto-detect it's a Next.js application

### Step 3: Configure Backend Directory

Since your backend is in the `backend/` subdirectory, you need to configure Railway:

1. After selecting the repo, Railway creates a service
2. Click on your service card
3. Go to **Settings** tab
4. Find **Build & Deploy** section
5. Set the following:
   - **Root Directory**: `backend`
   - **Build Command**: `npm run build` (should auto-detect)
   - **Start Command**: `npm start` (should auto-detect)

### Step 4: Add Environment Variables

This is the most important step! Click on the **Variables** tab and add these environment variables:

#### Database Configuration
```
DATABASE_URL
```
**Value:**
```
postgresql://bikash:5kbg25VI4mLfLhvXnPjmNg@quiet-dragon-16573.j77.aws-ap-south-1.cockroachlabs.cloud:26257/defaultdb?sslmode=verify-full
```

#### Firebase Admin SDK
```
FIREBASE_PROJECT_ID
```
**Value:** `sovereign-rise-1e8dd`

```
FIREBASE_CLIENT_EMAIL
```
**Value:** `firebase-adminsdk-xxxxx@sovereign-rise-1e8dd.iam.gserviceaccount.com`
*(Get this from your Firebase Admin SDK JSON)*

```
FIREBASE_PRIVATE_KEY
```
**Value:** *(Paste your actual private key - see instructions below)*

> ⚠️ **IMPORTANT: Firebase Private Key Formatting**
> 
> Railway handles multi-line environment variables correctly. When you paste your private key:
> 1. Open your Firebase Admin SDK JSON file
> 2. Copy the `private_key` value (including the `-----BEGIN PRIVATE KEY-----` and `-----END PRIVATE KEY-----`)
> 3. Paste it AS-IS into Railway (with actual line breaks, not `\n` characters)
> 4. Railway's UI will show it as multiple lines - this is correct!

#### Google Gemini API (for AI features)
```
GEMINI_API_KEY
```
**Value:** `your_gemini_api_key_here`
*(Get from Google AI Studio: https://makersuite.google.com/app/apikey)*

#### Environment
```
NODE_ENV
```
**Value:** `production`

### Step 5: Deploy 🎉

1. Click **"Deploy"** button (if not already deploying)
2. Railway will automatically:
   - Clone your repository
   - Install dependencies (`npm install`)
   - Build your app (`npm run build`)
   - Start your server (`npm start`)
3. Watch the **Deployments** tab for build logs
4. Wait 2-5 minutes for first deployment

### Step 6: Get Your Railway URL

Once deployment is successful:

1. Go to **Settings** tab of your service
2. Scroll to **Domains** section
3. Click **"Generate Domain"** if not already generated
4. Copy your Railway URL (e.g., `sovereignrise-production.up.railway.app`)
5. **This is your production API URL!** 🎯

#### Optional: Add Custom Domain

1. Click **"Custom Domain"** in the Domains section
2. Enter your domain (e.g., `api.sovereignrise.com`)
3. Add the CNAME record to your DNS provider:
   - **Type**: CNAME
   - **Name**: api
   - **Value**: `your-app.up.railway.app`
4. Wait for DNS propagation (5-30 minutes)

---

## 📱 Update Android App

Now that your backend is deployed, update your Android app to use the Railway URL:

### Option 1: Production-Only URL

**File:** `app/src/main/java/com/sovereign_rise/app/data/remote/NetworkModule.kt`

```kotlin
object NetworkModule {
    
    // Production Railway URL
    private const val BASE_URL = "https://your-railway-url.up.railway.app/"
    
    // Production timeouts
    private const val CONNECT_TIMEOUT = 15L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L
    
    // ... rest of your code
}
```

### Option 2: Environment-Based URL (Recommended)

Create a build configuration that switches between local and production:

**File:** `app/src/main/java/com/sovereign_rise/app/data/remote/NetworkModule.kt`

```kotlin
object NetworkModule {
    
    // Automatically switches based on build type
    private const val BASE_URL = if (BuildConfig.DEBUG) {
        // Development: Local backend
        "http://10.0.2.2:3000/"  // For emulator
        // "http://192.168.x.x:3000/"  // For physical device
    } else {
        // Production: Railway backend
        "https://your-railway-url.up.railway.app/"
    }
    
    private const val CONNECT_TIMEOUT = 15L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L
    
    // ... rest of your code
}
```

**Benefits:**
- ✅ Debug builds use local backend for development
- ✅ Release builds automatically use Railway
- ✅ No code changes needed when switching environments

### Rebuild Your App

After updating the URL:
1. In Android Studio, go to **Build → Clean Project**
2. Then **Build → Rebuild Project**
3. Run your app
4. Test authentication and API calls

---

## 🧪 Test Your Deployment

### 1. Basic Health Check

Test if your backend is accessible:

```bash
curl https://your-railway-url.up.railway.app/api/user/profile
```

Expected response:
```json
{
  "error": "Unauthorized"
}
```
*(This is expected - it means the API is working, just needs authentication)*

### 2. Test with Android App

1. Open your app
2. Try to sign in with Firebase
3. Check if user profile loads
4. Create a task or habit
5. Monitor Railway logs for any errors

### 3. View Railway Logs

1. Go to Railway dashboard
2. Click on your service
3. Go to **Deployments** tab
4. Click on the latest deployment
5. View real-time logs

---

## 🔄 Continuous Deployment

Railway automatically redeploys your backend when you push to GitHub:

### Automatic Deployment

1. Make changes to your backend code
2. Commit and push to GitHub:
   ```bash
   cd backend
   git add .
   git commit -m "Update API endpoints"
   git push origin main
   ```
3. Railway automatically detects the push
4. Builds and deploys the new version
5. Zero downtime deployment!

### Configure Deployment Branch

By default, Railway deploys from `main` branch:

1. Go to **Settings** → **Build & Deploy**
2. Under **Watch Paths**, you can set:
   - Deploy only when `backend/` changes
   - Deploy from specific branches
3. Click **Save**

### Manual Deployment

If you need to manually trigger a deployment:

1. Go to **Deployments** tab
2. Click **"Deploy"** button
3. Select a commit or use latest

---

## 📊 Monitoring & Management

### View Metrics

Railway provides built-in monitoring:

1. **CPU Usage**: Track processing power
2. **Memory Usage**: Monitor RAM consumption
3. **Network Traffic**: See incoming/outgoing data
4. **Request Count**: API call volume

Access metrics: **Service → Metrics** tab

### View Logs

Real-time application logs:

1. Go to **Deployments** tab
2. Click on active deployment
3. View logs in real-time
4. Filter by log level (info, warn, error)

### Database Connection

Your CockroachDB is already hosted in the cloud, so:
- ✅ Railway connects to it via `DATABASE_URL`
- ✅ No additional database setup needed
- ✅ Same database for dev and production
- ✅ Connection is SSL-encrypted

---

## 💰 Railway Pricing

Railway offers several plans:

| Plan | Price | Resources | Best For |
|------|-------|-----------|----------|
| **Trial** | $5 credit (one-time) | Limited resources | Testing |
| **Hobby** | $5/month | 512MB RAM, 1GB disk | Personal projects |
| **Developer** | $10/month | 8GB RAM, 100GB disk | Small apps |
| **Team** | $20/month | 32GB RAM, 100GB disk | Production apps |

**Cost Estimate for Your App:**
- Small user base: Trial or Hobby plan ($5/month)
- Growing app: Developer plan ($10/month)
- Production scale: Team plan ($20/month)

### Monitor Usage

1. Go to **Account Settings**
2. View **Usage** tab
3. See current month's costs
4. Set up billing alerts

---

## 🚨 Troubleshooting

### Build Failed

**Error**: `npm install` fails

**Solution:**
1. Check `backend/package.json` exists
2. Verify `package-lock.json` is committed
3. Check Railway build logs for specific error
4. Ensure Node.js version compatibility

**Error**: `next build` fails

**Solution:**
1. Test build locally: `cd backend && npm run build`
2. Fix any TypeScript errors
3. Check `next.config.js` is valid
4. Push fixes and redeploy

### App Crashes on Startup

**Error**: "Application failed to respond"

**Solution:**
1. Check environment variables are set correctly
2. Verify `DATABASE_URL` is accessible
3. Check logs: **Deployments → View Logs**
4. Look for connection errors or missing variables

### Can't Connect from Android

**Error**: "Unable to resolve host"

**Solution:**
1. ✅ Verify Railway URL is correct in `NetworkModule.kt`
2. ✅ Ensure URL starts with `https://` (not `http://`)
3. ✅ Check Railway service is running (green status)
4. ✅ Test with curl or Postman first
5. ✅ Rebuild Android app after changing URL

### Database Connection Issues

**Error**: "Connection timeout" or "SSL error"

**Solution:**
1. Verify `DATABASE_URL` in Railway matches your CockroachDB credentials
2. Check CockroachDB cluster is active
3. Test connection from Railway:
   ```bash
   railway run npx ts-node -e "import('pg').then(pg => new pg.Pool({connectionString: process.env.DATABASE_URL}).query('SELECT NOW()'))"
   ```

### Firebase Authentication Fails

**Error**: "Invalid credentials" or "Auth error"

**Solution:**
1. Verify `FIREBASE_PRIVATE_KEY` is formatted correctly (with line breaks)
2. Check `FIREBASE_CLIENT_EMAIL` matches your Firebase project
3. Ensure Firebase project ID is correct
4. Re-download Firebase Admin SDK JSON if needed

---

## 🔒 Security Best Practices

### ✅ Do This

- **Use environment variables** for all secrets
- **Enable HTTPS only** (Railway does this automatically)
- **Set `NODE_ENV=production`** in Railway
- **Rotate API keys periodically**
- **Monitor Railway logs** for suspicious activity
- **Keep dependencies updated**: `npm audit fix`
- **Use Railway's IP allowlist** if needed

### ❌ Don't Do This

- **Never commit `.env` files** to Git
- **Don't expose API keys** in client-side code
- **Don't use HTTP** in production
- **Don't share Railway dashboard** access
- **Don't hardcode credentials** in code
- **Don't ignore security warnings** from npm audit

### Recommended Headers

Your `next.config.js` already has CORS configured. For additional security, consider adding:

```javascript
// backend/next.config.js
async headers() {
  return [
    {
      source: '/api/:path*',
      headers: [
        { key: 'X-Content-Type-Options', value: 'nosniff' },
        { key: 'X-Frame-Options', value: 'DENY' },
        { key: 'X-XSS-Protection', value: '1; mode=block' },
        // ... existing CORS headers
      ]
    }
  ]
}
```

---

## 🎯 Railway CLI (Advanced)

For power users, Railway offers a CLI for local deployment:

### Installation

```bash
npm install -g @railway/cli
```

### Login

```bash
railway login
```

### Link Project

```bash
cd backend
railway link
```

### Set Environment Variables

```bash
railway variables set DATABASE_URL="postgresql://..."
railway variables set FIREBASE_PROJECT_ID="sovereign-rise-1e8dd"
# ... set other variables
```

### Deploy from CLI

```bash
railway up
```

### Run Commands on Railway

```bash
# Initialize database
railway run npm run init-db

# Check environment
railway run printenv | grep DATABASE_URL
```

### View Logs from CLI

```bash
railway logs
```

---

## 📋 Post-Deployment Checklist

After your first successful deployment:

- [ ] ✅ Railway URL is accessible (test with curl)
- [ ] ✅ All environment variables are set
- [ ] ✅ Database connection works
- [ ] ✅ Firebase authentication works
- [ ] ✅ Android app connects successfully
- [ ] ✅ API endpoints return expected data
- [ ] ✅ Logs show no errors
- [ ] ✅ Custom domain configured (optional)
- [ ] ✅ Monitoring alerts set up
- [ ] ✅ Backup environment variables saved securely
- [ ] ✅ Team members have access (if needed)

---

## 🎓 Additional Resources

### Railway Documentation
- [Railway Docs](https://docs.railway.app/)
- [Next.js on Railway](https://docs.railway.app/guides/nextjs)
- [Environment Variables](https://docs.railway.app/develop/variables)

### Your Project Documentation
- `BACKEND_SETUP_GUIDE.md` - Local development setup
- `backend/README.md` - Backend API documentation
- `QUICK_REFERENCE.md` - Quick commands reference

### Support
- Railway Discord: [discord.gg/railway](https://discord.gg/railway)
- Railway Status: [status.railway.app](https://status.railway.app)

---

## 🎉 Success! What's Next?

Once deployed successfully, you have:

✅ **24/7 Backend Availability** - No need to keep computer running  
✅ **Auto-Deployment** - Push to GitHub = automatic deployment  
✅ **Production Infrastructure** - SSL, monitoring, logging  
✅ **Scalability** - Railway handles traffic spikes  
✅ **Professional Setup** - Ready for real users  

### Recommended Next Steps

1. **Test thoroughly** - Try all features from your Android app
2. **Monitor for a week** - Watch logs and metrics
3. **Set up alerts** - Get notified of errors
4. **Add custom domain** - Use your own domain name
5. **Create staging environment** - Separate dev/prod deployments
6. **Document your API** - Consider adding Swagger/OpenAPI docs
7. **Set up CI/CD** - Add automated testing before deployment

### Performance Optimization

Once stable, consider:
- Adding Redis caching (Railway add-on)
- Implementing rate limiting
- Optimizing database queries
- Adding CDN for static assets
- Setting up monitoring (Sentry, LogRocket)

---

## 📞 Need Help?

If you run into issues:

1. **Check Railway Logs** - Most issues are visible in logs
2. **Test Locally First** - Verify changes work with `npm run dev`
3. **Review Environment Variables** - Double-check all are set correctly
4. **Check Railway Status** - Verify no platform outages
5. **Railway Discord** - Active community for support

---

**Created for Sovereign Rise Project**  
Last Updated: October 2025  
Railway Version: 3.x

