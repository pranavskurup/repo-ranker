# repo-ranker

A backend application for scoring GitHub repositories based on popularity metrics.

## Overview

This project implements a REST API that fetches repositories from GitHub's public search API and assigns a popularity score based on stars, forks, and recency of updates.

## Features

- Search GitHub repositories with configurable filters (language, earliest creation date)
- Popularity scoring algorithm based on:
  - Number of stars
  - Number of forks
  - Recency of last update
- Ranked results sorted by score

## Tech Stack

- Java 21
- Spring Boot

## Prerequisites

- Java 21 (JDK)
- Maven

## Getting Started

### Build

```bash
./mvnw clean install
```

### Run

```bash
./mvnw spring-boot:run
```

### API

#### Search and Rank Repositories

```
GET /api/repositories?language=java&created_after=2024-01-01
```

**Query Parameters:**

| Parameter       | Type   | Description                      |
|----------------|--------|----------------------------------|
| `language`     | string | Filter by programming language   |
| `created_after`| string | Earliest creation date (ISO 8601)|

**Response:**

```json
[
  {
    "id": 123456,
    "name": "repository-name",
    "full_name": "owner/repository-name",
    "url": "https://github.com/owner/repository-name",
    "stars": 1500,
    "forks": 300,
    "language": "Java",
    "created_at": "2024-01-15T10:30:00Z",
    "updated_at": "2025-06-01T14:20:00Z",
    "score": 85.42
  }
]
```

## Scoring Algorithm

### Formula

```
score = (w_stars × normalized_stars) + (w_forks × normalized_forks) + (w_recency × recency_score)
```

### Default Weights

| Factor    | Weight | Rationale |
|-----------|--------|-----------|
| Stars     | 0.50   | Primary indicator of community approval and visibility |
| Forks     | 0.30   | Signals active usage and contribution interest |
| Recency   | 0.20   | Freshness matters — actively maintained projects are more relevant |

### Normalization

**Stars and Forks** are normalized using a log scale to prevent massive repos from dominating the results:

```
normalized_stars = log(1 + stars) / log(1 + max_stars_in_result_set)
```

**Why log scale?** The difference between 10 and 100 stars is more meaningful than between 10,000 and 10,100. Log normalization compresses the range while preserving relative ordering.

**Recency Score** uses an exponential decay function based on days since the last update:

```
recency_score = e^(-λ × days_since_update)
```

Where `λ = 0.001` (half-life ~693 days). This means:
- Updated today → score ≈ 1.0
- Updated 1 year ago → score ≈ 0.69
- Updated 2 years ago → score ≈ 0.47
- Updated 5 years ago → score ≈ 0.07

**Why exponential decay?** It naturally penalizes stale projects without abruptly excluding them. A project updated last week is slightly preferred over one updated last month, but both score well.

### Example

| Repository | Stars | Forks | Last Updated | Score |
|------------|-------|-------|-------------|-------|
| Repo A     | 5000  | 800   | 5 days ago  | 92.3  |
| Repo B     | 12000 | 2000  | 180 days ago| 85.1  |
| Repo C     | 500   | 200   | 10 days ago | 78.6  |

Repo A scores highest despite fewer stars than Repo B because its recent activity (5 days vs 180 days) gives it a significant recency advantage.

## Design Decisions

- **Scoring Algorithm**: Log-normalized stars and forks with exponential decay recency, weighted and summed to a 0–100 scale
- **Caching**: GitHub API responses are cached to reduce rate limit consumption
- **Pagination**: Results are paginated to handle large result sets efficiently

## Development

```bash
# Run tests
./mvnw test

# Run locally with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## License

MIT
