# Phase 2 Completion Guide: Infrastructure Setup

This guide walks you through the **remaining** Phase 2 checklist items. Use it after you’ve completed Phase 1 and the items already checked in Phase 2.

---

## Remaining Phase 2 Items

| Service | Remaining | Status |
|--------|-----------|--------|
| **Database (Supabase)** | Copy connection string, test locally | ⬜ |
| **Kafka (Confluent Cloud)** | Create API keys, copy bootstrap + credentials | ⬜ |
| **Redis (Redis Cloud)** | Copy connection details | ⬜ |

---

## 1. Database (Supabase) — Copy Connection String & Test

### 1.1 Copy Connection String

1. Log in to [Supabase](https://supabase.com) and open your project.
2. Go to **Project Settings** (gear icon in the left sidebar).
3. Click **Database** in the left menu.
4. Under **Connection string**, you’ll see:
   - **URI** (full JDBC-style URL)
   - **Session mode** vs **Transaction mode** — use **Session mode** for typical Spring Boot use.

**What to copy:**

- **Connection string (URI)**  
  ```
  postgresql://postgres:[YOUR-PASSWORD]@db.gqowzkwhievclfezsvnq.supabase.co:5432/postgres
  ```
- **Database password**  
  If you don’t have it, use **Reset database password** in the same page, then store it securely.

**Build `DATABASE_URL` for Spring Boot:**

Spring Boot expects a **JDBC** URL. Convert the Supabase URI like this:

- Supabase URI:  
  `postgresql://user:password@host:port/postgres`
- JDBC URL:  
  `jdbc:postgresql://host:port/postgres`

So:

```text
DATABASE_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:6543/postgres

jdbc:postgresql://db.gqowzkwhievclfezsvnq.supabase.co:5432/postgres?user=postgres&password=[YOUR-PASSWORD]
```

Use the **Session pooler** host and port (often `6543`). Replace `<region>` with your actual region (e.g. `us-east-1`).

**Also note:**

- **DB_USERNAME:** Supabase uses `postgres.[PROJECT-REF]` (e.g. `postgres.abcdefghijk`). You can find it in the URI or in the **Database** settings.
- **DB_PASSWORD:** The password you set or reset.

### 1.2 Test Connection Locally

**Option A: Using `psql` (if installed)**

```bash
psql "postgresql://postgres.[PROJECT-REF]:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:6543/postgres?sslmode=require"
psql "postgresql://postgres.gqowzkwhievclfezsvnq:BAminh1305200@aws-1-ap-southeast-2.pooler.supabase.com:5432/postgres?sslmode=require"

```

Replace `[PROJECT-REF]`, `[PASSWORD]`, and `[REGION]`. If you see a `postgres=#` prompt, the connection works.

**Option B: Using Spring Boot**

1. Create a small `application-local-test.properties` (or use `application-production.properties` with local overrides) **only on your machine** — **do not commit** secrets.
2. Set:
   ```properties
   spring.datasource.url=jdbc:postgresql://<host>:6543/postgres
   spring.datasource.username=postgres.<project-ref>
   spring.datasource.password=<your-password>
   ```
   Add `?sslmode=require` to the URL if Supabase requires SSL.
3. Run your Spring Boot app (or a minimal app that only configures `DataSource`) with the profile that loads these properties.
4. Check logs: no `Connection refused` or `authentication failed` means the connection works.

**Checklist:**

- [x] Copied Supabase connection URI and password.
- [x] Built `DATABASE_URL` (JDBC format) and noted `DB_USERNAME` / `DB_PASSWORD`.
- [x] Tested connection via `psql` **or** Spring Boot.

---

## 2. Kafka (Confluent Cloud) — API Keys & Bootstrap

### 2.1 Create API Keys

1. Log in to [Confluent Cloud](https://confluent.cloud).
2. Select your **environment** and **cluster**.
3. Open **Data integration** → **API keys** (or **Cluster** → **API keys**).
4. Click **Create API key**.
5. Choose **Granular access** (recommended) or **Global access**.
6. **Granular access:**  
   - Create a service account (or use existing).  
   - Grant **CloudClusterAdmin** or at least **Topic read/write** on the cluster (and on `orders`, `orders-dlq` if you use topic-level ACLs).
7. Create the key. You’ll see:
   - **API Key** (username: FR3ALREYF7L5DRFJ)
   - **API Secret** (password: cfltFzyLQgGFGhMXhV1/Azyg+w8olJnFlESKNMPA0A8O84iFfXZBS+uVn0Z+fu8g)  
   **Copy both immediately** — the secret is shown only once.

**Suggested approach:**

- Use **one** API key for **all** producers/consumers (Ingress + Matching servers) if they share the same cluster, **or**
- Use **separate** keys for producer vs consumer (e.g. one for Ingress, one for Matching) if you prefer.  
Either way, you need at least one key for bootstrap + JAAS.

### 2.2 Copy Bootstrap Servers & Credentials

1. In Confluent Cloud, go to your **Cluster**.
2. Open **Cluster overview** (or **Settings**).
3. Find **Bootstrap server**. It looks like:
   ```text
   pkc-817wq.ap-east-1.aws.confluent.cloud:9092
   ```

4. **Credentials** come from the API key you created:
   - **Username** = API Key  
   - **Password** = API Secret  

**Build JAAS config for Spring Boot:**

Your app expects `KAFKA_JAAS_CONFIG`. Format:

```text
org.apache.kafka.common.security.plain.PlainLoginModule required username="<API_KEY>" password="<API_SECRET>";
```

Example:

```text
KAFKA_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="FR3ALREYF7L5DRFJ" password="cfltFzyLQgGFGhMXhV1/Azyg+w8olJnFlESKNMPA0A8O84iFfXZBS+uVn0Z+fu8g";
```

**Values to store (and use later in Phase 3):**

| Variable | Example | Description |
|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `pkc-xxxxx.us-east-1.aws.confluent.cloud:9092` | From cluster overview |
| `KAFKA_SECURITY_PROTOCOL` | `SASL_SSL` | Confluent uses SASL_SSL |
| `KAFKA_SASL_MECHANISM` | `PLAIN` | Confluent cloud default |
| `KAFKA_JAAS_CONFIG` | `org.apache.kafka... username="..." password="...";` | From API key |

**Checklist:**

- [x] Created at least one API key in Confluent Cloud.
- [x] Saved API Key and API Secret securely.
- [x] Copied **Bootstrap server** (host:port).
- [x] Built `KAFKA_JAAS_CONFIG` string with that key/secret.

---


## 3. Redis (Redis Cloud) — Copy Connection Details

### 3.1 Get Host, Port, Password

1. Log in to [Redis Cloud](https://app.redislabs.com).
2. Open your **subscription** and the **database** you created.
3. Go to the **Configuration** / **Connection** section.
4. You’ll see:
   - **Public endpoint** redis-14295.crce264.ap-east-1-1.ec2.cloud.redislabs.com:14295
   - **Port** 14295
   - **Default user** (often `default`)
   - **Password** 9VvgFI2kSu4nrIzHMODrJ0sFj9zw6ds6

**Build connection details:**

| Variable | Where to get it | Example |
|----------|------------------|--------|
| `REDIS_HOST` | Public endpoint hostname | `redis-12345.c123.us-east-1-1.ec2.cloud.redislabs.com` |
| `REDIS_PORT` | Port in endpoint | `12345` |
| `REDIS_PASSWORD` | Database password | From UI or “Edit” database |
| `REDIS_SSL` | Redis Cloud uses TLS | `true` |

**Optional:** If you use a Redis URL instead of separate vars:

```text
rediss://default:<password>@<host>:<port>
```

`rediss` = TLS. Your `application-production.properties` uses host/port/password, so the table above is enough.

### 3.2 Test Connection (Optional)

**Using Redis CLI:**

```bash
redis-cli -u "rediss://default:<REDIS_PASSWORD>@<REDIS_HOST>:<REDIS_PORT>" PING
redis-cli -u redis://default:9VvgFI2kSu4nrIzHMODrJ0sFj9zw6ds6@redis-14295.crce264.ap-east-1-1.ec2.cloud.redislabs.com:14295
```

Expected: `PONG`.

**Checklist:**

- [x] Copied **Public endpoint** (host) and **port**.
- [x] Copied **password**.
- [x] Noted `REDIS_SSL=true` for Redis Cloud.
- [x] (Optional) Verified with `redis-cli` PING.

---

## 4. Where These Go Next (Phase 3)

You will **not** paste these into the app repo. You’ll add them as **environment variables** in your hosting platform (e.g. Railway) when you deploy.

**Suggested secure storage for now:**

- **Password manager** (1Password, Bitwarden, etc.):  
  - Supabase DB password  
  - Confluent API key + secret  
  - Redis password  
- **Local only:**  
  - A `.env.example` or notes file with **placeholders** (no real secrets).  
  - Real values only in env vars or secret manager, never committed.

**Quick reference — values you should have by end of Phase 2:**

```text
# Database (Supabase)
DATABASE_URL=jdbc:postgresql://<host>:6543/postgres
DB_USERNAME=postgres.<project-ref>
DB_PASSWORD=<saved-securely>

# Kafka (Confluent)
KAFKA_BOOTSTRAP_SERVERS=pkc-xxxxx.<region>.aws.confluent.cloud:9092
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=PLAIN
KAFKA_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="<key>" password="<secret>";

# Redis (Redis Cloud)
REDIS_HOST=<redis-cloud-host>
REDIS_PORT=<redis-cloud-port>
REDIS_PASSWORD=<saved-securely>
REDIS_SSL=true
```

---

## 5. Update Your Checklist

After each step, mark the corresponding box in `DEPLOYMENT_CHECKLIST.md`:

- **Database:** `[x]` for “Copy connection string” and “Test connection locally”.
- **Kafka:** `[x]` for “Create API keys” and “Copy bootstrap servers and credentials”.
- **Redis:** `[x]` for “Copy connection details”.

---

## 6. Troubleshooting

**Supabase “connection refused” or “timeout”**

- Use the **pooler** endpoint (port `6543`), not the direct DB port.
- Ensure `sslmode=require` if you add query params.
- Check Supabase **Database** settings for IP allowlist; add your IP if needed.

**Confluent “Authentication failed”**

- Confirm API key is for the **correct** cluster and environment.
- Check ACLs: the key’s principal must have **read/write** on `orders` and `orders-dlq`.

**Redis “Connection reset” or “Unable to connect”**

- Use `rediss://` and `REDIS_SSL=true`.
- Confirm host/port from Redis Cloud **Configuration**.
- Some networks block non‑443 ports; check firewall/VPN.

---

Once all Phase 2 boxes are checked, you’re ready to move on to **Phase 3: Backend Deployment** and plug these values into your Railway (or other) environment variables.
