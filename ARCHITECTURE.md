# SafeBox — как устроен проект

Документ описывает архитектуру, потоки данных и **как именно шифруются записи**. Читайте сверху вниз.

---

## Общая схема

```
┌──────────────────────────────────────────────────────────────────┐
│  JavaFX UI (FXML + CSS)                                          │
│  LoginController → MainController → EditController / Generator     │
└────────────────────────────┬─────────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────────┐
│  MainApp — точка входа, сборка сервисов, тема, язык              │
└────────────────────────────┬─────────────────────────────────────┘
                             │
     ┌───────────────────────┼───────────────────────┐
     ▼                       ▼                       ▼
PasswordManager      SessionManager          ClipboardService
VaultTransferService GeneratorService        DialogHelper, I18n
     │                       DonationInfo
     ▼
PasswordRepository → DatabaseConnection (SQLite)
     │
     ▼
CryptoService (PBKDF2 + AES-256-GCM)
```

**Главный принцип:** мастер-пароль **никогда не сохраняется** на диске. Из него выводится ключ шифрования. В БД лежат только зашифрованные пароли и заметки + хэш-проверка (verifier) для входа.

---

## Как шифруются данные

### Что защищено, а что нет

| Поле записи | В SQLite | Почему |
|-------------|----------|--------|
| `site` | Открытый текст | Нужен быстрый поиск по сайту |
| `login` | Открытый текст | Нужен быстрый поиск по логину |
| `password` | **AES-256-GCM** | Секрет |
| `notes` | **AES-256-GCM** | Секрет |
| Мастер-пароль | **Не хранится** | Только verifier (хэш ключа) |

### Ключ из мастер-пароля (PBKDF2)

Файл: `CryptoService.java`

1. При создании хранилища генерируется случайная **соль** 16 байт → сохраняется в `~/.safebox/salt.bin`.
2. Мастер-пароль + соль проходят через **PBKDF2-HMAC-SHA256**, **600 000 итераций** → получается **AES-256 ключ** (256 бит).
3. Этот ключ живёт в памяти (`SessionManager`) пока сейф разблокирован.

```
мастер-пароль + salt.bin ──PBKDF2(600k)──► SecretKey (AES-256)
```

Мастер-пароль может быть **любой длины** (кроме пустого).

### Проверка пароля при входе (verifier)

Сам пароль в БД не пишется. Вместо этого:

1. Из пароля и соли выводится ключ (как выше).
2. Берутся байты ключа → **SHA-256** → Base64-строка.
3. Эта строка сохраняется в таблице `settings` (`key = 'master_verifier'`).

При входе процесс повторяется и сравнивается с сохранённым verifier через `MessageDigest.isEqual()` (защита от timing-атак на уровне сравнения строк).

### Шифрование полей записи (AES-256-GCM)

Для каждого поля `password` и `notes` при сохранении:

1. Генерируется случайный **IV** 12 байт (уникальный для каждого шифрования).
2. Текст шифруется **AES/GCM/NoPadding**, тег аутентификации 128 бит.
3. В БД пишется Base64 от конкатенации: `IV (12 байт) + ciphertext`.

```
plaintext ──AES-256-GCM(ключ сессии, случайный IV)──► Base64(IV ‖ ciphertext)
                                                          │
                                                          ▼
                                              колонка password_encrypted
                                              или notes_encrypted
```

При чтении: декодирование Base64 → отделение IV → расшифровка. Если ключ неверный или данные повреждены — ошибка дешифрования.

**Важно:** у каждой записи и каждого поля свой IV. Одинаковые пароли в разных записях в БД выглядят по-разному.

### Смена мастер-пароля

`PasswordManager.changeMasterPassword()`:

1. Проверяет текущий пароль через verifier.
2. Загружает все записи и расшифровывает старым ключом.
3. Генерирует **новую соль**, новый ключ, новый verifier.
4. В **одной SQL-транзакции** перешифровывает все записи новым ключом и обновляет verifier.
5. Записывает новый `salt.bin`.
6. `SessionManager.rotateKey()` обновляет ключ в памяти без выхода из приложения.

### Экспорт в файл `.safebox`

Отдельный слой — `VaultTransferService` + `VaultTransferCodec`.

1. Все записи сериализуются в бинарный формат (сайт, логин, пароль, заметки, даты).
2. Пользователь задаёт **пароль экспорта** (отдельный от мастер-пароля, мин. 8 символов).
3. Полезная нагрузка шифруется тем же **AES-256-GCM** через ключ из пароля экспорта + новая соль.
4. Файл: `SBX1` (4 байта) + соль (16 байт) + Base64-шифротекст.

На другом ПК мастер-пароль может быть другим — для импорта нужен только пароль экспорта.

### Что хранится на диске (`~/.safebox/`)

| Файл / объект | Содержимое |
|---------------|------------|
| `safebox.db` | SQLite: записи, verifier, зашифрованные поля |
| `salt.bin` | Соль PBKDF2 (16 байт) |
| `theme.txt` | `light` или `dark` |
| `locale.txt` | `en` или `ru` |

---

## Схема базы данных

Таблица **`passwords`**:

| Колонка | Тип | Описание |
|---------|-----|----------|
| `id` | INTEGER PK | Автоинкремент |
| `site` | TEXT | Сайт (открытый текст) |
| `login` | TEXT | Логин (открытый текст) |
| `password_encrypted` | TEXT | Зашифрованный пароль |
| `notes_encrypted` | TEXT | Зашифрованные заметки |
| `created_at` | INTEGER | Unix ms |
| `updated_at` | INTEGER | Unix ms |

Таблица **`settings`**:

| key | value |
|-----|-------|
| `master_verifier` | Base64 SHA-256 от ключа |

---

## Точка входа — `MainApp`

`src/main/java/com/safebox/MainApp.java`

- Создаёт сервисы: `CryptoService`, `DatabaseConnection`, `PasswordRepository`, `PasswordManager`, `VaultTransferService`, `GeneratorService`, `ClipboardService`, `SessionManager`.
- Загружает тему и язык (`I18n.init()`).
- Frameless-окно (`StageStyle.UNDECORATED`) + кастомная шапка (`WindowChrome`).
- `showLogin()` / `showMain()` — переключение экранов.
- `toggleLanguage()` — EN ↔ RU, обновление всех зарегистрированных UI-колбэков.

---

## Слой UI — контроллеры

| Класс | FXML | Назначение |
|-------|------|------------|
| `LoginController` | `login.fxml` | Создание хранилища / вход |
| `MainController` | `main.fxml` | Список, поиск, CRUD, экспорт, донат |
| `EditController` | `edit.fxml` | Добавление / редактирование записи |
| `GeneratorController` | `generator.fxml` | Генератор паролей |

### `LoginController`

- Первый запуск: `PasswordManager.setupVault()` → `SessionManager.start()` → главное окно.
- Повторный вход: `unlockVault()` → проверка verifier → сессия.
- «Забыли пароль?» → `DialogHelper.showWarning()` (восстановление невозможно).
- Блок доната внизу: адрес кошелька, копирование, подробности.

### `MainController`

**Таблица записей**

- Колонки: сайт, логин, дата обновления.
- Множественный выбор (Ctrl / Shift).
- Клик по строке → панель деталей внизу.

**Панель деталей**

- Показывает сайт, логин, пароль (скрыт точками), заметки.
- Клик по сайту / логину / паролю → копирование + toast-уведомление.
- Кнопка Show/Hide для пароля.
- Длинные заметки: предпросмотр + «Читать полностью» с прокруткой.

**Верхняя панель**

- Export / Import — перенос `.safebox` между ПК.
- Theme, Language (EN/RU), Lock, **Change password**.

**Нижняя панель**

- Add, Edit, Delete, Bulk delete, Generate password.
- Таймер автоблокировки (5 мин).

**Поддержка проекта**

- Полоска с EVM-адресом, кнопки Copy и Details (`DonationInfo`).

### `EditController` / `GeneratorController`

- Редактирование полей записи, встроенный вызов генератора.
- Сохранение через `PasswordManager.saveEntry()` с шифрованием.

### Диалоги — `DialogHelper`

Кастомные скруглённые окна (`StageStyle.TRANSPARENT` + `WindowClip`):

| Метод | Назначение |
|-------|------------|
| `showInfo` | Информация |
| `showWarning` | Предупреждения (оранжевая шапка) |
| `showError` | Ошибки (красная шапка) |
| `showConfirm` | Подтверждение (удаление и т.д.) |
| `showChoice` | Выбор из двух вариантов |
| `showPasswordPrompt` | Пароль экспорта/импорта |
| `showChangePassword` | Смена мастер-пароля |

FXML: `src/main/resources/fxml/dialogs/`. Стили: `dialogs.css` + тема.

Удаление использует `EntryFormatHelper` — короткий предпросмотр (сайт/логин с обрезкой), без длинных заметок в диалоге.

---

## Слой сервисов

### `PasswordManager`

| Метод | Описание |
|-------|----------|
| `isFirstRun()` | Нет `salt.bin` или пустая БД |
| `setupVault(password)` | Создание хранилища |
| `unlockVault(password)` | Вход, возврат ключа |
| `changeMasterPassword(current, new, key)` | Смена пароля + перешифровка |
| `listEntries(key, filter)` | Список с расшифровкой |
| `saveEntry(entry, key)` | Insert / update |
| `deleteEntry(id)` | Удаление одной |
| `deleteEntries(ids)` | Массовое удаление |
| `deleteAllEntries()` | Очистка хранилища |
| `importEntries(entries, key)` | Импорт как новых записей |
| `countEntries()` | Количество записей |

### `VaultTransferService`

- `export(path, entries, exportPassword)` — файл `.safebox`.
- `importFrom(path, exportPassword)` — расшифровка и список записей.

### `GeneratorService`

Случайный пароль 8–128 символов, настраиваемые наборы символов.

### `ClipboardService`

Копирование в буфер + автоочистка через **30 секунд**.

### `SessionManager`

- Хранит `SecretKey` и копию мастер-пароля в памяти.
- Автоблокировка через **5 минут** бездействия (`touch()` сбрасывает таймер).
- `lock()` — затирает ключ и пароль, возврат на экран входа.
- `rotateKey()` — обновление ключа после смены пароля.

---

## Утилиты

| Класс | Назначение |
|-------|------------|
| `I18n` | Локализация EN/RU (`messages_*.properties`) |
| `AppPaths` | Пути к `~/.safebox/` |
| `WindowChrome` | Перетаскивание окна, кнопки −/× |
| `WindowClip` | Скругление углов диалогов |
| `DialogHelper` | Показ кастомных диалогов |
| `EntryFormatHelper` | Форматирование текста для диалогов удаления |
| `DonationInfo` | Адрес кошелька и диалог поддержки |

---

## Потоки данных

### Первый запуск

```
Ввод мастер-пароля
  → setupVault()
      → salt.bin
      → verifier в settings
      → deriveKey() → SecretKey
  → SessionManager.start()
  → showMain()
```

### Вход

```
Мастер-пароль
  → unlockVault() → verifyPassword() → deriveKey()
  → SessionManager.start()
  → showMain()
```

### Сохранение записи

```
EditController.onSave()
  → getEncryptionKey()
  → saveEntry()
      → encrypt(password), encrypt(notes)
      → INSERT / UPDATE в passwords
```

### Копирование

```
Выбор строки → панель деталей
  → клик по полю → ClipboardService.copy() → toast
```

### Экспорт / импорт

```
Export: listEntries → VaultTransferService.export(.safebox)
Import: importFrom() → importEntries() как новые записи
```

### Смена мастер-пароля

```
Change password → диалог (текущий + новый + подтверждение)
  → changeMasterPassword() → перешифровка всех записей
  → rotateKey() → toast
```

### Массовое удаление

```
Bulk delete → выбор: выбранные / все
  → confirm с EntryFormatHelper
  → deleteEntries() / deleteAllEntries()
```

---

## Ресурсы

```
src/main/resources/
├── fxml/
│   ├── login.fxml, main.fxml, edit.fxml, generator.fxml
│   └── dialogs/          # info, confirm, choice, password-prompt, change-password
├── css/
│   ├── light.css, dark.css
│   └── dialogs.css
├── i18n/
│   ├── messages_en.properties
│   └── messages_ru.properties
└── images/icon.png
```

---

## Тесты

`src/test/java/com/safebox/service/`:

| Тест | Что проверяет |
|------|---------------|
| `CryptoServiceTest` | Шифрование, verifier |
| `GeneratorServiceTest` | Длина и charset генератора |
| `PasswordManagerIntegrationTest` | setup → save → list |
| `PasswordManagerBulkDeleteTest` | Массовое удаление |
| `PasswordManagerChangePasswordTest` | Смена пароля, короткий пароль |
| `VaultTransferServiceTest` | Round-trip экспорта |

```bash
mvn clean test
```

---

## Сборка

- **Java 17**, **JavaFX 17**, **SQLite**, **Maven**.
- `module-info.java` — JPMS-модуль `com.safebox`.
- GitHub Actions: `.github/workflows/release.yml`.
- Запуск: `mvn clean javafx:run`.

---

## Что не коммитить

`.gitignore`: `target/`, `dist/`, `.idea/`, `.tools/`, `~/.safebox/`, `*.db`, `salt.bin`.

---

## Быстрый навигатор

| Вопрос | Файл |
|--------|------|
| Старт приложения | `MainApp.java` |
| Вход / создание сейфа | `LoginController.java`, `PasswordManager.java` |
| **Как шифруется** | `CryptoService.java` |
| SQL и схема | `DatabaseConnection.java`, `PasswordRepository.java` |
| Экспорт между ПК | `VaultTransferService.java` |
| Смена пароля | `PasswordManager.changeMasterPassword()` |
| Сессия и автоблокировка | `SessionManager.java` |
| UI строки | `messages_*.properties`, `I18n.java` |
| Диалоги | `DialogHelper.java`, `fxml/dialogs/` |
| Донат | `DonationInfo.java` |
