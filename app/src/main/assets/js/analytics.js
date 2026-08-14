/* analytics.js - Price Analytics Screen & Insights (v1.4) */

window.renderAnalyticsScreen = function() {
  window.renderAnalyticsSummary();
};

window.renderAnalyticsSummary = function() {
  const emptyState = document.getElementById('analyticsEmptyState');
  const content = document.getElementById('analyticsContent');
  const avgPriceEl = document.getElementById('metric-avg-price');
  const totalSavingsEl = document.getElementById('metric-total-savings');
  const insightsContainer = document.getElementById('analyticsInsights');

  if (window.WbBridge && typeof window.WbBridge.getAnalyticsSummary === 'function') {
    try {
      const raw = window.WbBridge.getAnalyticsSummary();
      const res = JSON.parse(raw);
      if (res.status === 'success') {
        if (!res.hasProducts) {
          if (emptyState) emptyState.style.display = 'block';
          if (content) content.style.display = 'none';
          return;
        }

        if (emptyState) emptyState.style.display = 'none';
        if (content) content.style.display = 'block';

        if (avgPriceEl) avgPriceEl.textContent = res.avgPriceFormatted || '0 ₽';
        if (totalSavingsEl) totalSavingsEl.textContent = res.totalSavingsFormatted || '0 ₽';

        if (res.chartData && Array.isArray(res.chartData.points)) {
          window.renderChart(res.chartData);
        }

        if (typeof window.renderHeroSparkline === 'function') {
          window.renderHeroSparkline(res);
        }

        if (insightsContainer && Array.isArray(res.insights)) {
          insightsContainer.innerHTML = res.insights.map(item => `
            <div class="insight-card" style="display: flex; gap: 12px; padding: 12px 14px; border-radius: 14px; background: var(--bg-card); border: 1px solid var(--border-color); margin-bottom: 8px; align-items: center;">
              <span class="insight-icon" style="font-size: 20px;">${item.icon || '💡'}</span>
              <div style="flex: 1;">
                <div class="insight-text" style="font-size: 13px; font-weight: 600; color: var(--text-primary);">${item.text || ''}</div>
                <div class="insight-time" style="font-size: 11px; color: var(--text-muted); margin-top: 2px;">${item.time || 'Сегодня'}</div>
              </div>
            </div>
          `).join('');
        }
        return;
      }
    } catch(e) {
      console.error('Error fetching analytics from bridge:', e);
    }
  }

  // Fallback calculations from local AppState
  const products = window.AppState ? window.AppState.products || [] : [];
  if (products.length === 0) {
    if (emptyState) emptyState.style.display = 'block';
    if (content) content.style.display = 'none';
    return;
  }

  if (emptyState) emptyState.style.display = 'none';
  if (content) content.style.display = 'block';

  let totalPrice = 0;
  let totalSavings = 0;
  let drops = 0;

  products.forEach(p => {
    const wPrice = p.walletPrice || p.price || 0;
    const iPrice = p.initialWalletPrice || wPrice;
    totalPrice += wPrice;
    if (wPrice < iPrice) {
      totalSavings += (iPrice - wPrice);
      drops++;
    }
  });

  const avgPrice = Math.round(totalPrice / products.length);

  if (avgPriceEl) avgPriceEl.textContent = `${avgPrice.toLocaleString('ru-RU')} ₽`;
  if (totalSavingsEl) totalSavingsEl.textContent = `${Math.round(totalSavings).toLocaleString('ru-RU')} ₽`;

  if (insightsContainer) {
    const dropsText = drops > 0
      ? `Снижение цены на ${drops} ${window.pluralize ? window.pluralize(drops, ['товар', 'товара', 'товаров']) : 'товаров'}`
      : 'Цены на отслеживаемые товары стабильны';
    const dropsIcon = drops > 0 ? '🔥' : '📈';
    insightsContainer.innerHTML = `
      <div class="insight-card" style="display: flex; gap: 12px; padding: 12px 14px; border-radius: 14px; background: var(--bg-card); border: 1px solid var(--border-color); margin-bottom: 8px; align-items: center;">
        <span class="insight-icon" style="font-size: 20px;">${dropsIcon}</span>
        <div style="flex: 1;">
          <div class="insight-text" style="font-size: 13px; font-weight: 600; color: var(--text-primary);">${dropsText}</div>
          <div class="insight-time" style="font-size: 11px; color: var(--text-muted); margin-top: 2px;">Сегодня</div>
        </div>
      </div>
    `;
  }
};
