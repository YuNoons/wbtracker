/* gestures.js - Touch Gestures, Sheets Drag & Android Back Handler (v1.4) */

window.initSwipeDownToDismiss = function(sheetEl, closeCallback) {
  if (!sheetEl) return;
  let startY = 0;
  let currentY = 0;
  let isDragging = false;
  let startTime = 0;

  sheetEl.addEventListener('touchstart', (e) => {
    if (e.touches && e.touches.length === 1) {
      if (sheetEl.scrollTop <= 0) {
        startY = e.touches[0].clientY;
        currentY = startY;
        isDragging = true;
        startTime = Date.now();
        sheetEl.style.transition = 'none';
      } else {
        isDragging = false;
      }
    }
  }, { passive: true });

  sheetEl.addEventListener('touchmove', (e) => {
    if (!isDragging || !e.touches || e.touches.length === 0) return;
    currentY = e.touches[0].clientY;
    const deltaY = currentY - startY;
    if (deltaY > 0) {
      sheetEl.style.transform = `translateY(${deltaY}px)`;
      const overlay = sheetEl.closest('.modal-overlay');
      if (overlay) {
        const progress = Math.max(0, 1 - (deltaY / (sheetEl.offsetHeight || 500)));
        overlay.style.backgroundColor = `rgba(0, 0, 0, ${0.65 * progress})`;
      }
    }
  }, { passive: true });

  sheetEl.addEventListener('touchend', () => {
    if (!isDragging) return;
    isDragging = false;
    const deltaY = currentY - startY;
    const deltaTime = Date.now() - startTime;
    const velocityY = deltaY / Math.max(1, deltaTime);
    const overlay = sheetEl.closest('.modal-overlay');

    if (deltaY > 80 || (deltaY > 20 && velocityY > 0.5)) {
      sheetEl.style.transition = 'transform 0.25s ease-out';
      sheetEl.style.transform = 'translateY(100%)';
      if (overlay) {
        overlay.style.transition = 'background-color 0.25s ease-out';
        overlay.style.backgroundColor = 'rgba(0, 0, 0, 0)';
      }
      setTimeout(() => {
        closeCallback();
        sheetEl.style.transform = '';
        sheetEl.style.transition = '';
        if (overlay) {
          overlay.style.backgroundColor = '';
          overlay.style.transition = '';
        }
      }, 250);
    } else {
      sheetEl.style.transition = 'transform 0.2s ease-out';
      sheetEl.style.transform = 'translateY(0)';
      if (overlay) {
        overlay.style.transition = 'background-color 0.2s ease-out';
        overlay.style.backgroundColor = '';
      }
      setTimeout(() => {
        sheetEl.style.transform = '';
        sheetEl.style.transition = '';
        if (overlay) {
          overlay.style.transition = '';
        }
      }, 200);
    }
  }, { passive: true });
};

let confirmResolver = null;

window.showConfirmSheet = function({ title, message, actionText }) {
  return new Promise((resolve) => {
    confirmResolver = resolve;
    const modal = document.getElementById('confirmSheetModal');
    const titleEl = document.getElementById('confirmSheetTitle');
    const msgEl = document.getElementById('confirmSheetMessage');
    const actionBtn = document.getElementById('confirmSheetActionBtn');

    if (titleEl) titleEl.textContent = title || 'Подтверждение';
    if (msgEl) msgEl.textContent = message || 'Вы действительно хотите выполнить это действие?';
    if (actionBtn) actionBtn.textContent = actionText || 'Удалить';

    if (modal) {
      modal.classList.add('active');
      if (window.WbNative) window.WbNative.triggerHaptic(40);
    }
  });
};

window.closeConfirmSheet = function(confirmed) {
  const modal = document.getElementById('confirmSheetModal');
  if (modal) {
    modal.classList.remove('active');
  }
  if (confirmResolver) {
    confirmResolver(!!confirmed);
    confirmResolver = null;
  }
};

window.setupAllGestures = function() {
  const alertsSheet = document.querySelector('#alertsSheet .bottom-sheet');
  if (alertsSheet && window.AlertsCenter) {
    window.initSwipeDownToDismiss(alertsSheet, () => window.AlertsCenter.closeAlertsSheet());
  }

  const addSheet = document.querySelector('#addModal .bottom-sheet');
  if (addSheet && typeof window.closeAddModal === 'function') {
    window.initSwipeDownToDismiss(addSheet, () => window.closeAddModal());
  }

  const detailSheet = document.querySelector('#productDetailModal .detail-modal-sheet');
  if (detailSheet && typeof window.closeProductDetailModal === 'function') {
    window.initSwipeDownToDismiss(detailSheet, () => window.closeProductDetailModal());
  }

  const confirmSheet = document.querySelector('#confirmSheetModal .confirm-sheet');
  if (confirmSheet) {
    window.initSwipeDownToDismiss(confirmSheet, () => window.closeConfirmSheet(false));
  }
};

window.handleAndroidBack = function() {
  const confirmModal = document.getElementById('confirmSheetModal');
  if (confirmModal && confirmModal.classList.contains('active')) {
    window.closeConfirmSheet(false);
    return true;
  }

  const alertsSheet = document.getElementById('alertsSheet');
  if (alertsSheet && alertsSheet.classList.contains('active')) {
    if (window.AlertsCenter) window.AlertsCenter.closeAlertsSheet();
    return true;
  }

  const productDetailModal = document.getElementById('productDetailModal');
  if (productDetailModal && productDetailModal.classList.contains('active')) {
    if (typeof window.closeProductDetailModal === 'function') window.closeProductDetailModal();
    return true;
  }

  const addModal = document.getElementById('addModal');
  if (addModal && addModal.classList.contains('active')) {
    if (typeof window.closeAddModal === 'function') window.closeAddModal();
    return true;
  }

  const onboardingModal = document.getElementById('onboardingModal');
  if (onboardingModal && onboardingModal.classList.contains('active')) {
    return true;
  }

  if (window.AppState && window.AppState.currentTab !== 'dashboard') {
    if (typeof window.switchTab === 'function') window.switchTab('dashboard');
    return true;
  }

  return false;
};
