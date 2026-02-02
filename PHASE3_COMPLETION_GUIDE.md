# Phase 3 Completion Guide: Backend Deployment (Railway)

Guide for getting your backend deployable on Railway using **GitHub + auto-deploy**. Assumes **Phase 2 is done** (Supabase, Confluent, Redis Cloud).

---

## Your Current Setup ✓

You have already:

- **GitHub connected:** `minhng1305/MatchingEngine` linked to Railway
- **Root Directory:** `/backend` (Railway builds from the `backend` folder)
- **Branch:** `feature/minhnguyen_work` → production (auto-deploy on push)
- **Environment variables:** DATABASE_URL, DB_USERNAME, DB_PASSWORD, KAFKA_*, REDIS_*

---

## Table of Contents

1. [Step 1: Add Missing Variables](#step-1-add-missing-variables)
2. [Step 2: Check Root Directory](#step-2-check-root-directory)
3. [Step 3: Trigger a Deploy](#step-3-trigger-a-deploy)
4. [Step 4: Generate Domain & Verify](#step-4-generate-domain--verify)
5. [Step 5: Troubleshoot If Needed](#step-5-troubleshoot-if-needed)
6. [Reference: Full Setup (From Scratch)](#reference-full-setup-from-scratch)

---

## Step 1: Add Missing Variables

Your app expects a few more variables. In **Service Variables**, add any that are not yet set:

| Variable | Value | Required? |
|----------|-------|-----------|
| `ALLOWED_ORIGINS` | `https://app.yourdomain.com` or `http://localhost:3000` (temporary) | Yes — CORS will fail without it |
| `SPRING_PROFILES_ACTIVE` | `production` | Yes — loads production config |
| `PORT` | `8080` | Optional — Railway usually sets this |

**ALLOWED_ORIGINS:** If you don’t have a frontend URL yet, use `http://localhost:3000` for now. Update it in Phase 4 when the frontend is deployed.

---

## Step 2: Check Root Directory

Your Root Directory is set to `/backend`.

- If builds fail with "no such file" or "build failed", try changing it to `backend` (no leading slash). Some setups expect a relative path.
- If builds succeed, keep `/backend`.

---

## Step 3: Trigger a Deploy

Because `feature/minhnguyen_work` is connected to production, a deploy starts when you push to that branch.

**Option A: Push your current code**

```bash
git checkout feature/minhnguyen_work
git add .
git commit -m "Deploy backend to Railway"
git push origin feature/minhnguyen_work
```

**Option B: Trigger a redeploy from Railway**

1. Open your backend service in Railway.
2. Go to **Deployments**.
3. Click the **⋮** on the latest deployment → **Redeploy** (if you don’t need new code).

After a push, Railway will:

1. Pull the latest code from `feature/minhnguyen_work`
2. Build from the `backend` folder (Gradle or Dockerfile)
3. Start the app with production config
4. Expose it once a domain is set

---

## Step 4: Generate Domain & Verify

### 4.1 Generate a Public Domain

1. In Railway, open your **backend service**.
2. Go to **Settings** → **Networking** (or **Domains**).
3. Under **Public Networking**, click **Generate Domain**.
4. Copy the URL (e.g. `https://matchingengine-production-xxxx.up.railway.app`).

### 4.2 Test the Health Endpoint

Replace `YOUR-RAILWAY-URL` with your domain:

```bash
curl https://YOUR-RAILWAY-URL.up.railway.app/api/health
```

**Expected response:**

```json
{"status":"UP","timestamp":"...","version":"..."}
```

If you see this, the backend is up.

### 4.3 Check Deployment Status

- **Deployments** tab: confirm the latest deployment is **Success** (green).
- **Logs:** open the deployment and check for `Started Application` and any errors.

---

## Step 5: Troubleshoot If Needed

| Issue | What to check |
|-------|----------------|
| **Build fails** | Logs → look for Gradle/Java errors. Ensure Root Directory points to the folder with `build.gradle`. Try `backend` instead of `/backend` if needed. |
| **502 Bad Gateway** | App is crashing. Check logs for DB, Kafka, or Redis connection errors. Verify all required variables are set. |
| **Database connection failed** | Ensure `DATABASE_URL` is JDBC format (`jdbc:postgresql://...`). Check `DB_USERNAME` and `DB_PASSWORD`. |
| **Kafka connection failed** | Check `KAFKA_BOOTSTRAP_SERVERS` and `KAFKA_JAAS_CONFIG`. Confirm API key has access to `orders` and `orders-dlq` topics. |
| **Redis connection failed** | Verify `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL=true`. |
| **CORS errors in browser** | Add your frontend URL to `ALLOWED_ORIGINS`. |

---

## Quick Checklist (Your Current Approach)

- [ ] Add `ALLOWED_ORIGINS` and `SPRING_PROFILES_ACTIVE` if missing
- [ ] Push to `feature/minhnguyen_work` to trigger deploy
- [ ] Generate domain in **Settings** → **Networking**
- [ ] Test: `curl https://YOUR-URL/api/health`
- [ ] Check logs if deploy or health check fails

---

## Next: Phase 4

Once the backend is running and the health check succeeds, deploy the frontend to Vercel and update `ALLOWED_ORIGINS` to your Vercel URL.

---

## Reference: Full Setup (From Scratch)

*The sections below describe the full setup for reference. You can skip to the next phase if your deploy is working.*

---

### Part 1: Railway Setup (From Scratch)

### 1.1 Install Railway CLI

Open a terminal and run:

```bash
npm install -g @railway/cli
```

Verify:

```bash
railway --version
```

---

### 1.2 Log In to Railway

```bash
railway login
```

This opens a browser window. Sign in or create a Railway account.

---

### 1.3 Create a New Project (or Use Existing)

**Option A: Create project via dashboard**

1. Go to [railway.app](https://railway.app).
2. Click **New Project**.
3. Choose **Deploy from GitHub repo** (recommended) or **Empty project**.

**Option B: Create project via CLI**

```bash
railway init
```

When prompted:
- **Create new project** or link to existing.
- Give the project a name (e.g. `matching-engine`).

---

### 1.4 Create the Backend Service

**If you chose "Deploy from GitHub repo":**

1. Select your repository (e.g. `MatchingEngine`).
2. Railway creates a service. Go to **Settings**.
3. Set **Root Directory** to `backend` (critical — your backend code is in the `backend` folder).
4. Railway will detect the build (Gradle/Dockerfile).

**If you chose "Empty project":**

1. Click **+ New** → **GitHub Repo**.
2. Connect GitHub and select your repo.
3. Set **Root Directory** to `backend`.

---

### 1.5 Add PostgreSQL (Optional — If Using Railway's Database)

If you want Railway's managed PostgreSQL instead of Supabase:

1. In your Railway project, click **+ New** → **Database** → **PostgreSQL**.
2. Railway creates a PostgreSQL service and exposes `DATABASE_URL`, `PGHOST`, etc.
3. You can **reference** these in your backend service (Railway supports variable references like `${{Postgres.DATABASE_URL}}`).

If you use **Supabase** (from Phase 2), skip this and use your Supabase credentials.

---

### 1.6 Link Your Local Project (for CLI deploys)

From your project root:

```bash
cd backend
railway link
```

Select:
- Your Railway project
- Your backend service (the one with your repo, not PostgreSQL)

---

## Part 2: Environment Variables

Set these in the **Railway dashboard**. Get values from Phase 2 (Supabase, Confluent, Redis Cloud).

### 2.1 Where to Set Variables

1. Go to [railway.app](https://railway.app) and open your project.
2. Click your **backend service** (the web service, not PostgreSQL if you added it).
3. Go to the **Variables** tab.
4. Click **+ New Variable** (or **Raw Editor** to paste multiple).
5. Add each variable below. Railway restarts the service when you save.

---

### 2.2 Database Variables

**Option A: Using Supabase (from Phase 2)**

| Variable | Value | Where to get it |
|----------|-------|------------------|
| `DATABASE_URL` | `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:6543/postgres` | Supabase → Settings → Database → Connection string (Session mode) → Convert to JDBC format |
| `DB_USERNAME` | `postgres.<project-ref>` | Supabase connection URI (the part after `postgresql://` and before `:`) |
| `DB_PASSWORD` | Your Supabase database password | Supabase → Settings → Database → Reset password if needed |

**Note:** If your Supabase URI uses `?sslmode=require`, add it to the JDBC URL:
```
jdbc:postgresql://host:6543/postgres?sslmode=require
```

**Option B: Using Railway PostgreSQL (if you added it)**

1. Click on your **PostgreSQL** service in the project.
2. Go to **Variables** or **Connect** tab.
3. Railway exposes `DATABASE_URL` (or `DATABASE_PRIVATE_URL`). Copy it.
4. For JDBC, ensure it starts with `jdbc:postgresql://`. Railway sometimes gives `postgresql://` — add `jdbc` at the start.
5. Set `DB_USERNAME` and `DB_PASSWORD` from the same connection info (or reference the PostgreSQL service variables if Railway supports variable references).

---

### 2.3 Kafka Variables (Confluent Cloud)

| Variable | Value | Example |
|----------|-------|---------|
| `KAFKA_BOOTSTRAP_SERVERS` | Bootstrap server from Confluent | `pkc-xxxxx.us-east-1.aws.confluent.cloud:9092` |
| `KAFKA_SECURITY_PROTOCOL` | `SASL_SSL` | (required for Confluent) |
| `KAFKA_SASL_MECHANISM` | `PLAIN` | (required for Confluent) |
| `KAFKA_JAAS_CONFIG` | Full JAAS string | `org.apache.kafka.common.security.plain.PlainLoginModule required username="YOUR_API_KEY" password="YOUR_API_SECRET";` |

**Building KAFKA_JAAS_CONFIG:**
- Replace `YOUR_API_KEY` with your Confluent API key.
- Replace `YOUR_API_SECRET` with your Confluent API secret.
- Use straight double quotes. No line breaks.

---

### 2.4 Redis Variables (Redis Cloud)

| Variable | Value | Example |
|----------|-------|---------|
| `REDIS_HOST` | Public endpoint hostname | `redis-12345.c123.us-east-1-1.ec2.cloud.redislabs.com` |
| `REDIS_PORT` | Port from Redis Cloud | `12345` |
| `REDIS_PASSWORD` | Database password | From Redis Cloud dashboard |
| `REDIS_SSL` | `true` | (Redis Cloud uses TLS) |

Leave `REDIS_PASSWORD` empty only if your Redis has no password. Redis Cloud databases typically require a password.

---

### 2.5 Application Variables

| Variable | Value | Notes |
|----------|-------|-------|
| `ALLOWED_ORIGINS` | Your frontend URL(s) | e.g. `https://app.yourdomain.com` or `https://yourapp.vercel.app` (use your actual frontend URL; you can update after Phase 4) |
| `SPRING_PROFILES_ACTIVE` | `production` | Required for production config |
| `PORT` | `8080` | Railway usually sets this automatically; add only if needed |

**ALLOWED_ORIGINS:** Use a placeholder like `https://app.yourdomain.com` if you haven’t deployed the frontend yet. Update it in Phase 4 when you have the real URL. For local testing you can temporarily use `http://localhost:3000` (not recommended for production).

---

### 2.6 Variable Reference Table (Copy-Paste Checklist)

Use this when entering variables. Fill in your actual values:

```
DATABASE_URL=jdbc:postgresql://<host>:<port>/postgres
DB_USERNAME=<username>
DB_PASSWORD=<password>

KAFKA_BOOTSTRAP_SERVERS=<bootstrap-server>:9092
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=PLAIN
KAFKA_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="<api_key>" password="<api_secret>";

REDIS_HOST=<redis-host>
REDIS_PORT=<redis-port>
REDIS_PASSWORD=<redis-password>
REDIS_SSL=true

ALLOWED_ORIGINS=https://app.yourdomain.com
SPRING_PROFILES_ACTIVE=production
PORT=8080
```

---

### 2.7 Special Characters in Variables

- **KAFKA_JAAS_CONFIG:** If the password contains special characters (e.g. `"`, `\`, `$`), you may need to escape them. In Railway's UI, paste the full string as-is; it usually works.
- **DB_PASSWORD:** Same — paste directly. Railway handles special characters in the UI.

---

## Part 3: Deploy

### 3.1 Option A: Deploy via GitHub (Auto-Deploy)

1. **Connect GitHub to Railway** (if not already):
   - Project → **Settings** → **Connect Repo** (or create service from repo).
   - Authorize Railway to access your GitHub.

2. **Configure the service:**
   - **Root Directory:** `backend` (so Railway builds from the `backend` folder).
   - **Build Command:** Railway may auto-detect. If using Dockerfile: ensure the Dockerfile is in `backend/`. If using Nixpacks: `./gradlew build -x test` or similar.
   - **Start Command:** Usually auto-detected. For Java, it runs the JAR with `--spring.profiles.active=production` (or uses `SPRING_PROFILES_ACTIVE`).

3. **Push to GitHub:**
   ```bash
   git add .
   git commit -m "Deploy backend"
   git push origin main
   ```
   Railway will build and deploy automatically.

---

### 3.2 Option B: Deploy via Railway CLI

1. **Ensure you're in the backend directory and linked:**
   ```bash
   cd backend
   railway link
   ```
   Select your project and service when prompted.

2. **Deploy:**
   ```bash
   railway up
   ```
   This builds (or uses your Dockerfile) and deploys.

3. **Check status:**
   ```bash
   railway status
   railway logs
   ```

---

### 3.3 Root Directory (Important)

If your **repo root** is the project root (e.g. `MatchingEngine/` with `backend/` inside), Railway must use `backend` as the **Root Directory**:

- In Railway: Service → **Settings** → **Root Directory** → set to `backend`.
- Or run `railway up` from inside `backend/` so the CLI uses that as context.

---

### 3.4 Build Configuration

- **With Dockerfile:** If `backend/Dockerfile` exists, Railway typically uses it. The Dockerfile already sets `--spring.profiles.active=production`; `SPRING_PROFILES_ACTIVE` env var overrides it if set.
- **Without Dockerfile:** Railway uses Nixpacks. Set **Build Command** to `./gradlew build -x test` and **Start Command** to `java -jar build/libs/matchingengine-0.0.1-SNAPSHOT.jar`.

---

## Part 4: Verify Deployment

### 4.1 Get Your Deployment URL

1. In Railway: Open your backend service.
2. Go to **Settings** → **Networking** (or **Domains**).
3. Click **Generate Domain** if none exists. You'll get a URL like:
   ```
   https://matchingengine-production-xxxx.up.railway.app
   ```
4. Copy this URL.

---

### 4.2 Test Health Endpoint

Replace `YOUR-RAILWAY-URL` with your actual domain:

```bash
curl https://YOUR-RAILWAY-URL.up.railway.app/api/health
```

**Expected response (example):**
```json
{"status":"UP","timestamp":"...","version":"..."}
```

If you get a response, the backend is running.

---

### 4.3 Common Issues

| Issue | What to check |
|-------|----------------|
| **Connection refused / timeout** | Domain may still be provisioning. Wait 1–2 minutes and retry. |
| **502 Bad Gateway** | App may be crashing. Check **Logs** in Railway. Often DB, Kafka, or Redis connection failure. |
| **404 on /api/health** | Confirm the path is `/api/health` (with `/api` prefix). Check `HealthController` mapping. |
| **CORS errors** | Verify `ALLOWED_ORIGINS` includes your frontend URL. |
| **Database connection failed** | Check `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`. For Supabase, ensure you use the **pooler** endpoint and JDBC format. |
| **Kafka connection failed** | Check `KAFKA_BOOTSTRAP_SERVERS` and `KAFKA_JAAS_CONFIG`. Ensure API key has access to `orders` and `orders-dlq`. |
| **Redis connection failed** | Check `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL=true`. |

---

### 4.4 Check Logs

In Railway: Service → **Deployments** → click latest deployment → **View Logs**.

Look for:
- `Started Application` — app started successfully.
- `Flyway` migrations — DB migrations ran.
- Any stack traces or "Connection refused" — indicates a config or connectivity issue.

---

## Part 5: Update Your Checklist

After completing each step, mark the boxes in `DEPLOYMENT_CHECKLIST.md`:

**Railway Setup:**
- [ ] Install Railway CLI: `npm i -g @railway/cli`
- [ ] Login: `railway login`
- [ ] Initialize project: `cd backend && railway init`
- [ ] Link to project: `railway link`
- [ ] Add PostgreSQL in Railway (optional — skip if using Supabase from Phase 2)

**Environment Variables:**
- [ ] Set `DATABASE_URL`
- [ ] Set `DB_USERNAME`
- [ ] Set `DB_PASSWORD`
- [ ] Set `KAFKA_BOOTSTRAP_SERVERS`
- [ ] Set `KAFKA_SECURITY_PROTOCOL`
- [ ] Set `KAFKA_SASL_MECHANISM`
- [ ] Set `KAFKA_JAAS_CONFIG`
- [ ] Set `REDIS_HOST`
- [ ] Set `REDIS_PORT`
- [ ] Set `REDIS_PASSWORD`
- [ ] Set `REDIS_SSL`
- [ ] Set `ALLOWED_ORIGINS`
- [ ] Set `SPRING_PROFILES_ACTIVE`
- [ ] Set `PORT`

**Deploy:**
- [ ] Push code to GitHub (if using auto-deploy) OR run `railway up`
- [ ] Verify deployment URL
- [ ] Test health endpoint: `curl https://YOUR-URL/api/health`

---

## Quick Reference: Full Order of Operations

1. **Railway Setup:** Install CLI → `railway login` → Create project (or link) → Add backend service from GitHub → Set Root Directory to `backend`.
2. **Environment Variables:** Backend service → Variables → Add all (DB, Kafka, Redis, app).
3. **Deploy:** Push to GitHub (auto-deploy) OR `cd backend && railway up`.
4. **Verify:** Generate domain → `curl https://YOUR-URL/api/health` → Check logs if needed.

---

## Next: Phase 4

After Phase 3 is complete, you'll deploy the frontend to Vercel and set `ALLOWED_ORIGINS` to your Vercel URL (e.g. `https://yourapp.vercel.app`).
