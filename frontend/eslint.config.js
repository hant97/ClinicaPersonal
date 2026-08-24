const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');

module.exports = tseslint.config(
  {
    ignores: ['dist/**', 'coverage/**', 'node_modules/**', 'playwright-report/**', 'test-results/**']
  },
  {
    files: ['src/**/*.ts'],
    extends: [
      tseslint.configs.base,
      ...angular.configs.tsRecommended
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': 'off',
      '@angular-eslint/prefer-inject': 'off',
      '@angular-eslint/no-output-native': 'warn',
      'no-control-regex': 'off',
      'no-debugger': 'error',
      'prefer-const': 'warn'
    }
  },
  {
    files: ['src/**/*.html'],
    extends: [...angular.configs.templateRecommended]
  }
);
