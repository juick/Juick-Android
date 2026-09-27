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

Juick microblogging Android client. Single activity (MainActivity) with Jetpack Compose UI and Navigation Compose. minSdk 24, targetSdk 37, Java 17, Kotlin 2.4.20.

**Flavors** (`notifications` dimension): `google` (FCM), `huawei` (HMS Push, hides NSFW), `free` (SSE polling).

**Build types**: `debug`, `release`, `store` (updater disabled).

**Key files**:
- `App.kt` — Application subclass. OkHttp client with auth interceptor (`Authorization: Juick <token>`), Retrofit singleton, Coil image loader. `App.instance` accessed everywhere. `App.instance.messages` is a StateFlow of posts received via push/SSE; screens observe it for real-time updates.
- `api/Api.kt` — Retrofit interface for all Juick API calls. Models in `api/model/` use kotlinx.serialization.
- `android/MainActivity.kt` — Single `ComponentActivity`, hosts `AppNavigation`, handles deep links (juick.com URLs), new event intents, ACTION_SEND shares, Custom Tabs.
- `android/service/AuthenticationService.kt` — Android AccountManager authenticator (runs in `:auth` process). Auth token = account "hash" stored via AccountManager.
- `android/Account.kt` — ViewModel for current user profile. `App.isAuthenticated` / `App.accountData` are extension properties from `AccountHelpers.kt`.
- `android/ui/navigation/Routes.kt` + `AppNavigation.kt` — Type-safe `@Serializable` routes and the single top-level NavHost. Start: `Route.Home` (authenticated) or `Route.Public`. `Thread` and `Tags` are dialog destinations.
- `android/ui/AppScaffold.kt` — Per-screen scaffold: top bar (search, discussions badge, avatar), bottom nav, FAB.
- `android/ui/screens/feed/FeedScreen.kt` — Reusable paginated feed (Home, Discover, Blog, Search, Discussions). `PostCard.kt` renders a post; `MessageFormatter.kt` turns post entities into annotated text blocks.
- `android/ui/screens/` — `thread`, `chat`, `chats`, `post` (new post), `tags`, `search`, `noauth`. Screens load data directly via `App.instance.api` in `LaunchedEffect`, no ViewModels.
- `android/NotificationSender.kt` — Local notifications from push data. Silenced when app in foreground.
- `android/updater/Updater.kt` — Checks GitHub Releases for APK updates, matches by flavor name.
- `android/LinkPreviewer.kt` — Plugin interface. Google flavor registers `YouTubePreviewer`.

**Auth flow**: `SignInActivity` (Compose `SignInScreen`; nick+pass or Google via `SignInProvider` interface) → `GoogleSignInProvider` in google flavor uses Credential Manager API → `SignUpActivity` for new Google accounts.

**Flavor-specific code** lives in `src/<flavor>/java/` and `src/<buildType>/`. Each flavor has its own `NotificationManager` and `JuickConfig`.
