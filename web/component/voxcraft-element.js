(function () {
  const currentScript = document.currentScript;
  const baseUrl = currentScript && currentScript.src
    ? new URL('.', currentScript.src).href
    : new URL('.', window.location.href).href;
  const state = window.__voxcraftComponentRuntime || (window.__voxcraftComponentRuntime = {
    loadPromise: null,
    started: false
  });

  function runtimeUrl() {
    return new URL('voxcraft-runtime.js', baseUrl).href;
  }

  function ensureRuntimeLoaded(host) {
    if (state.loadPromise) {
      return state.loadPromise;
    }
    window.__voxcraftEmbedState = Object.assign({}, window.__voxcraftEmbedState, {
      baseUrl
    });
    state.loadPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = runtimeUrl();
      script.async = true;
      script.onload = () => {
        try {
          if (!state.started) {
            if (typeof window.main !== 'function') {
              reject(new Error('voxcraft runtime loaded but main() is not available.'));
              return;
            }
            state.started = true;
            window.main();
          }
          host.dispatchEvent(new CustomEvent('ready', {
            detail: { status: 'ready' },
            bubbles: true,
            composed: true
          }));
          resolve();
        } catch (error) {
          reject(error);
        }
      };
      script.onerror = () => reject(new Error('Failed to load voxcraft-runtime.js'));
      document.head.appendChild(script);
    });
    return state.loadPromise;
  }

  function suppressContextMenu(event) {
    event.preventDefault();
    event.stopPropagation();
  }

  class VoxcraftEditorElement extends HTMLElement {
    connectedCallback() {
      if (this.__connected) {
        return;
      }
      this.__connected = true;
      this.style.display = this.style.display || 'block';
      if (!this.style.minHeight) {
        this.style.minHeight = '480px';
      }
      this.textContent = '';
      const canvas = document.createElement('canvas');
      canvas.id = 'canvas';
      canvas.style.width = '100%';
      canvas.style.height = '100%';
      canvas.style.display = 'block';
      this.addEventListener('contextmenu', suppressContextMenu);
      canvas.addEventListener('contextmenu', suppressContextMenu);
      this.appendChild(canvas);
      ensureRuntimeLoaded(this).catch((error) => {
        this.dispatchEvent(new CustomEvent('error', {
          detail: { message: String(error && error.message ? error.message : error) },
          bubbles: true,
          composed: true
        }));
      });
    }
  }

  if (!customElements.get('voxcraft-editor')) {
    customElements.define('voxcraft-editor', VoxcraftEditorElement);
  }
})();
