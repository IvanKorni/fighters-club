# Fight Club Game System

Система для онлайн-игры «Бойцовский клуб» (PvP-файтинг).
Микросервисная архитектура, обеспечивающая матчмейкинг, игровой процесс в реальном времени, статистику и управление профилями игроков.

---

## Архитектура

Система построена на базе микросервисов:

*   **API (Individuals API)**: Точка входа для аутентификации и регистрации. Интеграция с Keycloak.
*   **Person Service**: Управление профилями пользователей (CRUD).
*   **Queue Service**: Матчмейкинг и управление очередью игроков (Redis).
*   **Game Service**: Игровая логика, расчет ходов, урон.
*   **Stats Service**: История матчей, статистика и рейтинги.

### Инфраструктура
*   **Keycloak**: Identity Provider (SSO, OAuth2).
*   **PostgreSQL**: Основное хранилище данных (отдельные базы для каждого сервиса).
*   **Redis**: Очереди и кэширование.
*   **Observability**: Grafana, Prometheus, Tempo, Loki, Alloy.

---

## Стек технологий

*   **Java 24**, **Spring Boot 3.5**
*   **Spring WebFlux** (Reactive Stack)
*   **Spring Security** (OAuth2 Resource Server)
*   **OpenFeign** (Межсервисное взаимодействие)
*   **MapStruct**, **Lombok**
*   **Docker**, **Docker Compose**
*   **Testcontainers**, **WireMock**

---

## Быстрый старт

### Требования
*   Docker & Docker Compose
*   JDK 24
*   Make

### Запуск

Используйте `Makefile` для управления жизненным циклом проекта.

```bash
# Полный запуск (инфраструктура + сборка + сервисы)
make all

# Остановить всё
make stop
```

Также можно запустить через Docker Compose напрямую:
```bash
docker compose up -d --build
```

### Сервисы и Порты

| Сервис | Порт | Health Check | Swagger UI |
|--------|------|--------------|------------|
| **API Gateway** | 8091 | `http://localhost:8091/actuator/health` | [Swagger](http://localhost:8091/swagger-ui/index.html) |
| **Person Service** | 8092 | `http://localhost:8092/actuator/health` | [Swagger](http://localhost:8092/swagger-ui/index.html) |
| **Queue Service** | 8093 | `http://localhost:8093/actuator/health` | [Swagger](http://localhost:8093/swagger-ui/index.html) |
| **Game Service** | 8094 | `http://localhost:8094/actuator/health` | [Swagger](http://localhost:8094/swagger-ui/index.html) |
| **Stats Service** | 8095 | `http://localhost:8095/actuator/health` | [Swagger](http://localhost:8095/swagger-ui/index.html) |
| **Keycloak** | 8080 | `http://localhost:8080/` | - |

### Мониторинг и Логирование

| Инструмент | Порт | Логин/Пароль | Описание |
|------------|------|--------------|----------|
| **Grafana** | 3000 | `admin` / `admin` | Дашборды метрик и логов |
| **Prometheus** | 9090 | - | Сбор метрик |
| **Tempo** | 3200 | - | Трассировка запросов |
| **Loki** | 3100 | - | Агрегация логов |

---

## Разработка

### Структура проекта

*   `api/` - Входной шлюз и API аутентификации.
*   `person-service/` - Сервис хранения персональных данных.
*   `queue-service/` - Логика очередей и подбора соперников.
*   `game-service/` - Основная логика боя (ходы, таймеры).
*   `stats-service/` - Агрегация статистики и истории игр.
*   `infrastructure/` - Конфигурационные файлы инфраструктуры (Keycloak, Prometheus и др.).
*   `k8s/` - Манифесты для развертывания в Kubernetes.

### Тестирование

Для запуска интеграционных тестов (используются Testcontainers):

```bash
cd api
./gradlew test
```
