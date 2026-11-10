import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import react from 'eslint-plugin-react';
import reactHooks from 'eslint-plugin-react-hooks';
import jsxA11y from 'eslint-plugin-jsx-a11y';

const restrict = (patterns, message) => [
  'error',
  { patterns: patterns.map((group) => ({ group: [group], message, allowTypeImports: true })) },
];

export default tseslint.config(
  { ignores: ['dist', 'node_modules', 'coverage'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  react.configs.flat.recommended,
  react.configs.flat['jsx-runtime'],
  reactHooks.configs.flat.recommended,
  jsxA11y.flatConfigs.recommended,
  {
    settings: { react: { version: 'detect' } },
    languageOptions: { ecmaVersion: 2022, sourceType: 'module' },
    rules: {
      // Security: never render HTML strings (XSS). Model/user content is always plain text.
      'react/no-danger': 'error',
      'react/no-danger-with-children': 'error',
      'no-restricted-syntax': [
        'error',
        { selector: "MemberExpression[property.name='innerHTML']", message: 'Do not use innerHTML; render text with React.' },
        { selector: "MemberExpression[property.name='outerHTML']", message: 'Do not use outerHTML; render text with React.' },
        { selector: "CallExpression[callee.property.name='insertAdjacentHTML']", message: 'Do not inject HTML strings.' },
        { selector: "JSXAttribute[name.name='style']", message: 'No inline styles: they break the strict Content-Security-Policy. Use a CSS module.' },
      ],
      'no-console': ['error', { allow: ['warn', 'error'] }],
      'react/prop-types': 'off',
    },
  },
  // Layering, enforced: each layer only reaches the one below it (type-only imports are fine).
  {
    files: ['src/components/**/*.{ts,tsx}'],
    ignores: ['**/*.test.{ts,tsx}'],
    rules: {
      '@typescript-eslint/no-restricted-imports': restrict(
        ['**/services', '**/services/*', '**/state/*/*', '**/state/*'],
        'Components talk to hooks, not to services or state directly.',
      ),
    },
  },
  {
    files: ['src/hooks/**/*.{ts,tsx}'],
    ignores: ['**/*.test.{ts,tsx}'],
    rules: {
      '@typescript-eslint/no-restricted-imports': restrict(['**/components/*', '**/components/**'], 'Hooks must not depend on components.'),
    },
  },
  {
    files: ['src/services/**/*.ts', 'src/schemas/**/*.ts', 'src/util/**/*.ts', 'src/config/**/*.ts'],
    ignores: ['**/*.test.{ts,tsx}'],
    rules: {
      '@typescript-eslint/no-restricted-imports': restrict(
        ['**/components/*', '**/components/**', '**/hooks/*', '**/state/*', '**/state/**'],
        'Lower layers must not depend on UI, hooks or state.',
      ),
    },
  },
);
