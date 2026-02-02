# Complete Deployment Plan: Matching Engine to Production

## 📋 Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Infrastructure Requirements](#infrastructure-requirements)
3. [Pre-Deployment Checklist](#pre-deployment-checklist)
4. [Phase 1: Domain & DNS Setup](#phase-1-domain--dns-setup)
5. [Phase 2: Backend Infrastructure](#phase-2-backend-infrastructure)
6. [Phase 3: Frontend Deployment (Vercel)](#phase-3-frontend-deployment-vercel)
7. [Phase 4: Environment Configuration](#phase-4-environment-configuration)
8. [Phase 5: Database Migration](#phase-5-database-migration)
9. [Phase 6: Testing & Validation](#phase-6-testing--validation)
10. [Phase 7: Monitoring & Maintenance](#phase-7-monitoring--maintenance)
11. [Cost Estimates](#cost-estimates)
12. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

### Current Architecture
```
Frontend (React/TypeScript)
  ↓
Backend Servers:
  - Ingress Server (Port 8085) → Kafka Producer
  - Matching Server 1 (Port 8080) → Kafka Consumer
  - Matching Server 2 (Port 8081) → Kafka Consumer  
  - Matching Server 3 (Port 8082) → Kafka Consumer
  ↓
Infrastructure:
  - PostgreSQL Database
  - Kafka Cluster
  - Redis Cache
```

### Production Architecture
```
Domain: greentrader.org
  ├── Frontend: app.greentrader.org (Vercel)
  └── Backend API: api.greentrader.org (Load Balancer)
        ├── Ingress Server (1 instance)
        └── Matching Servers (3 instances)
              ↓
        Managed Services:
          - PostgreSQL (Cloud Provider)
          - Kafka (Confluent Cloud / AWS MSK)
          - Redis (Redis Cloud / AWS ElastiCache)
```

---

## Infrastructure Requirements

### Services Needed

| Service | Purpose | Recommended Provider | Alternative |
|---------|---------|---------------------|-------------|
| **Frontend Hosting** | React App | Vercel | Netlify, Cloudflare Pages |
| **Backend Hosting** | Spring Boot API | Railway, Render | AWS ECS, Google Cloud Run |
| **PostgreSQL** | Database | Railway PostgreSQL | AWS RDS, Supabase, Neon |
| **Kafka** | Message Queue | Confluent Cloud | AWS MSK, Upstash Kafka |
| **Redis** | Caching | Redis Cloud | Upstash Redis, AWS ElastiCache |
| **Domain** | Custom Domain | Namecheap, Google Domains | Cloudflare, GoDaddy |
| **DNS** | Domain Management | Cloudflare (Free) | Route53, Namecheap DNS |

### Why This Stack?

1. **Vercel for Frontend**: 
   - Zero-config deployment
   - Automatic HTTPS
   - Global CDN
   - Free tier available
   - Perfect for React apps

2. **Railway/Render for Backend**:
   - Easy Java/Spring Boot deployment
   - Built-in PostgreSQL
   - Environment variable management
   - Auto-scaling
   - Reasonable pricing

3. **Managed Services**:
   - No infrastructure management
   - Automatic backups
   - High availability
   - Security patches handled

---

## Pre-Deployment Checklist

### Code Preparation
- [ ] Remove hardcoded localhost URLs
- [ ] Add environment variable support
- [ ] Configure CORS for production domains
- [ ] Set up proper error handling
- [ ] Add health check endpoints
- [ ] Configure logging
- [ ] Remove debug/console logs from production
- [ ] Test database migrations
- [ ] Review security configurations

### Infrastructure Preparation
- [ ] Choose domain name
- [ ] Set up accounts:
  - [ ] Vercel account
  - [ ] Railway/Render account
  - [ ] Confluent Cloud account (for Kafka)
  - [ ] Redis Cloud account
  - [ ] Domain registrar account
  - [ ] Cloudflare account (for DNS)

---

## Phase 1: Domain & DNS Setup

### Step 1.1: Purchase Domain

**Recommended Registrars:**
- **Namecheap**: ~$10-15/year, good UI
- **Google Domains**: ~$12/year, simple
- **Cloudflare Registrar**: At-cost pricing (~$8-10/year)

**Action:**
1. Choose a domain name (e.g., `matchingengine.com`, `tradingengine.io`)
2. Purchase domain
3. Note: DNS propagation takes 24-48 hours

### Step 1.2: Set Up Cloudflare DNS (Free)

**Why Cloudflare:**
- Free DNS management
- DDoS protection
- SSL/TLS certificates
- CDN (optional)
- Easy to use

**Action:**
1. Sign up at [cloudflare.com](https://cloudflare.com)
2. Add your domain
3. Update nameservers at your registrar (Cloudflare will provide)
4. Wait for DNS propagation (check with `dig greentrader.org`)

### Step 1.3: Plan Subdomains

You'll need:
- `app.greentrader.org` → Frontend (Vercel)
- `api.greentrader.org` → Backend API (Railway/Render)
- Optional: `www.greentrader.org` → Redirect to app

**DNS Records to Create (in Cloudflare):**
```
Type    Name    Content                    TTL
CNAME   app     cname.vercel-dns.com       Auto
CNAME   api     your-railway-domain.com    Auto
A       @       (Vercel IP - will add later) Auto
```

**Note:** We'll configure these after deploying services.

---

## Phase 2: Backend Infrastructure

### Step 2.1: Set Up PostgreSQL Database

**Supabase (Free Tier Available)**
1. Sign up at [supabase.com](https://supabase.com)
2. Create new project
3. Go to Settings → Database
4. Copy connection string


### Step 2.2: Set Up Kafka

**Confluent Cloud (Recommended)**
1. Sign up at [confluent.cloud](https://confluent.cloud)
2. Create cluster (Basic plan: $1/hour ≈ $720/month)
3. Create topics:
   - `orders` (12 partitions)
   - `orders-dlq` (3 partitions)
4. Create API keys (for producer/consumer)
5. Copy bootstrap servers and credentials


### Step 2.3: Set Up Redis

**Redis Cloud (Recommended)**
1. Sign up at [redis.com/cloud](https://redis.com/cloud)
2. Create database (Free tier: 30MB)
3. Copy connection details


### Step 2.4: Prepare Backend for Deployment

**Create Production Configuration Files:**

1. **Create `application-production.properties`**:
```properties
spring.application.name=matchingengine

# Server Configuration
server.port=${PORT:8080}
server.servlet.session.cookie.same-site=strict
server.servlet.session.cookie.secure=true

# Database (from Railway/Supabase)
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=false

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:/db/migration
spring.flyway.baseline-on-migrate=true

# Kafka Configuration
spring.kafka.producer.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS}
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.properties.security.protocol=${KAFKA_SECURITY_PROTOCOL:SASL_SSL}
spring.kafka.producer.properties.sasl.mechanism=${KAFKA_SASL_MECHANISM:PLAIN}
spring.kafka.producer.properties.sasl.jaas.config=${KAFKA_JAAS_CONFIG}

spring.kafka.consumer.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS}
spring.kafka.consumer.group-id=matching-engine-group
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.security.protocol=${KAFKA_SECURITY_PROTOCOL:SASL_SSL}
spring.kafka.consumer.properties.sasl.mechanism=${KAFKA_SASL_MECHANISM:PLAIN}
spring.kafka.consumer.properties.sasl.jaas.config=${KAFKA_JAAS_CONFIG}

# Kafka Topics
app.kafka.topic.orders=orders
app.kafka.dlq.topic=orders-dlq
app.kafka.consumer.concurrency=4

# Redis Configuration
spring.redis.host=${REDIS_HOST}
spring.redis.port=${REDIS_PORT:6379}
spring.redis.password=${REDIS_PASSWORD:}
spring.redis.ssl=${REDIS_SSL:true}
spring.cache.type=redis
spring.cache.redis.time-to-live=3600000

# CORS Configuration
spring.web.cors.allowed-origins=${ALLOWED_ORIGINS:https://app.greentrader.org}
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*

# Logging
logging.level.root=INFO
logging.level.com.project.matchingengine=DEBUG
logging.file.name=application.log
```

2. **Create `Dockerfile` for Backend**:
```dockerfile
# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN gradle build -x test --no-daemon

# Runtime stage
FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=production"]
```

3. **Create `.dockerignore`**:
```
.gradle
build
.idea
*.iml
*.log
.git
.gitignore
```

### Step 2.5: Deploy Backend to Railway

**Railway Setup:**
1. Install Railway CLI: `npm i -g @railway/cli`
2. Login: `railway login`
3. Initialize: `cd backend && railway init`
4. Link to project: `railway link`

**Configure Environment Variables in Railway:**
```
DATABASE_URL=postgresql://...
DB_USERNAME=postgres
DB_PASSWORD=...
KAFKA_BOOTSTRAP_SERVERS=pkc-xxxxx.confluent.cloud:9092
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=PLAIN
KAFKA_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="..." password="...";
REDIS_HOST=redis-xxxxx.cloud.redislabs.com
REDIS_PORT=12345
REDIS_PASSWORD=...
REDIS_SSL=true
ALLOWED_ORIGINS=https://app.greentrader.org
SPRING_PROFILES_ACTIVE=production
PORT=8080
```

**Deploy:**
1. Railway will auto-detect Dockerfile
2. Or use: `railway up`
3. Get deployment URL (e.g., `matching-engine-production.up.railway.app`)

**For Multiple Servers (Ingress + 3 Matching):**
- Create 4 separate Railway services
- Each with different `SPRING_PROFILES_ACTIVE`:
  - Service 1: `ingress`
  - Service 2: `server1`
  - Service 3: `server2`
  - Service 4: `server3`
- Use Railway's load balancer or set up custom routing

**Alternative: Render**
1. Sign up at [render.com](https://render.com)
2. Create new Web Service
3. Connect GitHub repo
4. Set build command: `cd backend && ./gradlew build`
5. Set start command: `java -jar build/libs/*.jar --spring.profiles.active=production`
6. Add environment variables
7. Deploy

---

## Phase 3: Frontend Deployment (Vercel)

### Step 3.1: Prepare Frontend for Production

**Update `trading-frontend/src/services/api.ts`:**

Replace hardcoded URLs with environment variables:

```typescript
class ApiService {
    private token: string | null = null;

    // Use environment variables
    private defaultBaseUrl = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';
    private ingressBaseUrl = process.env.REACT_APP_INGRESS_BASE_URL || 'http://localhost:8085/api';

    // ... rest of the code
}
```

**Update `trading-frontend/src/services/serverRouter.ts`:**

```typescript
export const SERVER_CONFIGS: ServerConfig[] = [
    {
        port: 8080,
        baseUrl: process.env.REACT_APP_SERVER1_URL || 'http://localhost:8080',
        wsPort: 8080,
        symbols: ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NFLX'],
        kafkaTopics: ['order-aapl', 'order-googl', 'order-msft', 'order-amzn',
            'order-tsla', 'order-meta', 'order-nflx']
    }
    // Add other servers if needed
];
```

**Create `trading-frontend/.env.production`:**
```env
REACT_APP_API_BASE_URL=https://api.greentrader.org/api
REACT_APP_INGRESS_BASE_URL=https://api.greentrader.org/api
REACT_APP_SERVER1_URL=https://api.greentrader.org
REACT_APP_SERVER2_URL=https://api.greentrader.org
REACT_APP_SERVER3_URL=https://api.greentrader.org
```

**Update `trading-frontend/package.json`** (if needed):
```json
{
  "scripts": {
    "build": "react-scripts build",
    "start": "react-scripts start"
  }
}
```

### Step 3.2: Deploy to Vercel

**Option A: Vercel CLI**
1. Install: `npm i -g vercel`
2. Login: `vercel login`
3. Deploy: `cd trading-frontend && vercel --prod`

**Option B: Vercel Dashboard**
1. Sign up at [vercel.com](https://vercel.com)
2. Click "New Project"
3. Import GitHub repository
4. Set root directory: `trading-frontend`
5. Configure build:
   - Build Command: `npm run build`
   - Output Directory: `build`
6. Add Environment Variables:
   ```
   REACT_APP_API_BASE_URL=https://api.greentrader.org/api
   REACT_APP_INGRESS_BASE_URL=https://api.greentrader.org/api
   REACT_APP_SERVER1_URL=https://api.greentrader.org
   REACT_APP_SERVER2_URL=https://api.greentrader.org
   REACT_APP_SERVER3_URL=https://api.greentrader.org
   ```
7. Deploy

### Step 3.3: Configure Custom Domain in Vercel

1. Go to Project Settings → Domains
2. Add domain: `app.greentrader.org`
3. Vercel will provide DNS records
4. Add CNAME record in Cloudflare:
   ```
   Type: CNAME
   Name: app
   Content: cname.vercel-dns.com
   TTL: Auto
   ```
5. Wait for DNS propagation (5-30 minutes)
6. SSL certificate will be auto-provisioned

---

## Phase 4: Environment Configuration

### Step 4.1: Update DNS Records

**In Cloudflare, add/update:**

```
Type    Name    Content                             Proxy
CNAME   app     cname.vercel-dns.com                ✅ Proxied
CNAME   api     your-railway-domain.up.railway.app  ✅ Proxied
A       @       (Vercel IP - optional)              ✅ Proxied
```

**Note:** Use Cloudflare proxy (orange cloud) for:
- DDoS protection
- SSL/TLS encryption
- IP hiding

### Step 4.2: Configure Backend CORS

Update backend `WebConfig.java` or `SecurityConfig.java`:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${spring.web.cors.allowed-origins}")
    private String allowedOrigins;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

### Step 4.3: Set Up Load Balancer (If Multiple Backend Instances)

**Option A: Railway Load Balancer**
- Railway provides built-in load balancing
- Use custom domain: `api.greentrader.org`

**Option B: Cloudflare Load Balancer**
- Requires Cloudflare Pro ($20/month)
- Better for high availability

**Option C: Nginx Reverse Proxy**
- Deploy Nginx on Railway/Render
- Configure upstream servers
- More control, more setup

---

## Phase 5: Database Migration

### Step 5.1: Run Flyway Migrations

**Option A: Automatic (Recommended)**
- Flyway will run on application startup
- Ensure `spring.flyway.enabled=true` in production config

**Option B: Manual**
```bash
# Connect to production database
psql $DATABASE_URL

# Or use Flyway CLI
flyway -url=$DATABASE_URL -user=$DB_USERNAME -password=$DB_PASSWORD migrate
```

### Step 5.2: Verify Database Schema

```sql
-- Check tables exist
\dt

-- Verify schema
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public';
```

---

## Phase 6: Testing & Validation

### Step 6.1: Health Checks

**Backend Health Endpoint:**
```java
@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    @GetMapping
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString()
        );
    }
}
```

**Test:**
```bash
curl https://api.greentrader.org/api/health
```

### Step 6.2: End-to-End Testing

1. **Frontend Access:**
   - Visit `https://app.greentrader.org`
   - Verify page loads
   - Check browser console for errors

2. **API Connectivity:**
- Open browser DevTools → Network tab
- Try logging in
- Verify API calls go to `api.greentrader.org`

3. **Kafka Integration:**
   - Submit a test order
   - Check Kafka consumer logs
   - Verify order processing

4. **Database:**
   - Verify user registration works
   - Check data persists

5. **Redis:**
   - Test caching functionality
   - Verify session management

### Step 6.3: Performance Testing

```bash
# Test API response times
curl -w "@curl-format.txt" -o /dev/null -s https://api.greentrader.org/api/health

# Load testing (install Apache Bench)
ab -n 1000 -c 10 https://api.greentrader.org/api/health
```

---

## Phase 7: Monitoring & Maintenance

### Step 7.1: Set Up Monitoring

**Application Monitoring:**
- **Sentry**: Error tracking (free tier available)
- **LogRocket**: User session replay
- **Datadog**: Full-stack monitoring (paid)

**Infrastructure Monitoring:**
- **Railway**: Built-in metrics
- **Vercel Analytics**: Frontend performance
- **Uptime Robot**: Uptime monitoring (free)

### Step 7.2: Logging

**Backend Logging:**
- Use structured logging (JSON format)
- Send logs to:
  - **Railway Logs**: Built-in
  - **Logtail**: Centralized logging
  - **Papertrail**: Simple log aggregation

**Frontend Logging:**
- Use error boundary
- Send errors to Sentry
- Log API failures

### Step 7.3: Backup Strategy

**Database Backups:**
- Railway: Automatic daily backups
- Supabase: Point-in-time recovery
- Manual: `pg_dump` scheduled job

**Configuration Backups:**
- Store all environment variables in:
  - `.env.example` (without secrets)
  - Password manager (1Password, LastPass)
  - GitHub Secrets (for CI/CD)

### Step 7.4: Security Checklist

- [ ] HTTPS enabled (automatic with Vercel/Cloudflare)
- [ ] Environment variables secured (not in code)
- [ ] Database credentials rotated
- [ ] API rate limiting configured
- [ ] CORS properly configured
- [ ] SQL injection prevention (using JPA)
- [ ] XSS protection (React auto-escapes)
- [ ] CSRF protection (if using sessions)
- [ ] JWT tokens properly secured
- [ ] Secrets management in place

---

## Cost Estimates

### Monthly Costs (Approximate)

| Service | Plan | Monthly Cost |
|---------|------|--------------|
| **Domain** | Namecheap | $1-2 |
| **Cloudflare DNS** | Free | $0 |
| **Vercel** | Hobby (Free) | $0 |
| **Railway** | Starter | $5-20 |
| **PostgreSQL** | Railway/Supabase Free | $0-25 |
| **Kafka** | Confluent Basic | $720 |
| **Kafka** | Upstash (Pay-per-use) | $10-50 |
| **Redis** | Redis Cloud Free | $0 |
| **Redis** | Upstash Free | $0 |
| **Total (with Confluent)** | | **~$750/month** |
| **Total (with Upstash)** | | **~$30-100/month** |

### Cost Optimization Tips

1. **Kafka**: Start with Upstash (pay-per-use) instead of Confluent
2. **PostgreSQL**: Use Supabase free tier (500MB) or Neon free tier
3. **Redis**: Use Upstash free tier (10K commands/day)
4. **Backend**: Use Railway free tier initially (500 hours/month)
5. **Monitoring**: Use free tiers (Sentry, Uptime Robot)

**Minimum Viable Cost: ~$10-30/month**

---

## Troubleshooting

### Common Issues

**1. CORS Errors**
- **Symptom**: Browser blocks API requests
- **Fix**: Update `ALLOWED_ORIGINS` in backend env vars

**2. Database Connection Failed**
- **Symptom**: Application won't start
- **Fix**: Check `DATABASE_URL` format, verify credentials

**3. Kafka Connection Issues**
- **Symptom**: Orders not processing
- **Fix**: Verify `KAFKA_BOOTSTRAP_SERVERS` and credentials

**4. Frontend Can't Reach Backend**
- **Symptom**: API calls fail
- **Fix**: Check `REACT_APP_API_BASE_URL` matches backend domain

**5. DNS Not Propagating**
- **Symptom**: Domain not resolving
- **Fix**: Wait 24-48 hours, check DNS records in Cloudflare

**6. SSL Certificate Issues**
- **Symptom**: HTTPS errors
- **Fix**: Cloudflare/Vercel auto-provisions SSL, wait for activation

---

## Quick Start Checklist

### Week 1: Setup
- [ ] Purchase domain
- [ ] Set up Cloudflare DNS
- [ ] Create accounts (Vercel, Railway, Kafka, Redis)
- [ ] Set up PostgreSQL database
- [ ] Set up Kafka cluster
- [ ] Set up Redis instance

### Week 2: Backend Deployment
- [ ] Create production config files
- [ ] Create Dockerfile
- [ ] Deploy backend to Railway
- [ ] Configure environment variables
- [ ] Test health endpoints
- [ ] Run database migrations

### Week 3: Frontend Deployment
- [ ] Update frontend for environment variables
- [ ] Deploy to Vercel
- [ ] Configure custom domain
- [ ] Update DNS records
- [ ] Test end-to-end

### Week 4: Testing & Optimization
- [ ] Performance testing
- [ ] Security audit
- [ ] Set up monitoring
- [ ] Configure backups
- [ ] Document deployment process

---

## Next Steps

1. **Start with Phase 1**: Purchase domain and set up DNS
2. **Set up infrastructure**: Create accounts and services
3. **Prepare code**: Update configuration files
4. **Deploy incrementally**: Backend first, then frontend
5. **Test thoroughly**: Before going live
6. **Monitor closely**: First few days/weeks

---

## Additional Resources

- [Vercel Deployment Docs](https://vercel.com/docs)
- [Railway Deployment Guide](https://docs.railway.app)
- [Cloudflare DNS Setup](https://developers.cloudflare.com/dns)
- [Confluent Cloud Quick Start](https://docs.confluent.io/cloud/current/get-started/index.html)
- [Redis Cloud Setup](https://redis.com/redis-enterprise-cloud/overview/)

---

**Good luck with your deployment! 🚀**
