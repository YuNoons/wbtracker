# 📋 WORK LOG — WB Price & Review Tracker (v1.3)

Краткий журнал ключевых релизов проекта.

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
