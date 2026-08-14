/* state.js - App State Management (v1.4) */
window.AppState = {
  products: [],
  favorites: [],
  currentTab: 'dashboard',
  activeFilter: 'all',
  activeSort: 'date-desc',
  batteryBannerDismissed: localStorage.getItem('battery_banner_dismissed') === 'true',
  onboardingCompleted: localStorage.getItem('onboarding_completed') === 'true',
  alerts: [],
  currentDetailProductId: null,
  detailChartRange: '30',
  analyticsChartRange: '30',
  pendingDeleteProduct: null,
  undoTimeoutId: null
};
