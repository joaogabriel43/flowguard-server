# FlowGuard Server

[![Build Status](https://github.com/joaogabriel43/flowguard-server/actions/workflows/ci.yml/badge.svg)](https://github.com/joaogabriel43/flowguard-server/actions/workflows/ci.yml)
[![Deploy Status](https://img.shields.io/badge/Railway-Live-blueviolet?style=flat&logo=railway)](https://railway.app)
[![Java Version](https://img.shields.io/badge/Java-21-orange?style=flat&logo=openjdk)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Self-hosted feature flag platform with progressive rollout, real-time SSE push, Redis caching and multi-tenant support**

---

## What is FlowGuard?

Feature flags are the industry standard for safe deployments and continuous deliveries, but SaaS alternatives like LaunchDarkly are prohibitively expensive at scale. **FlowGuard** is a highly performant, open-source, self-hosted alternative that provides sub-millisecond local evaluations, immediate global synchronization, and total multi-tenant database isolation.

<!-- GIF: dashboard toggle demo -->

---

## Architecture

FlowGuard is engineered following rigorous **Clean Architecture** patterns in the Java backend and a reactive **Standalone Component** design in the Angular frontend.

```mermaid
graph TD
    subgraph Client Application
        SDK["FlowGuard Java SDK"]
        Dashboard["Angular Management Dashboard"]
    end

    subgraph FlowGuard Server
        API["Spring Boot REST API"]
        SSE["Server-Sent Events Emitter"]
        Cache["Redis (Cache-Aside)"]
        Broker["Redis Pub/Sub (Sse Broadcast)"]
        DB[(PostgreSQL Store)]
    end

    Dashboard -->|Manage Flags & Rules (JWT)| API
    Dashboard -->|Listen for Toggles (EventSource)| SSE
    SDK -->|Evaluate Offline (Cache Snapshots)| SSE
    SDK -->|On-demand REST Evaluation| API

    API -->|Read/Write Metadata| DB
    API -->|Cache Eviction / Snapshots| Cache
    API -->|Notify Flag Toggle| Broker
    Broker -->|Propagate Updates| SSE
```

### Clean Architecture Backend
The server is split into highly decoupled domain, application, and infrastructure boundaries:
*   **Domain Layer:** Core value objects and entities (`FeatureFlag`, `FlagRule`, `RolloutEvaluator`) containing business logic, free from database or framework libraries.
*   **Application Layer:** Core use-cases, transactions, and event orchestrators (`FeatureFlagService`, `AuthService`) exposing clean interfaces.
*   **Infrastructure Layer:** Database entities, adapters, repositories (`PostgreSQL`, `Flyway`), and caching components (`Redis`).
*   **Web Presentation Layer:** Security filters (`Spring Security JWT`) and controllers (`REST API`, `SpaController`, `SseController`).

---

### Architectural Decision Records (ADRs)

| Decision | Chosen Tech / Pattern | Alternatives Considered | Technical Justification |
| :--- | :--- | :--- | :--- |
| **Real-time Push** | **Server-Sent Events (SSE)** | WebSockets, Long Polling | SSE runs natively over HTTP/1.1 and HTTP/2, uses standard keepalive headers, traverses proxy firewalls cleanly, and consumes minimal server sockets for unidirectional status streaming. |
| **Scaling Cache & SSE** | **Redis Cache-Aside + Pub/Sub** | In-Memory (Guava), RabbitMQ | Decouples multiple REST instances. When a flag toggles on Instance A, it publishes an eviction message to all nodes via Redis Pub/Sub, updating in-memory SSE registries globally in sub-milliseconds. |
| **Progressive Rollout** | **MurmurHash3 (32-bit)** | MD5, SHA-256 | MurmurHash3 offers excellent hash distribution and is mathematically lightweight, ensuring consistent, deterministic user bucket evaluation across server-side APIs and client-side SDKs. |
| **Multi-Tenancy** | **Column-Level Isolation (tenant_id)** | Schema-per-tenant, Row-Level Security (RLS) | Single-database multi-tenancy utilizing indexed columns provides optimal latency and scaling. Isolation is enforced at the JPA repository layer using ThreadLocal context parsing. |

---

## Technical Highlights

*   **Deterministic Progressive Rollout:** Powered by MurmurHash3 to partition user IDs deterministically without storing individual state.
*   **Sub-Millisecond SSE Streaming:** Broadcasts state mutations instantly to the management portal and local SDK caches.
*   **Redis Caching-Aside Strategy:** Optimizes performance by bypassing the PostgreSQL relational model for frequently evaluated flags.
*   **Absolute Multi-Tenant Isolation:** ThreadLocal tenant context bounds every API request, data query, and SSE connection.
*   **Property-Based Testing:** Rigorously validated with `jqwik` property testing, executing thousands of randomized evaluations.
*   **DevOps Ready Secure CI/CD:** Fully scanned using OWASP Dependency Check, Trivy FS, GitLeaks, and SpotBugs.

---

## Quick Start

### Prerequisites
*   [Java 21 JRE/JDK](https://openjdk.org)
*   [Docker & Docker Compose](https://www.docker.com)

### Installation & Run

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/joaogabriel43/flowguard-server.git
    ```

2.  **Start Database & Redis Dependencies:**
    ```bash
    docker compose up -d
    ```

3.  **Boot the Spring Boot Unified Server:**
    ```bash
    ./mvnw spring-boot:run
    ```

### Accessing the Portal
Open `http://localhost:8080` in your web browser. 

*   To create your tenant and obtain credentials, click **Sign Up** on the authentication page.
*   Log in using the registered credentials to start creating flags and progressive rollout rules immediately!

---

## API Reference

| Method | Route | Description | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | Register a new tenant & admin user | Public |
| `POST` | `/auth/login` | Authenticate user & get JWT token | Public |
| `GET` | `/api/flags` | List all feature flags for tenant | Authenticated (JWT) |
| `POST` | `/api/flags` | Create a new feature flag | Authenticated (JWT) |
| `GET` | `/api/flags/{key}` | Retrieve flag configuration details | Authenticated (JWT) |
| `POST` | `/api/flags/{key}/evaluate` | Dynamically evaluate flag status | Authenticated (JWT / SDK) |
| `POST` | `/api/flags/{key}/rules` | Append a new segmentation rule | Authenticated (JWT) |
| `GET` | `/api/sse/flags` | Open real-time SSE push stream | Authenticated (JWT / Param) |

### On-Demand Flag Evaluation Spec
**Endpoint:** `POST /api/flags/{key}/evaluate`

#### Request Payload
```json
{
  "userId": "usr_94f83b28",
  "attributes": {
    "country": "BR",
    "device": "mobile",
    "tier": "enterprise"
  }
}
```

#### Response Payload
```json
{
  "enabled": true,
  "reason": "ROLLOUT"
}
```

---

## Tech Stack & Versioning Matrix

| Technology | Version | Architectural Justification |
| :--- | :--- | :--- |
| **Java** | 21 | Long Term Support version, native thread structures, and record matching features. |
| **Spring Boot** | 3.2.3 | Modern dependency injection, built-in reactive streaming, and native security filters. |
| **Angular** | 17.3.0 | Standalone lightweight components, reactive FormArrays, and native HTTP routing. |
| **PostgreSQL** | 16 | ACID compliant relational model, ideal for structured metadata and tenant indexing. |
| **Redis** | 7.2 | Ultra-fast in-memory storage, cache eviction policies, and lightweight Pub/Sub messaging. |

---

## Integration SDKs

For lightning-fast offline evaluations, seamless SSE cache invalidations, and native performance, integrate our dedicated Java SDK directly into your service.

👉 **Get started now:** Visit the [FlowGuard Java SDK](https://github.com/joaogabriel43/flowguard-sdk) repository to integrate features flags in less than 5 minutes!
