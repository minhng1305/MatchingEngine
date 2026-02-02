# Quick Start Deployment Guide

This is a condensed version of the full deployment plan. Follow these steps to deploy your application.

## Prerequisites

- GitHub account
- Domain name (purchase from Namecheap, Google Domains, etc.)
- Credit card for cloud services (some have free tiers)

## Step 1: Purchase Domain & Set Up DNS (30 minutes)

1. **Purchase domain** from Namecheap or Google Domains (~$10-15/year)
2. **Sign up for Cloudflare** (free): https://cloudflare.com
3. **Add domain to Cloudflare** and update nameservers at your registrar
4. Wait for DNS propagation (5-30 minutes)

## Step 2: Set Up Infrastructure (1-2 hours)

### 2.1 PostgreSQL Database
- **Railway**: Create project → Add PostgreSQL → Copy connection string
- **OR Supabase**: Create project → Settings → Database → Copy connection string

### 2.2 Kafka
- **Upstash** (recommended for cost): https://upstash.com
  - Create Kafka cluster
  - Create topics: `orders` (12 partitions), `orders-dlq` (3 partitions)
  - Copy bootstrap servers and credentials
- **OR Confluent Cloud**: More expensive but enterprise-grade

### 2.3 Redis
- **Upstash Redis** (free tier): https://upstash.com
  - Create Redis database
  - Copy connection details
- **OR Redis Cloud**: Free tier available

## Step 3: Deploy Backend (1 hour)

### Option A: Railway (Recommended)

1. **Install Railway CLI**: `npm i -g @railway/cli`
2. **Login**: `railway login`
3. **Initialize**: `cd backend && railway init`
4. **Link project**: `railway link`
5. **Add PostgreSQL**: Railway dashboard → New → Database → PostgreSQL
6. **Set environment variables** in Railway dashboard:
   ```
   DATABASE_URL=<from Railway PostgreSQL>
   DB_USERNAME=postgres
   DB_PASSWORD=<from Railway>
   KAFKA_BOOTSTRAP_SERVERS=<from Upstash/Confluent>
   KAFKA_SECURITY_PROTOCOL=SASL_SSL
   KAFKA_SASL_MECHANISM=PLAIN
   KAFKA_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="..." password="...";
   REDIS_HOST=<from Upstash>
   REDIS_PORT=<from Upstash>
   REDIS_PASSWORD=<from Upstash>
   REDIS_SSL=true
   ALLOWED_ORIGINS=https://app.yourdomain.com
   SPRING_PROFILES_ACTIVE=production
   PORT=8080
   ```
7. **Deploy**: `railway up` or push to GitHub (auto-deploy)
8. **Get deployment URL**: Railway will provide (e.g., `matching-engine.up.railway.app`)

### Option B: Render

1. Sign up at https://render.com
2. New → Web Service
3. Connect GitHub repo
4. Set:
   - **Root Directory**: `backend`
   - **Build Command**: `./gradlew build`
   - **Start Command**: `java -jar build/libs/*.jar --spring.profiles.active=production`
5. Add environment variables (same as Railway)
6. Deploy

## Step 4: Deploy Frontend to Vercel (30 minutes)

1. **Sign up** at https://vercel.com
2. **New Project** → Import GitHub repository
3. **Configure**:
   - Root Directory: `trading-frontend`
   - Build Command: `npm run build`
   - Output Directory: `build`
4. **Add Environment Variables**:
   ```
   REACT_APP_API_BASE_URL=https://api.yourdomain.com/api
   REACT_APP_INGRESS_BASE_URL=https://api.yourdomain.com/api
   REACT_APP_SERVER1_URL=https://api.yourdomain.com
   REACT_APP_SERVER2_URL=https://api.yourdomain.com
   REACT_APP_SERVER3_URL=https://api.yourdomain.com
   ```
5. **Deploy**
6. **Add Custom Domain**: Settings → Domains → Add `app.yourdomain.com`

## Step 5: Configure DNS (15 minutes)

In Cloudflare, add these records:

```
Type    Name    Content                          Proxy
CNAME   app     cname.vercel-dns.com             ✅ (Proxied)
CNAME   api     your-railway-domain.up.railway.app ✅ (Proxied)
```

Wait 5-30 minutes for DNS propagation.

## Step 6: Test (30 minutes)

1. **Visit**: `https://app.yourdomain.com`
2. **Check health**: `curl https://api.yourdomain.com/api/health`
3. **Test login/registration**
4. **Submit test order**
5. **Check logs** in Railway/Vercel dashboards

## Step 7: Run Database Migrations

Flyway will run automatically on first startup. Verify:

```sql
-- Connect to your production database
\dt  -- Should show: users, orders, trades, portfolios
```

## Troubleshooting

### CORS Errors
- Update `ALLOWED_ORIGINS` in backend env vars
- Ensure it matches your frontend domain exactly

### Database Connection Failed
- Check `DATABASE_URL` format
- Verify credentials
- Check firewall rules (Railway/Supabase allow all by default)

### Kafka Connection Issues
- Verify `KAFKA_BOOTSTRAP_SERVERS` is correct
- Check `KAFKA_JAAS_CONFIG` format
- Ensure topics exist in Kafka cluster

### Frontend Can't Reach Backend
- Verify `REACT_APP_API_BASE_URL` matches backend domain
- Check CORS configuration
- Test backend directly: `curl https://api.yourdomain.com/api/health`

## Cost Estimate

**Minimum (with free tiers):**
- Domain: $1-2/month
- Railway: $5/month (starter)
- Upstash Kafka: $10-20/month (pay-per-use)
- Upstash Redis: Free
- Vercel: Free
- **Total: ~$15-30/month**

**With Confluent Cloud:**
- Add ~$720/month for Kafka
- **Total: ~$750/month**

## Next Steps

1. Set up monitoring (Sentry, LogRocket)
2. Configure backups
3. Set up CI/CD
4. Add rate limiting
5. Set up alerts

## Need Help?

Refer to the full `DEPLOYMENT_PLAN.md` for detailed explanations and troubleshooting.
