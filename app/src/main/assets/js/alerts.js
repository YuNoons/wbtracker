/* alerts.js - Alerts Center Module (v1.4) */
window.AlertsCenter = {
  alerts: [],

  init: function() {
    this.loadAlerts();
  },

  loadAlerts: function() {
    if (window.WbBridge && typeof window.WbBridge.getAlertsAsync === 'function') {
      window.WbBridge.getAlertsAsync('req_alerts');
    } else if (window.WbBridge && typeof window.WbBridge.getAlerts === 'function') {
      try {
        const raw = window.WbBridge.getAlerts();
        const res = JSON.parse(raw);
        if (res.status === 'success' && Array.isArray(res.data)) {
          this.renderAlerts(res.data);
          this.updateBadge();
        }
      } catch(e) {
        console.error('Error getting alerts:', e);
      }
    } else {
      this.renderAlerts(window.AppState ? window.AppState.alerts || [] : []);
      this.updateBadge();
    }
  },

  openAlertsSheet: function() {
    const sheet = document.getElementById('alertsSheet');
    if (sheet) {
      sheet.classList.add('active');
      if (window.WbNative) window.WbNative.triggerHaptic(40);
      this.loadAlerts();
    }
  },

  closeAlertsSheet: function(event) {
    if (event && event.target && !event.target.classList.contains('modal-overlay')) {
      return;
    }
    const sheet = document.getElementById('alertsSheet');
    if (sheet) {
      sheet.classList.remove('active');
    }
  },

  markRead: function(alertId) {
    if (window.WbNative) window.WbNative.triggerHaptic(30);
    if (window.WbBridge && typeof window.WbBridge.markAlertRead === 'function') {
      window.WbBridge.markAlertRead(String(alertId));
    }
    this.alerts = this.alerts.map(a => String(a.id) === String(alertId) ? { ...a, isRead: true } : a);
    this.updateBadge();
    this.renderAlerts(this.alerts);
  },

  clearAll: function() {
    if (window.WbNative) window.WbNative.triggerHaptic(40);
    if (window.WbBridge && typeof window.WbBridge.clearAllAlerts === 'function') {
      window.WbBridge.clearAllAlerts();
    }
    this.alerts = [];
    if (window.AppState) window.AppState.alerts = [];
    this.updateBadge();
    this.renderAlerts([]);
  },

  updateBadge: function() {
    const badge = document.getElementById('alerts-unread-badge');
    const hasUnread = this.alerts.some(a => !a.isRead);
    if (badge) {
      badge.style.display = hasUnread ? 'block' : 'none';
    }
  },

  renderAlerts: function(list) {
    this.alerts = list || [];
    const container = document.getElementById('alerts-list-container');
    if (!container) return;

    if (!list || list.length === 0) {
      container.innerHTML = `
        <div class="empty-state" style="padding: 32px 16px; text-align: center;">
          <div class="empty-icon" style="font-size: 40px; margin-bottom: 8px;">🔔</div>
          <h3 class="empty-title" style="font-size: 16px; font-weight: 700; color: var(--text-primary);">Нет новых уведомлений</h3>
          <p class="empty-subtitle" style="font-size: 13px; color: var(--text-muted); margin-top: 4px;">Здесь будут отображаться сработавшие оповещения о снижении цен</p>
        </div>
      `;
      return;
    }

    container.innerHTML = list.map(alert => {
      const oldP = alert.oldPrice || alert.targetPrice || 0;
      const newP = alert.newPrice || alert.triggeredPrice || 0;
      const oldPriceText = alert.oldPriceFormatted || (oldP ? Math.round(oldP) + ' ₽' : '');
      const newPriceText = alert.triggeredPriceFormatted || (newP ? Math.round(newP) + ' ₽' : '0 ₽');
      const isUnread = !alert.isRead;
      const img = alert.thumbnailUrl || alert.productImage || 'img/placeholder.svg';

      let diffText = '';
      if (oldP > 0 && newP > 0 && oldP > newP) {
        const diffVal = Math.round(oldP - newP);
        const pct = Math.round((diffVal / oldP) * 100);
        diffText = ` <span style="font-size: 11px; background: rgba(5, 150, 105, 0.15); color: var(--color-emerald); padding: 2px 6px; border-radius: 6px; font-weight: 700; margin-left: 4px;">-${diffVal} ₽ (-${pct}%)</span>`;
      }

      return `
        <div class="alert-item ${isUnread ? 'unread' : 'read'}" onclick="window.AlertsCenter.markRead('${alert.id}')" style="display: flex; gap: 12px; padding: 14px; border-radius: 16px; background: var(--bg-card); border: 1px solid var(--border-color); margin-bottom: 10px; align-items: center; cursor: pointer;">
          <img src="${img}" style="width: 48px; height: 60px; object-fit: cover; border-radius: 10px; background: var(--bg-secondary);" referrerpolicy="no-referrer" onerror="window.handleImgError(this)">
          <div style="flex: 1; min-width: 0;">
            <div style="font-size: 14px; font-weight: 700; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${alert.title || alert.productTitle || 'Товар WB'}</div>
            <div style="font-size: 13px; color: var(--color-emerald); font-weight: 700; margin-top: 4px; display: flex; align-items: center; flex-wrap: wrap;">
              📉 ${oldPriceText ? '<span style="text-decoration: line-through; color: var(--text-muted); font-weight: 500; margin-right: 4px;">' + oldPriceText + '</span> → ' : ''}${newPriceText}${diffText}
            </div>
            <div style="font-size: 11px; color: var(--text-muted); margin-top: 2px;">${alert.timestampFormatted || alert.dateFormatted || 'Сегодня'}</div>
          </div>
          ${isUnread ? '<span style="width: 10px; height: 10px; border-radius: 50%; background: var(--accent-pink); flex-shrink: 0; box-shadow: 0 0 8px var(--accent-pink);"></span>' : ''}
        </div>
      `;
    }).join('');
  }
};
