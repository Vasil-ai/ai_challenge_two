# SDD — декомпозиция на таски

**Назначение:** операционный бэклог реализации по [SDD.md](SDD.md) и карточкам [FEATURES.md](FEATURES.md).  
**Источник плана:** вертикальные срезы SDD §11.

---

## Ревью спецификации (кратко)

| Тема | Статус | Действие |
|------|--------|----------|
| Покрытие requirements.txt | Ок | F01–F20 + матрица SDD §10 |
| Модель Report / роли | Ок | SDD §0 согласована с F10, F17 |
| Waitlist в ER | На уточнение при коде | В §5 одновременно `RsvpRegistration` (status) и `WaitlistEntry` — выбрать **одну** каноническую схему (например только registration + `waitlistPosition`, или отдельная очередь) и отразить в Flyway |
| Гостевой репорт | Ок | SDD §0; в схеме БД — nullable `reporterUserId` + идентификатор сессии/IP hash при необходимости |
| Sequence-диаграммы | Частично | В SDD есть RSVP / cancel / check-in; нет отдельных seq для gallery approve и feedback — опциональный таск DOC-01 |
| Трассировка L19–22 | Ок | Строка «L53» в SDD §10 — это валидная строка ТЗ (поведение signed-in) |

---

## Эпики и порядок (как в SDD §11)

| Эпик | Срез SDD | Фичи |
|------|-----------|------|
| E1 | §11.1 | F01 + каркас репо |
| E2 | §11.2 | F02, F03, F04 |
| E3 | §11.3 | F05, F06 |
| E4 | §11.4 | F07, F08 |
| E5 | §11.5 | F09, F10, F14 |
| E6 | §11.6 | F11, F12, F13 |
| E7 | §11.7 | F15, F16, F17 |
| E8 | §11.8 | F18, F19, F20 |

---

## Таски по эпикам

Формат: **ID** — заголовок — **Фича** — критерий готовности (см. чеклисты в FEATURES).

### E1 — Каркас и идентичность

| ID | Задача | Фича | Definition of Done |
|----|--------|------|---------------------|
| T1.1 | Инициализация backend (Spring Boot 3, Java 21), health, конфиг профилей | — | Сборка `./mvnw verify` (или эквивалент) зелёная |
| T1.2 | Подключение PostgreSQL + Flyway baseline | — | Миграции накатываются на пустую БД |
| T1.3 | Модель `User` + интеграция Spring Security (OIDC/форма — по выбору из SDD §12) | F01 | Защищённый эндпоинт отдаёт 401 без сессии |
| T1.4 | `returnUrl`: сохранение, валидация same-origin, редирект после логина | F01 | AC F01 в FEATURES |
| T1.5 | Инициализация frontend (Vite React TS), роутинг, API-клиент | — | SPA собирается, ходит на API в dev |

### E2 — Host и события

| ID | Задача | Фича | Definition of Done |
|----|--------|------|---------------------|
| T2.1 | Сущности `Host`, `HostMembership`; POST become host; публичный GET по slug | F02 | AC F02 |
| T2.2 | Сущность `Event` + CRUD под hostId; enums lifecycle / visibility | F03 | AC F03 |
| T2.3 | Действия publish / unpublish / duplicate | F03 | Поведение Duplicate = новый Draft |
| T2.4 | UI: форма события, список черновиков, обложка (upload или URL — зафиксировать) | F03 | E2E ручной сценарий create → publish |
| T2.5 | Редактор: Free/Paid toggle, Paid disabled + tooltip | F04 | AC F04 |

### E3 — Открытие и шаринг

| ID | Задача | Фича | Definition of Done |
|----|--------|------|---------------------|
| T3.1 | GET explore: query, dates, location, includePast; только Public + Published | F05 | AC F05; Unlisted не в листинге |
| T3.2 | UI Explore + фильтры + бейдж Ended | F05 | Прошедшее событие не показывает RSVP |
| T3.3 | Публичная страница события (гость): мета OG/Twitter | F06 | AC F06 |
| T3.4 | Публичная страница Host: OG/Twitter | F06 | AC F06 |

### E4 — RSVP и билеты

| ID | Задача | Фича | Definition of Done |
|----|--------|------|---------------------|
| T4.1 | Транзакционный POST RSVP: confirmed vs waitlist; блокировка от гонок | F07 | AC F07 (capacity, FIFO) |
| T4.2 | DELETE RSVP + промоушн головы очереди + уведомление/флаг «promoted» | F07 | AC F07 (L25 видимость в приложении) |
| T4.3 | Увеличение capacity → промоушн до лимита свободных мест | F07 | Покрыть тестом или сценарием |
| T4.4 | `Ticket` + уникальный код; генерация payload для QR | F08 | AC F08 |
| T4.5 | Add to Calendar (.ics) | F08 | Файл открывается с корректным временем |
| T4.6 | GET my tickets (только upcoming / не ended) | F08 | AC F08 |

### E5 — Роли, права, check-in

| ID | Задача | Фича | Definition of Done |
|----|--------|------|---------------------|
| T5.1 | `InviteLink` + генерация ссылки; accept flow | F09 | AC F09 |
| T5.2 | Метод безопасности `@PreAuthorize` / проверки роли Host vs Checker на всех эндпоинтах из SDD §7 | F10 | AC F10 (403 для Checker на dashboard и т.д.) |
| T5.3 | Check-in POST по коду; 409 duplicate | F14 | AC F14 |
| T5.4 | Счётчики check-in; polling или SSE (зафиксировать в README) | F14 | Live counters |
| T5.5 | Undo last в рамках политики SDD §4 п.4 | F14 | AC F14 |
| T5.6 | UI страницы check-in для Checker/Host | F14 | Ручной ввод кода, клавиатура |

### E6 — Операции Host

| ID | Задача | Фича | Definition of Done |
|----|--------|------|---------------------|
| T6.1 | Dashboard summary: Going / Waitlist / Checked-in по событиям | F11 | AC F11 |
| T6.2 | CSV export: колонки L34 + BOM при необходимости | F12 | AC F12 |
| T6.3 | Закоммитить пример `*.csv` в репозиторий (артефакт L66) | F12 | Файл в task-2 |
| T6.4 | My Events: агрегация, фильтры, quick actions по роли | F13 | AC F13 |

### E7 — Контент и модерация

| ID | Задача | Фича | Definition of Done |
|----|--------|------|---------------------|
| T7.1 | POST feedback только после endAt | F15 | AC F15 |
| T7.2 | Gallery upload + статусы модерации; публично только approved | F16 | AC F16 |
| T7.3 | Host UI: очередь фото на approve | F16 | — |
| T7.4 | POST report (event/photo); гость — с rate limit / CAPTCHA по SDD §0 | F17 | AC F17 |
| T7.5 | GET queue + resolve hide/dismiss только Host владельца | F17 | AC F17; Checker 403 |

### E8 — Сдача

| ID | Задача | Фича | Definition of Done |
|----|--------|------|---------------------|
| T8.1 | Flyway/Java seed: 1 Host, 1 upcoming, 1 past Published | F18 | AC F18 |
| T8.2 | Dockerfile / инструкция деплоя; публичный URL | F19 | AC F19 |
| T8.3 | `report.md` по шаблону требований | F20 | L65 |
| T8.4 | README: сценарий Publish → RSVP → Ticket → Check-in | F20 | L67, не копипаст ТЗ |

---

## Опциональные документные таски

| ID | Задача |
|----|--------|
| DOC-01 | Дополнить SDD §6 sequence: approve gallery; POST feedback после end |
| DOC-02 | В SDD §5 явно описать выбранную waitlist-модель после решения в коде |

---

## Сводная матрица: таск → фича

```text
T1.* → F01        T5.* → F09,F10,F14
T2.* → F02–F04    T6.* → F11–F13
T3.* → F05–F06    T7.* → F15–F17
T4.* → F07–F08    T8.* → F18–F20
```

---

## Связанные файлы

- [SDD.md](SDD.md) — архитектура, API, инварианты, стек  
- [FEATURES.md](FEATURES.md) — детальные AC по F01–F20  
- [requirements.txt](../requirements.txt) — исходное ТЗ
