# Atmosphere: User Authentication, AdSense Banner & Ad-Free Experience

Integrating persistent user authentication (supporting Google Sign-In and local saved accounts), Google AdSense/AdMob bottom banner ad unit for guest visitors, complete ad removal for authenticated users, an Admin account suite for `peterslloyd472@gmail.com`, and multi-device cloud synchronization for saved favorite locations.

### User Review & Critical Decisions

> [!IMPORTANT]
> The three core clarifications from your responses have been integrated:
> 1. **Authentication & Persistent Session**: Both Google Sign-In (via Android Jetpack Credential Manager) and account login are supported, with persistent session saving across app restarts.
> 2. **Bottom Banner Only**: When the user is a guest (not signed in), a clean non-intrusive bottom banner ad unit is displayed at the base of the screen.
> 3. **Strict Zero-Ads Policy on Sign-In**: The moment any user signs in, the ad banner is **completely eradicated** from the layout hierarchy, giving users an immaculate ad-free weather experience.
> 4. **Dedicated Admin Account**: The account `peterslloyd472@gmail.com` is granted Administrator status, displaying an Admin badge, permanent ad-free status, and an Admin Diagnostics & Analytics console.

- **Confirmed Decision 1**: Persistent session storage keeps users signed in without repeatedly requiring credentials on relaunch.
- **Confirmed Decision 2**: Zero ads once signed in. Guest users see a streamlined bottom banner that respects navigation bar insets.
- **Confirmed Decision 3**: Admin privileges automatically activated for `peterslloyd472@gmail.com` with quick admin credentials.

---

### 1. Overview & Core Concept

- **What It Delivers**: Atmosphere now features a dedicated user authentication layer. Guest users enjoy full weather functionality accompanied by a standard bottom ad banner. Signing in unlocks an ad-free experience, cross-session favorites persistence, profile details, and administrator management tools for the designated admin.
- **Target Audience / Persona**: Users who appreciate clean ad-free weather tracking upon signing in, and administrators needing oversight and system status.
- **Key Value**: Clear value exchange—guests get full free access with a bottom ad, while signed-in users enjoy zero ads, cloud-synced cities, and admin diagnostic controls.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Guest Browsing & Banner Ad**: On first launch without signing in, the user has full access to current weather, hourly forecast, 5-day projections, and city search. A clean, standardized bottom banner ad is visible above the bottom navigation bar.
2. **One-Tap Sign In**: The user taps the Profile / Sign In button in the top app bar or the favorites drawer. A clean authentication sheet opens, offering:
   - "Continue with Google"
   - Email & Password sign-in / registration
   - Quick "Sign In as Admin (`peterslloyd472@gmail.com`)" for instant admin access
3. **Instant Ad Eradication**: Once authenticated, the bottom banner ad completely disappears with an animated transition. A subtle "Ad-Free Activated" status chip is displayed in the profile sheet.
4. **Admin Dashboard Access**: When logged in as `peterslloyd472@gmail.com`, an "Admin" badge appears in the top app bar and drawer. Tapping it opens the Admin Diagnostics Panel:
   - Live API request rate & latency stats for Open-Meteo, OpenWeatherMap, WeatherAPI
   - Active ad telemetry & ad-free subscriber count
   - Cache clearing and database reset tools
5. **Sign Out & Session Persistence**: Sessions survive app restarts. Tapping "Sign Out" restores guest mode and the bottom ad banner.

#### Visual Identity & Theme
- **Ad Banner Design**: Clean standard M3 320x50 / 468x60 container with a subtle "Ad" tag, high contrast text, and smooth padding ensuring zero overlap with navigation gestures.
- **Auth Modals & Badges**:
  - Admin Badge: Sleek amber/gold status pill (`#F59E0B`) with crown/shield icon.
  - User Avatar: Clean Material circular avatar with user initials or Google profile picture.
  - Ad-Free Pill: Sky-blue chip indicating "Ad-Free Subscriber".

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Modular Auth State Manager (Google + Credential Manager + Saved Sessions)**
  - *Chosen Approach*: Implement an `AuthManager` with reactive `StateFlow<UserSession?>` backed by encrypted SharedPreferences. Integrates Jetpack Credential Manager for Google Sign-In and robust account credential persistence.
  - *Why*: Allows instantaneous offline and online sign-in without breaking when Google Play Services is missing or in emulated environments.

- **Decision 2: Strict Composable Conditional Ad Layout**
  - *Chosen Approach*: Wrap the bottom banner in `if (userSession == null) { BottomBannerAd() }`.
  - *Why*: Ensures zero render overhead, zero network ad calls, and zero visual space consumption for signed-in users.

- **Decision 3: Admin Privilege Gate for `peterslloyd472@gmail.com`**
  - *Chosen Approach*: Match user email or user role against the specified admin email. Automatically unlock administrative tools and diagnostics.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Compose UI Layer                              │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Top App Bar (Drawer, City Title, Admin Badge, Auth Avatar)       │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     ▼                                  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Main Weather Content (Current, Hourly, 5-Day Forecast, Metrics)  │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     ▼                                  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ CONDITIONAL AD UNIT:                                             │  │
│  │ if (currentUser == null) -> Render Bottom Banner Ad              │  │
│  │ if (currentUser != null) -> GONE (Zero Ads)                      │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────┬──────────────────────────────────┘
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        WeatherViewModel                                │
│  - userSession: StateFlow<UserSession?>                                │
│  - isAdmin: StateFlow<Boolean>                                         │
│  - isAdFree: StateFlow<Boolean>                                        │
│  - WeatherUiState & Search & Unit Preferences                          │
└─────────────────────────────────────┬──────────────────────────────────┘
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       AuthRepository & AuthManager                     │
│  - Google Sign-In via Jetpack Credential Manager                       │
│  - Email/Password persistent credential storage                        │
│  - Admin verification (peterslloyd472@gmail.com)                       │
│  - Session auto-restoration across cold starts                         │
└────────────────────────────────────────────────────────────────────────┘
```

#### Data Models & State
- `UserSession`: `id: String`, `email: String`, `displayName: String`, `isAdmin: Boolean`, `isAdFree: Boolean`, `loginType: LoginType` (GOOGLE, EMAIL, ADMIN).
- `AdminStats`: `totalRequests: Long`, `activeProvider: String`, `cacheSizeKb: Long`, `adFreeUsers: Int`.
- `AuthUiState`: `Idle`, `Loading`, `Success(UserSession)`, `Error(String)`.

#### Interactive Component & State Mapping
- **Sign In Dialog**: Displays Google Sign-In button, Email/Password inputs, and Quick Admin Login button.
- **Bottom Banner Ad**: Displayed exclusively when `userSession == null`. Automatically handles window navigation bar insets.
- **Admin Diagnostics Dialog**: Visible and accessible only when `userSession.isAdmin == true`.
- **Sign Out Action**: Resets active session and immediately re-enables guest banner.
