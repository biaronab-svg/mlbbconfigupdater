# Hero Config Manager — Android

Native Android application built with **Kotlin**, **XML layouts**, and **Gradle Kotlin DSL**.

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK 26+ (Android 8.0 minimum)
- A working internet connection (GitHub API)

## Open the project

1. Open **Android Studio**
2. Choose **Open** and select this `android/` folder
3. Wait for Gradle sync to complete
4. Run on a device or emulator (API 26+)

## Project structure

```
android/
├── build.gradle.kts               ← root Gradle Kotlin DSL
├── settings.gradle.kts            ← module includes
├── gradle/libs.versions.toml      ← version catalog
└── app/
    ├── build.gradle.kts           ← app module Kotlin DSL
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/heroconfigmanager/
        │   ├── HeroConfigApp.kt
        │   ├── data/
        │   │   ├── model/Models.kt          ← Kotlin data classes
        │   │   ├── remote/GitHubApiService.kt
        │   │   ├── remote/NetworkClient.kt
        │   │   └── repository/ConfigRepository.kt
        │   └── ui/
        │       ├── SharedViewModel.kt
        │       ├── main/MainActivity.kt
        │       ├── heroes/HeroesFragment.kt
        │       ├── heroes/HeroListFragment.kt
        │       ├── heroes/HeroAdapter.kt
        │       ├── hero/HeroEditorActivity.kt
        │       ├── hero/HeroEditorViewModel.kt
        │       ├── hero/BasicInfoFragment.kt
        │       ├── hero/SkinsFragment.kt
        │       ├── hero/UpgradeSkinsFragment.kt
        │       └── config/ConfigFragment.kt
        └── res/
            ├── layout/            ← all XML layouts
            ├── navigation/nav_graph.xml
            ├── menu/
            ├── drawable/          ← vector icons + shapes
            └── values/            ← strings, colors, themes
```

## Features

| Feature | Details |
|---|---|
| Role tabs | Scrollable TabLayout + ViewPager2, one tab per role |
| Hero cards | Splash image, logo, name, title, skin/upgrade counts |
| Pull-to-refresh | SwipeRefreshLayout triggers GitHub fetch |
| Hero editor | 3-tab Activity: Basic Info / Skins / Upgrade Skins |
| Skin dialog | MaterialAlertDialog with all skin fields |
| Config screen | Version + note editor, role distribution bars, GitHub push |
| GitHub sync | Retrofit + OkHttp, Base64 encode/decode, SHA caching |

## Build and sign

To generate a release APK:

```bash
./gradlew assembleRelease
```

The unsigned APK will be at `app/build/outputs/apk/release/app-release-unsigned.apk`.
