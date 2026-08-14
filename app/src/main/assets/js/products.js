window.handleImgError = function(img) {
  const retry = parseInt(img.dataset.retry || '0', 10);
  if (retry === 0) {
    img.dataset.retry = '1';
    img.src = img.src.replace(/\.webp(\?.*)?$/i, '.jpg');
  } else if (retry === 1) {
    img.dataset.retry = '2';
    img.src = 'img/placeholder.svg';
  } else {
    img.onerror = null;
  }
};

window.pluralize = function(n, forms) {
  const r10 = n % 10, r100 = n % 100;
  if (r100 >= 11 && r100 <= 19) return forms[2];
  if (r10 === 1) return forms[0];
  if (r10 >= 2 && r10 <= 4) return forms[1];
  return forms[2];
};

window.generateProductCardHtml = function(p) {
  const img = p.image || p.thumbnailUrl || 'img/placeholder.svg';
  const title = p.name || p.title || 'Товар WB';
  const brand = p.brand || '';
  const priceFormatted = p.primaryPriceFormatted || p.walletPriceFormatted || (p.primaryPrice !== undefined ? Math.round(p.primaryPrice) + ' ₽' : (p.price ? Math.round(p.price) + ' ₽' : (p.walletPrice ? Math.round(p.walletPrice) + ' ₽' : '0 ₽')));
  const isFav = p.favorite || p.isFavorite || false;

  const wPrice = p.primaryPrice !== undefined ? p.primaryPrice : (p.walletPrice !== undefined ? p.walletPrice : (p.price !== undefined ? p.price : 0));
  const iPrice = p.initialWalletPrice !== undefined && p.initialWalletPrice > 0 ? p.initialWalletPrice : wPrice;
  const delta = Math.round(wPrice - iPrice);

  let deltaText = 'Без изменений';
  let deltaClass = '';

  if (delta < 0) {
    deltaText = `${delta.toLocaleString('ru-RU')} ₽`;
    deltaClass = '';
  } else if (delta > 0) {
    deltaText = `+${delta.toLocaleString('ru-RU')} ₽`;
    deltaClass = 'up';
  }

  const updatedAtText = p.lastUpdatedAtFormatted || (p.updatedAt ? new Date(p.updatedAt).toLocaleDateString('ru-RU') : 'Обновлено');

  return `
    <div class="product-card" onclick="openProductDetail('${p.id}')">
      <img class="product-img" src="${img}" alt="${title}" referrerpolicy="no-referrer" onerror="window.handleImgError(this)">
      <div class="product-info">
        <div>
          ${brand ? `<div class="product-brand">${brand}</div>` : ''}
          <div class="product-title">${title}</div>
          <div class="price-row">
            <span class="wallet-price">${priceFormatted}</span>
            <span class="price-delta-badge ${deltaClass}">${deltaText}</span>
          </div>
        </div>
        <div class="card-footer-meta">
          <span class="updated-at">${updatedAtText}</span>
          <div style="display: flex; gap: 4px;" onclick="event.stopPropagation()">
            <button class="fav-btn ${isFav ? 'active' : ''}" onclick="toggleFav('${p.id}', ${isFav})">
              ${isFav ? '★' : '☆'}
            </button>
            <button class="delete-btn" onclick="deleteProd('${p.id}')">
              🗑
            </button>
          </div>
        </div>
      </div>
    </div>
  `;
};

window.renderProducts = function() {
  const grid = document.getElementById('product-list-container');
  const favGrid = document.getElementById('favorites-list-container');
  const countBadge = document.getElementById('product-count-badge');
  const heroTrackedCount = document.getElementById('hero-tracked-count');
  const heroDropCount = document.getElementById('hero-drop-count');
  const heroSavingsVal = document.getElementById('hero-savings-val');

  if (!grid) return;

  let list = window.AppState.products || [];

  // Filter count updates
  const countAll = list.length;
  let countDrop = 0;
  let countRise = 0;
  let countSame = 0;
  let countFav = 0;
  let totalSavings = 0;

  list.forEach(p => {
    const wPrice = p.primaryPrice !== undefined ? p.primaryPrice : (p.walletPrice !== undefined ? p.walletPrice : (p.price || 0));
    const iPrice = p.initialWalletPrice !== undefined && p.initialWalletPrice > 0 ? p.initialWalletPrice : wPrice;

    if (wPrice < iPrice) countDrop++;
    else if (wPrice > iPrice) countRise++;
    else countSame++;

    if (p.favorite || p.isFavorite) countFav++;

    const itemSavings = Math.max(0, iPrice - wPrice);
    totalSavings += itemSavings;
  });

  const elAll = document.getElementById('count-all');
  const elDrop = document.getElementById('count-drop');
  const elRise = document.getElementById('count-rise');
  const elSame = document.getElementById('count-same');
  const elFav = document.getElementById('count-fav');

  if (elAll) elAll.textContent = countAll;
  if (elDrop) elDrop.textContent = countDrop;
  if (elRise) elRise.textContent = countRise;
  if (elSame) elSame.textContent = countSame;
  if (elFav) elFav.textContent = countFav;

  if (countBadge) countBadge.textContent = `${countAll} ${window.pluralize(countAll, ['товар', 'товара', 'товаров'])}`;
  if (heroTrackedCount) heroTrackedCount.textContent = `${countAll} ${window.pluralize(countAll, ['товар', 'товара', 'товаров'])}`;
  if (heroDropCount) heroDropCount.textContent = `${countDrop} ${window.pluralize(countDrop, ['снижение', 'снижения', 'снижений'])}`;
  if (heroSavingsVal) heroSavingsVal.textContent = `${Math.round(totalSavings).toLocaleString('ru-RU')} ₽`;

  // Apply Filter
  let filtered = list;
  const filter = window.AppState.activeFilter || 'all';
  if (filter === 'drop') {
    filtered = list.filter(p => {
      const wPrice = p.primaryPrice !== undefined ? p.primaryPrice : (p.walletPrice !== undefined ? p.walletPrice : (p.price || 0));
      const iPrice = p.initialWalletPrice !== undefined && p.initialWalletPrice > 0 ? p.initialWalletPrice : wPrice;
      return wPrice < iPrice;
    });
  } else if (filter === 'rise') {
    filtered = list.filter(p => {
      const wPrice = p.primaryPrice !== undefined ? p.primaryPrice : (p.walletPrice !== undefined ? p.walletPrice : (p.price || 0));
      const iPrice = p.initialWalletPrice !== undefined && p.initialWalletPrice > 0 ? p.initialWalletPrice : wPrice;
      return wPrice > iPrice;
    });
  } else if (filter === 'same') {
    filtered = list.filter(p => {
      const wPrice = p.primaryPrice !== undefined ? p.primaryPrice : (p.walletPrice !== undefined ? p.walletPrice : (p.price || 0));
      const iPrice = p.initialWalletPrice !== undefined && p.initialWalletPrice > 0 ? p.initialWalletPrice : wPrice;
      return wPrice === iPrice;
    });
  } else if (filter === 'fav') {
    filtered = list.filter(p => p.favorite || p.isFavorite);
  }

  // Apply Sort
  const sort = window.AppState.activeSort || 'date-desc';
  filtered.sort((a, b) => {
    const pA = a.walletPrice || a.price || 0;
    const pB = b.walletPrice || b.price || 0;
    const dateA = a.updatedAt || a.lastUpdatedAt || 0;
    const dateB = b.updatedAt || b.lastUpdatedAt || 0;
    const discA = a.basicPrice ? (a.basicPrice - pA) / a.basicPrice : 0;
    const discB = b.basicPrice ? (b.basicPrice - pB) / b.basicPrice : 0;

    if (sort === 'date-desc') return dateB - dateA;
    if (sort === 'date-asc') return dateA - dateB;
    if (sort === 'price-asc') return pA - pB;
    if (sort === 'price-desc') return pB - pA;
    if (sort === 'discount-desc') return discB - discA;
    return 0;
  });

  if (filtered.length === 0) {
    grid.innerHTML = `
      <div class="empty-state" style="grid-column: 1 / -1; padding: 48px 16px;">
        <div class="empty-icon">🛍️</div>
        <h3 class="empty-title">Товары не найдены</h3>
        <p class="empty-subtitle">Добавьте артикул WB или измените фильтры для отображения</p>
        <button class="empty-action-btn" onclick="openAddModalWithHaptic()">+ Добавить товар</button>
      </div>
    `;
  } else {
    grid.innerHTML = filtered.map(p => window.generateProductCardHtml(p)).join('');
  }

  if (favGrid) {
    const favs = list.filter(p => p.favorite || p.isFavorite);
    if (favs.length === 0) {
      favGrid.innerHTML = `
        <div class="empty-state" style="grid-column: 1 / -1; padding: 48px 16px;">
          <div class="empty-icon">⭐</div>
          <h3 class="empty-title">Список избранного пуст</h3>
          <p class="empty-subtitle">Нажмите звездочку на карточке товара, чтобы добавить его сюда</p>
        </div>
      `;
    } else {
      favGrid.innerHTML = favs.map(p => window.generateProductCardHtml(p)).join('');
    }
  }

  let sparklineData = [];
  if (window.WbBridge && typeof window.WbBridge.getAnalyticsSummary === 'function') {
    try {
      const raw = window.WbBridge.getAnalyticsSummary();
      const summary = JSON.parse(raw);
      if (summary && summary.status === 'success' && Array.isArray(summary.savingsSparkline)) {
        sparklineData = summary.savingsSparkline;
      }
    } catch (e) {
      console.error('Error fetching analytics summary for sparkline:', e);
    }
  }

  window.renderHeroSparkline({ totalSavings, countDrop, countAll, savingsSparkline: sparklineData });
};

window.setProductFilter = function(filter, el) {
  window.AppState.activeFilter = filter;
  const chips = document.querySelectorAll('.filter-chip');
  chips.forEach(c => c.classList.remove('active'));
  if (el) el.classList.add('active');
  window.renderProducts();
  if (window.WbNative) window.WbNative.triggerHaptic(30);
};

window.setProductSort = function(sortVal) {
  window.AppState.activeSort = sortVal;
  window.renderProducts();
  if (window.WbNative) window.WbNative.triggerHaptic(30);
};

window.openAddModal = function() {
  const modal = document.getElementById('addModal');
  if (modal) {
    modal.classList.add('active');
    const input = document.getElementById('wb-url-input');
    if (input) {
      input.value = '';
      setTimeout(() => input.focus(), 150);
    }
  }
};

window.openAddModalWithHaptic = function() {
  if (window.WbNative) window.WbNative.triggerHaptic(40);
  window.openAddModal();
};

window.closeAddModal = function(e) {
  if (e && e.target && !e.target.classList.contains('modal-overlay')) return;
  const modal = document.getElementById('addModal');
  if (modal) {
    modal.classList.remove('active');
  }
};

window.submitAddProduct = function() {
  const input = document.getElementById('wb-url-input');
  if (!input) return;
  const val = input.value.trim();
  if (!val) {
    window.showTopToast('error', 'Вставьте ссылку или артикул WB');
    return;
  }

  if (window.WbNative) window.WbNative.triggerHaptic(40);
  window.showTopToast('search', 'Поиск и загрузка товара WB...');

  if (window.WbBridge && typeof window.WbBridge.addProductAsync === 'function') {
    window.WbBridge.addProductAsync(val);
  } else if (window.WbBridge && typeof window.WbBridge.addProductByUrl === 'function') {
    try {
      const res = window.WbBridge.addProductByUrl(val);
      const json = JSON.parse(res);
      if (json.status === 'success') {
        window.onProductAddSuccess(json.message || 'Товар успешно добавлен');
      } else {
        window.onProductAddError(json.message || 'Ошибка добавления товара');
      }
    } catch(e) {
      window.onProductAddError('Ошибка связи с WB');
    }
  } else {
    setTimeout(() => {
      window.onProductAddSuccess('Товар добавлен в демо-режиме');
    }, 1000);
  }
  window.closeAddModal();
};

window.toggleFav = function(id, currentFav) {
  const idStr = String(id);
  const prod = window.AppState.products.find(p => String(p.id) === idStr);
  if (prod) {
    prod.favorite = !currentFav;
    prod.isFavorite = !currentFav;
  }
  window.renderProducts();
  if (window.WbNative) window.WbNative.triggerHaptic(30);

  if (window.WbBridge && typeof window.WbBridge.toggleFavorite === 'function') {
    try {
      window.WbBridge.toggleFavorite(idStr, !currentFav);
    } catch(e) {}
  }
};

window.deleteProd = async function(id) {
  const confirmed = await window.showConfirmSheet({
    title: 'Удалить товар?',
    message: 'Вы действительно хотите удалить этот товар из отслеживания?',
    actionText: 'Удалить'
  });
  if (!confirmed) return;

  const idStr = String(id);
  const existingIndex = window.AppState.products.findIndex(p => String(p.id) === idStr);
  let removedItem = null;
  if (existingIndex !== -1) {
    removedItem = window.AppState.products[existingIndex];
    window.AppState.products.splice(existingIndex, 1);
  }

  window.renderProducts();
  if (window.WbNative) window.WbNative.triggerHaptic(40);

  if (window.WbBridge && typeof window.WbBridge.deleteProduct === 'function') {
    try {
      window.WbBridge.deleteProduct(idStr);
    } catch(e) {}
  }

  window.showTopToast('info', 'Товар удален из отслеживания', 6000, 'Отменить', () => {
    if (removedItem) {
      window.AppState.products.splice(existingIndex >= 0 ? existingIndex : 0, 0, removedItem);
      if (window.WbBridge && typeof window.WbBridge.restoreProduct === 'function') {
        try {
          window.WbBridge.restoreProduct(String(removedItem.id));
        } catch(e) {}
      }
      window.renderProducts();
      window.showTopToast('success', 'Удаление отменено');
    }
  });
};

window.refreshData = function() {
  window.showSkeletonLoaders();
  const banner = document.getElementById('network-error-banner');
  if (banner) banner.style.display = 'none';

  if (window.WbBridge && typeof window.WbBridge.getProductsJson === 'function') {
    try {
      const raw = window.WbBridge.getProductsJson();
      const res = JSON.parse(raw);
      if (res.status === 'success' && Array.isArray(res.data)) {
        window.AppState.products = res.data;
        window.renderProducts();
      } else {
        window.renderErrorState();
      }
    } catch(e) {
      window.renderErrorState();
    }
  } else {
    setTimeout(() => {
      window.renderProducts();
    }, 500);
  }

  if (window.AlertsCenter) window.AlertsCenter.loadAlerts();
};

window.showSkeletonLoaders = function() {
  const grid = document.getElementById('product-list-container');
  if (!grid) return;
  grid.innerHTML = `
    <div class="skeleton-card"><div class="skeleton-img"></div><div class="skeleton-line" style="width: 70%;"></div><div class="skeleton-line" style="width: 40%;"></div></div>
    <div class="skeleton-card"><div class="skeleton-img"></div><div class="skeleton-line" style="width: 70%;"></div><div class="skeleton-line" style="width: 40%;"></div></div>
  `;
};

window.renderErrorState = function(container) {
  const banner = document.getElementById('network-error-banner');
  if (banner) banner.style.display = 'flex';
};

window.animateCountUp = function(targetVal, durationMs = 800) {
  const el = document.getElementById('hero-savings-val');
  if (!el) return;
  const startVal = 0;
  const startTime = performance.now();

  function step(currentTime) {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / durationMs, 1);
    const easeOut = 1 - Math.pow(1 - progress, 3);
    const currentVal = Math.round(startVal + easeOut * (targetVal - startVal));
    el.textContent = `${currentVal.toLocaleString('ru-RU')} ₽`;
    if (progress < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
};

window.renderHeroSparkline = function(stats) {
  const canvas = document.getElementById('heroSparkline');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const w = canvas.width = 120;
  const h = canvas.height = 48;

  ctx.clearRect(0, 0, w, h);

  let points = (stats && Array.isArray(stats.savingsSparkline)) ? stats.savingsSparkline : null;

  if (!points && window.WbBridge && typeof window.WbBridge.getAnalyticsSummary === 'function') {
    try {
      const raw = window.WbBridge.getAnalyticsSummary();
      const res = JSON.parse(raw);
      if (res && res.status === 'success' && Array.isArray(res.savingsSparkline)) {
        points = res.savingsSparkline;
      }
    } catch (e) {
      console.error('Error getting sparkline data:', e);
    }
  }

  if (!points || points.length < 2) {
    ctx.beginPath();
    ctx.setLineDash([4, 4]);
    ctx.moveTo(0, h / 2);
    ctx.lineTo(w, h / 2);
    ctx.strokeStyle = 'rgba(236, 72, 153, 0.4)';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.setLineDash([]);
    return;
  }

  const max = Math.max(...points);
  const min = Math.min(...points);
  const range = max - min;

  const coords = points.map((val, idx) => {
    const x = (idx / (points.length - 1)) * w;
    const y = range === 0 ? h / 2 : h - ((val - min) / range) * (h - 12) - 6;
    return { x, y };
  });

  const grad = ctx.createLinearGradient(0, 0, 0, h);
  grad.addColorStop(0, 'rgba(236, 72, 153, 0.4)');
  grad.addColorStop(1, 'rgba(124, 58, 237, 0.0)');

  ctx.beginPath();
  ctx.moveTo(coords[0].x, h);
  ctx.lineTo(coords[0].x, coords[0].y);
  for (let i = 0; i < coords.length - 1; i++) {
    const xc = (coords[i].x + coords[i + 1].x) / 2;
    const yc = (coords[i].y + coords[i + 1].y) / 2;
    ctx.quadraticCurveTo(coords[i].x, coords[i].y, xc, yc);
  }
  ctx.lineTo(coords[coords.length - 1].x, coords[coords.length - 1].y);
  ctx.lineTo(coords[coords.length - 1].x, h);
  ctx.closePath();
  ctx.fillStyle = grad;
  ctx.fill();

  ctx.beginPath();
  ctx.moveTo(coords[0].x, coords[0].y);
  for (let i = 0; i < coords.length - 1; i++) {
    const xc = (coords[i].x + coords[i + 1].x) / 2;
    const yc = (coords[i].y + coords[i + 1].y) / 2;
    ctx.quadraticCurveTo(coords[i].x, coords[i].y, xc, yc);
  }
  ctx.lineTo(coords[coords.length - 1].x, coords[coords.length - 1].y);
  ctx.strokeStyle = '#EC4899';
  ctx.lineWidth = 2.5;
  ctx.stroke();
};
