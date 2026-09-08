# Cloud Resource Monitoring Dashboard

A full-stack cloud infrastructure monitoring platform built with Java, Spring Boot, React.js, PostgreSQL, Docker, and AWS CloudWatch.

## Features
- JWT authentication and role-based authorization
- AWS EC2 resource monitoring
- CloudWatch metrics for CPU utilization, network traffic, and instance status
- REST APIs for monitoring and resource management
- Responsive React dashboard with charts and analytics
- PostgreSQL persistence
- Dockerized deployment
- AWS EC2 deployment ready

## Architecture

React Dashboard → Spring Boot REST API → AWS SDK / CloudWatch → EC2
                         ↓
                    PostgreSQL

## Project Structure

```text
backend/     Spring Boot REST API
frontend/    React dashboard
docs/        Architecture and deployment documentation
```

## Configuration

Set AWS credentials through environment variables or an IAM role. Never commit credentials to Git.

```text
AWS_REGION=ap-south-1
AWS_ACCESS_KEY_ID=<your-key>
AWS_SECRET_ACCESS_KEY=<your-secret>
JWT_SECRET=<strong-secret>
DB_URL=jdbc:postgresql://localhost:5432/cloud_monitor
DB_USERNAME=postgres
DB_PASSWORD=<password>
```

## Status

Initial project scaffold. Backend and frontend implementation will be added incrementally.

## License

MIT
