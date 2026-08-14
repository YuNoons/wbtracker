/* bridge.js - Native WbBridge Wrapper (v1.4) */
window.WbNative = {
  isBridgeAvailable: function() {
    return !!(window.WbBridge);
  },

  isBatteryOptimizationIgnored: function() {
    if (this.isBridgeAvailable() && typeof window.WbBridge.isBatteryOptimizationIgnored === 'function') {
      return window.WbBridge.isBatteryOptimizationIgnored();
    }
    return true;
  },

  requestBatteryExemption: function() {
    if (this.isBridgeAvailable() && typeof window.WbBridge.requestBatteryOptimizationExemption === 'function') {
      window.WbBridge.requestBatteryOptimizationExemption();
    }
  },

  setSyncInterval: function(hours) {
    if (this.isBridgeAvailable() && typeof window.WbBridge.setSyncInterval === 'function') {
      return window.WbBridge.setSyncInterval(JSON.stringify({ hours: hours }));
    }
  },

  triggerHaptic: function(duration) {
    if (window.navigator && typeof window.navigator.vibrate === 'function') {
      try { window.navigator.vibrate(duration || 40); } catch(e) {}
    }
  },

  handleShortcut: function(action) {
    if (typeof window.handleShortcutAction === 'function') {
      window.handleShortcutAction(action);
    }
  }
};

window.triggerHaptic = function(duration) {
  window.WbNative.triggerHaptic(duration || 40);
};

window.handleShortcutAction = function(action) {
  if (!action) return;
  if (action === 'add') {
    if (typeof window.openAddModal === 'function') window.openAddModal();
  } else if (action === 'favorites') {
    if (typeof window.switchTab === 'function') window.switchTab('favorites');
  } else if (action === 'analytics') {
    if (typeof window.switchTab === 'function') window.switchTab('analytics');
  } else if (action === 'refresh') {
    if (typeof window.refreshData === 'function') window.refreshData();
  }
};

window.onBridgeResult = function(reqId, resultJson) {
  if (reqId === 'req_alerts') {
    try {
      const res = typeof resultJson === 'string' ? JSON.parse(resultJson) : resultJson;
      if (res.status === 'success' && Array.isArray(res.data)) {
        if (window.AlertsCenter) {
          window.AlertsCenter.renderAlerts(res.data);
          window.AlertsCenter.updateBadge();
        }
      }
    } catch(e) {
      console.error('Error parsing bridge alerts result', e);
    }
  }
};
