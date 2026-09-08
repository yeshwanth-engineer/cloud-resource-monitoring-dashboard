# Cloud Resource Monitoring Dashboard

A full-stack cloud infrastructure monitoring platform built with Java, Spring Boot, React.js, PostgreSQL, Docker, and AWS CloudWatch.

## Features
- JWT authentication endpoint with an ADMIN demo role
- AWS EC2 resource monitoring through AWS SDK
- CloudWatch CPU utilization history
- REST APIs for cloud resource monitoring
- Responsive React dashboard with interactive charts
- PostgreSQL-ready backend
- Dockerized backend, frontend, and PostgreSQL
- AWS EC2 deployment ready

## Architecture

```text
React Dashboard → Spring Boot REST API → AWS SDK → EC2 / CloudWatch
                         ↓
                    PostgreSQL
```

## Local Run

### Option 1: Docker Compose

Set AWS credentials in your shell environment, then run:

```bash
docker compose up --build
```

Frontend: http://localhost:3000
Backend: http://localhost:8080
Health: http://localhost:8080/api/health

### Option 2: Run separately

Backend:
```bash
cd backend
mvn spring-boot:run
```

Frontend:
```bash
cd frontend
npm install
npm run dev
```

## Authentication

For local demonstration, `POST /api/auth/login` accepts:

```json
{"username":"admin","password":"admin123"}
```

This returns a signed JWT. The demo credential is intentionally simple and must be replaced by database-backed credentials before production use.

## AWS Configuration

The AWS SDK uses the default AWS credential provider chain, so credentials can be supplied through environment variables, AWS CLI configuration, or an IAM role attached to EC2. Never commit credentials.

```text
AWS_REGION=ap-south-1
AWS_ACCESS_KEY_ID=<your-key>
AWS_SECRET_ACCESS_KEY=<your-secret>
JWT_SECRET=<strong-random-secret>
DB_URL=jdbc:postgresql://localhost:5432/cloud_monitor
DB_USERNAME=postgres
DB_PASSWORD=<password>
```

## API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Service health |
| POST | `/api/auth/login` | Generate JWT |
| GET | `/api/monitoring/instances` | List EC2 instances |
| GET | `/api/monitoring/instances/{id}/cpu?hours=3` | CloudWatch CPU history |

## Important Production Notes

The monitoring endpoints are currently open for development. Before a production deployment, add a JWT authentication filter and enforce ADMIN/USER authorities at the API layer. Also replace the demo login with PostgreSQL-backed user accounts.

## License

MIT
