# Coworking Hub

Full-stack web application for coworking space management, built with Angular, Spring Boot, and MySQL.
It supports role-based workflows for members, managers, and administrators, including authentication, space listing, reservations, and basic reporting.

## Tech Stack
- **Frontend:** Angular 20
- **Backend:** Spring Boot (Java 25), Maven
- **Database:** MySQL 8
- **Containerization:** Docker + Docker Compose

## What is included
- Role-based flows for **member**, **manager**, and **admin**
- Authentication with JWT
- Space browsing, details, and reservations
- Manager space/calendar management
- Admin user/approval and reporting features
- Image uploads for profiles and spaces

## Quick Start (Docker)
From the project root:

```bash
docker compose up --build
```

Services:
- Frontend: `http://localhost:4200`
- Backend API: `http://localhost:8080/api`
- MySQL: `localhost:3307` (internal container port `3306`)

The database schema and seed data are loaded automatically from:
- `backend/baza/schema.sql`

## Default Test Accounts
Seed data includes ready-to-use users:
- Admin: `admin` / `Admin123!`
- Member examples: `ana`, `marko`, `maja`, `petar` / `User123!`
- Manager examples: `menadzer_beograd`, `menadzer_novisad`, `menadzer_nis` / `User123!`

## Local Development (without Docker)
### Backend
```bash
cd backend/app
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm start
```

## Documentation
- API specification: `docs/REST.md`
