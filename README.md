# 🏨 Hotel Management System (HMS) - Backend

<div align="center">

![Java](https://img.shields.io/badge/Java-17+-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-green?logo=spring-boot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue?logo=docker)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Coverage](https://img.shields.io/badge/Coverage-85%25-brightgreen)

**A comprehensive RESTful API for hotel operations including bookings, rooms, customers, staff management, invoicing, and reporting.**

[![Postman Collection](https://img.shields.io/badge/Postman-Collection-orange?logo=postman)](https://magdumom-ml-5959315.postman.co/workspace/6e8884c4-ed53-4c75-b0c1-d3e1d79b9d11)

</div>

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [API Documentation](#-api-documentation)
- [Authentication & Authorization](#-authentication--authorization)
- [Database Schema](#-database-schema)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

### Core Functionality
| Module | Features |
|--------|----------|
| **🔐 Authentication** | JWT-based authentication, role-based access control (ADMIN, MANAGER, RECEPTIONIST) |
| **📅 Bookings** | Create, update, cancel bookings with conflict detection and date validation |
| **🛏️ Rooms** | Room CRUD operations, availability checking, status management (AVAILABLE, OCCUPIED, MAINTENANCE) |
| **👥 Customers** | Customer management with duplicate email/phone validation |
| **👨‍💼 Staff** | Staff management with role assignment and password encryption |
| **📊 Dashboard** | Real-time counters, occupancy distribution, revenue charts, recent activity |
| **📄 Invoicing** | PDF invoice generation, email delivery with attachments |
| **📈 Reports** | Daily revenue, monthly occupancy, customer history (PDF/Excel export) |

### Key Highlights
- ✅ **Concurrency Control** - Pessimistic locking prevents double bookings
- ✅ **Validation** - Comprehensive input validation at API boundary
- ✅ **Error Handling** - Centralized exception handling with consistent error responses
- ✅ **Async Processing** - Non-blocking email sending for invoices
- ✅ **Multi-Environment** - Separate configurations for dev, test, and production
- ✅ **Containerized** - Docker & Docker Compose support for easy deployment
- ✅ **Test Coverage** - 85% code coverage with unit & integration tests

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Java 17+ |
| **Framework** | Spring Boot 3.5.10 |
| **Security** | Spring Security + JWT (jjwt) |
| **Database** | MySQL 8.0 / H2 (testing) |
| **ORM** | Spring Data JPA + Hibernate |
| **Build Tool** | Maven |
| **Containerization** | Docker, Docker Compose |
| **Reporting** | Apache POI (Excel), OpenPDF/iText (PDF) |
| **Email** | Spring Boot Mail (SMTP) |
| **Testing** | JUnit 5, Mockito, AssertJ |
| **Code Quality** | JaCoCo (85% coverage) |
| **Utilities** | Lombok, ModelMapper |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (Frontend)                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway / Load Balancer                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application (Port 8080)           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ Controllers │  │   Security  │  │   Exception Handler     │  │
│  │  (REST API) │  │  (JWT Filter)│  │   (@ControllerAdvice)   │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
│                              │                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │   Services  │  │    DTOs     │  │   Configuration         │  │
│  │ (Business   │  │ (Data Transfer)│  │   (YAML Profiles)     │  │
│  │   Logic)    │  │             │  │                         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
│                              │                                   │
│  ┌─────────────┐  ┌─────────────┐                                │
│  │ Repositories│  │  Entities   │                                │
│  │  (JPA)      │  │   (JPA)     │                                │
│  └─────────────┘  └─────────────┘                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      MySQL Database (Port 3306)                  │
│  Tables: bookings, customers, rooms, staff                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites

- **Java** 17 or higher
- **Maven** 3.6+
- **MySQL** 8.0+ (or use Docker)
- **Docker** & **Docker Compose** (optional, recommended)

### Option 1: Docker Compose (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd hotel-management-system

# Create .env file with your secrets
cp .env.example .env
# Edit .env with your values (see Configuration section)

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

### Option 2: Local Development

```bash
# Clone the repository
git clone <repository-url>
cd hotel-management-system

# Configure environment variables
export MYSQL_PASSWORD=your_password
export MAIL_ID=your_email@example.com
export MAIL_PASSWORD=your_app_password
export JWT_SECRET=your_secret_key_at_least_32_characters

# Run the application
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/hms-0.0.1-SNAPSHOT.jar
```

### Default Admin Credentials

On first startup, a default admin user is created:

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | ADMIN |

> ⚠️ **Important:** Change the default password immediately in production!

---

## ⚙️ Configuration

### Environment Variables

Create a `.env` file in the project root:

```bash
# Database
MYSQL_PASSWORD=your_secure_password

# Email (SMTP)
MAIL_ID=your_email@example.com
MAIL_PASSWORD=your_app_specific_password

# Security
JWT_SECRET=your_secret_key_minimum_32_characters_long

# Frontend URLs (comma-separated for production)
FRONTEND_URLS=https://your-frontend-domain.com

# Database URL (for production)
SPRING_DATASOURCE_URL=jdbc:mysql://your-host:3306/hotel_management
```

### Application Profiles

| Profile | File | Purpose |
|---------|------|---------|
| `default` | `application.yaml` | Development (ddl-auto=update, verbose logging) |
| `prod` | `application-prod.yaml` | Production (ddl-auto=validate, connection pooling) |
| `test` | `application-test.yaml` | Testing (H2 in-memory database) |

---

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Postman Collection
Import the collection to test all endpoints:

[![Open in Postman](https://img.shields.io/badge/Open%20in-Postman-orange?logo=postman)](https://magdumom-ml-5959315.postman.co/workspace/6e8884c4-ed53-4c75-b0c1-d3e1d79b9d11)

### API Endpoints Overview

| Module | Endpoint | Methods | Access |
|--------|----------|---------|--------|
| **Auth** | `/api/auth/login` | POST | Public |
| **Bookings** | `/api/bookings/**` | GET, POST, PUT | ADMIN, MANAGER, RECEPTIONIST |
| **Customers** | `/api/customers/**` | GET, POST, PUT, DELETE | ADMIN, MANAGER, RECEPTIONIST |
| **Rooms** | `/api/rooms/**` | GET, POST, PUT, DELETE | ADMIN, RECEPTIONIST |
| **Rooms (Public)** | `/api/rooms/available` | GET | Public |
| **Staff** | `/api/staff/**` | GET, POST, PUT, DELETE | ADMIN only |
| **Dashboard** | `/api/dashboard/**` | GET | ADMIN, MANAGER |
| **Invoice** | `/api/invoice/**` | GET, POST | ADMIN, MANAGER, RECEPTIONIST |
| **Reports** | `/api/reports/**` | GET | ADMIN, MANAGER |

### Response Format

All responses follow a consistent structure:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... }
}
```

Error Response:
```json
{
  "timestamp": "2026-03-04T10:30:00",
  "message": "Resource not found",
  "details": "/api/bookings/999",
  "status": 404
}
```

---

## 🔐 Authentication & Authorization

### JWT Token Flow

1. **Login** - Send credentials to `/api/auth/login`
2. **Receive Token** - Get JWT token in response
3. **Include Token** - Add `Authorization: Bearer <token>` header to subsequent requests
4. **Token Validation** - Filter validates token on each request

### Role-Based Access Control

| Role | Permissions |
|------|-------------|
| **ADMIN** | Full access to all endpoints |
| **MANAGER** | Dashboard, Reports, Bookings, Customers, Invoices |
| **RECEPTIONIST** | Bookings, Customers, Rooms, Invoices |

### Example Request

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Use token in subsequent requests
curl -X GET http://localhost:8080/api/bookings \
  -H "Authorization: Bearer <your_token_here>"
```

---

## 🗄️ Database Schema

### Entity Relationship Diagram

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   Customer   │       │   Booking    │       │     Room     │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ customerId   │◄──────│ customerId   │       │ roomId       │
│ name         │       │ roomId       │──────►│ roomNumber   │
│ email        │       │ checkIn      │       │ roomType     │
│ phone        │       │ checkOut     │       │ price        │
│ address      │       │ totalAmount  │       │ status       │
│ createdAt    │       │ status       │       │ createdAt    │
│ updatedAt    │       │ createdAt    │       │ updatedAt    │
└──────────────┘       │ updatedAt    │       └──────────────┘
                       └──────────────┘

┌──────────────┐
│     Staff    │
├──────────────┤
│ staffId      │
│ username     │
│ password     │
│ name         │
│ role         │
│ contact      │
│ salary       │
│ enabled      │
│ createdAt    │
│ updatedAt    │
└──────────────┘
```

### Tables

| Table | Description |
|-------|-------------|
| `customers` | Guest information with unique email/phone |
| `rooms` | Hotel rooms with type, price, and status |
| `bookings` | Reservations linking customers to rooms |
| `staff` | System users with roles and credentials |

---

## 🧪 Testing

### Run All Tests

```bash
# Run tests with coverage report
mvn clean verify

# View coverage report
open target/site/jacoco/index.html
```

### Test Coverage

| Layer | Coverage |
|-------|----------|
| **Overall** | 85% |
| **Service Layer** | 86% |
| **Security/JWT** | 95% |
| **Controller Layer** | 76% |
| **Configuration** | 100% |

### Test Types

| Type | Location | Description |
|------|----------|-------------|
| **Unit Tests** | `src/test/java/.../service/` | Mocked repository tests |
| **Integration Tests** | `src/test/java/.../integration/` | Full HTTP endpoint tests with H2 |
| **Security Tests** | `src/test/java/.../security/` | JWT and authentication tests |

---

## 🚢 Deployment

### Docker Deployment

```bash
# Build the image
docker build -t hms-backend:latest .

# Run with Docker Compose
docker-compose -f docker-compose.yml up -d
```

### Production Checklist

- [ ] Set strong `JWT_SECRET` (minimum 32 characters)
- [ ] Configure `FRONTEND_URLS` with your domain(s)
- [ ] Use production database (not H2)
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Change default admin password
- [ ] Configure SSL/TLS for production
- [ ] Set up monitoring and logging

### Live Demo

> 🚧 **Coming Soon** - A live deployment will be available at:
> ```
> https://api.hms-demo.com
> ```

---

## 📁 Project Structure

```
hotel-management-system/
├── src/
│   ├── main/
│   │   ├── java/com/vinayakit/hms/
│   │   │   ├── config/          # Security, CORS, Beans
│   │   │   ├── controller/      # REST API endpoints
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA Entities
│   │   │   ├── exception/       # Custom exceptions & handler
│   │   │   ├── repository/      # Spring Data JPA repositories
│   │   │   ├── security/        # JWT, UserDetailsService, Filter
│   │   │   ├── service/         # Business logic
│   │   │   └── HmsApplication.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-prod.yaml
│   │       └── application-test.yaml
│   └── test/
│       └── java/com/vinayakit/hms/
│           ├── integration/     # Integration tests
│           ├── security/        # Security unit tests
│           └── service/         # Service unit tests
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── .env.example
└── README.md
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Guidelines

- Follow existing code style
- Write tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2026 Hotel Management System

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

---

## 📞 Support

For issues, questions, or contributions:

- 🐛 **Bug Reports:** [Create an Issue](../../issues)
- 💡 **Feature Requests:** [Create an Issue](../../issues)
- 📧 **Contact:** magdumom.ml@gmail.com

---

<div align="center">

**Made with ❤️ using Spring Boot**

⭐ **Star this repository if you find it helpful!**

</div>
