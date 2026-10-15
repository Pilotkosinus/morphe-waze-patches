# Contributing

Thanks for helping with the Morphe Waze patches.

## Contributors

* [Pilotkosinus](https://github.com/Pilotkosinus) – maintainer
* Claude (Anthropic) – patch development, co-author of the commits

## Reporting bugs

Use the [bug report template](https://github.com/Pilotkosinus/morphe-waze-patches/issues/new/choose) and add:

* the Waze version and the patches you applied,
* the Android version and device,
* a logcat if possible. The patches log with the tags `WazePatchedYTMusic`, `MorpheWazeSettings` and
  `MorpheWazeAssistant`:

  ```sh
  adb logcat -s WazePatchedYTMusic MorpheWazeSettings MorpheWazeAssistant AndroidRuntime
  ```

## Pull requests

* Make changes on the `dev` branch.
* Use [semantic commit](https://www.conventionalcommits.org) messages: `feat:` for new features,
  `fix:` for bug fixes, `chore:` for everything that should not create a release.
* Do not edit generated files (`patches-list.json`, `patches-bundle.json`, `CHANGELOG.md`, the patch list in the README).
* Build with `./gradlew buildAndroid` and test the patched Waze before opening a pull request.
* New patches that can be switched on and off: see "Adding a new patch to the settings" in the [README](README.md).
