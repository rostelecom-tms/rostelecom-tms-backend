# Rostelecom TMS Backend

## Ingestion запусков (Runs) из внешних систем

Для CI/CD (Jenkins, GitLab CI, TeamCity и т.д.) добавлены 2 входа с одинаковым поведением:

1. `POST /api/integrations/runs`
2. `POST /api/webhooks/runs`

Оба endpoint принимают массовую загрузку результатов прогонов.

### Аутентификация для ingestion

Для этих путей используется отдельный токен `RUNS_INGESTION_TOKEN`.

Передача токена:

1. Заголовком `X-Runs-Token: <token>` (предпочтительно)
2. Или query-параметром `?token=<token>`

Если токен не задан в конфиге приложения или не совпадает, сервер вернет `403 Forbidden`.

### Откуда взять токен и как сгенерировать

Токен не выдается системой автоматически. Его нужно сгенерировать и сохранить в окружение бэкенда.

Пример генерации в PowerShell (Windows):

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 } | ForEach-Object {[byte]$_}))
```

Пример генерации в Linux/macOS:

```bash
openssl rand -base64 48
```

Далее добавьте токен в `.env` (или переменные окружения контейнера):

```env
RUNS_INGESTION_TOKEN=вставьте_свой_длинный_секрет
```

После изменения переменных окружения перезапустите backend.

### Формат запроса (bulk)

```json
{
  "planId": 12,
  "executedBy": 5,
  "results": [
    {
      "caseId": 101,
      "statusSlug": "passed",
      "executedAt": "2026-04-15T09:00:00Z"
    },
    {
      "caseId": 102,
      "statusSlug": "failed",
      "executedAt": "2026-04-15T09:01:00Z"
    }
  ]
}
```

Поля:

1. `planId` — ID тест-плана
2. `executedBy` — ID исполнителя (опционально)
3. `results` — список результатов
4. `results[].caseId` — ID тест-кейса (должен входить в указанный план)
5. `results[].statusSlug` — статус (`passed`, `failed`, `broken`, `skipped`, либо другой существующий slug в `run_statuses`)
6. `results[].executedAt` — время запуска в ISO-8601

### Примеры вызова

Через integration endpoint:

```bash
curl -X POST "http://localhost:8080/api/integrations/runs" \
  -H "Content-Type: application/json" \
  -H "X-Runs-Token: ${RUNS_INGESTION_TOKEN}" \
  -d '{
    "planId": 12,
    "executedBy": 5,
    "results": [
      {"caseId": 101, "statusSlug": "passed", "executedAt": "2026-04-15T09:00:00Z"}
    ]
  }'
```

Через webhook endpoint:

```bash
curl -X POST "http://localhost:8080/api/webhooks/runs?token=${RUNS_INGESTION_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "planId": 12,
    "results": [
      {"caseId": 101, "statusSlug": "passed", "executedAt": "2026-04-15T09:00:00Z"}
    ]
  }'
```

### Ответ

При успехе возвращается `201 Created` и массив созданных запусков.
