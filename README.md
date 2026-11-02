# Bíblia Wear OS

A simple, offline Bible app for **Wear OS 3 or later**, with a European Portuguese
interface and the **Almeida Revista e Corrigida (Portugal)** Bible text.

## Features

- 66 books, with full chapters and numbered verses.
- Verse of the day, favourites and the last passage saved on the watch.
- Adjustable text size, rotary scrolling and animated page navigation.
- No ads, accounts or Internet connection required.

## Install

Download the APK from [Releases](https://github.com/ponces/biblia-wear-os/releases)
and install it on your watch.

## Build

Requires JDK 17 or 21 and Android SDK 36. Set `ANDROID_HOME` and the signing
variables `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`
and `ANDROID_KEY_PASSWORD`, then run:

```sh
./gradlew :app:assembleRelease
```

The APK is generated at `app/build/outputs/apk/release/app-release.apk`.

## License

App code: GPL-3.0-or-later. Corresponding source is available in this repository
and each release's automatic source downloads.

Bible text: **Copyright © 2001 Sociedade Bíblica de Portugal**, obtained from
[mrk214/snapshots](https://github.com/mrk214/snapshots/blob/f8f3ceeb66859c19e86bcb1f6d8de8c8975ba7e4/pt___por___por_pt/ARC_vid_215.json).
The Bible text is not covered by the app's GPL license. Its online availability
does not establish redistribution permission; confirm authorization before public
distribution. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
