# 📜 BRIDGE CONTRACT — Спецификация Методов WbBridge (v1.3)

Документ описывает API контракты взаимодействия между JavaScript (WebView) и Kotlin бэкендом в приложении **WB Tracker**.

---

## ⚡ 1. Правила Вызова Методов (Async & Callbacks)
- Для быстрых чтения синхронных данных из `SharedPreferences` используются прямые вызовы `WbBridge.method()`.
- Для тяжелых вычислений, операций с файловой системой и сетью используются асинхронные вызовы или методы с callback-событиями:
  ```javascript
  // Вызов из JS
  WbBridge.getAlertsAsync("req_123");

  // Ответ из Kotlin
  window.onBridgeResult("req_123", JSON.stringify(data));
  ```

---

## 🛠️ 2. Реестр Контрактов Методов

### 2.1. Onboarding & Permissions
- `WbBridge.isOnboardingCompleted(): Boolean` — проверяет статус первого запуска.
- `WbBridge.completeOnboarding()` — помечает первый запуск как пройденный.
- `WbBridge.isBatteryOptimizationIgnored(): Boolean` — проверяет разрешение 24/7 фонового режима.
- `WbBridge.requestBatteryOptimizationExemption()` — открывает системный интент разрешения фоновой работы.
- `WbBridge.requestNotificationPermission()` — запрашивает разрешение на уведомления в Android 13+.

### 2.2. Alerts Center (Уведомления)
- `WbBridge.getAlertsAsync(requestId: String)` — запрашивает список сработавших алертов из Room таблицы `alert_history`.
- `WbBridge.markAlertRead(alertId: Long)` — помечает уведомление прочитанным.
- `WbBridge.clearAllAlerts()` — очищает историю уведомлений.

### 2.3. Data Export & Backup (Локально & Бесплатно)
- `WbBridge.exportCsv()` — экспортирует список отслеживаемых товаров в формат CSV.
- `WbBridge.exportJson()` — экспортирует полную резервную копию со всей историей цен в JSON.
- `WbBridge.importBackup()` — открывает системный файловый диалог для восстановления из резервной копии.

### 2.4. Insights & Analytics 2.0
- `WbBridge.getAnalyticsSummary()` — возвращает расширенный JSON инсайтов (лучшая экономия, самые стабильные товары, достижение целей).

### 2.5. Theme & Design System Packs
- `WbBridge.getActiveTheme(): String` — возвращает текущую активную тему и стиль (например, `finance`, `aurora`, `clean`, `neon`, `carbon`).
- `WbBridge.setActiveTheme(themeId: String)` — сохраняет выбор темы в нативном хранилище.

### 2.6. Native App Shortcuts
- `WbBridge.openShortcutAction(action: String)` — обрабатывает быстрые клики по иконке приложения (`add`, `favorites`, `analytics`, `refresh`).
