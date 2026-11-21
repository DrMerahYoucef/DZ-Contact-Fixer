# DZ Contact Fixer

**Fix Caller ID matching on car Bluetooth head units and Android dialers by pairing international Algerian mobile numbers (`+213 5/6/7...`) with their national (`05/06/07...`) equivalents.**

![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-006233?style=flat-square)
![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-10B981?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

---

## Why DZ Contact Fixer?

In Algeria, mobile carriers (**Mobilis `06`**, **Djezzy `07`**, and **Ooredoo `05`**) deliver incoming voice Caller ID in the **10-digit national format** (`05XXXXXXXX`, `06XXXXXXXX`, `07XXXXXXXX`), while SMS, WhatsApp, and international routing rely on the **international format** (`+2135XXXXXXXX`, `+2136XXXXXXXX`, `+2137XXXXXXXX`).

Many automotive Bluetooth PBAP head units perform strict character-by-character string matching against the synced phonebook. As a result:
- Contacts stored only as `+213...` appear as raw unknown numbers (`05...`, `06...`, `07...`) on your car dashboard when someone calls.
- Contacts stored only as `0X...` can fail international dialing or messaging lookups.

**DZ Contact Fixer** solves this locally on your device by ensuring every Algerian mobile number in your address book has both clean, unspaced formats paired under the same contact—without touching foreign numbers, landlines, or special codes.

---

## Key Features

- **Targeted Algerian Mobile Scope**
  - Strictly processes Algerian mobile numbers (`+213[567]\d{8}` ↔ `0[567]\d{8}`) regardless of spaces, dashes, or parentheses.
  - Leaves Algerian landlines (`02x`, `03x`, `04x`, `+2132x`...), foreign numbers (`+33`, `+90`, `+41`, `+34`, etc.), and short/special codes completely untouched.
- **Non-Destructive `"DZ Local"` Pairing**
  - Adds missing national `0X` numbers under a custom **`DZ Local`** label alongside your existing `+213` `Mobile` entries rather than overwriting your primary numbers.
  - Supports two normalization modes:
    - **Clean & Normalize (Default):** Collapses spaced duplicates (such as `+213 542 09 98 32` alongside `+213542099832`) into clean unspaced pairs to conserve limited car Bluetooth PBAP phonebook slots.
    - **Safe Append Only:** Inserts missing clean `0X` or `+213` counterparts without deleting or modifying existing spaced rows.
- **Dry-Run Preview & Selective Control**
  - Displays a live telemetry dashboard (Scanned, Compliant, Missing `0X`, Missing `+213`, Spaced Cleanups, and Skipped numbers) before writing anything to Android `ContactsContract`.
  - Searchable, filterable preview list with monospaced diff badges and per-contact checkboxes to exclude specific contacts.
  - Automatically detects linked read-only sync entries (e.g., WhatsApp, Telegram) as **Covered by writable contact**, and offers automatic writable mirror creation in your primary Google/Device account for orphan read-only contacts.
- **Safe One-Tap Rollback**
  - Includes a **Remove Added Local (0X)** action that cleanly deletes only secondary `0[567]\d{8}` numbers tagged with the custom **`DZ Local`** label (as well as legacy **`Toyota`** labels from earlier builds), keeping all original user `0X` and `+213` numbers intact.
- **VCF + JSON Backup & Restore**
  - Built-in one-tap backup saves both a full vCard 3.0 file (`contacts_backup_YYYYMMDD.vcf`) and a structured JSON snapshot (`contacts_backup_YYYYMMDD.json`) directly to your `Downloads` folder before modifying contacts.
  - Restore anytime by importing the exported `.vcf` file through the standard Android Contacts app or inspecting the `.json` snapshot.
- **Material 3 Automotive Design**
  - Clean Jetpack Compose UI with adaptive **Light** and **Dark** Algerian emerald-green automotive themes, tabular monospaced number badges, and responsive phone/tablet layouts.
  - **100% Offline & Private:** Operates directly against the local Android `ContactsContract` database with zero network permissions or external servers.

---

## Screenshots

Drop your device screenshots into `docs/screenshots/` using the filenames below to populate this showcase:

| Dashboard & Summary | Dry-Run Preview & Diffs | Backup & Rollback |
| :---: | :---: | :---: |
| ![Main Screen](docs/screenshots/main-screen.png) | ![Preview Changes](docs/screenshots/preview-changes.png) | ![Backup and Rollback](docs/screenshots/backup-rollback.png) |
| *Telemetry dashboard & cleanup mode toggle* | *Searchable contact diff cards with checkboxes* | *VCF + JSON backup & one-tap Local (0X) revert* |

---

## How to Use

1. **Grant Contacts Permissions**
   - Launch **DZ Contact Fixer** and tap **Grant Contacts Access** to allow `READ_CONTACTS` and `WRITE_CONTACTS`. The app immediately scans your live phonebook locally.
2. **Create a Backup (Recommended)**
   - Tap **Backup VCF + JSON** in the bottom action bar (or overflow menu) to save `contacts_backup_YYYYMMDD.vcf` and `contacts_backup_YYYYMMDD.json` to your device's `Downloads` folder before making any edits.
3. **Review & Pair `+213` Numbers with `DZ Local` `0X` Numbers**
   - Review the summary counts and choose whether to keep **Clean Up Spaced Duplicates** enabled (recommended for car head units) or switch to **Safe Append Only**.
   - Use the search bar, filter chips, and per-contact checkboxes to inspect or exclude any entries, then tap **Apply Fix** and confirm. Updates run in safe batches of up to 400 `ContentProviderOperation` calls with live progress feedback.
4. **Re-Sync Bluetooth Contacts on Your Car Head Unit**
   - Turn Bluetooth off and back on (or trigger a manual phonebook refresh in your car's Bluetooth settings) so the head unit pulls the updated PBAP contact list with both `+213` and `0X` formats.
5. **Revert Changes Anytime (Optional)**
   - If you ever want to remove the added national `0X` entries, tap **Remove Local (0X)** (from the bottom bar or overflow menu) and confirm. Only `0[567]\d{8}` entries labeled `DZ Local` (or legacy `Toyota`) will be deleted.

---

## Build & Installation

### Requirements

Derived from the module Gradle configuration:

| Requirement | Version |
| :--- | :--- |
| **Minimum Android SDK** | API 24 (Android 7.0 Nougat) |
| **Target SDK** | API 36 |
| **Compile SDK** | API 36 |
| **JDK** | Java 21 (builds Java 11 bytecode) |
| **Kotlin** | 2.2.10 |
| **Android Gradle Plugin** | 9.1.1 |

### Build from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/dz-contact-fixer.git
   cd dz-contact-fixer
   ```
2. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
   The generated APK will be located at:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```
3. Install onto a connected Android device via ADB:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Run Unit & Robolectric Tests

Verify Algerian mobile classification, Mode A/B diffing, read-only contact resolution, rollback label detection, and VCF/JSON backup formatting on the JVM:

```bash
./gradlew testDebugUnitTest
```

---

## Permissions & Privacy

| Permission | Purpose |
| :--- | :--- |
| `READ_CONTACTS` | Scans live device contacts grouped by `raw_contact_id` to compute the dry-run preview and backups. |
| `WRITE_CONTACTS` | Applies batched `ContentProviderOperation` inserts, cleanups, and rollback deletions. |
| `WRITE_EXTERNAL_STORAGE` *(API ≤ 28 only)* | Legacy fallback for saving VCF/JSON backups to `Downloads` on Android 7.0–9.0. Android 10+ uses zero-permission `MediaStore.Downloads`. |

The app declares **no** `INTERNET` permission—your contacts never leave your device.

---

## License

This project is licensed under the **MIT License**. See the `LICENSE` file for details.
