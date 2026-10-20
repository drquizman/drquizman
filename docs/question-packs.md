# Question packs in the Android game

Implemented in the game: localized question picker (ru/en/de/es/he), persistent per-language base checkbox, persistent per-pack checkboxes, validated atomic local installation, purchased-pack download queue, and a device-link API client. One file is one purchasable pack. Newly installed packs default to selected; existing selections survive restarts and language switches. Only packs in the current language enter a game.

Base exclusion requires **more than 8 distinct nonblank topics AND more than 1000 distinct questions in the selected extra packs**. Removing selections below this threshold re-enables the base. Duplicate content does not inflate this threshold. The base row never shows counts. Without the base, true/false rounds derive questions and proposed answers from the selected extra content rather than using the built-in true/false bank.

## Service integration still required

The initial Supabase backend for @DrQuizman_bot is prepared under `supabase/`, with deployment instructions in `supabase/README.md`. Deployment and live integration checks are pending project access and secret configuration. The app does not invent a link code or show fake purchases. Until configured, the link action explains that the service is unavailable; local selection works offline.

Build with `-PpackServiceUrl=https://YOUR-SERVICE` once the backend below exists. No bot secret belongs in the APK. This is a proposed service contract, implemented by the Android client, not a deployed API.

- `POST /v1/device-links` → `{code, pollToken, expiresAt}`. `expiresAt` is Unix seconds, within 15 minutes. Generate the user code and an independent high-entropy poll credential on the server. Rate-limit creation and bot attempts. The bot authenticates the Telegram user and explicitly approves linking the installation with that code. Expire and consume codes once; never derive credentials from user IDs or codes.
- `GET /v1/device-links/status`, bearer pollToken → `{status:"pending"}` or `{status:"approved",deviceToken}`. The app polls every 3 seconds until expiry or screen closure. The device token is a server-issued installation credential, stored in private app preferences. It must be revocable by the backend.
- `GET /v1/packs?language=ru`, bearer deviceToken → `{packs:[{id,title,language,count,topics,sha256}]}`. Return only owned packs for this account and language. IDs must be unique per file/language, match `[a-zA-Z0-9_-]{1,100}`, and must not be `base`.
- `GET /v1/packs/{id}/download`, bearer deviceToken → the original JSON bytes. Recheck entitlement server-side. Serve through this HTTPS endpoint, without redirects. SHA-256 must cover these exact bytes.

Files are limited to 10 MiB. Installation checks checksum, unique question IDs, count, topics, four distinct nonblank answers, correct index 0, and nonblank question/topic text. Files are committed atomically; a failed download does not remove existing packs. Failed entries remain available for retry. Already downloaded packs can be used offline.

Before production: implement and integration-test the backend and Telegram approval flow, configure the endpoint, and validate installation, account revocation, expired tokens and actual network interruptions on a device. Current core checks cover selection thresholds, duplicates, automatic base fallback, extra-only rounds, and remapped IDs.
