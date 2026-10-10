# Giwu Bible — Android (Kotlin)

A native Android recreation of the Flutter app in [`../flutter/fn.giwu`](../flutter/fn.giwu):
a multi-version Bible reader that works offline, with the same Laravel JSON API
at `/api` as its only backend.

Open this folder (`kotlin/`) directly in Android Studio.

| | |
|---|---|
| Language | Kotlin, Jetpack Compose (Material 3) |
| Min / target SDK | 21 / 36 |
| Application ID | `com.giwu.bible.kt` — distinct from the Flutter build's `com.giwu.bible`, so both install side by side |
| HTTP | OkHttp + kotlinx.serialization |
| Storage | `SQLiteOpenHelper` (bible text), SharedPreferences (reader settings), `EncryptedSharedPreferences` (auth token) |
| State | `StateFlow` + `ViewModel`, hand-rolled DI in [`AppContainer`](app/src/main/java/com/giwu/bible/AppContainer.kt) |
| Read aloud | Android's own `TextToSpeech` |

## Build and run

```bash
./gradlew :app:installDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

```bash
./gradlew :app:assembleRelease
```

`local.properties` needs `sdk.dir` pointing at the Android SDK (Android Studio
writes this automatically).

**Signing.** The release build is signed with the local debug keystore,
matching the Flutter build's `signingConfig`. That is fine for sideloading and
testing, but a debug-signed APK cannot go to Play, and the keystore differs per
machine — a build from another machine will not install over it. Replace
`signingConfig` in [`app/build.gradle.kts`](app/build.gradle.kts) with a real
release key before publishing anywhere.

A built APK is kept out of git (`*.apk` in `.gitignore`); `dist/` is where this
project's exported builds land.

## How it maps to the Flutter app

| Flutter | Kotlin |
|---|---|
| `lib/models/*.dart` | [`model/Models.kt`](app/src/main/java/com/giwu/bible/model/Models.kt) |
| `lib/data/bible_database.dart` | [`data/BibleDatabase.kt`](app/src/main/java/com/giwu/bible/data/BibleDatabase.kt) |
| `lib/data/bible_versions.dart`, `book_names.dart`, `chapter_counts.dart` | [`data/BibleSeed.kt`](app/src/main/java/com/giwu/bible/data/BibleSeed.kt) |
| `lib/api/client.dart` | [`data/remote/ApiClient.kt`](app/src/main/java/com/giwu/bible/data/remote/ApiClient.kt) |
| `lib/api/{bibles,books,chapter,verse,download}.dart` | [`data/remote/BibleApi.kt`](app/src/main/java/com/giwu/bible/data/remote/BibleApi.kt) |
| `lib/api/{auth,bookmarks}.dart` | [`data/remote/AccountApi.kt`](app/src/main/java/com/giwu/bible/data/remote/AccountApi.kt) |
| `providers/prefs_provider.dart` | [`data/AppPrefs.kt`](app/src/main/java/com/giwu/bible/data/AppPrefs.kt) |
| `providers/{bibles,chapter,comparison}_provider.dart` | [`repo/BibleRepository.kt`](app/src/main/java/com/giwu/bible/repo/BibleRepository.kt) |
| `providers/auth_provider.dart` | [`repo/AuthStore.kt`](app/src/main/java/com/giwu/bible/repo/AuthStore.kt) |
| `providers/bookmarks_provider.dart` | [`repo/BookmarkStore.kt`](app/src/main/java/com/giwu/bible/repo/BookmarkStore.kt) |
| `providers/server_url_provider.dart` | [`repo/ServerSettings.kt`](app/src/main/java/com/giwu/bible/repo/ServerSettings.kt) |
| `providers/tts_provider.dart` | [`tts/TtsController.kt`](app/src/main/java/com/giwu/bible/tts/TtsController.kt) |
| `services/tts_*.dart`, `voice_model*.dart` | [`tts/AndroidTtsEngine.kt`](app/src/main/java/com/giwu/bible/tts/AndroidTtsEngine.kt) |
| `pages/read_page.dart` + `widgets/*` | [`ui/reader/`](app/src/main/java/com/giwu/bible/ui/reader) |
| `pages/welcome_page.dart` | [`ui/welcome/`](app/src/main/java/com/giwu/bible/ui/welcome) |
| `pages/{settings,login,bookmarks}_page.dart` | [`ui/settings/`](app/src/main/java/com/giwu/bible/ui/settings), [`ui/auth/`](app/src/main/java/com/giwu/bible/ui/auth), [`ui/bookmarks/`](app/src/main/java/com/giwu/bible/ui/bookmarks) |

Behaviour kept as-is:

- Local-first reads. A downloaded translation answers from SQLite; the API is
  the fallback; the bundled seed lists are the last resort, so the book list
  and translation picker are never empty offline.
- The same database schema as the server's `bible-sqlite.db`, so an uploaded
  SQLite file can be imported as-is.
- The same preference keys (`giwu_dark`, `giwu_bible`, `giwu_book`, …) and the
  same server URL row in `app_settings`.
- Tap a verse to compare it; one translation per card, every ticked
  translation listed. An empty selection means "all of them".
- Bookmarks require an account (Sanctum token); reading does not.
- Read-aloud: per-verse playback with highlight and auto-scroll, speed
  presets, optional verse-number announcements, and continue-into-the-next-chapter.

## Deliberate differences

- **Read aloud uses Android's `TextToSpeech`** instead of the Flutter build's
  downloaded sherpa-onnx Piper voice. The device engine is offline too, so the
  63 MB voice download, the `.tar.bz2` extraction and the voice-management UI
  are gone. When a device has no voice data, the reader is offered the system
  installer (`ACTION_INSTALL_TTS_DATA`); when it has no engine at all, the
  read-aloud button is hidden. `TtsEngine` stays an interface, so another
  synthesizer can be dropped in without touching the playback state machine.
- **Android only.** The Flutter build also targets Windows desktop; the
  three-column layout here is driven by width, so it still works on a tablet
  or a foldable.
- **The auth token is in `EncryptedSharedPreferences`**, not in plain
  preferences, with a fallback to a private preference file on devices whose
  keystore refuses.
- **Share works.** The share button in the bottom bar was present but disabled
  in the Flutter build; here it sends the verse text and reference through
  `ACTION_SEND`.
- **Cleartext HTTP is restricted** to development hosts (`localhost`,
  `10.0.2.2`, `*.test`) in
  [`network_security_config.xml`](app/src/main/res/xml/network_security_config.xml).
  A custom server URL over plain HTTP to any other host will be blocked.
- **A translation download retries a dropped connection** (three attempts,
  backing off). The payload is ~5 MB and a mobile link drops often enough
  mid-transfer that one failure should not send the reader back to setup
  empty-handed. A response the server actually sent — a 404, a 500 — is not
  retried.

## Verified

Built and run against the live `https://giwu.co.za/api` server on a Pixel 4
emulator (API 35):

- Setup screen lists all seven translations offline from the bundled seed,
  then overlays the server's catalogue.
- Downloading KJV fetches the 5 MB payload and writes 31,102 verses into
  `giwu_bible.db` (5.2 MB on device), then drops straight into the reader.
- The reader shows Genesis 1 from the local database; tapping a verse
  highlights it and opens the comparison sheet.
- All five API response shapes (`/bibles`, `/books`, `/chapter`, `/verse`,
  `/bibles/{table}/download`) parse as-is, including the `"t"`-keyed quirk in
  `/books` and the quoted integers some PHP/SQLite builds emit.
- `./gradlew :app:testDebugUnitTest` — 54 tests, all passing.
- `./gradlew :app:lintDebug` — no errors.
- The R8-minified release APK (1.9 MB) was installed on the same emulator and
  ran the same download-and-read flow. That is the check that matters after
  minification: kotlinx.serialization decodes the download payload reflectively
  enough that a missing keep rule would only show up at runtime.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

JVM unit tests, no device needed, covering the parts where correctness is not
obvious by reading: the read-aloud state machine (`TtsControllerTest`), verse
text preparation, the seed data, the comparison-panel selection rule, and the
tolerant JSON readers.
