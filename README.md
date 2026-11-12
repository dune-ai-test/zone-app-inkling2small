# Zone — Android

A native Android port of the **Zone** personal desk — the everyday tools (diary, lists, habits, notes, money, passbook, shared-expense splitting, mileage, watchlist, links, subscriptions) in one quiet, iOS-style app with generous white space, a light-first theme, and full dark-mode support.

Built with **Kotlin** and **Jetpack Compose (Material 3)**. No account, no analytics — everything is stored in an on-device private workspace, mirroring the `zone.v1.*` local-first design of the web app.

> Web original: [`dune-ai-test/zone`](https://github.com/dune-ai-test/zone) (static HTML/JS on Cloudflare Pages + D1). This repository is the Android companion.

## Download

Every push to `main` builds the app in GitHub Actions and attaches the **debug APK** directly to a pre-release on this repository's [Releases page](../../releases) — tagged `v1.0.<run>-pre`. Grab `app-debug.apk` and sideload it (Android 8.0+).

The workflow intentionally skips artifact storage and uploads straight to the release page (`prerelease: true`). Debug builds are signed with the Android debug key.

## What's inside

| Desk | What it does |
| --- | --- |
| **Overview** | Live clock, month-spend vs budget, diary streak, open list items, average mileage, and a filterable `All / Personal / Money / Library / System` desk of twelve tools |
| **Diary** | Entries with mood, threads (tags), streaks, word counts, month-grouped timeline, CSV export |
| **Lists** | Nested checklists — lists → items → steps, progress, templates, search, reorder, copy-as-text |
| **Habits** | Monday-start weekly board, streaks, 56-day heat grid, 8-week pulse, CSV export |
| **Notes** | Colour-paper notes, pin/sort/search, reading time, Markdown export |
| **Money** | Monthly ledger with envelope budget, 6-month pulse chart, top categories, CSV export |
| **Passbook** | Multi-account statements (Bank/Savings/Wallet/Card/Cash), running balance, CSV export |
| **Split** | Group expense tables — equal splits, greedy minimum-cash-flow settle-up, record payments, CSV export |
| **Mileage** | Fill-up log with odometer chaining, fleet/vehicle km/l, cost per km, CSV export |
| **Watchlist** | Film/Series queue with episode progress, ratings, favourites, tonight's pick, CSV export |
| **Links** | Bookmark shelf with tags, favourites, spotlight shuffle, tag filters, CSV export |
| **Subscriptions** | Normalised monthly burn (Monthly / Yearly × 1/12 / Weekly × 52/12), renewals radar, pause/resume |
| **Settings** | Light / Dark / System appearance, display currency (INR, USD, EUR, GBP, AED, AUD, CAD, SGD) |
| **Admin** | On-device back office: record register per tool, full activity audit trail, JSON workspace export/restore, reset |

## Build (CI only — nothing builds locally)

There is deliberately **no local build requirement**. Everything is compiled in GitHub Actions:

```yaml
./gradlew :app:assembleDebug   # in the workflow, on ubuntu-latest
```

The release step publishes `app/build/outputs/apk/debug/app-debug.apk` to a GitHub pre-release via `softprops/action-gh-release` — no artifact upload, no signing secrets required.

### Project layout

```
.github/workflows/build-release.yml   # CI: assembleDebug → pre-release with APK
app/
  src/main/java/com/zone/app/
    MainActivity.kt / ZoneApplication.kt
    data/       # models + ZoneStore (local-first JSON workspace, atomic writes)
    util/       # dates, currency, settle-up math, share/CSV helpers
    ui/
      theme/    # iOS-style light/dark palette, type scale, shapes
      components/  # cards, segmented controls, dialogs, large titles
      navigation/  # the twelve-tool desk registry
      screens/     # Overview + 13 tools
```

## Data & privacy

All records live in a single atomic JSON workspace on-device (`zone.store.json`), mirroring the web app's `zone.v1.*` localStorage model. The Settings → Admin page exports the whole workspace as a JSON backup and can restore it later. Nothing is sent anywhere — there is no backend in this build.

## Tech

- Kotlin 2.0 · Jetpack Compose (BOM 2024.12.01) · Material 3
- Android Gradle Plugin 8.7.3 · Gradle 8.11.1 · JDK 17 · `minSdk 26` / `targetSdk 35`
- `navigation-compose`, `kotlinx-serialization-json`, `material-icons-extended`
- No network permission; icons are bundled system symbols.
