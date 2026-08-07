# ⚡ WB Price & Review Tracker («Пульс»)

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)
![Architecture](https://img.shields.io/badge/Architecture-3--Tier%20Hybrid-blue.svg)
![Security](https://img.shields.io/badge/Storage-SQLCipher%20Encrypted-red.svg)
![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)

**WB Price & Review Tracker («Пульс»)** — это современное мобильное приложение для отслеживания динамики цен и рейтинга отзывов на товары маркетплейса Wildberries. Приложение построено на базе высокопроизводительной 3-звенной гибридной архитектуры с защищенным шифрованием локальной базы данных.

---

## 🌟 Ключевые Возможности

- 🎨 **Премиальный интерфейс «Пульс»**: 3-точечные фиолетово-розовые градиенты, плавные свайп-действия на карточках, анимированные графики кривых Безье на Canvas и мгновенное переключение Светлой (`#F5F5F7`) и Тёмной (`#0A0A0E`) тем.
- 💳 **Точный расчет цены по WB Кошельку**: Приложение учитывает акционную цену по WB Кошельку/карте как основную цену покупки и вычисляет реальную выгоду за месяц.
- 🔍 **Универсальный парсер ссылок (`WbArticleExtractor`)**: Автоматическое извлечение артикула из любых вариаций ссылок Wildberries (`catalog/`, `detail.aspx`, мобильные URL, query-параметры `?nm=` / `?article=`) и чистых числовых строк.
- ⚡ **Каскадный перебор баскетов Wildberries**: Автоматическое сканирование распределенных CDN-узлов (`basket-01` .. `basket-45`), парсинг данных `v4/detail` и поддержка SSL-сертификатов Минцифры РФ.
- 🔐 **Аппаратно-зашифрованная локальная БД**: Хранилище SQLite зашифровано алгоритмом **SQLCipher for Android** с интеграцией ключей **Android KeyStore API (MasterKey AES-256 GCM)**.
- 🔔 **Фоновый мониторинг (WorkManager)**: Регулярный опрос цен в фоновом режиме с мгновенной отправкой Push-уведомлений при достижении установленной целевой цены.

---

## 🏛️ Архитектура Проекта (3-Звенная Гибридная)

```text
┌────────────────────────────────────────────────────────┐
│                   ФРОНТЕНД (UI)                        │
│   HTML5 / CSS3 / JavaScript (Chromium Hardware GPU)    │
│   Анимации Безье, Свайпы, Градиенты, Динамическая Тема  │
└───────────────────────────┬────────────────────────────┘
                            │ (window.WbBridge)
┌───────────────────────────▼────────────────────────────┐
│                    БЕКЕНД (Service)                    │
│   Kotlin Android Service + JavascriptInterface Bridge  │
│   WB API Parser (01..45), WorkManager Price Watcher    │
└───────────────────────────┬────────────────────────────┘
                            │ (MasterKey AES-256)
┌───────────────────────────▼────────────────────────────┐
│                ЗАШИФРОВАННОЕ ХРАНИЛИЩЕ                 │
│   SQLCipher Encrypted SQLite / Room Database           │
│   /data/data/com.wbtracker.app/databases/             │
└────────────────────────────────────────────────────────┘
```

### Стек Технологий:
- **Core / Language**: Kotlin, Coroutines, Flow.
- **Frontend Layer**: HTML5, CSS3, ES6 JavaScript, Canvas 2D Graphics.
- **Backend / Bridge Layer**: `@JavascriptInterface`, OkHttp3, Hilt Dependency Injection.
- **Security / Storage**: SQLCipher for Android, Android KeyStore MasterKey API, EncryptedSharedPreferences.
- **Background Tasks**: WorkManager, Android NotificationManager.

---

## 🛠️ Сборка и Запуск

### Требования:
- Android Studio Ladybug / Jellyfish (или новее)
- JDK 17+
- Android SDK 26+ (Android 8.0+)

### Инструкция по сборке:

1. Склонируйте репозиторий:
   ```bash
   git clone https://github.com/YuNoons/wbtracker.git
   cd wbtracker
   ```

2. Выполните сборку приложения:
   ```bash
   ./gradlew assembleDebug
   ```

3. Готовый APK-файл будет доступен по пути:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 Лицензия

Проект распространяется под лицензией MIT. Подробности в файле LICENSE.
