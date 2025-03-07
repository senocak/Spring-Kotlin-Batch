# Spring Kotlin Batch

A Spring Boot application written in Kotlin that processes traffic density data using Spring Batch. The application downloads CSV files containing traffic density information and processes them in batches.

## Technologies Used

- Kotlin 1.9.25
- Spring Boot 3.4.3
- Spring Batch
- Spring Data JPA
- PostgreSQL
- Gradle
- Java 21

## Prerequisites

- JDK 21
- PostgreSQL database
- Gradle (wrapper included)

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/Spring-Kotlin-Batch.git
   cd Spring-Kotlin-Batch
   ```

2. Configure the database:
   - Create a PostgreSQL database
   - Update the database configuration in `application.yml` if needed (default values provided)

3. Build the project:
   ```bash
   ./gradlew build
   ```

## Configuration

The application can be configured through `application.yml` or environment variables:

```yaml
SERVER_IP: localhost (default)
POSTGRESQL_PORT: 54321 (default)
POSTGRESQL_DB: batch (default)
POSTGRESQL_SCHEMA: public (default)
POSTGRESQL_USER: postgres (default)
POSTGRESQL_PASSWORD: senocak (default)
```

Additional configuration options:
- Server port: 8089
- Hikari connection pool settings
- JPA/Hibernate configuration
- Spring Batch settings

## Usage

### API Endpoints

1. Download Traffic Density Data:
```http
POST /api/batch/traffic-density/download
```
Parameters:
- `url` (optional): URL of the CSV file to download
  - Default: IBB traffic density data

2. Run Batch Job:
```http
POST /api/batch/traffic-density/run
```
Parameters:
- `csvName`: Name of the CSV file to process

### Example Usage

1. Download traffic density data:
```bash
curl -X POST "http://localhost:8089/api/batch/traffic-density/download"
```

2. Run the batch job:
```bash
curl -X POST "http://localhost:8089/api/batch/traffic-density/run?csvName=traffic_density_2024.02.20.10.30.00.csv"
```

## Features

- Asynchronous file download with progress tracking
- Batch processing of traffic density data
- Database storage of processed data
- RESTful API endpoints
- Configurable database connection
- Robust error handling

## Reference Documentation

For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.4.3/gradle-plugin)
* [Spring Batch](https://docs.spring.io/spring-boot/3.4.3/how-to/batch.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/3.4.3/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Web](https://docs.spring.io/spring-boot/3.4.3/reference/web/servlet.html)

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
