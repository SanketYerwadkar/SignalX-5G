# SignalX⁵ᴳ — Phase 1 design system & UI prototype

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · ViewModel + StateFlow.
Every screen runs on `MockNetworkRepository`; no telephony API is touched yet.

## Brand
"Signal" in light white type, **X** painted with a cyan→blue gradient (`SrcAtop` blend over
the glyph), and a filled circular **5G** badge anchored to the X's top-right corner with a
negative offset — it overhangs the glyph rather than sitting beside or below it.
`SignalXLogo` (wordmark) and `SignalXMark` (X + badge only) in `ui/components/Brand.kt`.
The launcher icon is the mark alone as an adaptive vector, with a monochrome layer.

## Color tokens (`SxColor`)
| Role | Dark | Light |
|---|---|---|
| Background | `#05070A` | `#F6F8FB` |
| Surface / card | `#0B0F15` | `#FFFFFF` |
| Outline (hairline) | `#1E2733` | `#DCE3EC` |
| Primary accent | `#22D3EE` cyan | `#0E7490` |
| Secondary | `#2563EB` blue | `#2563EB` |
| Text primary / secondary | `#F2F6FA` / `#9AA7B4` | `#0B1220` / `#5A6675` |
| Status | success `#34D399`, warn `#FBBF24`, danger `#F87171` | same |

Gradients appear in exactly two places: the X glyph and the "Set Network to 5G" card.
Cards are flat with a 1dp outline — elevation is expressed by surface value, not shadow.

## Typography & spacing
Display 44/Bold (the big `5G`), Headline 22/SemiBold, Title 16, Body 15/13,
Label 14/SemiBold, Section label 11 with 1.2sp tracking, uppercase.
Technical readouts (dBm, dB, cell IDs) use `SxMono` so columns align.
Spacing is a 4dp scale (`Sx.s1`…`s10`); radii 10/16/22/pill; min touch target 48dp.

## Screens
Splash (≈1.6s: fade + scale + three expanding signal rings, then auto-navigates) →
Dashboard (SIM chips, Current Network card with 5-bar meter and inline refresh,
"Set Network to 5G" gradient card, Network Status card, 5 quick actions) →
Network Details (expandable Connection / SIM & Operator / 5G-NR / LTE cards plus the
system-settings link list) → Settings (theme, auto-refresh, confirm-before-opening, about)
→ About → Privacy / Licenses placeholders. Bottom nav: Home · Network · Settings.

Non-happy paths are first-class `NetworkUiState` cases, each with its own copy:
Loading, PermissionRequired, NoSim, NoService, Error — all rendered by `StateMessage`.

## Honesty rules baked into the code
- Nullable domain fields. A null renders "Not available" — never a fabricated number.
- Tapping the primary action opens a bottom sheet explaining that Android controls
  preferred network mode, then hands off via intent. No root, no hidden APIs.
- `SettingsIntents` walks a fallback chain per target and returns `false` when nothing
  resolves, so the UI can say the shortcut doesn't exist on that device.
- Phase 1 manifest declares zero permissions; Phase 2 adds only `READ_PHONE_STATE` and
  `ACCESS_FINE_LOCATION` (the latter is what gates `CellInfo`/NR identity on API 29+).

## Accessibility
Logo and chips merge into single content descriptions; status is always colour **plus**
text; `InfoRow` announces "label: value / not available"; all rows ≥48dp; sp units
throughout so Display Size and Font Size scaling both work.

## To preview other states
`MockNetworkRepository.scenario` → `SINGLE_SIM_LTE`, `NO_SIM`, `NO_SERVICE`,
`PERMISSION_DENIED`, `ERROR`.

## Phase 2 swap point
`MainActivity` constructs `MainViewModel(MockNetworkRepository())`. Replace with
`TelephonyNetworkRepository(applicationContext)` implementing the same
`NetworkRepository` interface — no UI file changes.
