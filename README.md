# Shopping Platform Backend

## Project Overview

This project implements the backend for a simplified **online shopping platform** using a microservices architecture.  
The system allows users to create accounts, browse items, create orders, and process payments.

The platform is composed of **four main services**:

- **Account Service**
- **Item Service**
- **Order Service**
- **Payment Service**

Each service is implemented using **Spring Boot** and communicates through both **synchronous REST APIs** and **asynchronous Kafka events**.

---

# Architecture
           +------------------+
            |   Account Service |
            |  PostgreSQL DB    |
            +---------+---------+
                      |
                      | REST
                      v
+———––+    REST    +———––+
| Item Service | <––––> | Order Service |
| MongoDB      |            | Cassandra     |
+——+—––+            +——+––––+
|                           |
|                           | Kafka Events
|                           v
|                     +———––+
+––––––––––> Payment Service |
| Kafka Producer |
+––––––––+
---

# Tech Stack

- **Java 17**
- **Spring Boot**
- **Spring Data JPA**
- **Spring Data MongoDB**
- **Spring Data Cassandra**
- **Spring Cloud (REST communication)**
- **Kafka**
- **JUnit / Mockito**
- **Maven**
- **Docker**

---

# Databases

The project uses **multiple databases** as required.

| Service | Database | Purpose |
|------|------|------|
| Account Service | PostgreSQL | User accounts and authentication |
| Item Service | MongoDB | Item metadata and inventory |
| Order Service | Cassandra | Order data and state |
| Payment Service | In-memory / Kafka | Payment processing |

---

# Inter-Service Communication

## Synchronous Communication

Implemented using **RestTemplate**.

Example:
Order Service -> Item Service
GET /items/{id}
Used when creating orders to verify item existence.

---

## Asynchronous Communication

Implemented using **Kafka**.

### Topics
order-created
payment-processed
payment-refunded
Example event flow:
Order Created
|
v
Kafka Event (order-created)
|
v
Payment Service processes payment
|
v
Kafka Event (payment-processed)
|
v
Order Service updates order state
---

# Services

## Account Service

Handles user accounts.

### APIs
POST   /accounts           Create account
GET    /accounts           List accounts
GET    /accounts/{id}      Account lookup
PUT    /accounts/{id}      Update account
DELETE /accounts/{id}      Delete account
GET    /accounts/{id}/items
Stored in **PostgreSQL**.

---

## Item Service

Stores item metadata and inventory.

### Fields
id
name
price
inventory
upc
imageUrl
### APIs
POST   /items
GET    /items
GET    /items/{id}
DELETE /items/{id}
Stored in **MongoDB**.

---

## Order Service

Handles order lifecycle and state.

### Order States
CREATED
PAID
CANCELLED
REFUNDED
### APIs
POST   /orders
GET    /orders
GET    /orders/{id}
PUT    /orders/{id}
DELETE /orders/{id}
Stored in **Cassandra**.

Order service also publishes **Kafka events**.

---

## Payment Service

Handles payment processing and refunds.

### APIs
POST /payments        Submit payment
PUT  /payments/{id}   Update payment
POST /payments/{id}/refund
GET  /payments/{id}
### Idempotency

Payment operations guarantee **idempotency** to prevent double charging or double refunds.

Payment results are published to **Kafka topics**.

---

# Authentication

Authentication is implemented through the **Account Service**.

After login, the server generates a **token**.  
This token must be included in request headers for protected endpoints.

Example:
Authorization: Bearer
---

# How to Run

## Start Dependencies

Make sure the following services are running:
PostgreSQL
MongoDB
Cassandra
Kafka
Zookeeper
You can start them using Docker:
docker compose up
---

## Start Services

Each service can be started independently.

Example:
cd account-service
mvn spring-boot:run
Repeat for:
item-service
order-service
payment-service
---

# Demo API Flow

Example workflow for creating an order and processing payment.

---

## 1. Create Account
POST /accounts
Example:
{
“email”: “user@test.com”,
“displayName”: “Test User”,
“password”: “123456”
}
---

## 2. Create Item
POST /items
{
“name”: “Keyboard”,
“priceCents”: 5000
}
---

## 3. Create Order
POST /orders
{
“accountId”: 1,
“itemId”: “ITEM_ID”,
“quantity”: 1,
“priceCents”: 5000
}
Order Service performs a **synchronous call** to Item Service to verify the item.

---

## 4. Submit Payment
POST /payments
Payment Service processes payment and publishes Kafka event.

---

## 5. Order Status Update

Order Service consumes the payment event and updates order state.

---

# Unit Testing

Each service includes **JUnit tests** with Mockito.

Examples:
ItemControllerTest
AccountControllerTest
OrderControllerTest
PaymentControllerTest
Run tests:
mvn test
Coverage satisfies the **30% requirement** specified in the project description.

---

# Project Structure
shopping-platform
│
├── account-service
├── item-service
├── order-service
├── payment-service
│
└── docker-compose.yml
---

# Author

Angelina Liang