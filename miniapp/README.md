# Dr. Quizman — Telegram Mini App

One question JSON file is one pack. The five packs have 750 questions and 15 topics each. The compact catalog supports language, topic, question count, access and price filters. No question preview or payment flow is enabled.

The frontend sends Telegram.WebApp.initData to the Supabase quizman function. The server verifies the signature, timestamp and Telegram user ID, then reads pack access for that account. No user ID entry is needed. Signed session data stays in memory and is not persisted in browser storage.

Outside Telegram, the page shows an Open bot link. The backend URL is public configuration; no bot token or server credential is present in the frontend. catalog.js provides topic icons only; access and pack data come from Supabase.

Generate topic metadata: node import-packs.cjs <question-json-folder>
Local server: node serve.cjs
Backend tests from the repository root: node tools/check-miniapp.mjs

Publication uses the existing Sites project in .openai/hosting.json. The public visitor audience requires user approval before this page can open in Telegram. After publication, set MINIAPP_URL on Supabase and configure the bot menu with the protected /admin/configure-bot route. QUIZMAN_ADMIN_TOKEN must be temporary and removed immediately after setup. The bot token is already stored in Supabase.
