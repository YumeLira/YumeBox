<div align="center">

**English** | [简体中文](README_ZH_HANS.md)

<img src="logo.webp" style="width: 96px;" alt="logo">

## YumeBox

[![Latest release](https://img.shields.io/github/v/release/YumeYuka/YumeBox?label=Release&logo=github)](https://github.com/YumeYuka/YumeBox/releases/latest)
[![GitHub License](https://img.shields.io/github/license/YumeYuka/YumeBox?logo=gnu)](/LICENSE)
![Downloads](https://img.shields.io/github/downloads/YumeYuka/YumeBox/total)

**An open-source Android client based on the [mihomo](https://github.com/MetaCubeX/mihomo) kernel**

</div>

## Features

- Mihomo kernel
- SubStore Optional Support
- Web dashboard
- For more, please explore on your own...

## Compatibility

- Android 7.0 and above
- Supports `armeabi-v7a`, `arm64-v8a`, `x86`, and `x86_64` architectures

## Usage

- **Install**: Visit the [Releases](https://github.com/YumeYuka/YumeBox/releases) page
- **Build**: See the [Build section](#build)

## Discussion

- Telegram group: [@OOM_WG](https://t.me/OOM_Group)

## Contributing Translations

To translate YumeBox into your language or improve existing translations, please fork this project and create or update translation files in the `lang` directory.

## Build

1. **Sync core source code**

   ```bash
   sh scripts/sync-kernel.sh <alpha|meta|smart>
   ```

2. **Install dependencies**
   Ensure that OpenJDK 24, Android SDK, CMake, and Golang are installed.

3. **Create `local.properties` in the project root**

   ```properties
   sdk.dir=/path/to/android-sdk
   ```

4. **(Optional) Customize the package name by editing `gradle.properties`**

   ```properties
   project.namespace.base=plus.yumeyuka.yumebox
   project.namespace.core=${project.namespace.base}.core
   project.namespace.extension=${project.namespace.base}.extension
   project.namespace.buildlogic=${project.namespace.base}.buildlogic
   ```

5. **Create `signing.properties` in the project root**

   ```properties
   keystore.path=/path/to/keystore/file
   keystore.password=<key store password>
   key.alias=<key alias>
   key.password=<key password>
   ```

6. **Build the application**

   Ontology:

   ```bash
   ./gradlew app:assembleRelease
   ```

   SubStore Extension:

   ```bash
   ./gradlew extension:assembleRelease
   ```

## Acknowledgements

- [Mihomo](https://github.com/MetaCubeX/mihomo)
- [ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid)
- [SubStore](https://github.com/sub-store-org)
- [SubCase](https://github.com/sion-codin/SubCase)
- [Yacd-meta](https://github.com/MetaCubeX/Yacd-meta)
- [Zashboard](https://github.com/Zephyruso/zashboard)
