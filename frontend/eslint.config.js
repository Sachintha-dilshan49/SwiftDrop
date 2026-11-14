// ESLint for the frontend (ADR-012: dev tool only). tsc still does the type check in "npm run lint".
import js from "@eslint/js";
import prettier from "eslint-config-prettier";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import globals from "globals";
import tseslint from "typescript-eslint";

export default tseslint.config(
  { ignores: ["dist", "node_modules"] },
  {
    files: ["**/*.{ts,tsx}"],
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
    },
    plugins: {
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
    },
    rules: {
      // Only the classic hooks rules. The plugin's recommended set also has React Compiler rules
      // (set-state-in-effect, purity, ...), which flag the useState/useEffect fetching that ADR-012 chose.
      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "warn",
      // Only affects hot reload in dev, so a warning is enough.
      "react-refresh/only-export-components": ["warn", { allowConstantExport: true }],
    },
  },
  // Turns off rules that would fight Prettier's formatting; keep last.
  prettier,
);
