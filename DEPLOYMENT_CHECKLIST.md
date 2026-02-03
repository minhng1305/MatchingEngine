# Deployment Checklist

Use this checklist to track your deployment progress.

## Branching (see DEPLOYMENT_PLAN.md § Branching & Deployment Strategy)

- [ ] Use **main** as production branch (Vercel/Railway deploy from main)
- [ ] Work in feature branches (e.g. `feature/your-name`)
- [ ] Merge to main only via **Pull Request** (no direct push to main from feature branches)
- [ ] Optional: use a separate `prod` branch only if you need release-on-demand or hotfix workflow

## Phase 1: Domain & DNS Setup (DONE)
- [x] Purchase domain name (Namecheap, Google Domains, etc.)
- [x] Sign up for Cloudflare (free)
- [x] Add domain to Cloudflare
- [x] Update nameservers at registrar
- [x] Wait for DNS propagation (5-30 minutes)
- [x] Verify DNS is working: `dig yourdomain.com`

## Phase 2: Infrastructure Setup (DONE)

### Database
- [x] Create Supabase account
- [x] Create PostgreSQL database
- [x] Copy connection string
- [x] Test connection locally

### Kafka
- [x] Create Confluent Cloud account
- [x] Create Kafka cluster
- [x] Create topics:
  - [x] `orders` (12 partitions)
  - [x] `orders-dlq` (3 partitions)
- [x] Create API keys
- [x] Copy bootstrap servers and credentials

### Redis
- [x] Create Redis Cloud account
- [x] Create Redis database
- [x] Copy connection details

## Phase 3: Backend Deployment

### Railway Setup
- [x] Install Railway CLI: `npm i -g @railway/cli`
- [x] Login: `railway login`
- [x] Initialize project: `cd backend && railway init`
- [x] Link to project: `railway link`
- [x] Add PostgreSQL service in Railway dashboard

### Environment Variables
- [x] Set `DATABASE_URL`
- [x] Set `DB_USERNAME`
- [x] Set `DB_PASSWORD`
- [x] Set `KAFKA_BOOTSTRAP_SERVERS`
- [x] Set `KAFKA_SECURITY_PROTOCOL` (if using Confluent)
- [x] Set `KAFKA_SASL_MECHANISM` (if using Confluent)
- [x] Set `KAFKA_JAAS_CONFIG` (if using Confluent)
- [x] Set `REDIS_HOST`
- [x] Set `REDIS_PORT`
- [x] Set `REDIS_PASSWORD` (if required)
- [x] Set `REDIS_SSL` (true for cloud providers)
- [x] Set `ALLOWED_ORIGINS` (your frontend domain)
- [x] Set `SPRING_PROFILES_ACTIVE=production`
- [x] Set `PORT=8080`

### Deploy
- [x] Push code to GitHub (if using auto-deploy)
- [x] OR run `railway up` manually
- [x] Verify deployment URL
- [x] Test health endpoint: `curl https://your-railway-url.up.railway.app/api/health`

## Phase 4: Frontend Deployment

### Vercel Setup
- [x] Sign up for Vercel account
- [x] Connect GitHub repository
- [x] Create new project
- [x] Set root directory: `trading-frontend`
- [x] Set build command: `npm run build`
- [x] Set output directory: `build`

### Environment Variables
- [ ] Set `REACT_APP_API_BASE_URL` (your backend API URL)
- [ ] Set `REACT_APP_INGRESS_BASE_URL` (your backend API URL)
- [ ] Set `REACT_APP_SERVER1_URL` (your backend API URL)
- [ ] Set `REACT_APP_SERVER2_URL` (your backend API URL)
- [ ] Set `REACT_APP_SERVER3_URL` (your backend API URL)

### Deploy
- [ ] Deploy to Vercel
- [ ] Verify deployment
- [ ] Add custom domain: `app.yourdomain.com`
- [ ] Wait for SSL certificate

## Phase 5: DNS Configuration

### Cloudflare DNS Records
- [x] Add CNAME record for `app` → `cname.vercel-dns.com` (Proxied)
- [x] Add CNAME record for `api` → `your-railway-url.up.railway.app` (Proxied)
- [x] Wait for DNS propagation (5-30 minutes)
- [x] Verify: `dig app.yourdomain.com`
- [x] Verify: `dig api.yourdomain.com`

## Phase 6: Testing

### Backend Tests
- [ ] Health check: `curl https://api.yourdomain.com/api/health`
- [ ] Database connection (check logs)
- [ ] Kafka connection (check logs)
- [ ] Redis connection (check logs)

### Frontend Tests
- [ ] Visit `https://app.yourdomain.com`
- [ ] Check browser console for errors
- [ ] Test login/registration
- [ ] Test API connectivity
- [ ] Test order submission
- [ ] Test WebSocket connection

### Integration Tests
- [ ] End-to-end user flow
- [ ] Order processing
- [ ] Real-time updates
- [ ] Error handling

## Phase 7: Database Migration

- [ ] Verify Flyway migrations ran automatically
- [ ] OR run migrations manually
- [ ] Verify tables exist:
  - [ ] `users`
  - [ ] `orders`
  - [ ] `trades`
  - [ ] `portfolios`

## Phase 8: Monitoring & Maintenance

### Monitoring Setup
- [ ] Set up error tracking (Sentry, LogRocket)
- [ ] Set up uptime monitoring (Uptime Robot)
- [ ] Configure log aggregation
- [ ] Set up alerts

### Security
- [ ] Verify HTTPS is enabled
- [ ] Verify CORS is configured correctly
- [ ] Verify environment variables are secure
- [ ] Review security headers
- [ ] Set up rate limiting (optional)

### Backups
- [ ] Configure database backups
- [ ] Test backup restoration
- [ ] Document backup procedures

## Phase 9: Documentation

- [ ] Document deployment process
- [ ] Document environment variables
- [ ] Document rollback procedure
- [ ] Document troubleshooting steps
- [ ] Share credentials securely (password manager)

## Phase 10: Go Live

- [ ] Final end-to-end testing
- [ ] Performance testing
- [ ] Load testing (optional)
- [ ] Security audit
- [ ] Announce launch! 🎉

---

## Troubleshooting Checklist

If something doesn't work:

- [ ] Check backend logs in Railway/Render
- [ ] Check frontend logs in Vercel
- [ ] Verify environment variables are set correctly
- [ ] Check DNS propagation: `dig yourdomain.com`
- [ ] Test backend directly: `curl https://api.yourdomain.com/api/health`
- [ ] Check CORS configuration
- [ ] Verify database connection
- [ ] Verify Kafka connection
- [ ] Verify Redis connection
- [ ] Check browser console for errors
- [ ] Check network tab in browser DevTools

---

## Notes

- Keep this checklist updated as you progress
- Check off items as you complete them
- Add notes for any issues encountered
- Document any deviations from the plan
