# 📊 Analytics Dashboard (SQL Query Executor)

A Spring Boot REST API designed to define, store, and execute analytical SQL queries over a read-only dataset (Titanic passengers).

This project demonstrates a production-grade approach to handling potentially slow SQL operations using **Asynchronous Processing**, **Caching**, and **Strict Security Layers**.

---

## 🚀 Features

* **Query Management:** Store and list analytical SQL queries.
* **Synchronous Execution:** Execute queries immediately and get results as a 2D array.
* **⚡ Asynchronous Execution:** Handle long-running queries in the background with status tracking (PENDING → RUNNING → COMPLETED).
* **🛡️ Dual-Layer Security:** Prevents data modification via both SQL validation and low-level Database connection settings.
* **🚀 Performance Caching:** Uses Caffeine to cache results of identical queries.

---

## 🛠️ Technology Stack

* **Language:** Java 17
* **Framework:** Spring Boot 3.4
* **Database:** H2 In-Memory (initialized with Titanic dataset)
* **Persistence:** Spring Data JPA + JdbcTemplate
* **Async/Caching:** Spring Async, Caffeine Cache
* **Testing:** JUnit 5, Mockito, MockMvc

---

## 🏗️ Architecture & Design Decisions

### 1. Database Architecture (The "Two-DataSource" Strategy)
To ensure the analytical data remains immutable, the application configures two separate `DataSource` beans:

1.  **Primary DataSource (Read-Write):** Used by JPA/Hibernate. Connects to `titanicdb` with full permissions. Used *only* for managing application state (`StoredQuery` and `QueryExecution` entities).
2.  **Read-Only DataSource:** Used by `JdbcTemplate`. Connects to the same `titanicdb` but with the H2 flag `ACCESS_MODE_DATA=r`.
    * **Why?** This provides a hard guarantee at the database engine level that no `INSERT`, `UPDATE`, or `DELETE` commands can modify the passenger data, even if the SQL validation logic fails.

### 2. Asynchronous Execution Flow
For potentially slow queries, we implement a non-blocking flow using the **Polling Pattern**:

1.  Client requests async execution via `POST`.
2.  Service creates a `QueryExecution` record with status `PENDING` and returns a `UUID`.
3.  Spring's `@Async` executor picks up the task in a separate thread.
4.  Status updates to `RUNNING`. The query is executed.
5.  Results are serialized to JSON and stored in the database; status updates to `COMPLETED`.
6.  Client polls the status endpoint to retrieve results.

**Key Decision:** `UUID` is used for execution IDs instead of auto-incrementing Longs to prevent ID enumeration attacks (clients guessing other users' job IDs).

### 3. Caching Strategy
Since the Titanic dataset is static (read-only), we use **Caffeine Cache**.
* **Key:** The exact SQL string.
* **Policy:** `maximumSize=100`, `expireAfterWrite=24h`.
* **Benefit:** Frequent queries (e.g., dashboard KPIs) return instantly without hitting the database.

### 4. Data Transfer Objects (DTOs)
We strictly separate Database Entities from API Models to prevent leaking internal details.
* `QueryRequest` / `QueryResponse`: For CRUD operations.
* `AsyncExecutionResponse`: Lightweight response for starting a task (returns ID only).
* `AsyncExecutionStatusResponse`: Detailed response for polling (includes status, result, errors).

---

## ⚙️ Getting Started

### Prerequisites
* JDK 17+
* Maven (wrapper included)

### Installation

1.  **Clone the repository**
    ```bash
    git clone <repo-url>
    cd analytics_dashboard
    ```

2.  **Run the application**
    ```bash
    ./mvnw spring-boot:run
    ```

The app will start on `http://localhost:8080`. The H2 Console is available at `/h2-console` (JDBC URL: `jdbc:h2:mem:titanicdb`).

---

## 📖 API Documentation

### 1. Store & List Queries

**Add a Query**
```bash
POST /queries
Content-Type: application/json
{ "query": "SELECT Name, Age FROM passengers WHERE Survived = 1 LIMIT 5" }