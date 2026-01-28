# Deployment Checklist

Use this checklist to track your deployment progress.

## Phase 1: Domain & DNS Setup
- [x] Purchase domain name (Namecheap, Google Domains, etc.)
- [x] Sign up for Cloudflare (free)
- [x] Add domain to Cloudflare
- [x] Update nameservers at registrar
- [x] Wait for DNS propagation (5-30 minutes)
- [x] Verify DNS is working: `dig yourdomain.com`

## Phase 2: Infrastructure Setup

### Database
- [x] Create Supabase account
- [x] Create PostgreSQL database
- [ ] Copy connection string
- [ ] Test connection locally

### Kafka
- [x] Create Confluent Cloud account
- [x] Create Kafka cluster
- [x] Create topics:
  - [x] `orders` (12 partitions)
  - [x] `orders-dlq` (3 partitions)
- [ ] Create API keys
- [ ] Copy bootstrap servers and credentials

### Redis
- [x] Create Redis Cloud account
- [x] Create Redis database
- [ ] Copy connection details

## Phase 3: Backend Deployment

### Railway Setup
- [ ] Install Railway CLI: `npm i -g @railway/cli`
- [ ] Login: `railway login`
- [ ] Initialize project: `cd backend && railway init`
- [ ] Link to project: `railway link`
- [ ] Add PostgreSQL service in Railway dashboard

### Environment Variables
- [ ] Set `DATABASE_URL`
- [ ] Set `DB_USERNAME`
- [ ] Set `DB_PASSWORD`
- [ ] Set `KAFKA_BOOTSTRAP_SERVERS`
- [ ] Set `KAFKA_SECURITY_PROTOCOL` (if using Confluent)
- [ ] Set `KAFKA_SASL_MECHANISM` (if using Confluent)
- [ ] Set `KAFKA_JAAS_CONFIG` (if using Confluent)
- [ ] Set `REDIS_HOST`
- [ ] Set `REDIS_PORT`
- [ ] Set `REDIS_PASSWORD` (if required)
- [ ] Set `REDIS_SSL` (true for cloud providers)
- [ ] Set `ALLOWED_ORIGINS` (your frontend domain)
- [ ] Set `SPRING_PROFILES_ACTIVE=production`
- [ ] Set `PORT=8080`

### Deploy
- [ ] Push code to GitHub (if using auto-deploy)
- [ ] OR run `railway up` manually
- [ ] Verify deployment URL
- [ ] Test health endpoint: `curl https://your-railway-url.up.railway.app/api/health`

## Phase 4: Frontend Deployment

### Vercel Setup
- [ ] Sign up for Vercel account
- [ ] Connect GitHub repository
- [ ] Create new project
- [ ] Set root directory: `trading-frontend`
- [ ] Set build command: `npm run build`
- [ ] Set output directory: `build`

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
- [ ] Add CNAME record for `app` → `cname.vercel-dns.com` (Proxied)
- [ ] Add CNAME record for `api` → `your-railway-url.up.railway.app` (Proxied)
- [ ] Wait for DNS propagation (5-30 minutes)
- [ ] Verify: `dig app.yourdomain.com`
- [ ] Verify: `dig api.yourdomain.com`

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
