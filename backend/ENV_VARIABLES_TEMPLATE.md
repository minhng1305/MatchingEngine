# Environment Variables Template

Copy this template and fill in your actual values. **Never commit actual secrets to Git!**

## Required Environment Variables

### Database
```bash
DATABASE_URL=jdbc:postgresql://host:port/database
DB_USERNAME=postgres
DB_PASSWORD=your_secure_password
```

### Kafka
```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
# For Confluent Cloud:
KAFKA_BOOTSTRAP_SERVERS=pkc-xxxxx.region.provider.confluent.cloud:9092
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=PLAIN
KAFKA_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="your_api_key" password="your_api_secret";
```

### Redis
```bash
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SSL=false
# For Redis Cloud:
REDIS_HOST=redis-xxxxx.cloud.redislabs.com
REDIS_PORT=12345
REDIS_PASSWORD=your_redis_password
REDIS_SSL=true
```

### CORS
```bash
ALLOWED_ORIGINS=https://app.yourdomain.com
# Multiple origins (comma-separated):
ALLOWED_ORIGINS=https://app.yourdomain.com,https://www.yourdomain.com
```

### Server Configuration
```bash
PORT=8080
SPRING_PROFILES_ACTIVE=production
KAFKA_CONSUMER_CONCURRENCY=4
```

## How to Set in Railway

1. Go to your Railway project
2. Click on your service
3. Go to "Variables" tab
4. Add each variable with its value
5. Railway will automatically restart the service

## How to Set in Render

1. Go to your Render service
2. Go to "Environment" tab
3. Add each variable
4. Save and redeploy

## Security Notes

- Never commit `.env` files to Git
- Use secret management tools (1Password, LastPass, etc.)
- Rotate credentials regularly
- Use different credentials for staging and production
