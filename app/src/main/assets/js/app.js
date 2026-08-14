/* app.js - Main Application Entry Point & Navigation Coordinator (v1.4) */

window.switchTab = function(tabIdOrIndex) {
  let targetTabId = 'dashboard';
  let tabIdx = 0;

  if (typeof tabIdOrIndex === 'number') {
    tabIdx = tabIdOrIndex;
    if (tabIdx === 0) targetTabId = 'dashboard';
    else if (tabIdx === 1) targetTabId = 'favorites';
    else if (tabIdx === 2) targetTabId = 'analytics';
    else if (tabIdx === 3) targetTabId = 'profile';
  } else {
    targetTabId = tabIdOrIndex;
    if (targetTabId === 'dashboard') tabIdx = 0;
    else if (targetTabId === 'favorites') tabIdx = 1;
    else if (targetTabId === 'analytics') tabIdx = 2;
    else if (targetTabId === 'profile') tabIdx = 3;
  }

  window.AppState.currentTab = targetTabId;

  // Toggle screens
  const screens = document.querySelectorAll('.screen');
  screens.forEach(s => s.classList.remove('active'));

  const targetScreen = document.getElementById(`screen-${targetTabId}`);
  if (targetScreen) targetScreen.classList.add('active');

  // Toggle tab buttons
  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach((item, idx) => {
    if (idx === tabIdx) item.classList.add('active');
    else item.classList.remove('active');
  });

  window.updateNavIndicator(tabIdx);

  if (window.WbNative) window.WbNative.triggerHaptic(30);

  // Tab specific re-renders
  if (targetTabId === 'dashboard' || targetTabId === 'favorites') {
    window.renderProducts();
  } else if (targetTabId === 'analytics') {
    window.renderAnalyticsScreen();
  } else if (targetTabId === 'profile') {
    window.loadProfileScreen();
  }
};

window.updateNavIndicator = function(index) {
  const indicator = document.getElementById('nav-indicator');
  const navBar = document.querySelector('.bottom-nav');
  const navItems = document.querySelectorAll('.nav-item');
  if (!indicator || !navBar || !navItems || !navItems[index]) return;

  const navRect = navBar.getBoundingClientRect();
  const targetItem = navItems[index];
  const itemRect = targetItem.getBoundingClientRect();

  const leftPx = itemRect.left - navRect.left;
  const widthPx = itemRect.width;

  indicator.style.width = `${widthPx}px`;
  indicator.style.transform = `translateX(${leftPx}px)`;
};

window.onProductAddSuccess = function(msg) {
  window.showTopToast('success', msg || 'Товар добавлен в отслеживание');
  window.refreshData();
};

window.onProductAddError = function(errMsg) {
  window.showTopToast('error', errMsg || 'Ошибка добавления товара');
};

document.addEventListener('DOMContentLoaded', () => {
  // Hide splash screen after delay
  setTimeout(() => {
    const splash = document.getElementById('splash-screen');
    if (splash) {
      splash.style.opacity = '0';
      splash.style.transition = 'opacity 0.4s ease-out';
      setTimeout(() => { splash.style.display = 'none'; }, 400);
    }
  }, 400);

  // Init Modules
  if (window.ThemeEngine) window.ThemeEngine.init();
  if (window.AlertsCenter) window.AlertsCenter.init();
  if (window.OnboardingModule) window.OnboardingModule.init();
  if (typeof window.setupAllGestures === 'function') window.setupAllGestures();
  if (typeof window.checkBatteryOptimizationStatus === 'function') window.checkBatteryOptimizationStatus();

  window.refreshData();
  window.switchTab('dashboard');
});
