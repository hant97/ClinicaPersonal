import { defineConfig } from 'vitest/config';

// Configuración adicional que el builder @angular/build:unit-test combina con la suya.
export default defineConfig({
  test: {
    // Con muchos workers de jsdom en paralelo (y en runners de CI con pocos núcleos), una
    // prueba síncrona puede superar los 5 s por defecto solo por contención de CPU.
    testTimeout: 15_000
  }
});
