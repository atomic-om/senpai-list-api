# SenpaiList API

A multi-user RESTful API for tracking anime watchlists across multiple seasons, built with **Java**, **Spring Boot**, **Spring Security (JWT)**, and **MySQL**.

---

## ✨ Features

* 🔐 **Stateless JWT Auth** — Secure registration and login with BCrypt password hashing.
* 👤 **User Isolation** — Every user only sees and manages their own anime list.
* 📺 **Multi-Season Tracking** — Track seasons, episode counts, watch status, and ratings independently per anime.
* 🛡️ **Global Error Handling** — Clean, consistent JSON responses for `400`, `401`, `404`, and `409` errors.
* ⚡ **Optimized Queries** — Uses `@EntityGraph` and `open-in-view=false` to prevent N+1 query issues.

---

## 🛠️ Tech Stack

* **Backend:** Java 21, Spring Boot
* **Security:** Spring Security, JJWT
* **Database:** MySQL, Spring Data JPA (Hibernate)
* **Build:** Gradle, Lombok, Jakarta Validation

---

## 📁 Project Structure

```text
com
├── config          # Security & authentication config
├── controller      # REST API endpoints
├── dto             # Request & response payloads
├── entity          # JPA database models & WatchStatus enum
├── exception       # Custom exceptions & global handler
├── repository      # Scoped JPA queries
├── security        # JWT token generation & filter
└── service         # Business logic & ownership checks
```

---

## 🚀 Quick Start

### 1. Create the Database

```sql
CREATE DATABASE senpailist_db;
```

### 2. Set Environment Variables

Configure these variables in your IDE or terminal:

|Variable|Description|Example|
|-|-|-|
|`DB_NAME`|Database name|`senpailist_db`|
|`DB_USERNAME`|MySQL username|`root`|
|`DB_PASSWORD`|MySQL password|`your-password`|
|`JWT_SECRET`|Base64 256-bit secret key|`openssl rand -base64 32`|
|`JWT_EXPIRATION`|Token expiry in ms|`86400000` *(24h)*|

### 3. Run the Server

```bash
# macOS / Linux
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

Server runs at `http://localhost:8080`.

### 4. Explore with Swagger UI

Interactive OpenAPI documentation is available while the server is running:

* **Swagger UI:** `http://localhost:8080/swagger-ui.html`
* **OpenAPI Spec (JSON):** `http://localhost:8080/v3/api-docs`

---

## 📡 API Endpoints

> 🔒 **Authentication:** All `/api/v1/animes`, `/seasons`, and `/api/v1/discover` endpoints require a `Bearer <token>` header.

### 🔑 Auth (`/api/v1/auth`)

| Method | Endpoint | Action |
| :--- | :--- | :--- |
| `POST` | `/register` | Create a new account & get JWT |
| `POST` | `/login` | Log in & get JWT |

### 🎬 Animes (`/api/v1/animes`)

| Method | Endpoint | Action |
| :--- | :--- | :--- |
| `POST` | `/` | Add a new anime (with optional seasons) |
| `GET` | `/` | Get your paginated watchlist (`search`, `status`, `page`, `size`, `sort`) |
| `GET` | `/stats` | Get watchlist summary statistics & counts by status |
| `GET` | `/{animeId}` | Get one anime and its seasons |
| `PATCH` | `/{animeId}` | Update anime title or cover image |
| `DELETE` | `/{animeId}` | Delete an anime and its seasons |

#### Watchlist Query Parameters (`GET /api/v1/animes`)

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `search` | `String` | — | Case-insensitive substring filter on anime title |
| `status` | `WatchStatus` | — | Filter animes having at least one season with this status |
| `page` | `int` | `0` | Zero-based page index |
| `size` | `int` | `10` | Page size |
| `sort` | `String` | `updatedAt,desc` | Sort field and direction (e.g., `title,asc`, `updatedAt,desc`) |

### 🎞️ Seasons (`/api/v1/animes/{animeId}/seasons`)

| Method | Endpoint | Action |
| :--- | :--- | :--- |
| `POST` | `/` | Add a season to an anime |
| `GET` | `/` | List all seasons of an anime |
| `GET` | `/{seasonId}` | Get a single season|
| `PATCH` | `/{seasonId}` | Update progress, status, or rating |
| `DELETE` | `/{seasonId}` | Delete a season |

### 🔎 Discover (`/api/v1/discover`)

Powered by the **AniList GraphQL API** (`isAdult: false`, 12 results per page):

| Method | Endpoint | Query Params | Action |
| :--- | :--- | :--- | :--- |
| `GET` | `/search` | `q` (required, non-blank), `page` (default `1`) | Search anime catalog via AniList (sorted by popularity) |
| `GET` | `/top` | `page` (default `1`) | Browse trending anime via AniList |

---

## 📦 Sample JSON

### Register (`POST /api/v1/auth/register`)

```json
{
  "username": "six_seven",
  "email": "anime@senpailist.com",
  "password": "Password67"
}
```

### Add Anime (`POST /api/v1/animes`)

```json
{
  "title": "Jujutsu Kaisen",
  "coverImageUrl": "https://example.com/jjk.jpg",
  "seasons": [
    {
      "seasonNumber": 1,
      "title": "Season 1",
      "status": "COMPLETED",
      "episodesWatched": 24,
      "totalEpisodes": 24,
      "rating": 9
    }
  ]
}
```

### 🏷️ Watch Statuses

| Status | Description |
| :--- | :--- |
| `WATCHING` | Currently watching |
| `COMPLETED` | Finished all episodes |
| `ON_HOLD` | Paused temporarily |
| `DROPPED` | Stopped watching |
| `PLAN_TO_WATCH` | Saved for later (default) |

---

## ⚠️ Error Responses

All exceptions return a standardized JSON payload via `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-11-24T20:01:47.040",
  "status": 409,
  "error": "Conflict",
  "message": "Anime 'Jujutsu Kaisen' is already in your watchlist",
  "validationErrors": null
}
```

| Status | Error | Trigger |
| :--- | :--- | :--- |
| **`400`** | `Bad Request` | Failed `@Valid` input constraints, malformed JSON, invalid query/sort params, or `episodesWatched > totalEpisodes` |
| **`401`** | `Unauthorized` | Missing, expired, or invalid JWT token / bad credentials |
| **`404`** | `Not Found` | Anime or season ID does not exist (or belongs to another user) |
| **`409`** | `Conflict` | Duplicate email/username, or duplicate anime title/season number |
| **`502` / `503`** | `Bad Gateway` / `Service Unavailable` | Upstream AniList API failure, timeout, or rate limit exceeded |

---

<div align="center">

### ⭐ Support the Project
If you found **SenpaiList API** helpful or learned something from the codebase, consider giving the repository a **Star**!

Built with ☕ & **Spring Boot**

</div>