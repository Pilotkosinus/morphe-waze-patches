# 🧩 Morphe Waze Patches

Morphe patches for [Waze](https://play.google.com/store/apps/details?id=com.waze).

## ❓ About

Waze can control YouTube Music from its built-in music player, but only if the app is installed
under the original package name `com.google.android.apps.youtube.music`.
A YouTube Music app patched with Morphe (`app.morphe.android.apps.youtube.music`) is reported as
"not installed".

**Waze – Patched YouTube Music Support** patches Waze, not YouTube Music, so Waze recognizes the patched
YouTube Music app and can control it. The original YouTube Music app and all other music apps keep working.

## 🚀 Add these patches to Morphe

### 👉 [Click here to add these patches to Morphe](https://morphe.software/add-source?github=Pilotkosinus/morphe-waze-patches)

Or add the source manually in Morphe Manager: `https://github.com/Pilotkosinus/morphe-waze-patches`

Then select Waze in Morphe Manager, apply the patches and install the patched Waze.

> [!IMPORTANT]
> * The patched Waze is signed with a different key, so the Play Store version must be uninstalled first.
> * **"Sign in with Google" does not work in a re-signed Waze.** Google binds the Waze OAuth client
>   to the original signing certificate (`UNREGISTERED_ON_API_CONSOLE`). Sign in with your email address instead.
> * If you install the patched YouTube Music after Waze, restart Waze once so it detects it.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0-dev.1](https://github.com/Pilotkosinus/morphe-waze-patches/releases/tag/v1.0.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;1 patches total
<details open>
<summary>📦 Waze&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 5.25.0.1 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Waze – Patched YouTube Music Support](#waze-patched-youtube-music-support) | Adds a repackaged YouTube Music app (for example patched with Morphe) to the audio partners of Waze, so it can be selected and controlled from the Waze music player. The original YouTube Music app keeps working. | • Package names |

</details>

<!-- PATCHES_END -->

## 🔍 How it works

Waze 5.25.0.1, class `com.waze.sdk.SdkConfiguration`:

1. `updateAppList()` loads the audio partner allowlist from the Waze server
   (`NativeManager.configGetSdkConfigurationNTV()`), keyed by package name. It contains
   `com.google.android.apps.youtube.music` (scope `0x200007`), but not repackaged builds.
2. When the music app binds to `com.waze.sdk.SdkService`, Waze identifies the caller with
   `PackageManager.getNameForUid(Binder.getCallingUid())` and rejects packages that are not on the list.

Waze does not check the partner app's signature, and YouTube Music does not check Waze's signature.
The `com.waze.sdk.audio.ACTION_INIT` intent is already declared in Waze's `<queries>`.

The patch changes two methods:

| Method | Change |
|---|---|
| `SdkConfiguration.updateAppList()` | After the server list is loaded, adds every installed repackaged YouTube Music with the name and scope of the original YouTube Music entry. |
| `SdkConfiguration.getCustomizedAppIconUrl(Resources, String)` | Maps the patched package name to the original one, because the server only hosts the original icon. |

Patched YouTube Music apps are detected by their `MusicWazeBroadcastReceiver`, whatever the package name.
The `packageNames` patch option can add more package names (default `app.morphe.android.apps.youtube.music`).

## 🧪 Testing status

Tested on an Android 16 emulator with Waze 5.25.0.1 and YouTube Music 9.20.53 (Morphe):

* ✅ The patched YouTube Music is added to the Waze partner list (logcat tag `WazePatchedYTMusic`).
* ✅ SDK handshake works: YouTube Music binds to Waze's `SdkService`, and Waze connects to YouTube Music's
  `MusicBrowserService`. The unpatched Waze rejects the same handshake.
* ⚠️ Selecting the app in the Waze UI and play/pause/skip were not tested yet. Waze login does not work on
  the emulator (Play Integrity), not even with the unpatched app. Reports from real devices are welcome.

## 🧑‍💻 Development

```sh
./gradlew buildAndroid
# patches/build/libs/patches-*.mpp
```

Apply the `.mpp` with [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop), for example:

```sh
java -jar morphe-desktop-all.jar patch --patches=patches/build/libs/patches-1.0.0.mpp waze.apks
```

`waze.apks` is a zip of the Waze split APKs (`adb shell pm path com.waze`).

Building needs a GitHub token for the Morphe Gradle plugin on GitHub Packages
(`gpr.user`/`gpr.key` in `~/.gradle/gradle.properties`, or `GITHUB_ACTOR`/`GITHUB_TOKEN`).

### Updating for a new Waze version

1. Decompile `com.waze.sdk.SdkConfiguration` with jadx.
2. Check that `updateAppList()` still calls `configGetSdkConfigurationNTV()` followed by `move-result-object`,
   and that `getCustomizedAppIconUrl(Resources, String)` still exists. These names are not obfuscated.
3. Check that the SDK service still authorizes by package name (search for `isWhiteListed`).
4. Add the version as `AppTarget` in `Constants.kt`.

Releases are created by [release.yml](.github/workflows/release.yml) for `feat:`/`fix:` commits on `main` (stable) and `dev` (pre-release).

## 📜 License

These patches are licensed under the [GNU General Public License v3.0](LICENSE).
