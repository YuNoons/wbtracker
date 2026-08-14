/* theme.js - Theme Engine & Style Packs (v1.4) */
window.ThemeEngine = {
  init: function() {
    let savedTheme = localStorage.getItem('wb_theme') || 'dark';
    let savedStyle = localStorage.getItem('wb_style_pack') || 'finance';

    if (window.WbBridge && typeof window.WbBridge.getActiveTheme === 'function') {
      try {
        const t = window.WbBridge.getActiveTheme();
        if (t) savedStyle = t;
      } catch (e) {}
    }

    if (window.WbBridge && typeof window.WbBridge.getDarkTheme === 'function') {
      try {
        const dt = window.WbBridge.getDarkTheme();
        savedTheme = dt ? 'dark' : 'light';
      } catch (e) {}
    }

    this.setTheme(savedTheme);
    this.setStylePack(savedStyle, null, false);
  },

  setTheme: function(theme) {
    localStorage.setItem('wb_theme', theme);
    document.documentElement.setAttribute('data-theme', theme);
    if (document.body) document.body.setAttribute('data-theme', theme);
    
    const themeToggle = document.getElementById('theme-toggle');
    if (themeToggle) {
      themeToggle.checked = (theme === 'dark');
    }
  },

  setStylePack: function(style, elem, notifyBridge = true) {
    localStorage.setItem('wb_style_pack', style);
    document.documentElement.setAttribute('data-style', style);
    if (document.body) document.body.setAttribute('data-style', style);

    if (notifyBridge && window.WbBridge && typeof window.WbBridge.setActiveTheme === 'function') {
      try {
        window.WbBridge.setActiveTheme(style);
      } catch (e) {}
    }

    const chips = document.querySelectorAll('.theme-pack-chip');
    chips.forEach(c => {
      if (c.getAttribute('data-style') === style) {
        c.classList.add('active');
      } else {
        c.classList.remove('active');
      }
    });

    if (window.WbNative) window.WbNative.triggerHaptic(30);
  }
};

window.initTheme = function() {
  window.ThemeEngine.init();
};

window.toggleDarkTheme = function(enabled) {
  const newTheme = enabled ? 'dark' : 'light';
  window.ThemeEngine.setTheme(newTheme);
  if (window.WbBridge && typeof window.WbBridge.setDarkTheme === 'function') {
    try {
      window.WbBridge.setDarkTheme(JSON.stringify({ enabled: enabled }));
    } catch(e) {}
  }
};
