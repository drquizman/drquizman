# DrQuizman_bot — initial Supabase integration

Project: `xoerzqgvcpjrbuwtbtdo`.

## Deployment status (2026-09-23)

CLI 2.117.0 is authenticated and linked. Migration `202609230001_question_packs.sql` has been applied with `db push`. All five JSON packs and catalog rows are uploaded; the bucket is private. Function `quizman` is deployed at `https://xoerzqgvcpjrbuwtbtdo.supabase.co/functions/v1/quizman`. Live health, anonymous-access rejection, code creation and pending polling passed. The root `DrQuizman-debug.apk` is built with this endpoint; pack-selection and localization checks passed.

Telegram was connected on 2026-09-23: the helper validated the bot username, saved secrets, registered and verified the webhook, and deleted its temporary secret file. Its non-secret status is written to `supabase/.temp/telegram-setup-status.json` (`connected`). For future credential setup, `tools/connect-telegram-bot.ps1` accepts the token in a masked local field. Full Telegram approval and entitled download testing remain pending until the owner's exact Telegram ID is known.

This initial backend supports private-chat bot commands `/start`, `/id`, `/packs`, code confirmation, installation authentication, entitlement listing and private downloads. Payment processing and live Mini App purchases are deliberately not enabled; prices have not been approved. The existing Mini App is still a local design preview.

## Setup

1. Apply `migrations/202609230001_question_packs.sql` in the project's SQL Editor, once.
2. Run `node tools/prepare-supabase-packs.cjs <source-folder>` from the repository root. Upload the five generated JSON files to the root of the **private** `question-packs` bucket, preserving their names. Run generated `outputs/supabase-upload/catalog.sql` in SQL Editor. Do not upload manifest.json or catalog.sql to the bucket.
3. Create Edge Function `quizman` with `functions/quizman/index.mjs`. Configure gateway JWT verification off **for this new function only**: the handler verifies its own device bearer tokens and the Telegram webhook secret. Tables and bucket must remain private.
4. In Edge Function Secrets add `TELEGRAM_BOT_TOKEN` (from BotFather) and a random `TELEGRAM_WEBHOOK_SECRET` of at least 32 characters. Supabase supplies `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY`. Never place secrets in the APK, browser JS, tracked files or chat.
5. Register a Telegram webhook to `https://xoerzqgvcpjrbuwtbtdo.supabase.co/functions/v1/quizman/telegram`, with the same `secret_token` and `allowed_updates: ["message","callback_query"]`. Verify `getWebhookInfo` and `/start` in the private bot chat.
6. Build Android with `-PpackServiceUrl=https://xoerzqgvcpjrbuwtbtdo.supabase.co/functions/v1/quizman`. The APK then uses the existing `/v1/...` API paths.
7. For an owner's test, send `/id` to the bot, then deliberately grant that exact account entitlement from SQL Editor (replace the numeric ID): `insert into public.qm_entitlements(telegram_id,pack_id) select 123456789,id from public.qm_packs on conflict do nothing;`. This is a test grant, not a payment record. Never grant to an inferred ID.

## Checks and scope

`node tools/check-pack-service.mjs` checks API handling with a fake database transport. A live deployment still needs migration, storage, webhook and Android end-to-end checks. No live payment is accepted by this function. Prices remain null and files are not made free by default.

Codes last 10 minutes, are approved by an explicit bot button, and approvals use a SQL row lock. Only hashes of high-entropy credentials are stored. Duplicate approval by the same account is harmless; another account cannot claim an approved code. Revoking `qm_devices.revoked` blocks future API access but does not delete offline files. Rate limits protect creation, bot attempts and downloads; expired links/rate-limit records should be periodically pruned in production.

Do not expose the REST tables or Storage bucket to anon/authenticated users. The function's service role performs all access checks. This initial bot replies in Russian; the game UI remains localized in five languages.

## New packs and zero-cost test orders (2026-09-24)

`tools/prepare-new-300.cjs` validates and stages 30 files: one free pack of 250 questions and five test-purchase extras of 10 questions for each language. All answers use index 0. The 750-question pack plus these files contains 1,050 distinct questions per language.

Migration `202609240001_test_purchases.sql` adds explicitly enabled test-purchase packs and a separate zero-cost order ledger. `qm_test_purchase` atomically writes one order and its entitlement, with uniqueness on account+pack. It cannot purchase inactive, free or unmarked packs. The Mini App verifies signed Telegram identity; client-supplied user IDs and prices are not trusted. This is an internal no-charge checkout, not a Telegram Stars invoice or test of the Stars payment callback.

Existing APKs with the pack-service endpoint discover the new packs without an app update. In the game refresh available packs, then download; the 250-question packs are available automatically, extras only after their test checkout. No extra entitlements were pre-granted to the owner. Live SQL checks run inside a rolled-back transaction so the owner can test the real checkout from scratch.
