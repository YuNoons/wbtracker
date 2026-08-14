/* onboarding.js - 1-Time Onboarding Screen Module (v1.4) */
window.OnboardingModule = {
  currentSlide: 1,
  totalSlides: 4,

  init: function() {
    this.checkStatus();
  },

  selectPriceMode: function(mode) {
    if (window.WbBridge && typeof window.WbBridge.setPriceTrackingMode === 'function') {
      window.WbBridge.setPriceTrackingMode(mode);
    }
    localStorage.setItem('price_tracking_mode', mode);
    window.OnboardingModule.nextSlide();
  },

  checkStatus: function() {
    let isCompleted = localStorage.getItem('onboarding_completed') === 'true';
    if (window.WbBridge && typeof window.WbBridge.isOnboardingCompleted === 'function') {
      try {
        isCompleted = window.WbBridge.isOnboardingCompleted();
      } catch (e) {
        console.error('Error checking onboarding status:', e);
      }
    }
    if (!isCompleted) {
      this.showOnboarding();
    }
  },

  showOnboarding: function() {
    const modal = document.getElementById('onboardingModal');
    if (modal) {
      this.currentSlide = 1;
      this.renderSlide();
      modal.classList.add('active');
    }
  },

  nextSlide: function() {
    if (window.WbNative) window.WbNative.triggerHaptic(30);
    if (this.currentSlide < this.totalSlides) {
      this.currentSlide++;
      this.renderSlide();
    } else {
      this.completeOnboarding();
    }
  },

  prevSlide: function() {
    if (window.WbNative) window.WbNative.triggerHaptic(30);
    if (this.currentSlide > 1) {
      this.currentSlide--;
      this.renderSlide();
    }
  },

  goToSlide: function(slideNum) {
    if (window.WbNative) window.WbNative.triggerHaptic(30);
    this.currentSlide = slideNum;
    this.renderSlide();
  },

  renderSlide: function() {
    const slides = document.querySelectorAll('.onboarding-slide');
    const dots = document.querySelectorAll('.onboarding-dot');

    slides.forEach((slide, index) => {
      if (index + 1 === this.currentSlide) {
        slide.classList.add('active');
      } else {
        slide.classList.remove('active');
      }
    });

    dots.forEach((dot, index) => {
      if (index + 1 === this.currentSlide) {
        dot.classList.add('active');
      } else {
        dot.classList.remove('active');
      }
    });
  },

  requestBatteryPermission: function() {
    if (window.WbNative && typeof window.WbNative.requestBatteryExemption === 'function') {
      window.WbNative.requestBatteryExemption();
    }
  },

  requestNotificationPermission: function() {
    if (window.WbBridge && typeof window.WbBridge.requestNotificationPermission === 'function') {
      window.WbBridge.requestNotificationPermission();
    } else if (window.WbNative && typeof window.WbNative.requestBatteryExemption === 'function') {
      window.WbNative.requestBatteryExemption();
    }
  },

  completeOnboarding: function() {
    if (window.WbNative) window.WbNative.triggerHaptic(50);
    localStorage.setItem('onboarding_completed', 'true');
    if (window.AppState) window.AppState.onboardingCompleted = true;

    if (window.WbBridge && typeof window.WbBridge.completeOnboarding === 'function') {
      try {
        window.WbBridge.completeOnboarding();
      } catch (e) {
        console.error('Error completing onboarding:', e);
      }
    }

    const modal = document.getElementById('onboardingModal');
    if (modal) {
      modal.classList.remove('active');
    }
  }
};
