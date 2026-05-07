# Lucky 9 (Android, Java)

Filipino rotating-banker card game built per the project plan
(`Lucky9_Final_Plan.docx`). This MVP covers Phases 1–6 (project setup,
game engine, Wallet+Room DB, tier system, oval-table bot UI, wallet UI)
plus partial Phase 9 (Tagalog labels, in-app rules viewer, settings,
tutorial). Multiplayer screens (Hotspot, Online) are scaffolded with
"coming soon" stubs.

## Requirements

- Android Studio Hedgehog or newer (Jellyfish recommended)
- Android SDK platform 34, build-tools 34.0.0
- JDK 17
- Gradle 8.5 (wrapper provided)
- Target devices: Android 8.0 (API 26) and above

## Project Layout

```
app/src/main/java/com/lucky9/app/
├── App.java                       # Application + DI bootstrap
├── data/                          # Room DB (Wallet, Transaction, DAOs)
├── engine/                        # Card, Deck, Hand, Engine, BotPolicy, Tier
├── repo/                          # WalletRepository (sync API + LiveData)
└── ui/
    ├── MainActivity.java          # 8-button menu
    ├── TableSelectActivity.java   # Tier + seat-count picker
    ├── GameActivity.java          # Round orchestration (BET/DEAL/HIT/REVEAL/SETTLE)
    ├── WalletActivity.java        # Balance, deposit/withdraw, daily bonus, history
    ├── RulesActivity.java
    ├── TutorialActivity.java
    ├── SettingsActivity.java
    ├── HotspotActivity.java       # stub
    ├── OnlineActivity.java        # stub
    ├── TransactionAdapter.java
    └── views/                     # OvalTableView, CardFaceView, WinBarView
```

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease   # needs signing config (see below)
./gradlew test              # 10 engine unit tests
```

## Signing

`app/build.gradle` reads signing config from `local.properties` or
environment variables — never hard-code secrets. Add to
`local.properties` (gitignored):

```
LUCKY9_STORE_FILE=/absolute/path/to/lucky9.keystore
LUCKY9_STORE_PASS=...
LUCKY9_KEY_ALIAS=...
LUCKY9_KEY_PASS=...
```

Generate a new keystore with:

```
keytool -genkey -v -keystore lucky9.keystore -alias lucky9 \
  -keyalg RSA -keysize 2048 -validity 10000
```

Without these properties, the release variant builds unsigned.

## Game Rules (Implemented)

- 52-card deck. A=1, 2–9 face value, 10/J/Q/K = 0. Hand totals are mod 10.
- Each round: ante to jackpot, banker deals 2 cards to each player and
  themselves, optional 1-card hit, then reveal.
- Natural 9 (first two cards total 9): player auto-stands and pays 1.5×;
  banker N9 sweeps the table; both N9 = tie.
- Win bars: +1 for each win, ties bump both. First to 5 wins the jackpot
  (split if multiple).
- Banker rotates clockwise each round; skipped if seat can't cover
  `(wallet - reserve) / non-bankers >= min bet`.
- Tiers: 50/100/200/500/1000. Reserve = 90% min, min bet = 3% min,
  ante = 50% min bet, lockout = 20% min.
- All money is BigDecimal (no rounding artifacts).

## What's Stubbed

- Wi-Fi Hotspot multiplayer (Phase 7)
- Online relay multiplayer (Phase 8)

The screens exist with "coming soon" copy and a relay URL field in
Settings is wired for the Phase 8 sync layer.

## Running on a Device

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

## License

Academic project — virtual currency only, no real-money processing.
