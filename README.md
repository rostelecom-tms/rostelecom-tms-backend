<div align="center">

# Rostelecom TMS — Backend

<img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
<img src="https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
<img src="https://img.shields.io/badge/PostgreSQL-pgvector-4169E1?style=for-the-badge&logo=postgresql&logoColor=white"/>
<img src="https://img.shields.io/badge/Redis-Cache-DC382D?style=for-the-badge&logo=redis&logoColor=white"/>
<img src="https://img.shields.io/badge/Docker-ready-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>

</div>

---

## О проекте

TMS —  система управления тестированием, разработанная для внутренних нужд. REST API для системы управления тестированием. Бэкенд реализует полный жизненный цикл тестирования: от создания тест-кейсов до анализа дефектов с помощью LLM.

> Фронтенд находится в репозитории [rostelecom-tms-frontend](../rostelecom-tms-frontend)

**Основные возможности:**

- Иерархические тест-кейсы с группами, шагами и тегами
- Тест-планы с привязкой произвольного набора кейсов
- Bulk ingestion результатов из CI/CD через webhook
- RAG-анализ дефектов и семантический поиск кейсов (OpenAI / Ollama)
- JWT-аутентификация с ролевой моделью (admin / teamlead / user)
- Redis-кеширование горячих данных
- Дашборд со сводной статистикой

---

## Стек

| | Технология |
|---|---|
| **Runtime** | Java 21, Spring Boot 4.0 |
| **Web / Security** | Spring MVC, Spring Security, JWT (jjwt 0.13) |
| **База данных** | PostgreSQL 16 + расширение `pgvector` |
| **ORM / Миграции** | Spring Data JPA (Hibernate), Liquibase |
| **Кеш** | Redis 7, Spring Cache |
| **AI** | OpenAI API, Ollama (локальные LLM) |
| **PDF** | Apache PDFBox |
| **Документация** | SpringDoc OpenAPI 3 (Swagger UI) |
| **Сборка** | Maven 3.9 |
| **Контейнеры** | Docker (multi-stage, `eclipse-temurin:21-jre-alpine`) |
| **Утилиты** | Lombok |

---

## Быстрый старт

### Предварительные требования

- Docker и Docker Compose

Весь стек (PostgreSQL + pgvector, Redis, Ollama, бэкенд, фронтенд) поднимается одной командой. Устанавливать Java, Maven или Node.js локально не нужно.

### 1. Склонировать оба репозитория

```bash
git clone https://github.com/rostelecom-tms/rostelecom-tms-backend
git clone https://github.com/rostelecom-tms/rostelecom-tms-frontend
```

Репозитории должны лежать рядом — `compose.prod.yaml` обращается к фронтенду по относительному пути `../rostelecom-tms-frontend`.

### 2. Заполнить `.env`

В корне репозитория бэкенда создай файл `.env`:

```env
POSTGRES_USER=tms
POSTGRES_PASSWORD=tms
POSTGRES_DB=tms
APP_URL=http://localhost
VITE_API_URL=http://localhost/api
RUNS_INGESTION_TOKEN=change_me_in_production
```

### 3. Запустить

```bash
docker compose -f compose.prod.yaml up -d
```

Приложение доступно на `http://localhost`. При первом запуске Liquibase автоматически накатит все 17 миграций и засеет базу тестовыми данными.
> [!TIP]
> При первом запуске Ollama автоматически скачает модели согласно `ollama-bootstrap.sh`. Это может занять 5-10 минут.

### Остановить

```bash
docker compose -f compose.prod.yaml down
```

Данные сохраняются в именованных volumes (`tms-pgdata`, `tms-redisdata`, `tms-ollamadata`). Для полного сброса вместе с данными:

```bash
docker compose -f compose.prod.yaml down -v
```

---

## API

Все эндпоинты работают под префиксом `/api`. Для защищённых запросов:

```
Authorization: Bearer <jwt_token>
```

<details>
<summary> Все эндпоинты API</summary>
  
### Аутентификация

| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/auth/login` | Получить JWT-токен |
| `POST` | `/auth/register` | Отправить заявку на регистрацию |
| `POST` | `/auth/logout` | Выйти |
| `GET`  | `/auth/me` | Текущий пользователь |

### Тест-кейсы

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/cases` | Список кейсов (пагинация, фильтры) |
| `POST` | `/cases` | Создать кейс  |
| `GET` | `/cases/{id}` | Получить кейс  |
| `PUT` | `/cases/{id}` | Обновить кейс  |
| `DELETE` | `/cases/{id}` | Удалить кейс  |
| `GET/POST/DELETE` | `/cases/groups` | Управление группами  |
| `GET/POST/DELETE` | `/cases/{id}/steps` | Шаги кейса  |

### Тест-планы

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/plans` | Список планов  |
| `POST` | `/plans` | Создать план  |
| `PUT` | `/plans/{id}` | Обновить план  |
| `DELETE` | `/plans/{id}` | Удалить план  |
| `POST/DELETE` | `/plans/{id}/cases` | Привязать / открепить кейсы  |

### Запуски и CI/CD

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/runs` | Список запусков  |
| `POST` | `/integrations/runs` | Bulk ingestion из CI/CD |

Для CI/CD интеграции передавай токен через заголовок `X-Runs-Token` или query-параметр `?token=`.

### Дефекты и дашборд

| Метод | Путь | Описание |
|-------|------|----------|
| `GET/POST/PUT/DELETE` | `/defects` | CRUD дефектов  |
| `GET` | `/dashboard` | Сводная статистика  |
| `GET` | `/search` | Полнотекстовый поиск  |
</details>


## AI-функции

Бэкенд поддерживает два типа провайдеров — **embedding** и **LLM**. Провайдер указывается в теле каждого запроса, что позволяет переключаться между облаком и локальным inference без перезапуска сервиса.

### Провайдеры

| Тип | `provider` | Модель по умолчанию |
|-----|-----------|---------------------|
| Embedding | `ollama` | `nomic-embed-text` |
| Embedding | `openai` | `text-embedding-3-small` |
| LLM | `ollama` | `qwen3:4b` |
| LLM | `openai` | `gpt-4o-mini` |

### RAG-эндпоинты

| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/rag/defects/{id}/analysis` | AI-анализ дефекта по похожим кейсам |
| `POST` | `/rag/cases/{id}/suggest` | Семантически похожие кейсы |
| `POST` | `/rag/cases/suggest` | Похожие кейсы по произвольному тексту |
| `POST` | `/rag/logs/analysis` | Анализ логов из PDF-вложения дефекта |
| `GET` | `/rag/logs/history` | История AI-запросов по дефекту |

Векторные эмбеддинги хранятся в PostgreSQL через расширение **pgvector** (миграции 013–014, размерность 768).

### Пример запроса

```json
POST /api/rag/defects/42/analysis
Authorization: Bearer <token>

{
  "embeddingProvider": "ollama",
  "llmProvider": "ollama",
  "limit": 5,
  "onlySolved": true
}
```

---

## Конфигурация

Приложение читает переменные из `.env`-файла в корне (dev) или из окружения контейнера (prod).

| Переменная | Описание | По умолчанию |
|-----------|----------|:------------:|
| `POSTGRES_USER` | Пользователь PostgreSQL | — |
| `POSTGRES_PASSWORD` | Пароль PostgreSQL | — |
| `POSTGRES_DB` | Имя БД _(только prod)_ | — |
| `REDIS_HOST` | Хост Redis | `localhost` |
| `REDIS_PORT` | Порт Redis | `6379` |
| `REDIS_PASSWORD` | Пароль Redis | — |
| `APP_URL` | CORS origin (URL фронтенда) | — |
| `RUNS_INGESTION_TOKEN` | Токен для CI/CD bulk ingestion | — |
| `OLLAMA_BASE_URL` | URL Ollama-сервера | `http://localhost:11434` |

### Redis — TTL кешей

| Кеш | TTL |
|-----|-----|
| `dashboard` | 30 сек |
| `cases_page`, `plans_page` | 45 сек |
| `runs_page` | 30 сек |
| `projects_list` | 60 сек |
| `roles`, `run_statuses` | 12 часов |

Кеш работает в режиме **fail-open** — ошибки Redis не прерывают запросы, приложение обращается напрямую в БД.

---

## Миграции БД

Схема версионируется через Liquibase и накатывается автоматически при старте.

| Миграция | Описание |
|----------|----------|
| 001 | Пользователи и роли |
| 002 | Тест-кейсы и шаги |
| 003 | Тест-планы |
| 004 | Запуски и статусы |
| 005–008 | Seed-данные (роли, статусы, кейсы, планы) |
| 009 | Teamlead-роль, флаг `canCreatePlans` |
| 010 | Проекты и участники |
| 011 | Иерархия групп кейсов |
| 012 | Теги кейсов |
| 013–014 | pgvector — эмбеддинги (768 dim) |
| 015 | История AI-анализа логов |
| 016 | Запросы на доступ к проекту |
| 017 | Заявки на регистрацию |

---

## Импорт / Экспорт

### Экспорт

REST API предоставляет эндпоинты для выгрузки тест-кейсов в CSV и PDF форматы:

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/cases/export?format=csv&groupId=..&planId=..&title=..&tag=..` | Экспортировать список кейсов в CSV с фильтрацией |
| `GET` | `/cases/{id}/export/pdf` | Экспортировать один кейс в PDF |

**CSV содержит**: ID, название, группа (ID/name/slug), теги, описание, предусловия, постусловия, информацию о шагах (порядок, название, действие, ожидаемый результат).

**PDF содержит**: красиво отформатированный документ A4 с полной информацией о кейсе и его шагах.

### Импорт

REST API поддерживает импорт тест-кейсов из CSV и PDF файлов:

| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/cases/import?format=csv&groupId=..` | Импортировать кейсы (multipart/form-data) |

**Параметры импорта:**
- `format` (опционально) — формат файла: `csv` или `pdf` (default: `csv`)
- `file` (обязателен) — файл, экспортированный из TMS
- `groupId` (опционально для CSV, обязателен для PDF) — группа по умолчанию для кейсов
