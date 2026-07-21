# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

```bash
./gradlew assembleGoogleDebug          # build one flavor
./gradlew assembleDebug                # all debug flavors
./gradlew connectedGoogleDebugAndroidTest  # run instrumentation tests
./gradlew lint
```

Build scripts are Groovy DSL (`build.gradle`), not Kotlin DSL.

## Architecture

Juick microblogging Android client. Single-activity (MainActivity) + Navigation Component. minSdk 24, targetSdk 37, Java 17, Kotlin 2.4.10.

**Flavors** (`notifications` dimension): `google` (FCM), `huawei` (HMS Push, hides NSFW), `free` (SSE polling).

**Build types**: `debug`, `release`, `next` (Compose UI enabled), `store` (updater disabled).

**Key files**:
- `App.kt` — Application subclass. OkHttp client with auth interceptor (`Authorization: Juick <token>`), Retrofit singleton. `App.instance` accessed everywhere.
- `api/Api.kt` — Retrofit interface for all Juick API calls. Models in `api/model/` use kotlinx.serialization.
- `android/MainActivity.kt` — Single activity, bottom nav, handles deep links (juick.com URLs), new event intents, ACTION_SEND shares.
- `android/service/AuthenticationService.kt` — Android AccountManager authenticator (runs in `:auth` process). Auth token = account "hash" stored via AccountManager.
- `android/Account.kt` — ViewModel for current user profile. `App.isAuthenticated` / `App.accountData` are extension properties from `AccountHelpers.kt`.
- `android/screens/FeedFragment.kt` + `FeedViewModel.kt` — Reusable paginated feed. Other screens (Home, Discover, Blog, Search, Discussions) extend FeedFragment. ViewModel uses `apiUrl: StateFlow<Uri>` to reactively fetch.
- `android/NotificationSender.kt` — Local notifications from push data. Silenced when app in foreground.
- `android/updater/Updater.kt` — Checks GitHub Releases for APK updates, matches by flavor name.
- `android/LinkPreviewer.kt` — Plugin interface. Google flavor registers `YouTubePreviewer`.

**Auth flow**: `SignInActivity` (nick+pass or Google via `SignInProvider` interface) → `GoogleSignInProvider` in google flavor uses Credential Manager API → `SignUpActivity` for new Google accounts.

**Compose**: Only in `next` build type. `NextSignInActivity` in `src/next/`. Dependencies: Compose BOM, Material 3.

**Flavor-specific code** lives in `src/<flavor>/java/` and `src/<buildType>/`. Each flavor has its own `NotificationManager` and `JuickConfig`.

**Navigation**: `src/main/res/navigation/navigation.xml`. Start: `home`. Key destinations: `discover`, `chats`, `PMFragment` (chat), `thread` (dialog), `blog`, `search`, `tags` (dialog), `discussions`, `new_post`.
