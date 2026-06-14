# POS Backend

Spring Boot backend for a point-of-sale system with JWT authentication, role-based access control, product inventory management, and checkout processing.

## Features

- User registration and login with JWT tokens
- Role-based access control for `CASHIER`, `MANAGER`, and `ADMIN`
- Product management with barcode lookup and low-stock alerts
- Checkout processing with stock deduction and sale records
- PostgreSQL persistence with JPA/Hibernate
- Spring Boot Actuator for application monitoring

## Tech Stack

- Java 19
- Spring Boot 3.5.15
- Spring Security
- Spring Data JPA
- PostgreSQL
- JJWT
- Lombok

## Prerequisites

- JDK 19
- Maven
- PostgreSQL running locally

## Database Setup

Create a PostgreSQL database named `pos_db` and update the database credentials in `src/main/resources/application.properties` if needed.

Example configuration:

```properties
server.port=8085
spring.datasource.url=jdbc:postgresql://localhost:5432/pos_db
spring.datasource.username=postgres
spring.datasource.password=123
```

## Run the Project

From the project root:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

Build only:

```bash
./mvnw clean compile
```

## Authentication Flow

1. Register a user.
2. Log in with username and password.
3. Copy the returned JWT token.
4. Send the token in the `Authorization` header for protected endpoints.

Example header:

```http
Authorization: Bearer YOUR_JWT_TOKEN
```

## API Endpoints

### Auth

#### Register user
`POST /api/auth/register`

Example body:

```json
{
  "username": "cashier1",
  "password": "123456",
  "roles": ["ROLE_CASHIER"]
}
```

#### Login
`POST /api/auth/login`

Example body:

```json
{
  "username": "cashier1",
  "password": "123456"
}
```

### Products

#### Create product
`POST /api/products`

Allowed roles: `MANAGER`, `ADMIN`

Example body:

```json
{
  "barcode": "1234567890123",
  "sku": "SKU-001",
  "name": "Milk 1L",
  "price": 120.50,
  "stockQuantity": 20,
  "lowStockThreshold": 5
}
```

#### Get all products
`GET /api/products`

Allowed roles: `CASHIER`, `MANAGER`, `ADMIN`

#### Get product by barcode
`GET /api/products/barcode/{barcode}`

Allowed roles: `CASHIER`, `MANAGER`, `ADMIN`

#### Get low-stock products
`GET /api/products/low-stock`

Allowed roles: `MANAGER`, `ADMIN`

### Sales

#### Checkout
`POST /api/sales/checkout`

Allowed roles: authenticated users with a valid JWT

Example body:

```json
{
  "paymentMethod": "CASH",
  "discountAmount": 10.00,
  "amountPaid": 500.00,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2,
      "quantity": 1
    }
  ]
}
```

## Roles

- `ROLE_CASHIER`
- `ROLE_MANAGER`
- `ROLE_ADMIN`

## Notes

- The app runs on port `8085` by default.
- Product stock is reduced automatically during checkout.
- Low-stock products are determined by comparing `stockQuantity` with `lowStockThreshold`.
- If you change the database settings, reload the Maven project in your IDE so dependencies and config refresh correctly.

## Project Structure

```text
src/main/java/com/pos/backend
├── controller
├── dto
├── entity
├── exception
├── repository
├── security
└── service
```

## License

No license has been added yet.
