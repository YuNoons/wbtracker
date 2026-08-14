/* detail.js - Interactive Product Detail Screen Module (v1.4) */

window.openProductDetail = function(id) {
  const idStr = String(id);
  window.AppState.currentDetailProductId = idStr;

  const modal = document.getElementById('productDetailModal');
  if (!modal) return;

  if (window.WbNative) window.WbNative.triggerHaptic(40);

  const prod = window.AppState.products ? window.AppState.products.find(p => String(p.id) === idStr) : null;

  // DOM Elements
  const imgEl = document.getElementById('detail-img');
  const titleEl = document.getElementById('detail-title');
  const brandEl = document.getElementById('detail-brand');
  const articleEl = document.getElementById('detail-article');
  const sellerEl = document.getElementById('detail-seller');

  const wbPriceEl = document.getElementById('detail-wb-price');
  const sellerPriceEl = document.getElementById('detail-seller-price');
  const basePriceEl = document.getElementById('detail-base-price');
  const discountTagEl = document.getElementById('detail-discount-tag');

  const targetInput = document.getElementById('detail-target-price-input');
  const targetSwitch = document.getElementById('detail-target-switch');

  if (prod) {
    if (imgEl) {
      delete imgEl.dataset.retry;
      imgEl.setAttribute('referrerpolicy', 'no-referrer');
      imgEl.onerror = function() {
        if (window.handleImgError) {
          window.handleImgError(this);
        } else {
          this.src = 'img/placeholder.svg';
        }
      };
      imgEl.src = prod.image || prod.thumbnailUrl || 'img/placeholder.svg';
    }
    if (titleEl) titleEl.textContent = prod.name || prod.title || 'Товар WB';
    if (brandEl) brandEl.textContent = prod.brand || 'Бренд не указан';
    if (articleEl) articleEl.textContent = `Артикул: ${prod.id}`;
    if (sellerEl) sellerEl.textContent = `Продавец: ${prod.seller || 'Wildberries'}`;

    const wPrice = prod.primaryPriceFormatted || prod.walletPriceFormatted || (prod.price ? Math.round(prod.price) + ' ₽' : '0 ₽');
    const sPrice = prod.sellerPriceFormatted || (prod.sellerPrice ? Math.round(prod.sellerPrice) + ' ₽' : '');
    const bPrice = prod.basicPriceFormatted || (prod.oldPrice ? Math.round(prod.oldPrice) + ' ₽' : '');
    const discount = prod.discountPercentFormatted || (prod.discount ? '-' + prod.discount + '%' : '');

    if (wbPriceEl) wbPriceEl.textContent = wPrice;
    if (sellerPriceEl) sellerPriceEl.textContent = sPrice;
    if (basePriceEl) basePriceEl.textContent = bPrice;
    if (discountTagEl) {
      discountTagEl.textContent = discount;
      discountTagEl.style.display = discount ? 'inline-block' : 'none';
    }

    if (targetInput) targetInput.value = prod.targetPrice ? Math.round(prod.targetPrice) : '';
    if (targetSwitch) targetSwitch.checked = !!prod.targetEnabled;

    window.updateDetailFavBtn(prod.favorite || prod.isFavorite);
    window.renderDefaultRatingBars(prod);
  }

  modal.classList.add('active');

  // Load Price History
  if (window.WbBridge && typeof window.WbBridge.getPriceHistory === 'function') {
    try {
      const raw = window.WbBridge.getPriceHistory(idStr, 30);
      const res = JSON.parse(raw);
      if (res.status === 'success' && Array.isArray(res.points)) {
        window.renderDetailChart(res.points, prod ? { targetPrice: prod.targetPrice, targetEnabled: prod.targetEnabled } : {});
      } else {
        window.renderDetailChart([], prod ? { targetPrice: prod.targetPrice, targetEnabled: prod.targetEnabled } : {});
      }
    } catch(e) {
      window.renderDetailChart([], prod ? { targetPrice: prod.targetPrice, targetEnabled: prod.targetEnabled } : {});
    }
  } else {
    window.renderDetailChart([], prod ? { targetPrice: prod.targetPrice, targetEnabled: prod.targetEnabled } : {});
  }

  // Load Reviews
  if (window.WbBridge && typeof window.WbBridge.getReviews === 'function') {
    try {
      const rawR = window.WbBridge.getReviews(idStr);
      const resR = JSON.parse(rawR);
      if (resR.status === 'success' && resR.ratingDistribution) {
        window.renderRatingBars(resR.ratingDistribution);
      } else {
        window.renderRatingBars(null);
      }
    } catch(e) {
      window.renderRatingBars(null);
    }
  } else {
    window.renderRatingBars(null);
  }
};

window.closeProductDetailModal = function(e) {
  if (e && e.target && !e.target.classList.contains('modal-overlay')) return;
  const modal = document.getElementById('productDetailModal');
  if (modal) {
    modal.classList.remove('active');
  }
};

window.updateDetailFavBtn = function(isFav) {
  const starEl = document.getElementById('detail-fav-star');
  const textEl = document.getElementById('detail-fav-text');
  const btnEl = document.getElementById('detail-btn-fav');

  if (starEl) starEl.textContent = isFav ? '★' : '☆';
  if (textEl) textEl.textContent = isFav ? 'В избранном' : 'В избранное';
  if (btnEl) {
    if (isFav) btnEl.classList.add('active');
    else btnEl.classList.remove('active');
  }
};

window.toggleDetailFavorite = function() {
  const prodId = window.AppState.currentDetailProductId;
  if (!prodId) return;

  const prod = window.AppState.products.find(p => String(p.id) === String(prodId));
  const newFav = prod ? !(prod.favorite || prod.isFavorite) : true;

  if (prod) {
    prod.favorite = newFav;
    prod.isFavorite = newFav;
  }

  window.updateDetailFavBtn(newFav);
  window.renderProducts();

  if (window.WbBridge && typeof window.WbBridge.toggleFavorite === 'function') {
    try {
      window.WbBridge.toggleFavorite(String(prodId), newFav);
    } catch(e) {}
  }
};

window.deleteDetailProduct = async function() {
  const prodId = window.AppState.currentDetailProductId;
  if (!prodId) return;

  window.closeProductDetailModal();
  await window.deleteProd(prodId);
};

window.openOnWildberries = function() {
  const prodId = window.AppState ? window.AppState.currentDetailProductId : null;
  if (!prodId) return;

  if (window.WbNative) window.WbNative.triggerHaptic(40);

  if (window.WbBridge && typeof window.WbBridge.openProductById === 'function') {
    window.WbBridge.openProductById(String(prodId));
  } else if (window.WbBridge && typeof window.WbBridge.openProductUrl === 'function') {
    window.WbBridge.openProductUrl(String(prodId));
  } else if (window.WbBridge && typeof window.WbBridge.openUrl === 'function') {
    window.WbBridge.openUrl(`https://www.wildberries.ru/catalog/${prodId}/detail.aspx`);
  }
};

window.saveTargetPrice = function() {
  const prodId = window.AppState.currentDetailProductId;
  if (!prodId) return;

  const input = document.getElementById('detail-target-price-input');
  const switchEl = document.getElementById('detail-target-switch');
  if (!input || !switchEl) return;

  const priceVal = parseFloat(input.value) || 0;
  const enabled = switchEl.checked;

  if (enabled && priceVal <= 0) {
    window.showTopToast('error', 'Укажите корректную целевую цену');
    return;
  }

  const prod = window.AppState.products.find(p => String(p.id) === String(prodId));
  if (prod) {
    prod.targetPrice = priceVal;
    prod.targetEnabled = enabled;
  }

  if (window.WbBridge && typeof window.WbBridge.setTargetPrice === 'function') {
    try {
      window.WbBridge.setTargetPrice(JSON.stringify({ id: String(prodId), price: priceVal, enabled: enabled }));
    } catch(e) {}
  }

  window.showTopToast('success', 'Порог целевой цены сохранен');
  const targetOpt = { targetPrice: priceVal, targetEnabled: enabled };
  window.renderDetailChart(window.currentDetailRawPoints || [], targetOpt);
};

window.onTargetSwitchChanged = function() {
  const switchEl = document.getElementById('detail-target-switch');
  if (!switchEl) return;
  if (window.WbNative) window.WbNative.triggerHaptic(30);
};

window.renderDefaultRatingBars = function(prod) {
  const scoreEl = document.getElementById('detail-rating-score');
  const starsEl = document.getElementById('detail-rating-stars');
  const countEl = document.getElementById('detail-reviews-count');

  const rawRating = (prod && typeof prod.rating === 'number' && prod.rating > 0) ? prod.rating : null;
  const ratingText = rawRating ? rawRating.toFixed(1) : 'Нет оценок';
  
  const rawCount = (prod && typeof prod.reviewsCount === 'number') ? prod.reviewsCount : 0;
  const countText = prod?.reviewsCountFormatted || `${rawCount} отзывов`;

  if (scoreEl) scoreEl.textContent = ratingText;
  if (starsEl) starsEl.textContent = rawRating ? '★★★★★' : '☆☆☆☆☆';
  if (countEl) countEl.textContent = countText;

  window.renderRatingBars(null);
};

window.renderRatingBars = function(dist) {
  const container = document.getElementById('detail-star-bars');
  if (!container) return;

  if (!dist || typeof dist !== 'object' || !dist.star5Percent) {
    container.innerHTML = `
      <div style="font-size: 12px; color: var(--text-muted); padding: 8px 0; text-align: center;">
        Распределение оценок недоступно
      </div>
    `;
    return;
  }

  const stars = [
    { label: '5★', pct: dist.star5Percent || 0 },
    { label: '4★', pct: dist.star4Percent || 0 },
    { label: '3★', pct: dist.star3Percent || 0 },
    { label: '2★', pct: dist.star2Percent || 0 },
    { label: '1★', pct: dist.star1Percent || 0 }
  ];

  container.innerHTML = stars.map(s => `
    <div class="star-bar-row" style="display: flex; align-items: center; gap: 8px; font-size: 12px; margin-bottom: 4px;">
      <span style="width: 24px; color: var(--text-muted); font-weight: 600;">${s.label}</span>
      <div style="flex: 1; height: 6px; background: var(--bg-secondary); border-radius: 3px; overflow: hidden;">
        <div style="width: ${s.pct}%; height: 100%; background: linear-gradient(90deg, var(--accent-purple), var(--accent-pink)); border-radius: 3px;"></div>
      </div>
      <span style="width: 32px; text-align: right; color: var(--text-muted); font-weight: 500;">${s.pct}%</span>
    </div>
  `).join('');
};
