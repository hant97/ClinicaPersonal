// jsdom no implementa matchMedia. Se simula un viewport de escritorio amplio (como el Chrome
// que usaba Karma) para los componentes que adaptan el layout al tamaño de pantalla.
if (typeof window !== 'undefined' && typeof window.matchMedia !== 'function') {
  const DESKTOP_WIDTH = 1440;
  const matches = (query: string): boolean => {
    const min = /min-width:\s*(\d+)px/.exec(query);
    const max = /max-width:\s*(\d+)px/.exec(query);
    return (!min || DESKTOP_WIDTH >= Number(min[1])) && (!max || DESKTOP_WIDTH <= Number(max[1]));
  };

  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: (query: string): MediaQueryList => ({
      matches: matches(query),
      media: query,
      onchange: null,
      addListener: () => undefined,
      removeListener: () => undefined,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      dispatchEvent: () => false
    })
  });
}
