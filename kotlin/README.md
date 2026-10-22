# Giwu Bible — Android (Kotlin)

A native Android recreation of the Flutter app in [`../flutter/fn.giwu`](../flutter/fn.giwu):
a multi-version Bible reader that works offline, with the same Laravel JSON API
at `/api` as its only backend.

Open this folder (`kotlin/`) directly in Android Studio.

| | |
|---|---|
| Language | Kotlin, Jetpack Compose (Material 3) |
| Min / target SDK | 21 / 36 |
| Application ID | `com.giwu.bible` — the same identity the Flutter build shipped under, so this replaces it on a device |
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

## Signing

Android treats a new APK as an *update* to whatever is installed under the
same application ID, and refuses it outright when the signing key differs
(`INSTALL_FAILED_UPDATE_INCOMPATIBLE`, shown to the reader as "App not
installed"). Since the app also updates itself from GitHub Releases, **every
published APK has to carry the same key** — a debug keystore cannot do this,
because it is per machine and a CI runner makes a fresh one each run.

So the release build reads its keystore from four environment variables, never
from the repository and never from `gradle.properties`:

| Variable | Holds |
|---|---|
| `GIWU_KEYSTORE_FILE` | Path to the `.jks` |
| `GIWU_KEYSTORE_PASSWORD` | Keystore password |
| `GIWU_KEY_ALIAS` | Key alias inside it |
| `GIWU_KEY_PASSWORD` | Key password |

With all four set and the file present, `assembleRelease` signs with that key.
With any of them missing it falls back to debug keys, so you can still build a
release locally — *unless* you pass `-PgiwuRequireSigning=true`, which turns
the fallback into a build failure. CI always passes it.

### Creating the keystore, once

```bash
keytool -genkeypair -v -keystore giwu-release.jks -alias giwu -keyalg RSA -keysize 4096 -validity 10000
```

Then base64 it for GitHub:

```bash
base64 -w0 giwu-release.jks > giwu-release.jks.base64
```

Add four repository secrets under **Settings → Secrets and variables →
Actions**: `ANDROID_KEYSTORE_BASE64` (the contents of that file),
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`.

> **Keep a backup of `giwu-release.jks` somewhere other than GitHub.** Lose it
> and no future build can ever update an installed app — every reader has to
> uninstall and lose their downloaded translations. The 10000-day validity is
> deliberate for the same reason: an expired key has the same effect.

The first release signed this way cannot update an app installed from an
*earlier* debug-signed APK, including anything built from the Flutter project.
Those installs need removing by hand once; everything after that updates in
place.

### What CI does

The `release` job writes the keystore from `ANDROID_KEYSTORE_BASE64`, builds
with `-PgiwuRequireSigning=true`, then reads the certificate back out of the
finished APK with `keytool` and fails if it carries `CN=Android Debug`. The
key is deleted from the runner whether or not the build succeeded, and the
job summary prints the SHA-256 fingerprint — it must read the same on every
release.

A built APK is kept out of git (`*.apk` in `.gitignore`); `dist/` is where this
project's exported builds land.

**CI.** The `kotlin` job in [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)
runs the unit tests and lint on every push and pull request, and the `release`
job builds this project's APK and attaches it to the staging pre-release. The
version is stamped from the Flutter app's `pubspec.yaml` — one version for the
APK, the git tag and the Windows installer — through
`-PgiwuVersionName` and `-PgiwuVersionCode`:

```bash
./gradlew :app:assembleRelease -PgiwuVersionName=1.0.0 -PgiwuVersionCode=42
```

## In-app updates

The app is sideloaded, so no store will tell a reader that a newer build
exists. [`update/`](app/src/main/java/com/giwu/bible/update) does it instead:

- **When.** `MainActivity` calls `AppUpdates.checkOnLaunch()` on every launch,
  and the updater drops the check unless a day has passed since the last one
  that reached GitHub — so "daily" needs no scheduled job, no notification
  channel and no wakeup for someone who is not reading. Settings → ABOUT →
  *Check* forces one, and a switch there turns the automatic check off.
- **Where from.** `GithubReleaseSource` reads
  `api.github.com/repos/thabang-teddy/giwubible/releases` directly, not through
  the Giwu API, so the check still works when the reader has pointed the app at
  another server. `/releases` rather than `/releases/latest`, because CI
  publishes every build as a *pre-release* and `latest` skips those. The newest
  release carrying an `.apk` asset wins.
- **What counts as newer.** `ReleaseVersion` parses the CI tag
  `v<version>-<channel>.<run number>` and compares it with the installed
  `versionName` / `versionCode`. The core version decides first; only when the
  cores match does the run number — which *is* the `versionCode` CI stamps —
  break the tie.
- **Installing.** `AndroidApkInstaller` hands the download to the system
  `DownloadManager` (so it survives leaving the app), writes it to this app's
  external files directory, then opens the package installer through the
  `FileProvider` declared in the manifest. The reader has to allow *install
  unknown apps* once; the prompt walks them to that screen and back.
- **Declining.** *Skip this version* stores the tag, and the daily check stays
  quiet about it. An explicit *Check* still reports it.

This only works while every release carries the same signing key — see
[Signing](#signing). The first update onto a device still holding an older
debug-signed build will fail with "App not installed"; that one install has to
be removed by hand.

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
