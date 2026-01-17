# Enterprise Microservice Architecture

A **production-grade two-microservice system** with deep observability, clean architecture, and enterprise-style logging. Built with Java 21, Spring Boot 3.2.1, and designed for AWS EC2 + RDS deployment.

![Architecture](https://img.shields.io/badge/Architecture-Microservices-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-green)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Communication Patterns](#communication-patterns)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Observability](#observability)
- [Deployment](#deployment)
- [Shell Scripts](#shell-scripts)
- [Testing](#testing)
- [Contributing](#contributing)

## 🎯 Overview

This project implements a **shop-to-stock microservice architecture** consisting of:

| Service | Port | Description |
|---------|------|-------------|
| **Shop-Management** | 8081 | Manages shops, orders, and orchestrates stock operations |
| **Product-Stock** | 8082 | Manages products, stock levels, and reservations |

### Key Features

- ✅ **Multi-Protocol Communication**: REST (primary), SOAP (stock availability), GraphQL (cross-service queries)
- ✅ **Shared Database with Schema Isolation**: One RDS instance, separate schemas
- ✅ **Enterprise Logging**: Structured JSON logs with correlation IDs
- ✅ **Clean Architecture**: Layered design with clear separation of concerns
- ✅ **Production-Ready**: Connection pooling, health checks, graceful shutdown
- ✅ **Infrastructure as Code**: Terraform for AWS deployment
- ✅ **Shell-Script Driven**: Easy build, deploy, and manage operations

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTS                                         │
│                    (Web App, Mobile App, API Consumers)                      │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     APPLICATION LOAD BALANCER                                │
│                   (Path-based routing: /shop/*, /product/*)                  │
└──────────────────────┬──────────────────────────┬───────────────────────────┘
                       │                          │
                       ▼                          ▼
┌──────────────────────────────────┐  ┌──────────────────────────────────────┐
│     SHOP-MANAGEMENT SERVICE      │  │      PRODUCT-STOCK SERVICE           │
│          (Port 8081)             │  │          (Port 8082)                 │
│                                  │  │                                      │
│  ┌────────────────────────────┐  │  │  ┌──────────────────────────────┐   │
│  │     REST Controllers       │  │  │  │      REST Controllers        │   │
│  │  • ShopController          │◄─┼──┼──┤  • ProductController         │   │
│  │  • OrderController         │  │  │  │  • StockController           │   │
│  └────────────────────────────┘  │  │  └──────────────────────────────┘   │
│                                  │  │                                      │
│  ┌────────────────────────────┐  │  │  ┌──────────────────────────────┐   │
│  │   GraphQL Controller       │  │  │  │    SOAP Endpoint             │   │
│  │  • shopWithStock query     │──┼──┼──┤  • StockAvailabilityEndpoint │   │
│  └────────────────────────────┘  │  │  └──────────────────────────────┘   │
│                                  │  │                                      │
│  ┌────────────────────────────┐  │  │  ┌──────────────────────────────┐   │
│  │    Service Layer           │  │  │  │    GraphQL Controller        │   │
│  │  • ShopService             │  │  │  │  • productWithStock query    │   │
│  │  • OrderService            │  │  │  │  • checkAvailability query   │   │
│  │  • ProductServiceClient    │──┼──┼──►  • createProduct mutation    │   │
│  └────────────────────────────┘  │  │  └──────────────────────────────┘   │
│                                  │  │                                      │
│  ┌────────────────────────────┐  │  │  ┌──────────────────────────────┐   │
│  │    Repository Layer        │  │  │  │    Service Layer             │   │
│  │  • ShopRepository          │  │  │  │  • ProductService            │   │
│  │  • OrderRepository         │  │  │  │  • StockService              │   │
│  └────────────────────────────┘  │  │  └──────────────────────────────┘   │
│                                  │  │                                      │
└──────────────────┬───────────────┘  └──────────────────┬───────────────────┘
                   │                                      │
                   ▼                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AMAZON RDS (PostgreSQL 15)                          │
│  ┌─────────────────────────────┐    ┌─────────────────────────────────────┐ │
│  │       shop_schema           │    │         product_schema               │ │
│  │  • shops                    │    │  • products                          │ │
│  │  • orders                   │    │  • stocks                            │ │
│  │  • order_items              │    │  • stock_reservations                │ │
│  │  • flyway_schema_history    │    │  • stock_movements                   │ │
│  └─────────────────────────────┘    │  • flyway_schema_history             │ │
│                                     └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Communication Flow

```
┌─────────────┐     REST/GraphQL      ┌─────────────┐
│    Shop     │ ──────────────────►   │   Product   │
│  Management │                       │    Stock    │
│             │ ◄──────────────────   │             │
└─────────────┘     Stock Response    └─────────────┘
                                              │
                                              │ SOAP
                                              ▼
                                    ┌─────────────────┐
                                    │  Legacy Systems │
                                    └─────────────────┘
```

## 🛠️ Services

### Shop-Management Service (Port 8081)

Handles shop and order management with cross-service communication to Product-Stock.

**Entities:**
- `Shop` - Store information (code, name, address, manager)
- `Order` - Customer orders with status tracking
- `OrderItem` - Individual items within orders

**Key Features:**
- Shop CRUD operations
- Order lifecycle management (create, confirm, cancel)
- Stock reservation on order creation
- GraphQL cross-service queries

### Product-Stock Service (Port 8082)

Manages product catalog and stock operations with reservation support.

**Entities:**
- `Product` - Product catalog (code, name, category, price)
- `Stock` - Stock levels per product
- `StockReservation` - Time-limited stock reservations
- `StockMovement` - Stock change audit trail

**Key Features:**
- Product CRUD operations
- Stock management (add, reserve, release)
- Reservation with automatic expiration
- SOAP endpoint for legacy integration
- Low stock alerting

## 🔗 Communication Patterns

### 1. REST (Primary)

Inter-service communication for real-time operations.

```http
# Reserve stock for an order
POST /api/v1/stock/reserve
Content-Type: application/json

{
    "productId": "uuid",
    "orderId": "uuid",
    "quantity": 5,
    "reservationMinutes": 30
}
```

### 2. SOAP (Stock Availability)

Enterprise integration endpoint for legacy systems.

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:prod="http://enterprise.com/product/soap">
    <soap:Body>
        <prod:StockAvailabilityRequest>
            <prod:productId>uuid</prod:productId>
            <prod:quantity>10</prod:quantity>
        </prod:StockAvailabilityRequest>
    </soap:Body>
</soap:Envelope>
```

### 3. GraphQL (Cross-Service Queries)

Efficient data fetching across services.

```graphql
query {
    productWithStock(productId: "uuid") {
        product {
            id
            name
            price
        }
        stock {
            availableQuantity
            isLowStock
        }
    }
}
```

## 📦 Prerequisites

- **Java 21** (Amazon Corretto recommended)
- **Maven 3.9+**
- **PostgreSQL 15** (local or Amazon RDS)
- **AWS CLI** (for Terraform deployment)
- **Terraform 1.0+** (for infrastructure provisioning)

## 🚀 Quick Start

### 1. Clone and Build

```bash
# Clone repository
git clone <repository-url>
cd Microservice-Architecture

# Build all services
./scripts/build.sh --all
```

### 2. Configure Database

Create a PostgreSQL database and two schemas:

```sql
CREATE DATABASE microservices;
\c microservices
CREATE SCHEMA shop_schema;
CREATE SCHEMA product_schema;
```

### 3. Set Environment Variables

```bash
# Database
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=microservices
export DB_USERNAME=postgres
export DB_PASSWORD=your_password

# Inter-service communication
export PRODUCT_SERVICE_URL=http://localhost:8082
```

### 4. Start Services

```bash
# Start both services
./scripts/start.sh --service all

# Or start individually
./scripts/start.sh --service shop-management
./scripts/start.sh --service product-stock
```

### 5. Verify Health

```bash
# Check service status
./scripts/status.sh --all

# Or use health endpoints
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## ⚙️ Configuration

### Application Properties

Each service has environment-specific configuration:

| Property | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | localhost |
| `DB_PORT` | Database port | 5432 |
| `DB_NAME` | Database name | microservices |
| `DB_USERNAME` | Database user | postgres |
| `DB_PASSWORD` | Database password | - |
| `PRODUCT_SERVICE_URL` | Product service URL | http://localhost:8082 |

### Profiles

- `local` - Local development
- `dev` - Development environment
- `staging` - Staging environment
- `prod` - Production environment

```bash
# Start with specific profile
./scripts/start.sh --service shop-management --profile prod
```

## 📚 API Documentation

### OpenAPI/Swagger

- Shop-Management: http://localhost:8081/swagger-ui.html
- Product-Stock: http://localhost:8082/swagger-ui.html

### GraphQL Playground

- Shop-Management: http://localhost:8081/graphiql
- Product-Stock: http://localhost:8082/graphiql

### WSDL (SOAP)

- Stock Availability: http://localhost:8082/ws/stockAvailability.wsdl

### Postman Collection

Import the Postman collection from `postman/Microservice-Architecture.postman_collection.json`:

1. Open Postman
2. Click **Import**
3. Select the collection file
4. Set variables:
   - `shop_base_url`: http://localhost:8081
   - `product_base_url`: http://localhost:8082

## 📊 Observability

### Structured Logging

All logs are JSON-formatted with correlation IDs for request tracing:

```json
{
    "timestamp": "2024-01-15T10:30:00.000Z",
    "level": "INFO",
    "logger": "com.enterprise.shop.service.OrderService",
    "message": "Creating order",
    "traceId": "abc-123-def-456",
    "apiType": "REST",
    "service": "shop-management",
    "orderId": "order-uuid",
    "shopId": "shop-uuid"
}
```

### Correlation ID Propagation

The `X-Correlation-ID` header is automatically propagated across services:

```
Client → Shop-Management → Product-Stock
        X-Correlation-ID: abc-123
```

### Health Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Service health status |
| `/actuator/health/liveness` | Kubernetes liveness probe |
| `/actuator/health/readiness` | Kubernetes readiness probe |
| `/actuator/info` | Application info |
| `/actuator/metrics` | Micrometer metrics |

### Log Locations

- **Local**: Console output (STDOUT)
- **AWS**: CloudWatch Log Groups
  - `/microservices/shop-management`
  - `/microservices/product-stock`

## 🚢 Deployment

### AWS Infrastructure (Terraform)

The `terraform/` directory contains complete AWS infrastructure:

```
terraform/
├── main.tf              # Main infrastructure
├── variables.tf         # Input variables
├── outputs.tf           # Output values
├── user_data.sh         # EC2 bootstrap script
└── terraform.tfvars.example  # Example configuration
```

**Resources Created:**
- VPC with public/private subnets
- Application Load Balancer
- EC2 instances (one per service)
- RDS PostgreSQL (Multi-AZ optional)
- Security Groups
- IAM Roles
- CloudWatch Log Groups

### Deploy to AWS

```bash
cd terraform

# Copy and configure variables
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values

# Initialize Terraform
terraform init

# Preview changes
terraform plan

# Deploy infrastructure
terraform apply

# Get outputs
terraform output
```

### Manual Deployment

```bash
# Build the services
./scripts/build.sh --all

# Copy JARs to EC2 instances
scp shop-management/target/*.jar ec2-user@<shop-host>:/opt/app/
scp product-stock/target/*.jar ec2-user@<product-host>:/opt/app/

# Start services on EC2
./scripts/start.sh --service shop-management --profile prod
./scripts/start.sh --service product-stock --profile prod
```

## 📝 Shell Scripts

### build.sh

Build services with various options.

```bash
# Build all services
./scripts/build.sh --all

# Build specific service
./scripts/build.sh --service shop-management

# Build with tests
./scripts/build.sh --all --run-tests

# Build in parallel
./scripts/build.sh --all --parallel
```

### start.sh

Start services with configuration options.

```bash
# Start all services
./scripts/start.sh --service all

# Start with profile
./scripts/start.sh --service shop-management --profile prod

# Start with custom memory
./scripts/start.sh --service product-stock --memory 2g

# Start with debug port
./scripts/start.sh --service shop-management --debug 5005
```

### stop.sh

Stop services gracefully or forcefully.

```bash
# Stop all services
./scripts/stop.sh --all

# Stop specific service
./scripts/stop.sh --service shop-management

# Force stop
./scripts/stop.sh --all --force
```

### status.sh

Check service status and health.

```bash
# Check all services
./scripts/status.sh --all

# Verbose output
./scripts/status.sh --all --verbose

# JSON output for automation
./scripts/status.sh --all --json

# Continuous monitoring
./scripts/status.sh --all --watch
```

## 🧪 Testing

### Run Tests

```bash
# Run all tests
./scripts/build.sh --all --run-tests

# Run with Maven directly
mvn test -pl shop-management
mvn test -pl product-stock
```

### API Testing with Postman

1. Start both services
2. Import the Postman collection
3. Execute requests in order:
   - Create Product
   - Create Shop
   - Create Order (reserves stock)
   - Confirm Order
   - Check Stock

### Sample Test Flow

```bash
# 1. Create a product
curl -X POST http://localhost:8082/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "productCode": "PROD-001",
    "name": "Test Widget",
    "category": "Electronics",
    "price": 29.99,
    "initialStock": 100,
    "minimumStock": 10
  }'

# 2. Create a shop
curl -X POST http://localhost:8081/api/v1/shops \
  -H "Content-Type: application/json" \
  -d '{
    "shopCode": "SHOP-001",
    "name": "Test Shop",
    "city": "New York"
  }'

# 3. Create an order (reserves stock)
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "shopId": "<shop-id>",
    "customerName": "John Doe",
    "items": [{
      "productId": "<product-id>",
      "quantity": 5,
      "unitPrice": 29.99
    }]
  }'

# 4. Check stock via SOAP
curl -X POST http://localhost:8082/ws \
  -H "Content-Type: text/xml" \
  -d '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
        xmlns:prod="http://enterprise.com/product/soap">
    <soap:Body>
      <prod:StockAvailabilityRequest>
        <prod:productId><product-id></prod:productId>
        <prod:quantity>10</prod:quantity>
      </prod:StockAvailabilityRequest>
    </soap:Body>
  </soap:Envelope>'
```

## 📁 Project Structure

```
Microservice-Architecture/
├── shop-management/                 # Shop Management Service
│   ├── src/main/java/
│   │   └── com/enterprise/shop/
│   │       ├── config/             # Configuration classes
│   │       ├── controller/         # REST & GraphQL controllers
│   │       ├── dto/                # Data Transfer Objects
│   │       ├── entity/             # JPA entities
│   │       ├── exception/          # Exception handling
│   │       ├── logging/            # Logging infrastructure
│   │       ├── repository/         # Data repositories
│   │       └── service/            # Business logic
│   ├── src/main/resources/
│   │   ├── application.yml         # Main configuration
│   │   ├── application-*.yml       # Profile-specific config
│   │   ├── graphql/                # GraphQL schemas
│   │   ├── db/migration/           # Flyway migrations
│   │   └── logback-spring.xml      # Logging configuration
│   └── pom.xml
│
├── product-stock/                   # Product Stock Service
│   ├── src/main/java/
│   │   └── com/enterprise/product/
│   │       ├── config/             # Configuration classes
│   │       ├── controller/         # REST, SOAP & GraphQL
│   │       ├── dto/                # Data Transfer Objects
│   │       ├── entity/             # JPA entities
│   │       ├── exception/          # Exception handling
│   │       ├── logging/            # Logging infrastructure
│   │       ├── repository/         # Data repositories
│   │       └── service/            # Business logic
│   ├── src/main/resources/
│   │   ├── application.yml         # Main configuration
│   │   ├── application-*.yml       # Profile-specific config
│   │   ├── graphql/                # GraphQL schemas
│   │   ├── xsd/                    # SOAP schema definitions
│   │   ├── db/migration/           # Flyway migrations
│   │   └── logback-spring.xml      # Logging configuration
│   └── pom.xml
│
├── scripts/                         # Shell scripts
│   ├── build.sh                    # Build automation
│   ├── start.sh                    # Service startup
│   ├── stop.sh                     # Service shutdown
│   └── status.sh                   # Health monitoring
│
├── terraform/                       # Infrastructure as Code
│   ├── main.tf                     # AWS infrastructure
│   ├── variables.tf                # Input variables
│   ├── outputs.tf                  # Output values
│   ├── user_data.sh                # EC2 bootstrap
│   └── terraform.tfvars.example    # Example config
│
├── postman/                         # API testing
│   └── Microservice-Architecture.postman_collection.json
│
└── README.md                        # This file
```

## 🔧 Technology Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.1 |
| REST | Spring Web MVC |
| SOAP | Spring Web Services |
| GraphQL | Spring for GraphQL |
| Database | PostgreSQL 15 |
| ORM | Spring Data JPA / Hibernate |
| Migration | Flyway |
| Connection Pool | HikariCP |
| Logging | SLF4J + Logback + Logstash Encoder |
| Documentation | SpringDoc OpenAPI 3.0 |
| Build | Maven |
| Infrastructure | Terraform |
| Cloud | AWS (EC2, RDS, ALB, CloudWatch) |

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Built with ❤️ for enterprise-grade microservice architecture**
