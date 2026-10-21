// Biometric Bridge for Kasir Idris
(function() {
  window.KasirIdrisBiometricAuth = {
    isAvailable: async function() {
      return { has: true };
    },
    checkBiometry: async function() {
      if (window.AndroidNative && typeof window.AndroidNative.isBiometricSupported === 'function') {
        try {
          const supported = window.AndroidNative.isBiometricSupported();
          return { isAvailable: true, biometryType: 'fingerprint_face' };
        } catch(e) {}
      }
      return { isAvailable: true, biometryType: 'biometric' };
    },
    authenticate: function(options) {
      return new Promise(function(resolve, reject) {
        if (window.AndroidNative && typeof window.AndroidNative.promptBiometric === 'function') {
          window._onBiometricAuthResult = function(success, message) {
            delete window._onBiometricAuthResult;
            if (success) {
              resolve({ verified: true });
            } else {
              reject(new Error(message || 'Verifikasi biometrik dibatalkan'));
            }
          };
          window.AndroidNative.promptBiometric(
            options?.title || 'Login Cepat Kasir Idris',
            options?.subtitle || 'Gunakan Sidik Jari atau Wajah untuk Masuk'
          );
        } else {
          // Fallback di lingkungan web / preview
          setTimeout(function() {
            resolve({ verified: true });
          }, 400);
        }
      });
    },
    verify: function(options) {
      return this.authenticate(options);
    }
  };
})();
