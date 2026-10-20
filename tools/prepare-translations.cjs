// Export only untranslated text; drafts are never loaded by the app.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const destination = path.join(root, 'docs/localization/to-translate');
fs.mkdirSync(destination, { recursive: true });
for (const file of ['strings', 'remote', 'true-false', 'fallback-questions']) {
  const ru = JSON.parse(fs.readFileSync(path.join(root, `core/src/main/resources/locales/ru/${file}.json`), 'utf8'));
  const translatedPath = path.join(root, `core/src/main/resources/locales/he/${file}.json`);
  const he = fs.existsSync(translatedPath) ? JSON.parse(fs.readFileSync(translatedPath, 'utf8')) : {};
  const pending = file === 'strings' || file === 'remote'
    ? Object.fromEntries(Object.entries(ru).filter(([key]) => !(key in he))) : ru;
  fs.writeFileSync(path.join(destination, `${file}.he.json`), JSON.stringify(pending, null, 2) + '\n');
}
console.log('Translation drafts exported to docs/localization/to-translate');
