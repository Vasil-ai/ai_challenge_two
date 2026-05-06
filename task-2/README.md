# Event platform (task-2)

Spring Boot 3 + PostgreSQL + SPA (встроенный JS в `static/`, опционально React в `frontend/`) — события, RSVP, waitlist, билеты с QR, check-in, роли Host/Checker, галерея, отзывы, жалобы и экспорт CSV. Поведение и эпики описаны в [docs/SDD.md](docs/SDD.md), [docs/FEATURES.md](docs/FEATURES.md), [docs/SDD-TASKS.md](docs/SDD-TASKS.md).

## Требования

- JDK 21+, Maven 3.9+, Node 20+ и npm (только если собираете UI из `frontend/`).
- PostgreSQL 15+ (в репозитории — `docker-compose.yml`).

---

## Первый запуск с нуля

### Шаг 1. Репозиторий и ветка

Откройте терминал в каталоге задачи, например `task-2` (корень `pom.xml` и `docker-compose.yml`).

### Шаг 2. Поднять PostgreSQL

```bash
docker compose up -d
```

Дождитесь healthy-контейнера (имя сервиса обычно `postgres` или как в вашем `docker-compose.yml`). Порт по умолчанию **5432**, БД и пользователь совпадают с настройками Spring в `src/main/resources/application.properties` (или переопределяются переменными ниже).

### Шаг 3. Переменные окружения (по желанию)

Если не меняли `docker-compose.yml`, приложение подключится к БД из профиля по умолчанию. При необходимости задайте:

| Переменная | Назначение |
|------------|------------|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Подключение к PostgreSQL |
| `APP_BASE_URL` | Публичный URL (ссылки-приглашения, OG для `/share/...`). По умолчанию `http://localhost:8080` |
| `APP_UPLOAD_DIR` | Каталог файлов галереи (по умолчанию `./data/uploads`) |

В PowerShell для одной сессии, пример:

```powershell
$env:APP_BASE_URL = 'http://localhost:8080'
```

### Шаг 4. Собрать и запустить приложение

```bash
mvn spring-boot:run
```

На Windows, если Maven ругается на `JAVA_HOME`, см. раздел [Windows: JAVA_HOME](#windows-java_home) или:

```powershell
.\scripts\run.ps1 spring-boot:run
```

### Шаг 5. Убедиться, что старт прошёл

1. В логах нет ошибки Flyway / Hibernate validate / Spring Security.
2. В конце лога: Tomcat на порту **8080**.
3. В браузере откройте **http://localhost:8080** — должна открыться SPA (hash-роутинг `#/...`).

### Шаг 6. Пустая БД и сиды

При **первом** старте на **пустой** базе срабатывает `DataSeed`: создаются пользователи, хост `demo-community`, события, часть RSVP/билетов, галерея, отчёт, приглашение Checker. Повторный старт при непустой таблице `users` **не перезаписывает** данные.

Чтобы прогнать сиды заново:

```bash
docker compose down -v
docker compose up -d
mvn spring-boot:run
```

---

## Демо-учётные записи и фикстуры (после сидов)

Пароль у всех перечисленных учёток одинаковый: **`password123`**.

| Email | Роль в продукте | Зачем в ручных тестах |
|--------|-----------------|------------------------|
| `demo@example.com` | Host организации **Demo Community** | Дашборд, публикация, CSV, модерация галереи и жалоб, check-in как Host |
| `attendee@example.com` | Обычный участник | RSVP/очередь, отзыв на прошедшее событие, жалоба, фото в галерее (pending) |
| `checker@example.com` | Checker той же организации | Только страницы check-in по событиям хоста (без дашборда Host) |

Константы для сценариев (дублируются в коде в `SeedDemoConstants` / `DataSeed`):

| Артефакт | Значение | Назначение |
|----------|----------|------------|
| Код билета (upcoming meetup, пользователь demo) | `A1B2C3D4E5F67890` | Ручной ввод на **Check-in** для события **Community Meetup (Upcoming)** |
| Код билета (past, пользователь attendee) | `B2C3D4E5F67890A1` | Уже использован в сидах для check-in в прошлом (строки в CSV/дашборде) |
| Сырой токен приглашения Checker | `11111111111111111111111111111111` (32 символа `1`) | Принятие инвайта в SPA: `#/invite?token=...&hostId=<id>` (см. шаг получения `hostId` ниже) |

**Получить `hostId` для ссылок:** после входа как `demo@example.com` откройте в браузере или через curl:

`GET http://localhost:8080/api/hosts/by-slug/demo-community`

В JSON поле идентификатора хоста используйте в URL вида `#/host/<id>/dashboard` и в `hostId` для инвайта.

### Что уже лежит в базе (маппинг на SDD)

| Данные | SDD / фичи |
|--------|------------|
| Хост **demo-community**, пользователь **demo** — роль Host; **checker** — роль Checker | F02, F10 |
| Событие **Community Meetup (Upcoming)** — Public, Published, билет demo с фиксированным кодом | F05, F07, F08, F14 |
| **Past Workshop** — прошедшее, Public; attendee с билетом и **check-in** в прошлом; **отзыв 5★** | F05, F08, F14, F15 |
| **Capacity One Lab** — `capacity = 1`, demo **confirmed**, attendee **waitlisted** (позиция 1) | F07 |
| **Draft: Internal newsletter** — черновик | F03 |
| **Private team sync (Unlisted)** — Published, Unlisted (не в Explore) | F03, F05 |
| Фото в галерее: **pending** на upcoming (от attendee), **approved** на past | F16 |
| Жалоба **OPEN** на upcoming-событие (репортёр — attendee) | F17 |
| Строка в `invite_links` для роли Checker (токен выше) | F09 |

---

## Ручное тестирование по эпикам SDD (чеклист)

Отмечайте пункты по мере прохождения. Маршруты SPA — с префиксом `http://localhost:8080#/`.

### E1 — Каркас и идентичность (F01)

1. Без входа откройте `#/explore` — список событий загружается (`GET /api/events` разрешён гостю).
2. Откройте `#/login`, войдите как `demo@example.com` / `password123` — сессия, редирект/навигация с `My Tickets` / `My Events`.

### E2 — Host и события (F02–F04)

1. Публичная страница хоста: `#/hosts/by-slug/demo-community` (или через API выше).
2. Дашборд: `#/host/<hostId>/dashboard` — видны карточки событий, в т.ч. **Draft** и опубликованные.
3. **New event** — создайте черновик, сохраните; **Publish** на карточке; при необходимости **Unpublish** / **Duplicate** (F03).
4. В форме события убедитесь, что **Paid** недоступен (F04).

### E3 — Открытие и шаринг (F05–F06)

1. **Explore** `#/explore` — поиск по тексту, фильтры дат/локации, переключатель **Include past** — видно **Past Workshop** с признаком завершённости, RSVP скрыт для ended.
2. Убедитесь, что **Unlisted** событие **не** попадает в общий листинг, но открывается по прямой ссылке `#/events/<id>` (id возьмите из дашборда или API).
3. Поделитесь ссылкой превью: `/share/event/<eventId>` (краулеры / вставка в мессенджер).

### E4 — RSVP и билеты (F07–F08)

1. Выйдите, зарегистрируйте или войдите как **attendee** — на событии с свободными местами выполните **RSVP** → подтверждение и билет.
2. Событие **Capacity One Lab**: под **demo** место занято, под **attendee** вы уже в waitlist в сидах; войдите как **demo**, отмените RSVP на этом событии (если есть в UI) или увеличьте capacity в редакторе — attendee должен стать **confirmed** (промоушн F07).
3. **My Tickets** `#/tickets`, **Add to Calendar** (.ics) для предстоящего билета.

### E5 — Роли и check-in (F09–F10, F14)

1. **Инвайт Checker:** зарегистрируйте нового пользователя (например `invitee@example.com`), откройте  
   `#/invite?token=11111111111111111111111111111111&hostId=<hostId>`  
   — после принятия в организации появится членство (роль из сида — Checker). Повторное принятие тем же пользователем даёт конфликт.
2. Войдите как **checker** — доступны только сценарии check-in, **нет** полного Host dashboard (F10).
3. Войдите как **demo**, откройте **Check-in** для upcoming meetup, введите код **`A1B2C3D4E5F67890`** — успех; повтор — 409; **Undo last** — отмена последнего успешного скана в сессии (F14).

### E6 — Операции Host (F11–F13)

1. В дашборде сводные цифры Going / Waitlist / Checked-in по событиям.
2. Экспорт **CSV** RSVPs (кнопка/эндпоинт из UI дашборда) — UTF-8, при необходимости BOM; пример файла: [samples/rsvps-example.csv](samples/rsvps-example.csv).
3. **My Events** `#/mine` — агрегат по членствам и фильтры.

### E7 — Контент и модерация (F15–F17)

1. Отзыв на **Past Workshop** уже есть от attendee; под другим пользователем проверьте запрет отзыва до `endAt` на upcoming.
2. **Галерея:** под **demo** откройте очередь модерации `#/host/<hostId>/gallery-mod` (ссылка «Gallery moderation» в дашборде), одобрите **pending** фото сида.
3. **Reports** `#/host/<hostId>/reports` — открытая жалоба из сидов; **Dismiss** / **Hide** только под Host (не Checker).

### E8 — Сдача (F18–F20)

- Сиды соответствуют F18; `docker-compose`, `report.md`, этот README — F19–F20.

---

## Сквозной сценарий: Publish → RSVP → Ticket → Check-in

Ниже минимальный happy-path **поверх** уже существующих сидов (можно выполнять на свежей БД без ручного создания хоста).

1. **Войти как Host** `demo@example.com` / `password123`.
2. Открыть `#/host/<hostId>/dashboard` (hostId из `GET /api/hosts/by-slug/demo-community`).
3. **New event** — заполнить поля, сохранить черновик, нажать **Publish** (или взять уже опубликованное **Community Meetup** и перейти к шагу check-in).
4. **Выйти**, зарегистрировать **нового** пользователя или войти как `attendee@example.com`.
5. На странице предстоящего события нажать **RSVP** — получить билет и QR на странице события и в **My Tickets**.
6. **Войти как demo или checker**, открыть **Check-in** для этого `eventId`, ввести **публичный код билета** с карточки (для сидового билета demo на meetup используйте **`A1B2C3D4E5F67890`**).
7. Убедиться, что счётчики check-in обновляются; при втором вводе того же кода — отказ; **Undo last** откатывает последний успешный ввод в рамках сессии браузера.

---

## Опционально: сборка React UI

Полный React-клиент в [`frontend/`](frontend/) после `npm run build` перезапишет `static/`:

```bash
cd frontend
npm install
npm run build
cd ..
```

---

### Windows: JAVA_HOME

Maven ожидает, что **`JAVA_HOME`** указывает на **корень JDK** (каталог с `bin\java.exe`), а не на `bin`.

1. Установите JDK 21, например: `winget install Microsoft.OpenJDK.21`
2. Постоянно для пользователя (путь замените на свой):

   ```powershell
   [Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Path\To\jdk-21', 'User')
   ```

3. Либо запуск Maven:

   ```powershell
   .\scripts\run.ps1 spring-boot:run
   ```

4. Запись `JAVA_HOME` из текущего `java` в PATH:

   ```powershell
   .\scripts\set-java-home-user.ps1
   ```

---

## API

REST под префиксом `/api`. Аутентификация: сессионная cookie после `POST /api/auth/login` или `/api/auth/register`.

`GET /api/me` и ответы `POST /api/auth/login|register` возвращают пользователя и список **`hostMemberships`**: `{ hostId, role }` с `role` равным `HOST` или `CHECKER` — SPA использует это, чтобы не показывать ссылку Host **Dashboard** пользователю с ролью Checker.

## CI (GitHub Actions)

На каждый **pull request** (и push в `main` / `master`) запускается workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml): JDK 21, `mvn verify`.

### Где включить обязательную зелёную CI перед merge

Нужны права **Admin** (или «Manage branch protection rules») на репозитории. Название чека в списке обычно **`CI / build`** (`name` workflow = `CI`, `job` = `build`). Чек **появляется в списке только после хотя бы одного успешного запуска** этого workflow на этой ветке (часто достаточно открыть PR или сделать push в `main`).

**Вариант A — классические правила ветки**

1. Откройте репозиторий на GitHub → **Settings** (настройки репозитория, не вашего аккаунта).
2. Слева: **Code and automation** → **Branches**.
3. Блок **Branch protection rules** → кнопка **Add branch protection rule** (или **Add rule**).
4. В **Branch name pattern** введите имя ветки, например `main` или `master`.
5. Включите **Require status checks to pass before merging**.
6. В области **Status checks that are required** нажмите **Add checks** / поиск — выберите **`CI / build`** (или похожее имя из Actions). Если списка нет — см. раздел «Не вижу чек» ниже.
7. При необходимости включите **Require a pull request before merging** и сохраните (**Create** / **Save changes**).

**Вариант B — Rulesets (новый интерфейс GitHub)**

1. **Settings** → **Code and automation** → **Rules** → **Rulesets**.
2. **New ruleset** → **New branch ruleset**, target: нужная ветка (`main` и т.д.).
3. Включите правило вроде **Require status checks to pass** и добавьте обязательный check **`CI / build`**.

### Не вижу «Add rule» или не вижу чек `CI / build`

| Причина | Что сделать |
|--------|-------------|
| Нет прав **Admin** | Владелец репозитория должен выдать роль или настроить правила сам. |
| Открыт не тот **Settings** | Нужны настройки **репозитория** (вкладка репо → шестерёнка **Settings**), не глобальные настройки профиля. |
| Workflow ещё ни разу не выполнялся | Сделайте push в `.github/workflows/ci.yml` на `main` или откройте PR в `main` — дождитесь зелёного **Actions**. |
| Другая дефолтная ветка | Добавьте её в `on.push.branches` в `ci.yml` или переименуйте pattern в правиле под вашу ветку. |
| Форк / Actions выключены | В форке: **Settings** → **Actions** → **General** — разрешите Actions; для PR из форка чек идёт в базовый репозиторий по политике GitHub. |

Публичная справка GitHub: [About protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches), [Rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets).

## Документация по фичам

См. [docs/SDD.md](docs/SDD.md), [docs/FEATURES.md](docs/FEATURES.md), [docs/SDD-TASKS.md](docs/SDD-TASKS.md), [docs/project-map/PROJECT_MAP.md](docs/project-map/PROJECT_MAP.md) — карта репозитория и слоёв.
