# Shopping Platform Template

A simple microservices demo built with **Spring Boot**, **PostgreSQL**, and **Docker Compose**.

This project demonstrates a basic service architecture with two services:

- **Account Service** – manages user accounts
- **Item Service** – manages items and provides item data to Account Service

The services communicate via REST API.

---

# Architecture
Account Service (9001) → calls → Item Service (9002)
Item Service → uses → PostgreSQL (Docker)


---

# Prerequisites

Make sure the following tools are installed:

- Java 17
- Maven (or use the provided mvnw wrapper)
- Docker
- Docker Compose
- Postman (for API testing)

---

# Ports Used

| Service | Port |
|------|------|
| Account Service | 9001 |
| Item Service | 9002 |
| PostgreSQL | 5433 |

---

# Start the Environment

From the project root:

docker compose up -d postgres


Check container status:


docker compose ps

---

# Run the Services

Start the services separately.

### Start Account Service


cd account-service
../auth-service/mvnw.cmd -DskipTests spring-boot:run


### Start Item Service


cd item-service
../auth-service/mvnw.cmd -DskipTests spring-boot:run


---

# Health Check

Verify both services are running.

Account Service


GET http://localhost:9001/actuator/health


Item Service


GET http://localhost:9002/actuator/health


Expected response:


{
"status": "UP"
}


---

# API Test Flow (Postman)

Recommended test order:

### 1 Create Item


POST http://localhost:9002/items


Body


{
"name": "Apple",
"priceCents": 199
}


---

### 2 Get All Items


GET http://localhost:9002/items


---

### 3 Create Account


POST http://localhost:9001/accounts


Body


{
"email": "x@x.com
",
"displayName": "X"
}


Expected: **201 Created**

---

### 4 Duplicate Account (Error Test)

Send the same request again.

Expected:


409 Conflict


Response example:


{
"timestamp": "...",
"status": 409,
"error": "Conflict",
"message": "email already exists",
"path": "/accounts"
}


---

### 5 Get Accounts


GET http://localhost:9001/accounts


---

### 6 Get Items by Account


GET http://localhost:9001/accounts/{id}/items


This endpoint demonstrates **service-to-service communication** where Account Service calls Item Service.

---

### 7 Delete Account


DELETE http://localhost:9001/accounts/{id}


Expected: **204 No Content**

---

# Stop the Environment

Stop the Spring Boot services:


Ctrl + C


Stop Docker containers:


docker compose stop


---

# Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Docker Compose
- REST APIs

---

# Future Improvements

- Service discovery
- API gateway
- Authentication / authorization
- OpenAPI documentation
- Integration tests