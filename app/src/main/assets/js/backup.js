/* backup.js - Local Data Backup & Export (100% Free v1.4) */
window.DataBackup = {
  exportCsv: function() {
    if (window.WbBridge && typeof window.WbBridge.exportCsv === 'function') {
      try {
        const rawRes = window.WbBridge.exportCsv();
        const res = typeof rawRes === 'string' ? JSON.parse(rawRes) : rawRes;
        if (res && res.status === 'success') {
          if (typeof window.showTopToast === 'function') {
            window.showTopToast('success', res.message || 'Экспорт CSV выполнен');
          }
        } else {
          if (typeof window.showTopToast === 'function') {
            window.showTopToast('error', (res && res.message) || 'Ошибка экспорта CSV');
          }
        }
      } catch (e) {
        if (typeof window.showTopToast === 'function') {
          window.showTopToast('error', 'Ошибка экспорта CSV');
        }
      }
    } else {
      this.downloadCsvWeb();
    }
  },

  exportJson: function() {
    if (window.WbBridge && typeof window.WbBridge.exportJson === 'function') {
      try {
        const rawRes = window.WbBridge.exportJson();
        const res = typeof rawRes === 'string' ? JSON.parse(rawRes) : rawRes;
        if (res && res.status === 'success') {
          if (typeof window.showTopToast === 'function') {
            window.showTopToast('success', res.message || 'Резервная копия JSON создана');
          }
        } else {
          if (typeof window.showTopToast === 'function') {
            window.showTopToast('error', (res && res.message) || 'Ошибка создания резервной копии');
          }
        }
      } catch (e) {
        if (typeof window.showTopToast === 'function') {
          window.showTopToast('error', 'Ошибка создания резервной копии');
        }
      }
    } else {
      this.downloadJsonWeb();
    }
  },

  importBackup: function() {
    if (window.WbBridge && typeof window.WbBridge.importBackup === 'function') {
      const res = window.WbBridge.importBackup();
      if (typeof window.showTopToast === 'function') window.showTopToast('info', 'Резервная копия подготовлена');
    } else {
      if (typeof window.showTopToast === 'function') window.showTopToast('info', 'Выберите файл .json резервной копии');
    }
  },

  downloadCsvWeb: function() {
    const products = (window.AppState && window.AppState.products) ? window.AppState.products : [];
    let csv = 'ID;Артикул;Название;Бренд;Цена Кошелек;Базовая Цена;Экономия\n';
    products.forEach(p => {
      const title = (p.title || p.name || '').replace(/;/g, ',');
      const brand = (p.brand || '').replace(/;/g, ',');
      csv += `${p.id};${p.id};"${title}";"${brand}";${p.price || p.walletPrice || 0};${p.basicPrice || p.oldPrice || 0};${p.itemSavings || 0}\n`;
    });
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `wbtracker_export_${Date.now()}.csv`;
    a.click();
    if (typeof window.showTopToast === 'function') window.showTopToast('success', 'Файл CSV скачан');
  },

  downloadJsonWeb: function() {
    const data = {
      version: '1.4',
      exportDate: new Date().toISOString(),
      products: (window.AppState && window.AppState.products) ? window.AppState.products : []
    };
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `wbtracker_backup_${Date.now()}.json`;
    a.click();
    if (typeof window.showTopToast === 'function') window.showTopToast('success', 'Резервная копия JSON скачана');
  }
};
