/* toast.js - Top Toast Notification System with Undo Support (v1.4) */
let toastTimeout = null;

window.showTopToast = function(type, text, durationMs = 6000, actionLabel = null, actionCallback = null) {
  const toast = document.getElementById('topToast');
  const toastText = document.getElementById('topToastText');
  if (!toast || !toastText) return;

  if (toastTimeout) {
    clearTimeout(toastTimeout);
    toastTimeout = null;
  }

  toast.className = 'top-toast';

  if (type === 'error') {
    toast.classList.add('error');
  } else if (type === 'search' || type === 'info') {
    toast.classList.add('search');
  } else {
    toast.classList.add('success');
  }

  if (actionLabel && typeof actionCallback === 'function') {
    toastText.innerHTML = `<span>${text}</span> <button class="top-toast-undo-btn" id="toastUndoBtn">${actionLabel}</button>`;
    setTimeout(() => {
      const undoBtn = document.getElementById('toastUndoBtn');
      if (undoBtn) {
        undoBtn.onclick = function(e) {
          e.stopPropagation();
          actionCallback();
          window.hideTopToast();
        };
      }
    }, 50);
  } else {
    toastText.textContent = text;
  }

  toast.classList.add('show');

  toastTimeout = setTimeout(() => {
    window.hideTopToast();
  }, durationMs);
};

window.hideTopToast = function() {
  const toast = document.getElementById('topToast');
  if (toast) {
    toast.classList.remove('show');
  }
  if (toastTimeout) {
    clearTimeout(toastTimeout);
    toastTimeout = null;
  }
};

window.showToast = function(type, text, durationMs) {
  window.showTopToast(type, text, durationMs);
};
