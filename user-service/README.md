# User Service

Spring Boot-based user management service for the quant trading platform.

## Features

- User registration and authentication
- JWT-based session management
- User profile management
- Trading account settings
- Risk management configurations
- RESTful APIs for user operations

## Technology Stack

- **Spring Boot 3.1.5** - Application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database access
- **MySQL** - Primary database
- **Redis** - Session storage and caching
- **Apache Dubbo** - Microservices communication
- **Apache Kafka** - Message streaming
- **JWT** - Token-based authentication

## API Endpoints

### Authentication
- `POST /api/v1/users/register` - User registration
- `POST /api/v1/users/login` - User login
- `POST /api/v1/users/logout` - User logout
- `POST /api/v1/users/refresh` - Refresh JWT token

### User Management
- `GET /api/v1/users/profile` - Get current user profile
- `PUT /api/v1/users/profile` - Update user profile
- `GET /api/v1/users/check-username` - Check username availability
- `GET /api/v1/users/check-email` - Check email availability

## Database Schema

### Users Table
- Basic user information (username, email, password)
- Account balance and trading settings
- Risk management parameters
- User status and permissions

### User Sessions Table
- JWT session tracking
- Device and IP information
- Session expiration management

## Setup

1. Ensure MySQL and Redis are running
2. Configure database connection in `application.properties`
3. Run the application:

```bash
cd user-service
mvn spring-boot:run
```

## Configuration

Key configuration properties:
- Database connection settings
- JWT secret and expiration
- Dubbo service registration
- Kafka message configuration
- Redis connection settings