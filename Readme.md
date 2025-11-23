# 📊 Analytics Dashboard (SQL Query Executor)

A Spring Boot REST API designed to define, store, and execute analytical SQL queries over a read-only dataset (Titanic passengers).

This project demonstrates a production-grade approach to handling potentially slow SQL operations using **Asynchronous Processing**, **Caching**, and **Strict Security Layers**.

---

## 🚀 Features

* **Query Management:** Store and list analytical SQL queries.
* **Synchronous Execution:** Execute queries immediately and get results as a structured JSON object.
* **⚡ Asynchronous Execution:** Handle long-running queries in the background with status tracking (PENDING → RUNNING → COMPLETED).
* **🛡️ Dual-Layer Security:** Prevents data modification via both Java-level SQL validation and low-level Database connection settings (`ACCESS_MODE_DATA=r`).
* **🚀 Performance Caching:** Uses **Caffeine Cache** to store results of identical queries, serving repeated requests instantly.

---

## 🛠️ Technology Stack

* **Language:** Java 17
* **Framework:** Spring Boot 3.4 (Web, Data JPA)
* **Database:** H2 In-Memory (initialized with Titanic dataset)
* **Persistence:** Spring Data JPA + JdbcTemplate
* **Async/Caching:** Spring Async, Caffeine Cache
* **Testing:** JUnit 5, Mockito, MockMvc (Integration Tests)
* **Build Tool:** Maven

---

## 🏗️ Architecture & Design Decisions

### 1. Database Architecture (The "Two-DataSource" Strategy)
To ensure the analytical data remains immutable, the application configures two separate `DataSource` beans:

1. **Primary DataSource (Read-Write):**  
   Used by JPA/Hibernate. Connects to `titanicdb` with full permissions. Used *only* for managing application state (`StoredQuery` and `QueryExecution` entities).

2. **Read-Only DataSource:**  
   Used by `JdbcTemplate` for user queries. Connects to the same `titanicdb` but with the H2 flag `ACCESS_MODE_DATA=r`.

**Decision:**  
This provides a **hard guarantee** at the database engine level that no `INSERT`, `UPDATE`, or `DELETE` commands can modify the passenger data, acting as a failsafe even if the Java validation logic is bypassed.

---

### 2. Asynchronous Execution Flow

Using the **Polling Pattern**:

1. Client requests async execution via `POST`.
2. Service creates a `QueryExecution` record with status `PENDING` and returns a `UUID`.
3. Spring's `@Async` executor picks up the task in a separate thread.
4. Status updates to `RUNNING`.
5. Results are computed, serialized to JSON, and stored.
6. Client polls a status endpoint until the job is `COMPLETED`.

**Decision:**  
UUIDs prevent **ID enumeration attacks**.

---

### 3. Caching Strategy

Since the dataset is static:

* **Key:** The full SQL string
* **Policy:** `maximumSize=100`, `expireAfterWrite=24h`
* **Benefit:** Frequent analytical queries return instantly.

---

### 4. Data Transfer Objects (DTOs)

DTOs decouple persistence models from API contracts.

* Entities are never returned directly.
* All results are wrapped inside a consistent structure:
  ```json
  { "result": [...] }
  ```

---

## ⚙️ Getting Started

### Prerequisites
* JDK 17+
* Maven (wrapper included)

### Installation

```bash
git clone <repo-url>
cd analytics_dashboard
./mvnw clean install
./mvnw spring-boot:run
```

App starts at: `http://localhost:8080`

Database loads Titanic passengers automatically.

---

## 📖 API Documentation

### 1. Query Management

#### Add a New Query
**Endpoint:** `POST /queries`

**Body:**
```json
{
  "query": "SELECT Name, Age FROM passengers WHERE Survived = 1 LIMIT 5"
}
```

**Response:**
```json
{ "id": 1 }
```

---

### List All Queries
**Endpoint:** `GET /queries`

**Response:**
```json
[
  { "id": 1, "query": "SELECT Name, Age FROM passengers WHERE Survived = 1 LIMIT 5" },
  { "id": 2, "query": "SELECT COUNT(*) FROM passengers" }
]
```

---

## 2. Synchronous Execution

**Endpoint:** `GET /queries/execute?query={id}`

**Example Response:**
```json
{
  "result": [
    ["Braund, Mr. Owen Harris", 22.0],
    ["Heikkinen, Miss. Laina", 26.0],
    ["Futrelle, Mrs. Jacques Heath (Lily May Peel)", 35.0]
  ]
}
```

---

## 3. Asynchronous Execution

### Step 1 — Start async run

**Endpoint:** `POST /queries/execute/async?query={id}`

**Response (202):**
```json
{
  "executionId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "status": "PENDING"
}
```

---

### Step 2 — Poll job status

**Endpoint:** `GET /queries/execute/async/{executionId}`

**Running:**
```json
{
  "executionId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "status": "RUNNING"
}
```

**Completed:**
```json
{
  "executionId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "status": "COMPLETED",
  "result": [
    ["Braund, Mr. Owen Harris", 22.0],
    ["Heikkinen, Miss. Laina", 26.0]
  ]
}
```

---

## 4. Error Handling

### Example: Forbidden SQL
```json
{ "error": "Only SELECT queries are allowed" }
```

### Example: Invalid table
```json
{ "error": "SQL execution failed: Table \"NONEXISTENT\" not found..." }
```

---

## ⚠️ Limitations & Future Improvements

- **Authentication missing:** Add OAuth2/JWT for real deployments.
- **No pagination:** Big result sets can stress memory.
- **Database is in-memory:** Should switch to PostgreSQL for persistence.
- **SQL DoS protection needed:** Add execution timeouts and complexity analysis.

