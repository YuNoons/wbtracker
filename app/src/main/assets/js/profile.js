window.ProfileModule = {
  toggleNotifications: function(enabled) {
    if (window.WbNative) window.WbNative.triggerHaptic(30);

    if (window.WbBridge && typeof window.WbBridge.setNotificationsEnabled === 'function') {
      try {
        window.WbBridge.setNotificationsEnabled(JSON.stringify({ enabled: !!enabled }));
      } catch(e) {}
    }

    if (typeof window.showTopToast === 'function') {
      window.showTopToast('success', enabled ? 'Уведомления включены' : 'Уведомления отключены');
    }
  }
};

window.loadProfileScreen = function() {
  window.checkBatteryOptimizationStatus();

  if (window.WbBridge && typeof window.WbBridge.getProfile === 'function') {
    try {
      const raw = window.WbBridge.getProfile();
      const res = JSON.parse(raw);
      if (res.status === 'success') {
        const select = document.getElementById('profile-sync-interval') || document.getElementById('sync-interval-select');
        if (select && res.syncInterval) {
          const val = String(res.syncInterval).replace(/[^\d]/g, '');
          if (val) select.value = val;
        }

        const notifToggle = document.getElementById('notifications-toggle');
        if (notifToggle && (res.pushEnabled !== undefined || res.notificationsEnabled !== undefined)) {
          notifToggle.checked = res.pushEnabled !== undefined ? res.pushEnabled : res.notificationsEnabled;
        }

        const themeToggle = document.getElementById('theme-toggle');
        if (themeToggle && res.darkTheme !== undefined) {
          themeToggle.checked = res.darkTheme;
        }
      }
    } catch(e) {
      console.error('Error loading profile from bridge:', e);
    }
  const priceModeSelect = document.getElementById('price-mode-select');
  if (priceModeSelect && window.WbBridge && typeof window.WbBridge.getPriceTrackingMode === 'function') {
    try {
      priceModeSelect.value = window.WbBridge.getPriceTrackingMode();
    } catch (e) {
      console.error('Error loading price tracking mode:', e);
    }
  }
};

window.changePriceMode = function(mode) {
  if (window.WbNative) window.WbNative.triggerHaptic(30);

  if (window.WbBridge && typeof window.WbBridge.setPriceTrackingMode === 'function') {
    window.WbBridge.setPriceTrackingMode(mode);
  }
  window.showTopToast('info', 'Тип цены изменён. Новые данные будут записываться по новому типу.');
  if (typeof window.refreshData === 'function') {
    window.refreshData();
  }
};

window.changeSyncInterval = function(val) {
  if (window.WbNative) window.WbNative.triggerHaptic(30);

  if (window.WbBridge && typeof window.WbBridge.setSyncInterval === 'function') {
    try {
      const res = window.WbBridge.setSyncInterval(JSON.stringify({ hours: parseInt(val, 10) }));
      const json = JSON.parse(res);
      if (json.status === 'success') {
        window.showTopToast('success', json.message || 'Интервал фонового обновления обновлен');
      }
    } catch(e) {
      window.showTopToast('success', `Интервал обновления: каждые ${val} ч.`);
    }
  } else {
    window.showTopToast('success', `Интервал обновления: каждые ${val} ч.`);
  }
};

window.checkBatteryOptimizationStatus = function() {
  const isIgnored = window.WbNative ? window.WbNative.isBatteryOptimizationIgnored() : true;
  const isDismissed = localStorage.getItem('battery_banner_dismissed') === 'true';

  const bannerDash = document.getElementById('battery-banner-dashboard');
  const bannerProf = document.getElementById('battery-banner-profile');

  const shouldShow = !isIgnored && !isDismissed;

  if (bannerDash) bannerDash.style.display = shouldShow ? 'flex' : 'none';
  if (bannerProf) bannerProf.style.display = shouldShow ? 'flex' : 'none';
};

window.dismissBatteryBanner = function() {
  localStorage.setItem('battery_banner_dismissed', 'true');
  if (window.AppState) window.AppState.batteryBannerDismissed = true;

  const bannerDash = document.getElementById('battery-banner-dashboard');
  const bannerProf = document.getElementById('battery-banner-profile');

  if (bannerDash) bannerDash.style.display = 'none';
  if (bannerProf) bannerProf.style.display = 'none';

  if (window.WbNative) window.WbNative.triggerHaptic(30);
};

window.requestBatteryExemption = function() {
  if (window.WbNative) {
    window.WbNative.requestBatteryExemption();
  }
};
