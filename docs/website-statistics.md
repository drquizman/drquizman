# Website counters

Published 2026-09-27 at https://www.drquizman.ru/ from `drquizman/drquizman`, commits `b9fce14` and `31f74b1` (versioned stylesheet and decorative image hit testing).
The local website checkout is `website/`; the existing Android project is separate.
Layout revision `4c9b713` restores the original desktop hero proportions and places counters in the footer. Phone and tablet layouts keep the host clear of the text and download button.

## Meaning

- Visits start from installation of migration `202609270002_website_statistics.sql`. The browser stores a random UUID in localStorage; the server retains only a keyed hash, and counts another visit after 30 minutes without a page load. Repeated page loads do not add visits during that period. Clearing browser storage or using another browser creates a new session. This is a visit count, not a unique-person count.
- Downloads accumulate the highest observed GitHub `download_count` per unique APK asset ID. Replaced/deleted assets remain in the private history table, so a replacement no longer resets the total. They are not button clicks or confirmed installations. The server caches the number for 15 minutes. GitHub request errors keep the last verified count.
- Missing statistics appear as a dash; original download links work independently of statistics. Local previews do not register visits. Browser storage restrictions may prevent visit recording.

## Backend

Existing Supabase project `xoerzqgvcpjrbuwtbtdo`, separate function `website-stats`, with no changes to the `quizman` function or pack access.
The public API exposes only totals. Database tables and the visit RPC are accessible to service_role only; the key is never sent to the browser or GitHub. Allowed site origins can POST a UUID, and GET reads totals. Origin checks are a browser restriction, not authentication; public counts cannot be made fraud-proof.
Raw IPs are not persisted. A keyed daily IP hash limits new session IDs per address to 30 per hour; inactive session hashes older than one day are cleaned up on a visit. The aggregate persists.

## Validation

`node tools/check-website-stats.mjs` verifies origin restrictions, input validation, APK totals, cache handling, and failures.
`npx --yes supabase@2.117.0 db query --linked --file tools/check-website-stats.sql` checks database permissions and deduplication inside a rolled-back transaction.
`npx --yes supabase@2.117.0 db query --linked --file tools/check-download-history.sql` checks repeated snapshots, replacement IDs, stale lower counts, removed assets, and private history permissions inside a rolled-back transaction.

## Download history repair (2026-10-03)

The v2.0 APK was replaced at 12:03 UTC with asset `607745371`. GitHub then reported 1 download for this asset and 2 for the v1.0 asset `576912158`. The original implementation summed only surviving files and therefore dropped to 3.
Migration `202610030001_download_history.sql` restores a separately identified legacy lower bound of 2 downloads for the deleted v2.0 asset, verified by the API check on 2026-09-28. The first live sync after the repair returned 5 (2 legacy + 2 v1.0 + 1 new v2.0). This is the confirmed minimum, not an invented estimate of its final deleted total.
History sync is transactional and serialized; repeated/stale snapshots cannot double-count or decrease totals. Downloads occurring after the last poll but before an asset is deleted cannot be recovered from GitHub: preserve old assets/releases where possible. The 15-minute cache remains unchanged.

Deploy with `npx --yes supabase@2.117.0 db push` and `npx --yes supabase@2.117.0 functions deploy website-stats --use-api` after reviewing pending migrations.
GitHub authorization was performed in the browser; it does not automatically authorize CLI push.
