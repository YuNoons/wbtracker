/* charts.js - High-Contrast Adaptive Canvas Chart Engine (v1.4) */

let currentDetailRawPoints = [];
let currentDetailRange = '30';
let currentAnalyticsRawPoints = [];
let currentAnalyticsRange = '30';

window.drawRoundRect = function(ctx, x, y, width, height, radius) {
  ctx.beginPath();
  ctx.moveTo(x + radius, y);
  ctx.lineTo(x + width - radius, y);
  ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
  ctx.lineTo(x + width, y + height - radius);
  ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
  ctx.lineTo(x + radius, y + height);
  ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
  ctx.lineTo(x, y + radius);
  ctx.quadraticCurveTo(x, y, x + radius, y);
  ctx.closePath();
};

window.filterPointsByRange = function(rawPoints, range) {
  if (!Array.isArray(rawPoints) || rawPoints.length === 0) return [];
  if (range === 'all') return window.recalculatePointPercents(rawPoints);

  const days = range === '7' ? 7 : 30;
  const now = Date.now();
  const cutoff = now - (days * 24 * 60 * 60 * 1000);

  let filtered = rawPoints.filter(p => {
    if (p.timestamp && typeof p.timestamp === 'number') {
      return p.timestamp >= cutoff;
    }
    return true;
  });

  if (filtered.length < 2 && rawPoints.length >= 2) {
    const takeCount = range === '7' ? Math.min(7, rawPoints.length) : Math.min(30, rawPoints.length);
    filtered = rawPoints.slice(-takeCount);
  }

  return window.recalculatePointPercents(filtered);
};

window.recalculatePointPercents = function(points) {
  if (!points || points.length === 0) return [];
  const prices = points.map(p => typeof p.price === 'number' ? p.price : (parseFloat(String(p.priceFormatted).replace(/[^\d.]/g, '')) || 0));
  const minP = Math.min(...prices);
  const maxP = Math.max(...prices);
  const count = points.length;

  return points.map((p, idx) => {
    const priceVal = typeof p.price === 'number' ? p.price : (parseFloat(String(p.priceFormatted).replace(/[^\d.]/g, '')) || 0);
    const xPct = count > 1 ? (idx / (count - 1)) * 100.0 : 50.0;
    const yPct = maxP > minP ? 100.0 - ((priceVal - minP) / (maxP - minP)) * 100.0 : 50.0;
    return {
      ...p,
      xPercent: Math.round(xPct * 10) / 10,
      yPercent: Math.round(yPct * 10) / 10,
      price: priceVal
    };
  });
};

window.formatXLabel = function(dateStr, idx, total) {
  if (dateStr && (dateStr.includes('назад') || dateStr === 'Сегодня' || dateStr.includes('Начало'))) {
    return dateStr;
  }
  if (idx === total - 1) return 'Сегодня';
  if (total > 1) {
    const daysAgo = total - 1 - idx;
    if (daysAgo === 1) return '1 дн. назад';
    if (daysAgo > 1 && daysAgo <= 14) return `${daysAgo} дн. назад`;
  }
  return dateStr || '';
};

window.renderCanvasChart = function(canvasId, points, targetHeight = 200, options = {}) {
  const canvas = document.getElementById(canvasId);
  if (!canvas || !canvas.parentElement) return;
  const ctx = canvas.getContext('2d');
  const width = canvas.width = canvas.parentElement.clientWidth || 320;
  const height = canvas.height = targetHeight;

  ctx.clearRect(0, 0, width, height);

  if (!points || !Array.isArray(points) || points.length === 0) {
    const themeAttr = document.documentElement.getAttribute('data-theme') || (document.body ? document.body.getAttribute('data-theme') : '');
    const isDark = themeAttr === 'dark';
    ctx.fillStyle = isDark ? '#94A3B8' : '#64748B';
    ctx.font = '500 13px Inter, sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('Начало отслеживания (1 день)', width / 2, height / 2);
    return;
  }

  const themeAttr = document.documentElement.getAttribute('data-theme') || (document.body ? document.body.getAttribute('data-theme') : '');
  const isDark = themeAttr === 'dark';

  const compStyles = getComputedStyle(document.documentElement);
  const chartLineColor = compStyles.getPropertyValue('--chart-line').trim() || (isDark ? '#A855F7' : '#7C3AED');
  const chartFillColor = compStyles.getPropertyValue('--chart-fill').trim() || (isDark ? 'rgba(168, 85, 247, 0.15)' : 'rgba(124, 58, 237, 0.12)');

  const textColor = isDark ? '#F8FAFC' : '#1E293B';
  const subTextColor = isDark ? '#94A3B8' : '#64748B';
  const gridColor = isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.08)';
  const badgeBg = isDark ? 'rgba(30, 41, 59, 0.9)' : 'rgba(241, 245, 249, 0.95)';
  const badgeBorder = isDark ? 'rgba(255, 255, 255, 0.15)' : 'rgba(0, 0, 0, 0.12)';

  const paddingLeft = 62;
  const paddingRight = 20;
  const paddingTop = 32;
  const paddingBottom = 28;

  const drawWidth = width - paddingLeft - paddingRight;
  const drawHeight = height - paddingTop - paddingBottom;

  const targetPrice = (options && typeof options.targetPrice === 'number' && options.targetPrice > 0 && options.targetEnabled !== false)
    ? options.targetPrice
    : null;

  const pointPrices = points.map(pt => {
    let priceNum = typeof pt.price === 'number' ? pt.price : 0;
    if (!priceNum && pt.priceFormatted) {
      priceNum = parseFloat(String(pt.priceFormatted).replace(/[^\d.]/g, '')) || 0;
    }
    return priceNum;
  }).filter(p => p > 0);

  let minP = pointPrices.length > 0 ? Math.min(...pointPrices) : 0;
  let maxP = pointPrices.length > 0 ? Math.max(...pointPrices) : 0;

  let yMinVal = minP;
  let yMaxVal = maxP;
  if (targetPrice !== null) {
    if (yMinVal === 0 && yMaxVal === 0) {
      yMinVal = targetPrice * 0.9;
      yMaxVal = targetPrice * 1.1;
    } else {
      yMinVal = Math.min(yMinVal, targetPrice);
      yMaxVal = Math.max(yMaxVal, targetPrice);
    }
  }

  const isSinglePrice = (yMinVal === yMaxVal);
  if (isSinglePrice) {
    if (yMinVal === 0) {
      yMinVal = 1000;
      yMaxVal = 1200;
    } else {
      yMinVal = yMinVal * 0.9;
      yMaxVal = yMaxVal * 1.1;
    }
  }

  const avgP = Math.round((yMinVal + yMaxVal) / 2);

  const coords = points.map((pt, idx) => {
    let priceNum = typeof pt.price === 'number' ? pt.price : 0;
    if (!priceNum && pt.priceFormatted) {
      priceNum = parseFloat(String(pt.priceFormatted).replace(/[^\d.]/g, '')) || 0;
    }

    let xPct = typeof pt.xPercent === 'number' ? pt.xPercent : (points.length > 1 ? (idx / (points.length - 1)) * 100 : 50);
    const x = paddingLeft + (xPct / 100.0) * drawWidth;

    let y;
    if (yMaxVal > yMinVal && priceNum > 0) {
      y = paddingTop + (1.0 - (priceNum - yMinVal) / (yMaxVal - yMinVal)) * drawHeight;
    } else {
      y = paddingTop + (pt.yPercent !== undefined ? (pt.yPercent / 100.0) * drawHeight : drawHeight / 2);
    }

    return {
      x,
      y,
      priceNum,
      priceStr: pt.priceFormatted || (priceNum ? `${Math.round(priceNum)} ₽` : ''),
      dateStr: pt.dateFormatted || pt.dateStr || ''
    };
  });

  canvas._chartPoints = points;
  canvas._targetHeight = targetHeight;
  canvas._chartOptions = options;
  canvas._coords = coords;

  if (!canvas._scrubberAttached) {
    canvas._scrubberAttached = true;
    canvas.style.touchAction = 'none';

    let scrubberRafId = null;

    function updateScrubber(e) {
      if (scrubberRafId) cancelAnimationFrame(scrubberRafId);
      scrubberRafId = requestAnimationFrame(() => {
        if (!canvas._coords || canvas._coords.length === 0) return;
        const rect = canvas.getBoundingClientRect();
        let clientX = e.clientX;
        if (e.touches && e.touches.length > 0) {
          clientX = e.touches[0].clientX;
        }
        if (clientX === undefined) return;

        const x = clientX - rect.left;

        let closestIdx = 0;
        let minDistance = Math.abs(x - canvas._coords[0].x);
        for (let i = 1; i < canvas._coords.length; i++) {
          const dist = Math.abs(x - canvas._coords[i].x);
          if (dist < minDistance) {
            minDistance = dist;
            closestIdx = i;
          }
        }

        if (canvas._activePointIndex !== closestIdx) {
          canvas._activePointIndex = closestIdx;
          window.renderCanvasChart(canvasId, canvas._chartPoints, canvas._targetHeight, canvas._chartOptions);
        }
      });
    }

    function clearScrubber() {
      if (scrubberRafId) cancelAnimationFrame(scrubberRafId);
      if (canvas._activePointIndex !== undefined && canvas._activePointIndex !== null) {
        canvas._activePointIndex = null;
        window.renderCanvasChart(canvasId, canvas._chartPoints, canvas._targetHeight, canvas._chartOptions);
      }
    }

    canvas.addEventListener('pointerdown', (e) => { updateScrubber(e); });
    canvas.addEventListener('pointermove', (e) => { if (e.buttons > 0 || e.pointerType === 'touch') updateScrubber(e); });
    canvas.addEventListener('pointerup', clearScrubber);
    canvas.addEventListener('pointercancel', clearScrubber);
    canvas.addEventListener('pointerleave', clearScrubber);

    canvas.addEventListener('touchstart', (e) => { e.preventDefault(); updateScrubber(e); }, { passive: false });
    canvas.addEventListener('touchmove', (e) => { e.preventDefault(); updateScrubber(e); }, { passive: false });
    canvas.addEventListener('touchend', clearScrubber);
    canvas.addEventListener('touchcancel', clearScrubber);

    canvas.addEventListener('mousemove', (e) => { if (e.buttons > 0) updateScrubber(e); });
    canvas.addEventListener('mouseleave', clearScrubber);
  }

  // Y-Axis Horizontal Grid Lines
  const yMax = paddingTop;
  const yAvg = paddingTop + drawHeight / 2;
  const yMin = paddingTop + drawHeight;

  ctx.strokeStyle = gridColor;
  ctx.lineWidth = 1;

  // Max Price Line
  ctx.beginPath();
  ctx.setLineDash([]);
  ctx.moveTo(paddingLeft, yMax);
  ctx.lineTo(width - paddingRight, yMax);
  ctx.stroke();

  // Avg Price Line (Dashed)
  ctx.beginPath();
  ctx.setLineDash([4, 4]);
  ctx.moveTo(paddingLeft, yAvg);
  ctx.lineTo(width - paddingRight, yAvg);
  ctx.stroke();
  ctx.setLineDash([]);

  // Min Price Line
  ctx.beginPath();
  ctx.moveTo(paddingLeft, yMin);
  ctx.lineTo(width - paddingRight, yMin);
  ctx.stroke();

  // Price Labels on Y Axis
  ctx.fillStyle = subTextColor;
  ctx.font = '600 12px sans-serif';
  ctx.textAlign = 'right';
  ctx.textBaseline = 'middle';

  const formatYLabel = (val) => Math.round(val) + ' ₽';
  ctx.fillText(formatYLabel(yMaxVal), paddingLeft - 8, yMax);
  ctx.fillText(formatYLabel(avgP), paddingLeft - 8, yAvg);
  ctx.fillText(formatYLabel(yMinVal), paddingLeft - 8, yMin);

  // Target Price Line if enabled
  if (targetPrice !== null && yMaxVal > yMinVal) {
    const tY = paddingTop + (1.0 - (targetPrice - yMinVal) / (yMaxVal - yMinVal)) * drawHeight;

    ctx.save();
    ctx.strokeStyle = '#EF4444';
    ctx.lineWidth = 1.5;
    ctx.setLineDash([6, 4]);
    ctx.beginPath();
    ctx.moveTo(paddingLeft, tY);
    ctx.lineTo(width - paddingRight, tY);
    ctx.stroke();
    ctx.restore();

    ctx.font = 'bold 10px sans-serif';
    const tText = `Цель: ${Math.round(targetPrice)} ₽`;
    const tWidth = ctx.measureText(tText).width;
    const tBadgeW = tWidth + 10;
    const tBadgeH = 18;
    const tBadgeX = width - paddingRight - tBadgeW;
    const tBadgeY = Math.max(paddingTop, Math.min(paddingTop + drawHeight - tBadgeH, tY - tBadgeH / 2));

    ctx.fillStyle = isDark ? 'rgba(239, 68, 68, 0.9)' : 'rgba(239, 68, 68, 0.95)';
    window.drawRoundRect(ctx, tBadgeX, tBadgeY, tBadgeW, tBadgeH, 4);
    ctx.fill();

    ctx.fillStyle = '#FFFFFF';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(tText, tBadgeX + tBadgeW / 2, tBadgeY + tBadgeH / 2 + 0.5);
  }

  // X-Axis Days Labels
  ctx.fillStyle = subTextColor;
  ctx.font = '500 13px sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'top';

  const totalPoints = coords.length;
  if (totalPoints > 1) {
    let maxTicks = Math.min(5, Math.max(3, Math.floor(drawWidth / 65)));
    if (maxTicks > totalPoints) maxTicks = totalPoints;
    if (maxTicks < 2) maxTicks = 2;

    const step = (totalPoints - 1) / (maxTicks - 1);
    for (let k = 0; k < maxTicks; k++) {
      const idx = Math.round(k * step);
      const pt = coords[idx];
      if (pt) {
        const label = window.formatXLabel(pt.dateStr, idx, totalPoints);
        if (label) {
          ctx.fillText(label, pt.x, height - paddingBottom + 6);
        }
      }
    }
  } else if (totalPoints === 1) {
    ctx.fillText('Начало отслеживания (1 день)', paddingLeft + drawWidth / 2, height - paddingBottom + 6);
  }

  // Gradient Fill Under Curve
  if (coords.length > 0) {
    const grad = ctx.createLinearGradient(0, paddingTop, 0, paddingTop + drawHeight);
    grad.addColorStop(0, chartFillColor);
    grad.addColorStop(1, 'rgba(0, 0, 0, 0)');

    ctx.beginPath();
    if (coords.length === 1) {
      ctx.rect(paddingLeft, coords[0].y, drawWidth, paddingTop + drawHeight - coords[0].y);
    } else {
      ctx.moveTo(coords[0].x, paddingTop + drawHeight);
      ctx.lineTo(coords[0].x, coords[0].y);
      for (let i = 0; i < coords.length - 1; i++) {
        const xc = (coords[i].x + coords[i + 1].x) / 2;
        const yc = (coords[i].y + coords[i + 1].y) / 2;
        ctx.quadraticCurveTo(coords[i].x, coords[i].y, xc, yc);
      }
      ctx.lineTo(coords[coords.length - 1].x, coords[coords.length - 1].y);
      ctx.lineTo(coords[coords.length - 1].x, paddingTop + drawHeight);
      ctx.closePath();
    }
    ctx.fillStyle = grad;
    ctx.fill();
  }

  // Smooth Line Curve / Horizontal Line for 1 Point
  ctx.beginPath();
  ctx.strokeStyle = chartLineColor;
  ctx.lineWidth = 3;

  if (coords.length === 1) {
    ctx.moveTo(paddingLeft, coords[0].y);
    ctx.lineTo(width - paddingRight, coords[0].y);
  } else {
    ctx.moveTo(coords[0].x, coords[0].y);
    for (let i = 0; i < coords.length - 1; i++) {
      const xc = (coords[i].x + coords[i + 1].x) / 2;
      const yc = (coords[i].y + coords[i + 1].y) / 2;
      ctx.quadraticCurveTo(coords[i].x, coords[i].y, xc, yc);
    }
    ctx.lineTo(coords[coords.length - 1].x, coords[coords.length - 1].y);
  }
  ctx.stroke();

  const activeIdx = (canvas._activePointIndex !== undefined && canvas._activePointIndex !== null)
    ? canvas._activePointIndex
    : null;

  // Node Points & Price Badges
  coords.forEach((pt, idx) => {
    const isActive = (idx === activeIdx);

    if (!isActive) {
      ctx.beginPath();
      ctx.arc(pt.x, pt.y, 4.5, 0, Math.PI * 2);
      ctx.fillStyle = '#EC4899';
      ctx.fill();
      ctx.strokeStyle = isDark ? '#1E293B' : '#FFFFFF';
      ctx.lineWidth = 1.5;
      ctx.stroke();

      if (pt.priceStr) {
        ctx.font = 'bold 11px sans-serif';
        const textWidth = ctx.measureText(pt.priceStr).width;
        const badgeW = textWidth + 12;
        const badgeH = 20;

        let badgeX = pt.x - badgeW / 2;
        badgeX = Math.max(4, Math.min(width - badgeW - 4, badgeX));

        let badgeY = pt.y - badgeH - 6;
        if (badgeY < 6) {
          badgeY = pt.y + 8;
        }

        ctx.fillStyle = badgeBg;
        window.drawRoundRect(ctx, badgeX, badgeY, badgeW, badgeH, 6);
        ctx.fill();

        ctx.strokeStyle = badgeBorder;
        ctx.lineWidth = 1;
        ctx.stroke();

        ctx.fillStyle = textColor;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(pt.priceStr, badgeX + badgeW / 2, badgeY + badgeH / 2 + 0.5);
      }
    }
  });

  // Touch Scrubber & Tooltip Overlay
  if (activeIdx !== null && coords[activeIdx]) {
    const activePt = coords[activeIdx];

    ctx.save();
    ctx.setLineDash([3, 3]);
    ctx.strokeStyle = isDark ? 'rgba(168, 85, 247, 0.85)' : '#7C3AED';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(activePt.x, paddingTop);
    ctx.lineTo(activePt.x, height - paddingBottom);
    ctx.stroke();
    ctx.restore();

    ctx.save();
    ctx.shadowColor = '#EC4899';
    ctx.shadowBlur = 12;
    ctx.beginPath();
    ctx.arc(activePt.x, activePt.y, 6.5, 0, Math.PI * 2);
    ctx.fillStyle = '#EC4899';
    ctx.fill();
    ctx.strokeStyle = '#FFFFFF';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.restore();

    ctx.save();
    const dateLine = activePt.dateStr || '';
    const priceLine = activePt.priceStr || '';

    ctx.font = '500 11px sans-serif';
    const dateW = dateLine ? ctx.measureText(dateLine).width : 0;
    ctx.font = 'bold 13px sans-serif';
    const priceW = priceLine ? ctx.measureText(priceLine).width : 0;

    const ttW = Math.max(dateW, priceW) + 20;
    const ttH = dateLine ? 42 : 28;

    let ttX = activePt.x - ttW / 2;
    ttX = Math.max(6, Math.min(width - ttW - 6, ttX));

    let ttY = activePt.y - ttH - 12;
    if (ttY < paddingTop - 10) {
      ttY = activePt.y + 12;
    }

    ctx.shadowColor = 'rgba(0, 0, 0, 0.35)';
    ctx.shadowBlur = 10;
    ctx.fillStyle = isDark ? '#1E293B' : '#FFFFFF';
    window.drawRoundRect(ctx, ttX, ttY, ttW, ttH, 10);
    ctx.fill();

    ctx.shadowBlur = 0;
    ctx.strokeStyle = isDark ? 'rgba(168, 85, 247, 0.5)' : 'rgba(124, 58, 237, 0.4)';
    ctx.lineWidth = 1.5;
    ctx.stroke();

    if (dateLine) {
      ctx.fillStyle = subTextColor;
      ctx.font = '500 10px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'top';
      ctx.fillText(dateLine, ttX + ttW / 2, ttY + 6);

      ctx.fillStyle = isDark ? '#34D399' : '#059669';
      ctx.font = 'bold 13px sans-serif';
      ctx.fillText(priceLine, ttX + ttW / 2, ttY + 22);
    } else {
      ctx.fillStyle = textColor;
      ctx.font = 'bold 12px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(priceLine, ttX + ttW / 2, ttY + ttH / 2);
    }
    ctx.restore();
  }
};

window.renderChart = function(chartData) {
  if (!chartData || !Array.isArray(chartData.points)) return;
  currentAnalyticsRawPoints = chartData.points;
  window.renderAnalyticsChart();
};

window.renderAnalyticsChart = function() {
  const filteredPoints = window.filterPointsByRange(currentAnalyticsRawPoints, currentAnalyticsRange);
  window.renderCanvasChart('priceChart', filteredPoints, 200);
};

window.setAnalyticsChartRange = function(range) {
  currentAnalyticsRange = range;
  const chips = document.querySelectorAll('#analytics-range-selector .range-chip');
  chips.forEach(c => {
    if (c.getAttribute('data-range') === range) c.classList.add('active');
    else c.classList.remove('active');
  });
  window.renderAnalyticsChart();
};

window.renderDetailChart = function(points, options = {}) {
  currentDetailRawPoints = points || [];
  const filtered = window.filterPointsByRange(currentDetailRawPoints, currentDetailRange);
  window.renderCanvasChart('detailPriceChart', filtered, 180, options);
};

window.setDetailChartRange = function(range) {
  currentDetailRange = range;
  const chips = document.querySelectorAll('#detail-range-selector .range-chip');
  chips.forEach(c => {
    if (c.getAttribute('data-range') === range) c.classList.add('active');
    else c.classList.remove('active');
  });
  const prod = window.AppState && window.AppState.products ? window.AppState.products.find(p => String(p.id) === String(window.AppState.currentDetailProductId)) : null;
  const targetOpt = prod ? { targetPrice: prod.targetPrice, targetEnabled: prod.targetEnabled } : {};
  window.renderDetailChart(currentDetailRawPoints, targetOpt);
};
