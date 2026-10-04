# Cloud Resource Monitoring Dashboard

A full-stack dashboard for AWS infrastructure: a Spring Boot REST API that reads the EC2 inventory and
CloudWatch CPU history, a React dashboard that charts it, and Docker images for both.

It ships in two modes. **Demo mode** (the default) serves generated instances and metrics, so you can run
and screenshot the whole product with no AWS account and no credentials. **Live mode** (`DEMO_MODE=false`)
calls `ec2:DescribeInstances` and `cloudwatch:GetMetricData` for real.

```text
React dashboard ──/api──▶ nginx or Vite proxy ──▶ Spring Boot ──▶ AWS SDK ──▶ EC2 / CloudWatch
                                ▲                                            │
                                └────────────── JSON (JWT bearer) ◀──────────┘
```

## Requirements

| Tool    | Version        | Needed for                    |
| ------- | -------------- | ----------------------------- |
| Node    | ≥ 20.19        | frontend, stub API            |
| JDK     | 21             | backend                       |
| Maven   | 3.9+           | backend                       |
| Docker  | any recent     | container run (optional)      |
| AWS CLI | optional       | live mode credentials         |

`make help` lists every task; the rest of this file explains what they do.

## Quickstart

### A. Frontend only — no JVM, no AWS

Uses `frontend/mock-api.mjs`, a dependency-free Node stub that answers the same four endpoints with the
same JSON, so the UI can be developed and demoed anywhere:

```bash
make install      # npm ci in frontend/
make dev-mock     # stub API on :8080 + Vite on :5173
open http://localhost:5173
```

Sign in with `admin` / `admin123`.

### B. Full stack on your machine

```bash
make backend-run  # Spring Boot on :8080 (demo mode)
make frontend-run # Vite on :5173, proxies /api to :8080
```

### C. Docker

```bash
make docker-up    # builds both images, serves the dashboard on http://localhost:3000
make docker-logs
make docker-down
```

nginx inside the frontend container proxies `/api` to the backend, so the browser talks to one origin and
no CORS setup is involved.

## Real AWS data

Export credentials for whatever identity you want to use (env vars, `~/.aws/credentials`, SSO, or an IAM
instance profile on EC2), then flip the switch:

```bash
export AWS_REGION=ap-south-1
export DEMO_MODE=false
make backend-run
```

The identity needs exactly two read-only permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Sid": "ReadInventoryAndMetrics", "Effect": "Allow",
      "Action": ["ec2:DescribeInstances", "cloudwatch:GetMetricData"],
      "Resource": "*" }
  ]
}
```

CPU is read with `GetMetricData` at a 60s period for a 1-hour window, 300s up to 12 hours, and 900s beyond
that, so any window from 1 to 24 hours stays inside CloudWatch's response limits.

If credentials are missing or wrong the API answers **502** with a message naming the likely cause rather
than a stack trace; the dashboard shows that text in its error banner.

## Configuration

Everything is read through Spring Boot properties in `backend/src/main/resources/application.yml`, so each
line is overridable with the matching environment variable — no code edits, and no secrets in the repo.

| Environment variable     | Property                    | Default                                     | Meaning                                        |
| ------------------------ | --------------------------- | ------------------------------------------- | ---------------------------------------------- |
| `PORT`                   | `server.port`               | `8080`                                      | API port                                       |
| `AWS_REGION`             | `app.aws.region`            | `ap-south-1`                                | Region the SDK clients target                  |
| `DEMO_MODE`              | `app.aws.demo-mode`         | `true`                                      | `false` to call AWS                            |
| `JWT_SECRET`             | `app.jwt.secret`            | dev-only placeholder                        | HMAC key, **≥ 32 chars** or startup fails      |
| `JWT_ACCESS_TOKEN_TTL`   | `app.jwt.access-token-ttl`  | `8h`                                        | Token lifetime                                 |
| `DEMO_USERNAME`          | `app.demo-auth.username`    | `admin`                                     | Single demo account                            |
| `DEMO_PASSWORD`          | `app.demo-auth.password`    | `admin123`                                  | …and its password                              |
| `DEMO_ROLE`              | `app.demo-auth.role`        | `ADMIN`                                     | Authority granted (`ROLE_ADMIN`)               |
| `CORS_ALLOWED_ORIGINS`   | `app.cors.allowed-origins`  | `http://localhost:5173,http://localhost:3000` | Only needed for cross-origin calls             |
| `LOG_LEVEL`              | `logging.level.com.yeshwanth.cloudmonitor` | `INFO`             | App log level                                  |

Frontend variables (see `frontend/.env.example`): `VITE_API_BASE` (default `/api`), `VITE_DEV_API_TARGET`,
`VITE_PORT`, `VITE_ALLOWED_HOSTS`.

## API

All responses are JSON. `/api/health` is public; everything under `/api/monitoring` requires
`Authorization: Bearer <jwt>` **and** the `ADMIN` role.

| Method | Path                                     | Purpose                                   |
| ------ | ---------------------------------------- | ----------------------------------------- |
| `GET`  | `/api/health`                            | status, region and mode (public)          |
| `GET`  | `/actuator/health`                       | Spring readiness probe (public)           |
| `POST` | `/api/auth/login`                        | exchange credentials for a JWT            |
| `GET`  | `/api/monitoring/instances`              | EC2 inventory in the configured region    |
| `GET`  | `/api/monitoring/instances/{id}/cpu`     | CloudWatch CPU history, `?hours=1..24`    |

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'content-type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)

curl -s localhost:8080/api/monitoring/instances -H "Authorization: Bearer $TOKEN" | jq
curl -s "localhost:8080/api/monitoring/instances/i-0a1b2c3d4e5f60710/cpu?hours=6" \
  -H "Authorization: Bearer $TOKEN" | jq
```

Login returns `{ "token", "tokenType": "Bearer", "role", "expiresAt" }`.

Instances return `[{ "instanceId", "name", "instanceType", "state", "privateIp", "publicIp", "launchTime" }]`.

A CPU request returns one series, not two parallel arrays — easy to chart, easy to extend to other metrics:

```json
{
  "instanceId": "i-0a1b2c3d4e5f60710",
  "metric": "CPUUtilization",
  "unit": "percent",
  "source": "AWS",
  "periodSeconds": 300,
  "points": [{ "timestamp": "2026-10-04T09:05:00Z", "value": 34.12 }]
}
```

`source` is `AWS` or `DEMO`, which is what the header chip and the "Data source" card read. An instance that
publishes nothing (stopped, or launched minutes ago) returns `points: []`, and the chart explains it instead
of going blank.

Errors share one shape so the UI can show a real message:
`{ "status": 400, "error": "Bad Request", "message": "hours must be between 1 and 24", "timestamp": "…" }`.

## Project layout

```text
backend/
  src/main/java/com/yeshwanth/cloudmonitor/
    config/     AwsConfig, AwsProperties, JwtProperties, DemoAuthProperties, CorsProperties
    security/   JwtService, JwtAuthenticationFilter, SecurityConfig
    service/    Ec2MonitoringService (AWS calls), DemoData (generated series)
    controller/ AuthController, HealthController, MonitoringController
    dto/        InstanceSummary, MetricSeries, MetricPoint
    web/        ApiExceptionHandler, ErrorResponse
  src/main/resources/application.yml
  src/test/java/... (unit + HTTP contract tests)
frontend/
  src/components/  LoginCard, DashboardHeader, SummaryStats, InstanceTable, CpuPanel, Banner, StatusBadge
  src/hooks/       useAuth, useHealth, useMonitoring (instances + CPU series)
  src/api/         client.js (axios instance, token, error mapping)
  src/lib/         format.js
  mock-api.mjs     dev-only stub API
  vite.config.js   /api proxy, host allowlist, build options
  nginx.conf       same-origin /api proxy for the container image
docker-compose.yml backend + frontend
Makefile           every task above
```

## Tests

```bash
make backend-test
```

- `JwtServiceTest` — token round-trip, foreign signature rejected, expired rejected, weak secret fails fast.
- `Ec2MonitoringServiceTest` — demo/live inventory mapping, CloudWatch timestamp+value ziping, empty results,
  period selection.
- `ApiIntegrationTest` — boots the real application context and asserts the HTTP contract: public health,
  401 without a token, 401 on bad credentials, 400 on bad input, and login → instances → CPU in one flow.

Run `make backend-test` after any change to the security chain or the DTO shapes.

## Notes and limitations

- **Single demo account.** `admin` / `admin123` is a hardcoded identity check in `AuthController`, not a
  user store. It exists so the dashboard is demoable; replace it with a `UserDetailsService` and hashed
  passwords before anything real uses this.
- **No database.** The previous revision pulled in `spring-boot-starter-data-jpa` and a PostgreSQL service
  while having zero entities, which made the API fail at startup unless Postgres was running. With no
  persistence to model yet, that wiring is gone. To add it back: re-add `spring-boot-starter-data-jpa` and
  the `postgresql` runtime dependency, put the datasource block back in `application.yml`, and add a `User`
  entity — the JWT filter and role checks are already in place.
- **Tokens live in `localStorage`** for demo simplicity. That is readable by any XSS payload; an httpOnly
  cookie plus CSRF protection is the production-appropriate choice.
- **Demo mode needs no credentials** on purpose: the SDK resolves credentials while *building* a client, so
  `AwsConfig` hands the clients a fixed dummy provider while `app.aws.demo-mode=true`. Without that,
  starting in demo mode on a machine with no AWS config fails before it answers one request.
- **Read-only by design.** Nothing here can start, stop, or modify a resource.

## Troubleshooting

| Symptom                                              | Cause and fix                                                                                                     |
| ---------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `Blocked request. This host (...) is not allowed`     | Vite dev-server guard. Add that hostname to `VITE_ALLOWED_HOSTS` (leading dot allows a whole domain).             |
| Banner: `The API at /api is not reachable`            | Backend down or on another port. `make backend-run`, or set `VITE_DEV_API_TARGET`.                                |
| `401` right after signing in                          | `JWT_SECRET` changed between issuing and verifying, or the token expired.                                          |
| `502 The AWS API request failed`                     | Missing/expired credentials, wrong `AWS_REGION`, or missing IAM actions. Try `aws sts get-caller-identity`.        |
| Startup: `app.jwt.secret must be at least 32 characters` | Set a longer `JWT_SECRET`; the default is a development placeholder.                                             |
| Empty chart for a real instance                      | Expected while `Running` is under ~5 minutes, or when the instance is stopped. Widen the window.                  |
| `make frontend-run` fails on `npm ci`                | Run `make install` first; `package-lock.json` must stay committed.                                                |

## License

MIT — see [LICENSE](LICENSE).
