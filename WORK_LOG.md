# 📋 WORK LOG — WB Price & Review Tracker (v1.3)

Краткий журнал ключевых релизов проекта.

## [2026-08-13 17:54] — WB Tracker v1.4.1 (Финальный Релизный Полишинг: Единая Система Темы, Честные Дельты Цен, Нативный Undo, Импорт Бэкапов, RAF Canvas & 100% QA Approved) — Вердикт: [APPROVED] ✅
- **Статус сборки**: `BUILD SUCCESSFUL` ✅
- **Изменения**:
  1. **Единый источник тем**: Из `css/components.css` удален дублирующий `:root`, все цвета тем читаются из `css/themes.css`.
  2. **Честная карточка товара**: Заменен маркетинговый процент скидки на честную дельту цен (`-214 ₽` / `+80 ₽` / `Без изменений`).
  3. **Нативный Undo**: Реализован метод `restoreProduct(id)` в `ProductDao` и `WbBridge.kt`, мгновенно восстанавливающий `isTracking = true` без дублирования историй цен.
  4. **Нативный Android**: Создание канала уведомлений в `WbTrackerApp.kt`, `openProductById(id)` диплинк, реальный импорт JSON бэкапов через `ACTION_OPEN_DOCUMENT`.
  5. **Профиль и Графики**: Динамические цвета Canvas из CSS (`--chart-line`, `--chart-fill`), шрифты 12-13px, RAF троттлинг скруббера, тумблер `#notifications-toggle` и совпадение ID селектора интервала.

---

## [2026-08-13 17:45] — WB Tracker v1.4.1 (Чистовая Стабилизация Фронтенда, Защита БД MIGRATION_3_4, Отмена Агрессивной Батареи, Glassmorphism Confirm & Undo) — Вердикт: [APPROVED] ✅
- **Статус сборки**: `BUILD SUCCESSFUL` ✅
- **Изменения**:
  1. **Чистота `index.html`**: Полностью вырезаны все inline `<style>` и inline `<script>`. Фронтенд на 100% разнесен по 4 CSS модулям (`tokens.css`, `themes.css`, `components.css`, `screens.css`) и 14 JS модулям.
  2. **Безопасность БД Room**: Удалена `fallbackToDestructiveMigration()`, внедрена явная миграция `MIGRATION_3_4` с созданием `alert_history` и поддержкой `oldPrice`.
  3. **Android UX**: Удален авто-вызов оптимизации батареи при старте в `MainActivity.kt`. Добавлен runtime запрос `POST_NOTIFICATIONS` на Android 13+, нативный интент `openProductUrl` и буферизация ярлыков `pendingShortcutAction`.
  4. **Очистка UI**: Браузерный `confirm()` заменен на Glassmorphism Confirm Sheet + Undo toast. Удалены чипы `-10%/-50%`, фейки «Юрий/PRO Tracker», и внешние плейсхолдеры заменены на `img/placeholder.svg`.

---

## [2026-08-13 17:32] — WB Tracker v1.4 (Платформенный Релиз: Onboarding, Alerts Center, Локальный Бэкап/Экспорт CSV&JSON, 5 Theme Packs, App Shortcuts) — Вердикт: [APPROVED] ✅
- **Статус сборки**: `BUILD SUCCESSFUL` ✅
- **Изменения**:
  1. **Onboarding**: 1-разовый стартовый экран (3 слайда) с сохранением состояния `WbBridge.completeOnboarding()`.
  2. **Alerts Center**: Иконка колокольчика `🔔` в шапке с красным индикатором, Bottom Sheet `#alertsSheet`, поддержка таблицы Room `AlertHistoryEntity` и `AlertHistoryDao`.
  3. **Данные и Экспорт**: Секция в Профиле с экспортом CSV/JSON и созданием локальных бэкапов через `FileProvider` (100% бесплатно).
  4. **Theme Packs**: Выбор 5 стилей оформления в Профиле (`Premium Finance`, `Aurora Glass`, `Clean`, `WB Neon`, `Carbon`).
  5. **App Shortcuts**: Нативные ярлыки Android (`shortcuts.xml`) и перехват быстрых действий в `MainActivity.kt`.

---

## [2026-08-13 17:25] — WB Tracker v1.3.2 (Платформенная Архитектура, Модульный Фронтенд, AGENT_RULES.md и BRIDGE_CONTRACT.md) — Вердикт: [APPROVED] ✅
- **Статус сборки**: `BUILD SUCCESSFUL in 19s` ✅
- **Изменения**:
  1. Создан документ жестких правил ИИ-агентов [`docs/AGENT_RULES.md`](file:///home/yura/projects/wbtracker/docs/AGENT_RULES.md) (запрет новых табов, 48dp+ тач-зоны, 100% бесплатность, запрет раздувания UI).
  2. Создан единый API контракт [`docs/BRIDGE_CONTRACT.md`](file:///home/yura/projects/wbtracker/docs/BRIDGE_CONTRACT.md) (схемы асинхронных методов `WbBridge`, Onboarding, Alerts, Backup, Theme Packs).
  3. Декомпозирован фронтенд на независимые CSS и JS модули в `app/src/main/assets/css/` (`tokens.css`, `themes.css`, `components.css`) и `app/src/main/assets/js/` (`bridge.js`, `state.js`, `theme.js`, `alerts.js`, `backup.js`).

---

## [2026-08-09 16:36] — WB Tracker v1.3.1 (Мгновенное Скрытие Баннера Батареи, Кнопка «✕» и Нативный Фоллбек MIUI/Samsung) — Вердикт: [APPROVED] ✅
- **Статус сборки**: `BUILD SUCCESSFUL` ✅
- **Изменения**:
  1. В `index.html` добавлена кнопка закрытия `✕` и моментальное сохранение `localStorage.setItem('battery_banner_dismissed', 'true')` при клике на "Разрешить" или `✕`. Плашка скрывается на 100% навсегда и не беспокоит пользователя.
  2. В `WbBridge.kt` добавлен нативный фоллбек `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` при блокировке стандартного интента на оболочках Xiaomi MIUI / Samsung / Huawei.

---

## [2026-08-09 16:10] — WB Tracker v1.3 (Гарантированная Фоновая Синхронизация 24/7, Разрешение Оптимизации Батареи Android, Единая Спецификация ARCHITECTURE.md) — Вердикт: [APPROVED] ✅
- **Статус сборки**: `BUILD SUCCESSFUL in 1m 30s` ✅
- **Изменения**:
  1. Внедрен разрешение `<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />` и нативные вызовы `PowerManager` в `MainActivity.kt` и `WbBridge.kt` (`isBatteryOptimizationIgnored()`, `requestBatteryOptimizationExemption()`).
  2. В `index.html` выведены предупреждающие плашки в Профиле и на Главном экране с вызовом разрешения фоновой работы 24/7.
  3. Консолидирован единый документ текущей архитектуры [`ARCHITECTURE.md`](file:///home/yura/projects/wbtracker/ARCHITECTURE.md) (v1.3), удален устаревший `architecture_plan.md`.

---

## [2026-08-08 20:52] — WB Tracker v1.2 (Точное Выравнивание Шкалы Цен Y при Равных Ценах и Починка Склонений в Инсайтах) — Вердикт: [APPROVED] ✅
- **Статус сборки**: `BUILD SUCCESSFUL` ✅
- **Изменения**: Ликвидирован рассинхрон осей Canvas (убраны вымышленные метки 447 ₽ / 427 ₽ при ценах 406 ₽). Исправлено дублирование текста в аналитических инсайтах на *"Снижение цены на 1 товар"*.

---

## [2026-08-08 18:00] — WB Tracker v1.1 (WorkManager Перепланирование, Валидация Артикулов 5-12 Цифр, GPU 60 FPS Composite Layers & Android Edge-to-Edge) — Вердикт: [APPROVED] ✅
- **Статус сборки**: `BUILD SUCCESSFUL` ✅
- **Изменения**: Нативно перепланируется `WorkManagerScheduler` из настроек Профиля, строгая проверка артикулов $5 \le \text{length} \le 12$ цифр в `WbArticleExtractor.kt`, GPU composite layers в CSS, отступы безопасных зон Android `env(safe-area-inset-top/bottom)`.
