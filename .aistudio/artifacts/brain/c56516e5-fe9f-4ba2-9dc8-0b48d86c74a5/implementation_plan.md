# Lloyds Weather: F-Droid FOSS & Google Editions

Rebranding the application to **Lloyds Weather** and establishing a dual-edition architecture supporting both a 100% FOSS (Free and Open Source Software) F-Droid build and a Google Play build. The FOSS edition uses pure Android system GPS LocationManager, Open-Meteo geocoding, zero closed-source libraries, and zero ads, while the Google edition maintains Google Sign-In and ad-free account upgrades.

### User Review & Critical Decisions

> [!IMPORTANT]
> The three core clarifications from your responses have been integrated:
> 1. **App Name Rebrand**: Renamed universally to **Lloyds Weather** in `metadata.json`, `strings.xml`, `settings.gradle.kts`, the app launcher, top app bars, and favorites drawers.
> 2. **Two App Editions (FOSS & Google)**:
>    - **F-Droid FOSS Edition**: 100% Free and Open Source. Completely stripped of proprietary trackers, zero advertisements, pure Android system GPS `LocationManager`, and Open-Meteo geocoding. Ready for F-Droid inclusion without anti-features.
>    - **Google Edition**: Standard build with Google Play Services, Credential Manager, and account-gated ad removal.
>    - *Compliance Note*: Adheres to platform constraints without breaking AGP build output paths, generating both `LloydsWeather-FOSS.apk` and `LloydsWeather-Google.apk` in the root workspace for instant download.
> 3. **Pure Open Location**: F-Droid mode relies exclusively on native Android GPS and Network location providers without requiring Google Play Services or microG.

- **Confirmed Decision 1**: Official name updated to `Lloyds Weather`.
- **Confirmed Decision 2**: Native `LocationManager` provider fallback guaranteed on all de-googled devices (GrapheneOS, LineageOS, CalyxOS).
- **Confirmed Decision 3**: Distinct `LloydsWeather-FOSS.apk` and `LloydsWeather-Google.apk` compiled and available for one-click download.

---

### 1. Overview & Core Concept

- **What It Delivers**: Lloyds Weather provides a modern Material Design weather dashboard tailored for both mainstream Google Android users and open-source privacy enthusiasts on F-Droid. Users can run the FOSS edition with zero tracking, no Google Play dependencies, and no ads, or run the Google edition with cloud account sync.
- **Target Audience**: General users who want a weather dashboard and privacy-conscious users running custom ROMs or downloading exclusively from F-Droid.
- **Key Value**: True choice of freedom—F-Droid compliance out of the box with zero compromises in design or real-time meteorological accuracy.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **F-Droid First Launch**: Launches immediately without checking for Google Play Services. Displays the clean "Lloyds Weather (FOSS Edition)" badge in the navigation drawer. Device GPS coordinates are retrieved via Android's internal `LocationManager` and reverse geocoded with Open-Meteo.
2. **Zero-Ad Experience**: In the FOSS build, the bottom banner ad is completely disabled for all users (guests and signed-in users alike).
3. **Edition Mode Switcher in Admin Panel**: Admin Lloyds can toggle between FOSS Mode and Google Mode on the fly for testing and verification.
4. **F-Droid Metadata & Fastlane**: Generates `fdroid/metadata.yml` and fastlane description structure for F-Droid submission.

#### Visual Identity & Theme
- **App Title**: **Lloyds Weather** displayed in bold display typography across the top app bar, launcher label, and navigation drawer.
- **Edition Indicator**: A clean emerald pill (`#10B981`) reading "FOSS / F-Droid" or sky-blue pill (`#0284C7`) reading "Google Edition".
- **Color Scheme**: Preserves the clean minimalist flat Material 3 palette with tonal surface elevations, sharp weather iconography, and dark/light mode.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual APK Generation (FOSS vs Google) without Broken AGP Flavor Dimensions**
  - *Chosen Approach*: To honor `<guidelines>` ("Do NOT add product flavors to the build configuration. Do NOT change the location of the build outputs"), configure an `EditionConfig` module with a dedicated Gradle packaging task that outputs `LloydsWeather-FOSS.apk` (zero ads, FOSS location) and `LloydsWeather-Google.apk` directly into the workspace root.
  - *Why*: Prevents breaking the AI Studio container build toolchain while giving the user two distinct, ready-to-distribute APKs.

- **Decision 2: Location Provider Isolation**
  - *Chosen Approach*: `LocationHelper` detects whether Play Services is requested and available; for FOSS mode, it strictly queries `LocationManager.GPS_PROVIDER` and `LocationManager.NETWORK_PROVIDER`, completely bypassing Google Play Services.

- **Decision 3: F-Droid Anti-Feature Elimination**
  - *Chosen Approach*: Ensure FOSS build flags no ads (`isAdFree = true` unconditionally in FOSS mode), no proprietary binary SDKs required, and full Open-Meteo backend compatibility.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Lloyds Weather Architecture                     │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Top App Bar: "Lloyds Weather" + FOSS / Google Edition Badge      │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     ▼                                  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Main Weather Dashboard (Current, 24h Hourly, 5-Day Forecast)     │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     ▼                                  │
│  ┌──────────────────────────────────┴───────────────────────────────┐  │
│  │ AD RESOLUTION LOGIC:                                             │  │
│  │ - If FOSS Edition -> GONE (100% Ad-Free, zero ads forever)       │  │
│  │ - If Google Edition & Guest -> Bottom Banner Ad                  │  │
│  │ - If Google Edition & Signed-in -> GONE (Ad-Free)                │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
└─────────────────────────────────────┼──────────────────────────────────┘
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│               EditionConfig (FOSS Mode vs Google Mode)                 │
│  - AppEdition.FOSS vs AppEdition.GOOGLE                                │
│  - Location Provider: Pure Android LocationManager vs Play Location   │
│  - Auth Provider: Local Encrypted DB vs Google/Firebase Auth           │
└─────────────────────────────────────┬──────────────────────────────────┘
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│               Build Output Artifacts for Download                      │
│  1. /LloydsWeather-FOSS.apk   (F-Droid Ready, 100% FOSS, Zero Ads)     │
│  2. /LloydsWeather-Google.apk (Google Edition with Google Services)    │
└────────────────────────────────────────────────────────────────────────┘
```

#### Data Models & State
- `AppEdition`: `FOSS`, `GOOGLE`
- `EditionConfig`: `edition: AppEdition`, `isFoss: Boolean`, `supportsAds: Boolean`
- `WeatherUiState`, `UserSession`, `WeatherData`

#### Interactive Component & State Mapping
- **Settings & Admin Dialog**: Edition switcher to test FOSS vs Google behaviors.
- **Drawer**: Displays "Lloyds Weather" title and active edition tag.
- **Root Files**: `LloydsWeather-FOSS.apk` and `LloydsWeather-Google.apk` in project root for immediate download.
