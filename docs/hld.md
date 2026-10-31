# Shipping Wars — High-Level Design (HLD)

## System Overview

**Shipping Wars** is a reverse-auction platform for freight procurement. Shippers post RFQs (Requests for Quotation), suppliers place competitive bids, and the platform automatically extends auction windows based on configurable trigger rules to drive optimal pricing.

---

## Architecture Diagram

```mermaid
flowchart TD
    Client(["Client\n(Postman / Frontend)"])

    subgraph App ["Spring Boot Application (Java 23, Spring Boot 4.1.2)"]
        direction TB

        subgraph Controllers ["Controller Layer"]
            RC["RFQController\nGET /api/rfqs\nPOST /api/rfqs\nGET /api/rfqs/:refId\nGET /api/rfqs/active"]
            BC["BIDController\nGET /api/rfqs/:refId/bids\nPOST /api/rfqs/:refId/bids"]
            AC["AuctionActivityController\nGET /api/rfqs/:refId/activity"]
        end

        subgraph Services ["Service Layer"]
            RS["RFQService"]
            BS["BIDService"]
            SS["SupplierService"]
            AS["AuctionActivityService"]
        end

        subgraph Repositories ["Repository Layer (Spring Data JPA)"]
            RR["RFQRepository"]
            BR["BIDRepository"]
            SR["SupplierRepository"]
            AR["AuctionActivityRepository"]
        end

        subgraph OpenAPI ["API Docs"]
            SW["Swagger UI\n/swagger-ui.html"]
        end
    end

    subgraph DB ["PostgreSQL Database"]
        T1[("rfq")]
        T2[("auction_config")]
        T3[("bid")]
        T4[("supplier")]
        T5[("auction_activity_log")]
    end

    Client -->|HTTP/REST| Controllers
    Client -->|browse| SW

    RC --> RS
    BC --> BS
    AC --> AS

    BS --> RS
    BS --> SS
    BS --> AS

    RS --> RR
    BS --> BR
    SS --> SR
    AS --> AR

    RR --> T1
    RR --> T2
    BR --> T3
    SR --> T4
    AR --> T5

    T1 --- T2
    T1 --- T3
    T1 --- T5
    T4 --- T3
    T3 --- T5
```

---

## Layer Breakdown

### 1. Controller Layer
Thin REST layer — validates input, delegates to services, returns HTTP responses.

| Controller | Base Path | Responsibility |
|---|---|---|
| `RFQController` | `/api/rfqs` | CRUD for RFQs; fetch all, active, or by reference ID |
| `BIDController` | `/api/rfqs/{refId}/bids` | Submit and list bids for an RFQ |
| `AuctionActivityController` | `/api/rfqs/{refId}/activity` | Fetch the audit log for an RFQ |

### 2. Service Layer
All business logic lives here. Services are transactional and orchestrate cross-entity operations.

| Service | Key Responsibilities |
|---|---|
| `RFQService` | Create RFQ + AuctionConfig; fetch active RFQs; `findForBidSubmission` with lock |
| `BIDService` | Validate timing, validate improvement over previous bid, run auction extension logic, trigger activity logging |
| `SupplierService` | Supplier lookup by ID |
| `AuctionActivityService` | Write `BID_PLACED` and `TIME_EXTENDED` audit log entries |

### 3. Repository Layer
Spring Data JPA repositories with custom JPQL queries.

| Repository | Notable Queries |
|---|---|
| `RFQRepository` | `findByReferenceId` (with `JOIN FETCH auctionConfig`), `findActiveRFQs`, `findForBidSubmission` (optimistic force-increment lock) |
| `BIDRepository` | `findByRfqReferenceIdWithSupplier`, `findLatestBidsByRFQ`, `findLatestByRfqAndSupplier` |
| `SupplierRepository` | Standard `findById` |
| `AuctionActivityRepository` | `findByRfqReferenceIdWithBid` (with `JOIN FETCH bid → supplier`) |

---

## Core Flows

### POST /api/rfqs — Create RFQ

```mermaid
flowchart LR
    A["Client POST /api/rfqs"] --> B["RFQController"]
    B --> C["RFQService.createRFQ"]
    C --> D["Save RFQ + AuctionConfig\n(CASCADE)"]
    D --> E["PostgreSQL\nrfq + auction_config"]
    E --> F["Return RFQ with\nauto-generated referenceId"]
```

### POST /api/rfqs/:refId/bids — Submit Bid

```mermaid
flowchart TD
    A["Client POST bid"] --> B["BIDController"]
    B --> C["BIDService.createBid"]

    C --> D{"RFQ exists?\nOPTIMISTIC_FORCE_INCREMENT"}
    D -- No --> E["404 NOT FOUND"]
    D -- Yes --> F{"Supplier exists?"}
    F -- No --> G["404 NOT FOUND"]
    F -- Yes --> H{"now < bidCloseTime?"}
    H -- No --> I["400 BAD REQUEST\nAuction closed"]
    H -- Yes --> J{"Previous bid\nfrom supplier?"}
    J -- Yes --> K{"totalPrice lower\nthan previous bid?"}
    K -- No --> L["400 BAD REQUEST\nMust lower total price"]
    K -- Yes --> M["Save BID\nsaveAndFlush"]
    J -- No --> M
    M --> N["logBidPlaced\nBID_PLACED log"]
    N --> O["runExtensionLogic\nANY_BID / ANY_RANK_CHANGE / L1_CHANGE"]
    O --> P{"In trigger\nwindow?"}
    P -- No --> Q["Return saved BID"]
    P -- Yes --> R["Extend bidCloseTime\ncapped at forcedCloseTime"]
    R --> S["logTimeExtended\nTIME_EXTENDED log"]
    S --> Q
```

---

## Auction Extension Logic

| Trigger Type | Extension fires when… |
|---|---|
| `ANY_BID` | Any bid lands within `triggerWindowMinutes` of close |
| `ANY_RANK_CHANGE` | The supplier ranking order shifts after the new bid |
| `L1_CHANGE` | The leading (lowest total price) supplier changes |

> **Hard cap:** `bidCloseTime` can never exceed `forcedCloseTime`, regardless of how many extensions occur.

---

## Concurrency Strategy

| Mechanism | Where used | Purpose |
|---|---|---|
| **Optimistic Lock** (`@Version`) | `RFQ.version` | Hibernate increments the version on every flush; detects concurrent writes to the same RFQ row |
| **`OPTIMISTIC_FORCE_INCREMENT`** | `RFQRepository.findForBidSubmission` | Forces a version bump even if no field on the RFQ itself changes, ensuring any concurrent bid transaction that loaded the same RFQ will fail with `OptimisticLockingFailureException` |
| **`@Retryable`** (`maxRetries=3, delay=100ms`) | `BIDService.createBid` | On a version conflict the transaction is transparently retried up to 3 times before returning an error to the client |
| **`@Transactional`** | `BIDService.createBid` | Entire bid → save → extend → log pipeline runs in one atomic transaction; version check happens at commit time |

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 23 (preview features enabled) |
| Framework | Spring Boot 4.1.2 |
| Web | Spring Web MVC (`spring-boot-starter-webmvc`) |
| Persistence | Spring Data JPA + Hibernate (`spring-boot-starter-data-jpa`) |
| Database | PostgreSQL 5432 (`shipping_wars`) |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI 3.2.0 (Swagger UI) |
| Build | Maven |
| DDL strategy | `spring.jpa.hibernate.ddl-auto=update` |
| Timezone | All timestamps UTC (`hibernate.jdbc.time_zone=UTC`) |

---

## Project Structure

```
src/main/java/com/shipping/wars/shipper/
├── controller/
│   ├── RFQController.java
│   ├── BIDController.java
│   └── AuctionActivityController.java
├── service/
│   ├── RFQService.java
│   ├── BIDService.java
│   ├── SupplierService.java
│   └── AuctionActivityService.java
├── repository/
│   ├── RFQRepository.java
│   ├── BIDRepository.java
│   ├── SupplierRepository.java
│   └── AuctionActivityRepository.java
├── entity/
│   ├── RFQ.java
│   ├── AuctionConfig.java
│   ├── BID.java
│   ├── Supplier.java
│   ├── AuctionActivityLog.java
│   ├── TriggerType.java       (enum)
│   └── EventType.java         (enum)
└── dto/
    ├── CreateRfqRequest.java
    └── CreateBidRequest.java
docs/
├── hld.md                     ← this file
└── schema-design.md
```

---

## API Reference Summary

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/rfqs` | List all RFQs |
| `GET` | `/api/rfqs/active` | List currently active RFQs |
| `GET` | `/api/rfqs/{refId}` | Get single RFQ by reference ID |
| `POST` | `/api/rfqs` | Create a new RFQ |
| `GET` | `/api/rfqs/{refId}/bids` | List all bids for an RFQ |
| `POST` | `/api/rfqs/{refId}/bids` | Submit a bid for an RFQ |
| `GET` | `/api/rfqs/{refId}/activity` | Get auction activity log for an RFQ |
| `GET` | `/swagger-ui.html` | Interactive API docs (SpringDoc) |

