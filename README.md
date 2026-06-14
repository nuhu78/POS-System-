# POS Backend

This is the backend for a simple point-of-sale (POS) application implemented with Spring Boot. It provides user authentication (JWT), role-based authorization, product inventory management, and checkout processing.

This README reflects the project's current code and configuration.

---

## Quick facts

- Default HTTP port: `8085` (configurable via `SERVER_PORT` or `server.port`)
- Primary persistence: PostgreSQL (database settings in `src/main/resources/application.properties`)
- In-memory H2 is available as a runtime dependency for tests/local fallback

## Tech stack

- Java 19
- Spring Boot 3.5.15
- Spring Security + JWT (JJWT)
- Spring Data JPA (Hibernate)
- PostgreSQL (primary), H2 (runtime/test)
- Lombok

## Configuration

Edit `src/main/resources/application.properties` to configure the server port and database connection. The current file contains:

```properties
server.port=8085
spring.datasource.url=jdbc:postgresql://localhost:5432/pos_db
spring.datasource.username=postgres
spring.datasource.password=123
spring.jpa.hibernate.ddl-auto=update
```

Note: If you prefer to run with an embedded H2 database for quick local testing, set `SPRING_DATASOURCE_URL` environment variable to an H2 JDBC URL, or modify `application.properties` accordingly.

## Run

From the project root:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
mvnw.cmd spring-boot:run
```

Build only:

```bash
./mvnw clean package
```

If you change `pom.xml`, refresh Maven dependencies in your IDE (e.g., "Maven: Reload Project" in VS Code).

## Authentication and roles

Users register with `POST /api/auth/register` and authenticate with `POST /api/auth/login`. Successful login returns a JWT token in the response body (see `AuthResponse`). Include that token in the `Authorization` header as `Bearer <token>` for protected endpoints.

Roles used in the project:

- `ROLE_CASHIER`
- `ROLE_MANAGER`
- `ROLE_ADMIN`

Authorization summary (from `SecurityConfig`):

- `POST /api/products/**`, `PUT /api/products/**` — allowed for `MANAGER`, `ADMIN`
- `DELETE /api/products/**` — allowed for `ADMIN` only
- `GET /api/products/**` — allowed for `CASHIER`, `MANAGER`, `ADMIN`
- `GET /api/products/low-stock` — allowed for `MANAGER`, `ADMIN`

## REST API (summary)

All API paths are prefixed with `/api`.

### Auth

- `POST /api/auth/register` — Register a user. Body: `{ username, password, roles }` where `roles` is an array of strings like `"ROLE_CASHIER"`.
- `POST /api/auth/login` — Login, body `{ username, password }`. Returns `{ token, username, roles }`.

### Products

- `POST /api/products` — Create a product (MANAGER, ADMIN).
- `GET /api/products` — List products (CASHIER, MANAGER, ADMIN).
- `GET /api/products/barcode/{barcode}` — Find product by barcode (CASHIER, MANAGER, ADMIN).
- `GET /api/products/low-stock` — Low-stock alerts (MANAGER, ADMIN).

### Reports

- `GET /api/reports/summary` — Sales summary for a date range. Requires authentication.

Query parameters:

- `startDate` (required) — start date in `YYYY-MM-DD` format
- `endDate` (required) — end date in `YYYY-MM-DD` format

Example request:

```
GET http://localhost:8085/api/reports/summary?startDate=2026-06-01&endDate=2026-06-30
Authorization: Bearer <JWT>
```

Response shape (`SalesReportSummary`):

```json
{
  "reportingPeriod": "2026-06-01 to 2026-06-30",
  "totalRevenue": 12345.67,
  "totalTransactionCount": 256,
  "averageOrderValue": 48.22,
  "paymentMethodBreakdown": {
    "CASH": 8000.00,
    "CARD": 4345.67
  }
}
```

Notes:

- Dates are parsed as ISO dates and the service expands them to cover full days internally (start of day to end of day).
- The endpoint is not explicitly role-restricted in `SecurityConfig`, so any authenticated user may call it; adjust `SecurityConfig` if you want to restrict to `MANAGER`/`ADMIN` only.

Product JSON shape (entity `Product`):

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

### Sales

- `POST /api/sales/checkout` — Process a checkout. The currently authenticated user's username is recorded as the cashier.

Checkout request shape (`CheckoutRequest`):

```json
{
  "paymentMethod": "CASH",
  "discountAmount": 10.00,
  "amountPaid": 500.00,
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ]
}
```

The checkout flow:

- Verifies requested quantities against available `stockQuantity`.
- Deducts sold quantities from inventory.
- Calculates invoice (`subTotal`, `discountAmount`, `grandTotal`, `amountPaid`, `changeAmount`).
- Persists `Sale` and associated `SaleItem` records.

## Development notes

- JWT utilities live in `src/main/java/com/pos/backend/security` (`JwtUtils`, `JwtFilter`, `SecurityConfig`).
- User accounts include a `roles` set that stores role strings like `ROLE_CASHIER`.
- If you see `403 Forbidden` when calling `/api/products/low-stock`, it is because the endpoint is restricted to `MANAGER` and `ADMIN` roles.

## Project layout

```
src/main/java/com/pos/backend
├── controller   # REST controllers (Auth, Product, Sale)
├── dto          # Request/response DTOs
├── entity       # JPA entities (User, Product, Sale, SaleItem)
├── repository   # Spring Data JPA repositories
├── security     # JWT and Spring Security config
└── service      # Business logic (ProductService, SaleService)
```

## Next steps / suggestions

- Add `curl` examples to this README.
- Add a LICENSE file and CI pipeline (GitHub Actions) to run `mvn -DskipTests=false test`.

---

If you want, I can:

- Add `curl` examples to this README.
- Commit the README update and push to the remote (I can run `git commit -m "docs: update README" && git push`).
- Add badges (build, license) and a short intro for the repository landing page.
