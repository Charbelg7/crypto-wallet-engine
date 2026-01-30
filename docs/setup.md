# Setup Guide

Complete guide to setting up and running the Cryptocurrency Wallet & Trading Engine Simulator.

## Prerequisites

### Required Software

- **Java 21+** - [Download](https://adoptium.net/)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose** - [Download](https://www.docker.com/products/docker-desktop)

### Verify Installation

```bash
java -version    # Should show Java 21
mvn -version     # Should show Maven 3.9+
docker --version # Should show Docker 20.10+
docker-compose --version # Should show docker-compose 2.0+
```

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd crypto-wallet-engine
```

### 2. Start Infrastructure Services

Start PostgreSQL, Zookeeper, and Kafka:

```bash
docker-compose up -d postgres zookeeper kafka
```

Wait for services to be healthy (check logs):

```bash
docker-compose logs -f
```

You should see:
- PostgreSQL: `database system is ready to accept connections`
- Zookeeper: `binding to port 0.0.0.0/0.0.0.0:2181`
- Kafka: `started (kafka.server.KafkaServer)`

### 3. Build the Application

```bash
mvn clean package
```

This will:
- Download dependencies
- Compile Java code
- Run unit tests
- Package JAR file

### 4. Run the Application

**Option A: Maven**
```bash
mvn spring-boot:run
```

**Option B: JAR**
```bash
java -jar target/crypto-wallet-engine-1.0.0-SNAPSHOT.jar
```

**Option C: Docker**
```bash
docker-compose up --build app
```

### 5. Verify Application

Check health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

## Configuration

### Application Properties

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/crypto_wallet
spring.datasource.username=crypto_user
spring.datasource.password=crypto_password

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# Risk Engine
crypto.risk.max-exposure-usdt=100000
crypto.risk.enabled=true
```

### Docker Compose Configuration

Edit `docker-compose.yml` to customize:
- Database credentials
- Kafka settings
- Port mappings

## Development Setup

### IDE Setup

**IntelliJ IDEA:**
1. File → Open → Select project directory
2. Maven will auto-import dependencies
3. Set JDK 21 in Project Structure
4. Enable annotation processing for Lombok and MapStruct

**Eclipse:**
1. File → Import → Maven → Existing Maven Projects
2. Select project directory
3. Configure Java 21 in project properties

**VS Code:**
1. Install Java Extension Pack
2. Open project folder
3. Maven will auto-detect

### Database Schema

The application uses JPA with `spring.jpa.hibernate.ddl-auto=update`, so the schema is created automatically on first run.

To reset the database:

```bash
docker-compose down -v  # Remove volumes
docker-compose up -d postgres
```

### Kafka Topics

Topics are auto-created on first use. To manually create:

```bash
docker exec -it crypto-kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic order-placed-events \
  --partitions 3 \
  --replication-factor 1
```

## Running Tests

### Unit Tests

```bash
mvn test
```

### Integration Tests

Integration tests use Testcontainers (requires Docker):

```bash
mvn verify
```

### Load Tests (Gatling)

1. Start the application
2. Run Gatling:

```bash
mvn gatling:test
```

View results in `target/gatling/`

## Troubleshooting

### Port Already in Use

**Error:** `Port 8080 is already in use`

**Solution:**
```bash
# Find process using port 8080
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill process or change port in application.properties
server.port=8081
```

### Database Connection Failed

**Error:** `Connection refused` or `Connection timeout`

**Solution:**
1. Check PostgreSQL is running:
   ```bash
   docker-compose ps postgres
   ```
2. Check connection string in `application.properties`
3. Verify credentials match `docker-compose.yml`

### Kafka Connection Failed

**Error:** `Bootstrap broker localhost:9092 disconnected`

**Solution:**
1. Check Kafka is running:
   ```bash
   docker-compose ps kafka
   ```
2. Check Kafka logs:
   ```bash
   docker-compose logs kafka
   ```
3. Verify Kafka is ready:
   ```bash
   docker exec crypto-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
   ```

### Maven Build Fails

**Error:** `Could not resolve dependencies`

**Solution:**
1. Clear Maven cache:
   ```bash
   rm -rf ~/.m2/repository
   mvn clean install
   ```
2. Check internet connection
3. Verify Maven settings.xml

### Testcontainers Tests Fail

**Error:** `Could not find a valid Docker environment`

**Solution:**
1. Ensure Docker is running
2. Check Docker daemon:
   ```bash
   docker ps
   ```
3. Verify Docker socket permissions (Linux)

## Production Setup

### Environment Variables

Set these in production:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/crypto_wallet
SPRING_DATASOURCE_USERNAME=prod_user
SPRING_DATASOURCE_PASSWORD=<secure-password>
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-prod:9092
SPRING_PROFILES_ACTIVE=production
```

### Docker Production Build

```bash
docker build -t crypto-wallet-engine:latest .
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=... \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=... \
  crypto-wallet-engine:latest
```

### Health Checks

Configure health checks:

```bash
curl http://localhost:8080/actuator/health
```

## Next Steps

- Read [API Documentation](./api.md) to learn how to use the APIs
- Check [Architecture Overview](./architecture.md) to understand the design
- Review [Testing Guide](./testing.md) for testing strategies
