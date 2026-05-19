# Visual Language And Design System

## How To Use This File

- `_context/11_visual_language_and_design_system.md` is the canonical source of truth for NemoPill's Compose design-token vocabulary, semantic color roles, typography scale, spacing scale, component contract, motion rules, and content / voice conventions. Future `_source/` Compose theme infrastructure (`:core::theme/`, `:app::theme/`) and per-feature screens consume the names and values authored here; deviations require an ADR in `_context/09_decision_log.md`.
- Token names, semantic role names, component names, notification channel display names, and copy strings authored here are stable contracts across `_context/` files. A change to any name or string in this file must be reflected in `_context/02_domain_model_and_business_rules.md` (BR vocabulary), `_context/03_user_experience_and_use_cases.md` (UC / J / AC vocabulary), `_context/04_solution_architecture.md` (module / port vocabulary), `_context/05_engineering_quality_security_and_compliance.md` (snapshot baseline / data-protection vocabulary), `_context/06_integrations_data_flow_and_boundaries.md` (channel IDs, F-flow copy references), and `_context/12_environments_and_devops.md` (Privacy Policy path / format / loading vocabulary) before the change is considered durable.
- Sections that genuinely do not apply to an on-device-only single-Patient Android client may be resolved with the literal phrase `Not applicable` plus a one-sentence rationale rooted in earlier `_context/`. The phrase `Not applicable` must be paired with a rationale; a bare `Not applicable` is not acceptable per CLAUDE.md `\{\{OPTIONAL:\}\}` resolution rules.
- Any prose reference inside this file to the framework's placeholder marker syntax (`\{\{REQUIRED:\}\}`, `\{\{OPTIONAL:\}\}`, `\{\{EXAMPLE:\}\}`) must be backslash-escaped so `_framework/validate_framework.sh`'s `\{\{(REQUIRED|OPTIONAL|EXAMPLE):` regex does not false-fire. Markdown renders the escaped form identically to bare braces, so the human-reader experience is preserved. This convention was pinned in the 2026-05-17T14:23:24Z handoff and applies to every `_context/` file that discusses the template marker syntax.

## Applicability

- Visual surfaces in scope: four surfaces share the design-token vocabulary, semantic role names, and content / voice conventions authored here:
  - **Android app UI** — every Compose screen rendered inside the NemoPill app process, including the cross-cutting today's-Dose list and Adherence history views in `:app::presentation/` plus every feature-module screen in `:medication-management`, `:scheduling`, `:notifications`, and `:adherence-tracking` per `_context/04` § Module Or Component Map. Runtime surface; primary file-03 experience channel.
  - **Android system notifications** — lock-screen + notification-shade Reminders authored by the `:notifications` module, including BR-010's four copy variants (on-time, late-Reminder distinct text, silent-missed, batched-summary) across the four notification channels (`reminder_on_time`, `reminder_late`, `reminder_missed`, `reminder_summary`) pinned in `_context/06` § Section 2. Runtime surface; second file-03 experience channel.
  - **Play Console store-listing assets** — app icon (adaptive icon foreground + background; mono icon for Android 13+ themed icons), feature graphic, screenshots, in-store short / long description (bilingual: English + Spanish per the `_context/12` § Section 6 distribution scope of US + Latin America excluding Brazil). Authored at M-006 pre-launch per `_context/12` § Section 6's `_build/play-store-state.md` ADR. Screenshots derive from runtime-app Compose screens captured via the Roborazzi snapshot infrastructure pinned in `_context/12` § Section 4, so the listing inherits the runtime token vocabulary by construction. Build-time / distribution-time surface.
  - **Privacy Policy as a Markdown document** — `legal/PRIVACY_POLICY.{en,es}.md` (repo source-of-truth + GitHub Pages public URL) plus APK-bundled `res/raw{,-es}/privacy.md` rendered inside the in-app `Settings → Privacy Policy` screen per `_context/12` § Section 6's three-usage-paths ADR. The in-app rendering is governed by the same Compose token vocabulary as every other Android app UI surface; the GitHub Pages presentation inherits the typography and spacing scale but runs no Compose runtime. Build-time / distribution-time surface.
- Design system source of truth: `_context/11_visual_language_and_design_system.md` (this file) is the canonical source of truth for all design tokens, semantic role names, component contracts, motion rules, content / voice conventions, and notification copy. **Material 3** (`androidx.compose.material3`) provides the underlying token vocabulary and default values where they fit NemoPill's two-channel novice-Patient on-device-only single-user shape; this file overrides Material 3 defaults explicitly when needed (e.g., NemoPill-specific semantic role names, tightened contrast rules for the `_context/03` low-light / glanceable environment constraint, the `:notifications` module's BR-010 channel-importance posture that diverges from M3's notification-style defaults). The Material 3 choice is durable; switching to Material 2 in any later milestone requires an ADR in `_context/09_decision_log.md` with an explicit consequences-on-snapshot-baseline section per `_context/05` § Snapshot baseline matrix (the M3 / M2 flip invalidates every one of the 20 baseline captures).
- Brand owner: Isidro Rodriguez (Human gatekeeper). Visual changes follow the same `Approved for apply` gate that governs all `_context/` mutations: the appropriate role (Architect for file 11 edits; Developer for Compose theme-infrastructure source under `:core::theme/` and `:app::theme/`) drafts a proposal in a task packet; the Human gatekeeper flips `Task status: Approved for apply` in `_context/08_active_task_packet.md`; the change lands; an ADR is appended to `_context/09_decision_log.md` if the change is a durable design-system decision (token name or value change, semantic role re-mapping, Material version flip, component contract revision, notification channel display-name change, BR-010 copy change, hard-delete dialog copy change, deliberate-confirmation interaction pattern change, English Privacy Policy substance change). Inline-prose changes that do not affect any cross-file contract (clarifying wording, formatting fixes, typo corrections) do not require an ADR.

## Design Tokens

- Color values are expressed as 6-digit hex with optional 8-digit alpha suffix (e.g., `#1A1A1AB3` for 70% opacity). Compose consumes via `Color(0xFF1A1A1A)`. The Value column lists `Light: <hex> / Dark: <hex>` pairs; the active value is selected at the theme boundary by `LocalThemeMode.current` (see `## Theming And Customization`).
- Dimensional tokens (Spacing, Radius, Elevation) are expressed in `dp` and consumed by Compose `Modifier.padding()`, `RoundedCornerShape()`, and `Modifier.shadow()` respectively. The `dp` unit honors the file-03 phone / tablet / foldable responsive scope by scaling with device density.
- Z-index tokens are `Float` consumed by `Modifier.zIndex()`. The ramp is intentionally sparse — most Material 3 components manage their own z-order internally, so these tokens are reserved for cross-cutting composables that compose across feature module boundaries (modal dialogs from `:app::presentation/`, sticky headers in the today's-Dose list, the global snackbar host).

| Token Group | Token Name | Value | Usage Notes |
| --- | --- | --- | --- |
| `Color` | `color.surface` | Light: `#FFFFFF` / Dark: `#121212` | Default screen background; `Scaffold` container, modal backdrop background, top-of-stack composable. M3 `surface` analogue. |
| `Color` | `color.surface.muted` | Light: `#F4F4F5` / Dark: `#1E1E1E` | Recessed surface within a screen — list-row pressed state, disabled-control background, settings group container. M3 `surfaceContainerLow` analogue. |
| `Color` | `color.text.primary` | Light: `#1A1A1A` / Dark: `#F2F2F2` | Body text, headings, primary labels. Contrast pair against `color.surface`: light 16.2:1, dark 16.6:1 (WCAG AAA, both themes). |
| `Color` | `color.text.secondary` | Light: `#525252` / Dark: `#A1A1AA` | Supporting text, timestamps, helper text, captions. Contrast pair against `color.surface`: light 7.5:1, dark 7.0:1 (WCAG AAA, both themes). |
| `Color` | `color.border` | Light: `#D4D4D8` / Dark: `#3F3F46` | Input outlines, divider lines, card borders. Non-text UI component; WCAG 1.4.11 requires ≥ 3:1 against adjacent surface; both themes meet ≥ 3.0:1. |
| `Color` | `color.accent` | Light: `#2C5EAD` / Dark: `#4BB8FA` | Actionable color; primary buttons, active toggles, focused-state ring, in-app links to Settings → Privacy Policy. Sourced from NemoPill brand palette (Brand-1 light / Brand-3 dark). Contrast pair against `color.surface`: light 6.6:1, dark 10.2:1 (WCAG AAA, both themes). |
| `Color` | `color.success` | Light: `#2E7D32` / Dark: `#66BB6A` | Confirmed Dose status, Adherence-positive trend, hard-delete *completed* affirmation copy. Contrast pair against `color.surface`: light 5.4:1, dark 7.0:1 (WCAG AA light, AAA dark). |
| `Color` | `color.warning` | Light: `#B45309` / Dark: `#F59E0B` | Late-Reminder status text, retroactive-window-closing-soon advisory, permission-needed banner. Contrast pair against `color.surface`: light 5.0:1, dark 8.0:1 (WCAG AA light, AAA dark). |
| `Color` | `color.danger` | Light: `#C62828` / Dark: `#EF5350` | Missed Dose status, hard-delete confirmation primary action color, `Result.Err.ValidationFailed` / `RetroactiveWindowExpired` error-text color. Contrast pair against `color.surface`: light 5.7:1, dark 5.4:1 (WCAG AA, both themes). |
| `Color` | `color.info` | Light: `#0277BD` / Dark: `#1591DC` | Informational notices distinct from `color.accent` (which is reserved for actionables). Light value (`#0277BD`) is brand-adjacent (same blue family, outside the four canonical brand colors) because no brand-palette value passes WCAG AA normal text on `color.surface` light. Dark value sourced from NemoPill brand palette (Brand-2). Contrast pair against `color.surface`: light 5.4:1, dark 7.0:1 (WCAG AA light, AAA dark). |
| `Spacing` | `space.0` | `0dp` | Zero spacing; flush layouts; explicit "no gap" assertion vs. unset. |
| `Spacing` | `space.1` | `4dp` | Sub-component padding; icon-to-label internal padding in tight components. |
| `Spacing` | `space.2` | `8dp` | Small gap between related items; chip internal padding; tight list-row vertical padding. |
| `Spacing` | `space.3` | `12dp` | Medium gap; standard list-row vertical padding; form-field internal padding. |
| `Spacing` | `space.4` | `16dp` | Default screen edge padding; default vertical rhythm between sections within a screen. M3 baseline. |
| `Spacing` | `space.5` | `20dp` | Slightly larger gap for breathing room between distinct content blocks. |
| `Spacing` | `space.6` | `24dp` | Large gap between major sections within a screen; modal content padding. |
| `Spacing` | `space.8` | `32dp` | Section break; gap between header and first content block on a screen. |
| `Spacing` | `space.10` | `40dp` | Reserved for prominent visual separation between unrelated content groups. |
| `Spacing` | `space.12` | `48dp` | Largest standard gap; honors the file-03 one-handed-friendly minimum tap-target reach with bottom-of-screen breathing space. |
| `Radius` | `radius.none` | `0dp` | Square corners; explicit "no rounding" assertion. |
| `Radius` | `radius.xs` | `4dp` | Subtle rounding; chip corners, small badge corners. |
| `Radius` | `radius.sm` | `8dp` | Standard form-field corners, secondary button corners. |
| `Radius` | `radius.md` | `12dp` | Card corners, large form-field corners, default container rounding. |
| `Radius` | `radius.lg` | `16dp` | Primary button corners, prominent surface rounding. |
| `Radius` | `radius.xl` | `28dp` | Modal dialog corners — used by the hard-delete confirmation dialog and the defensive Room-migration failure screen. M3 modal default. |
| `Radius` | `radius.full` | `9999dp` | Pill shape; FAB, fully-rounded chips, status indicators. |
| `Elevation` | `elevation.0` | `0dp` | Flat surface; default screen content; no shadow. |
| `Elevation` | `elevation.1` | `1dp` | Subtle lift; cards in a list, top-app-bar at-rest. |
| `Elevation` | `elevation.2` | `3dp` | Lift; cards on a scrolled-under top-app-bar, selected list-row. |
| `Elevation` | `elevation.3` | `6dp` | Floating element; FAB at-rest, hovered card. |
| `Elevation` | `elevation.4` | `8dp` | Pressed-elevated FAB, dropdown menu. |
| `Elevation` | `elevation.5` | `12dp` | Modal dialog (including hard-delete confirmation dialog and defensive Room-migration failure screen), bottom-sheet at-rest. |
| `Z-index` | `z.base` | `0f` | Default Compose z-order; no explicit layering. |
| `Z-index` | `z.sticky` | `100f` | Sticky headers, scroll-anchored composables that need to render above standard content (today's-Dose list date header). |
| `Z-index` | `z.modal` | `1000f` | Modal dialog overlay (hard-delete confirmation dialog, defensive Room-migration failure screen); ensures dialog renders above scrollable list backgrounds. |
| `Z-index` | `z.snackbar` | `1500f` | Snackbar / inline toast surface; renders above modal in the rare case both are present. |
| `Z-index` | `z.debug` | `9000f` | Reserved for development-only debug overlays; never compiled into release builds. Konsist asserts no production code references this token. |

## Color System

- **Color roles (semantic) — 10 roles with explicit boundary contracts.** Each role has a single allowed usage and one or more explicit prohibitions. Never use a role outside its boundary; if a new use case appears that does not fit any existing role, file an ADR in `_context/09_decision_log.md` rather than re-purposing a role.

| Role | Boundary contract |
| --- | --- |
| `color.surface` | Default screen and modal background. Never used for text or to differentiate semantic state. |
| `color.surface.muted` | Recessed surface within a screen for visual hierarchy — pressed list-row state, disabled-control container, grouped Settings sections, retroactive-Confirmation badge background. Never used for borders or for text. |
| `color.text.primary` | All body and heading text. Never used for icons (icons inherit `color.accent` when actionable or `color.text.secondary` when non-actionable). |
| `color.text.secondary` | Supporting text — timestamps, helper text, captions, the "scheduled at 8:00 AM" line under a Dose status. Never used for primary body text; never used to convey error or success state. |
| `color.border` | Input outlines, divider lines, card borders, focus rings. Never used as a fill for buttons or icons. Compensating control on contrast failure documented below. |
| `color.accent` | Actionable color — primary buttons, active toggle, focused-state ring, in-app link to Settings → Privacy Policy, hyperlink. Never used to convey semantic state (success / warning / danger / info); never used as a generic decorative fill. Reserved exclusively for "tap me" surfaces. |
| `color.success` | Confirmed Dose status, Adherence-positive trend, hard-delete *completed* affirmation copy. Never used as a primary actionable color (use `color.accent`, even when the action's consequence is success). |
| `color.warning` | Late-Reminder status (BR-010), retroactive-window-closing-soon advisory (BR-011), permission-needed banner (`Result.Err.ExactAlarmPermissionRevoked` / `NotificationsPermissionRevoked`). Never used for missed-Dose status (that is `color.danger`); never used for permission-granted affirmation (that is `color.success`). |
| `color.danger` | Missed Dose status, hard-delete confirmation primary action color, `Result.Err.ValidationFailed` / `RetroactiveWindowExpired` error-text color. Never used as a generic emphasis color; reserved exclusively for genuinely-destructive or error-state surfaces. |
| `color.info` | Informational notices distinct from `color.accent` (which is reserved for actionables). Examples: the "this Dose was logged retroactively" badge in Adherence history, the "this Reminder fired late" badge on a Confirmation. Never used to convey error state; never used as an actionable color. |

- **Brand palette (raw)** — NemoPill has four canonical brand colors. Three are used directly as Section 3 color-token values mapped to `color.accent` and `color.info`; the fourth is reserved for decorative use in Section 7 Iconography And Imagery. The accent doubles as the brand color (no separate brand-vs-accent distinction); the brand palette is the source for blue-family token values where contrast rules allow.

| Brand color | Hex | Section 3 mapping | Light contrast on `color.surface` | Dark contrast on `color.surface` | Allowed use |
| --- | --- | --- | --- | --- | --- |
| Brand-1 | `#2C5EAD` | `color.accent` light value | 6.6:1 (AAA) | n/a | Light-theme primary actionable surfaces. |
| Brand-2 | `#1591DC` | `color.info` dark value | 3.5:1 (fails AA normal; AA large only) | 7.0:1 (AAA) | Dark-theme informational notices. Light-theme use is restricted to large-text badges (≥ 18pt regular / ≥ 14pt bold); cannot serve as a generic text color in light theme. |
| Brand-3 | `#4BB8FA` | `color.accent` dark value | 2.2:1 (fails) | 10.2:1 (AAA) | Dark-theme primary actionable surfaces. Cannot be used in light theme at token grade. |
| Brand-4 | `#C4E2F5` | `Not applicable` at Section 3 token grade — reserved for Section 7 | 1.33:1 (fails) | 14.3:1 (AAA, dark text only) | Decorative tint for Section 7 empty-state illustration accents and subtle list-row selected-state background tint. Cannot serve as a Section 3 token because of the light-theme WCAG 1.4.11 failure. |

- **Brand-adjacent token clarification** — `color.info` light value `#0277BD` is *not* from the four canonical brand colors but is brand-adjacent (same blue family, ~200° hue vs. the brand's ~210–220° range; visually compatible). It is promoted to a brand-adjacent role because no canonical brand-palette value satisfies WCAG AA normal text on `color.surface` light. A future revision that picks a darker brand-palette extension (e.g., `#1A6FB3`) for `color.info` light is an ADR amendment in `_context/09`; the Section 3 snapshot baseline survives the swap because the contrast band is preserved.

- **Theming model — light + dark only; no high-contrast variant.** Theme selection follows Android's `Configuration.uiMode` (`UI_MODE_NIGHT_YES` / `UI_MODE_NIGHT_NO`) — the system dark-mode toggle is the single source of truth for the active theme; the in-app Settings UI never exposes a separate theme picker. Rationale: (i) Section 3 values exceed WCAG AA across the board, with primary text at AAA, so a separate high-contrast theme is not needed for low-vision Patients; (ii) the `_context/05` § Snapshot baseline of 20 captures (4 screens × 2 breakpoints × 2 themes + 4 notifications) is maintainable; a third theme would push the baseline to 32+ captures and double the maintenance cost for every visual change; (iii) honoring the OS-level dark-mode preference aligns with the file-03 environment constraint that NemoPill be respectful of Patient device-level preferences. System-level accessibility amplifiers (`Settings.Secure.HIGH_TEXT_CONTRAST_ENABLED` on Android 13+) are honored implicitly by Material 3 components and do not require NemoPill-authored token overrides; Roborazzi snapshot tests under the M-006 accessibility pass verify this behavior.

- **Contrast rules — WCAG 2.1 AA is the floor; AAA is the achieved value for primary and secondary text.** The full rule set:

| Audience | Minimum ratio | Section 3 actual (light / dark) |
| --- | --- | --- |
| Normal text (< 18pt regular / < 14pt bold) on `color.surface` or `color.surface.muted` | ≥ 4.5:1 (WCAG 1.4.3 AA) | `color.text.primary`: 16.2 / 16.6 (AAA); `color.text.secondary`: 7.5 / 7.0 (AAA); semantic colors as text: 5.0–6.6 / 5.4–10.2 (AA, some AAA) |
| Large text (≥ 18pt regular / ≥ 14pt bold) on `color.surface` or `color.surface.muted` | ≥ 3.0:1 (WCAG 1.4.3 AA) | Every Section 3 color passes; `color.info` light at `#1591DC` (Brand-2) passes only in this row |
| Non-text UI components and graphical objects (icons, focus rings, input borders, checkboxes, divider lines) | ≥ 3.0:1 (WCAG 1.4.11 AA) | `color.border` at 1.6 / 1.4 fails — compensating control documented below |
| Disabled control text (WCAG 1.4.3 exempts this; NemoPill self-imposes a floor) | ≥ 3.0:1 (NemoPill self-imposed) | Implemented via `color.text.secondary` at 50% opacity; verified per-component during M-001 |
| Focus ring against both the focused element AND the adjacent surface (WCAG 2.4.11 enhanced) | ≥ 3.0:1 against both | `color.accent` against `color.surface` and against any focused composable container; verified per-component during M-001 |
| Pressed / hover state against default state | Detectable luminance shift, typically 4–8% | Implemented via Compose `ripple()` indication; M3 default ripple amplitudes accepted |

- **Compensating control for `color.border` failing WCAG 1.4.11.** The `color.border` values (`#D4D4D8` light = 1.6:1, `#3F3F46` dark = 1.4:1 against `color.surface`) fail the WCAG 1.4.11 ≥ 3:1 requirement for non-text UI component differentiation. The compensating control is that no element relies on `color.border` *alone* to convey meaning: form-field inputs additionally show a label and a `color.accent` focus ring when focused (which does pass 1.4.11); list-row dividers are paired with vertical spacing (`space.3`) that itself communicates the row boundary; card borders are paired with `elevation.1` shadow that communicates the surface boundary. A future component that needs `color.border` to carry meaning standalone — without spacing, focus ring, or elevation backup — must either bump `color.border` to passing values or file an ADR in `_context/09` documenting the accessibility trade-off.

- **Per-component-state contrast rules** are itemized in Section 8 Components for each component; the rules above are the floor every component must clear before it is added to the Components table.

## Typography

- **Font families** — primary: **Roboto** (the Android system default since API 11; ships in every device's system fonts, zero APK asset weight); monospace: **Roboto Mono** (similarly system-bundled, used only for `text.code`); fallback chain: `Roboto → sans-serif` for primary text, `Roboto Mono → monospace` for code. Roboto is consumed via M3's `MaterialTheme.typography` defaults; no `res/font/` assets are added to the APK for Section 5 styles. Rationale: zero APK-weight cost, predictable rendering across Android device generations from API 26 (Android 8 Oreo) through API 35 (Android 15), no font-loading edge cases on first launch, no licensing concerns. The accessibility argument for Atkinson Hyperlegible was considered and deferred; Section 12 Design System Risks And Trade-Offs records this as a planned-hardening candidate for any future revision that re-evaluates the novice-Patient low-vision posture.

- **Type scale — 8 NemoPill named styles.** Each style maps to a Material 3 baseline style for the underlying implementation; the NemoPill name is the stable contract that `:core::theme/Typography.kt` exposes and that downstream Compose code references by name.

| Style | Size | Line-height | Weight | Letter spacing | M3 equivalent | Intended use |
| --- | --- | --- | --- | --- | --- | --- |
| `text.display` | 28sp | 36sp | 400 | 0sp | `displaySmall` | First-launch onboarding hero; one-off "welcome to NemoPill" moments. Not used on the four production screens at MVP (today's-Dose list, Adherence history, Medication list, Settings); reserved for future onboarding additions. |
| `text.h1` | 24sp | 32sp | 700 | 0sp | `headlineSmall` | Screen titles in `TopAppBar` when uncollapsed; primary screen headings. One per screen. |
| `text.h2` | 20sp | 28sp | 700 | 0sp | `titleLarge` | Section headings within a screen ("Today's Doses", "This Week's Adherence", "Medications", "Notifications" Settings group). |
| `text.h3` | 16sp | 24sp | 700 | 0.15sp | `titleMedium` | Subsection headings within a section; primary button label; in-body emphasis text. |
| `text.body` | 16sp | 24sp | 400 | 0.5sp | `bodyLarge` | Default body text; Medication names; Dose times; Privacy Policy body; hard-delete confirmation dialog body. |
| `text.body.small` | 14sp | 20sp | 400 | 0.25sp | `bodyMedium` | Supporting body text; helper text below form fields; descriptive captions on Adherence visualizations. |
| `text.caption` | 12sp | 16sp | 400 | 0.4sp | `bodySmall` | Timestamps; metadata; the "scheduled at 8:00 AM" line under a Dose status; the "retroactive" / "late" badge text. |
| `text.code` | 14sp | 20sp | 400 | 0sp | `bodyMedium` with monospace family override | `Result.Err.Unexpected` stack-trace surfaces in development builds only. Konsist asserts no composable in `:medication-management`, `:scheduling`, `:notifications`, `:adherence-tracking`, or `:app::presentation/` references `text.code`. |

- **Weights in use — 3 weights: 400 (regular), 500 (medium), 700 (bold).** Weight 400 is the default for body text and supporting text. Weight 700 is used exclusively by `text.h1` / `text.h2` / `text.h3` and by the `Button` `primary` variant's label (per Section 8 Components). Weight 500 is reserved for **selected-state labels** in toggles, segmented controls, and the active item in a navigation rail / bottom-nav surface (per Section 8 Components); using 500 for non-selected emphasis is prohibited because it dilutes the selected-state signal. Other Roboto weights (100, 300, 900, etc.) are not loaded; using them in Compose code is a Konsist violation.

- **Numeric and tabular rules — tabular numerals (`fontFeatureSettings = "tnum"`) for all numeric data.** Compose `TextStyle` applies `tnum` globally via the `:core::theme/Typography.kt` defaults; all eight NemoPill styles inherit it. Effect: digits in `text.body` and `text.caption` render at uniform widths, so `08:00 AM` and `11:00 AM` align perfectly in the today's-Dose list time column and so adherence percentages (`78 %`, `100 %`) align in the Adherence history table. Non-numeric characters render with their default proportional widths; only digits are affected. Decimal alignment in adherence-trend visualizations is achieved by the layout primitive (`Modifier.alignment`) rather than by typographic feature; no currency formatting is needed because NemoPill has no monetary surfaces.

- **Line length / measure — phone breakpoint imposes no explicit measure constraint; tablet and foldable-unfolded breakpoints impose `Modifier.widthIn(max = 600.dp)` on body-text containers.** Rationale: at phone breakpoint, screen edge padding (`space.4` = 16dp on each side) plus typical 360–420dp phone widths yields ~50–75 characters per `text.body` line, which sits in the WCAG 2.4.8 reading-comfort optimal range. At tablet (≥ 600dp) and foldable-unfolded breakpoints, body-text containers without a width cap stretch to 100+ characters per line, which degrades reading comfort and contradicts the file-03 novice-Patient persona. Containers that hold body text (the in-app `Settings → Privacy Policy` screen, hard-delete confirmation dialog body, error explanation strings, defensive Room-migration failure screen body) apply `Modifier.widthIn(max = 600.dp)` and center horizontally on tablet / foldable-unfolded. Containers that hold tabular or list data (today's-Dose list, Adherence history, Medication list) do not apply a measure constraint — they use the full available width because their content is row-based rather than paragraph-based.

- **Underline rule for hyperlinks** — hyperlinks within `text.body` (e.g., the in-app `Settings → Privacy Policy` text "View Privacy Policy") use `color.accent` plus a 1dp underline at the text baseline; the underline is the non-color affordance that satisfies WCAG 1.4.1 (information not conveyed by color alone). No other body text uses underlines.

- **Italic usage** — discouraged in NemoPill copy. Italic is not used for emphasis (weight 700 via `text.h3` is the emphasis mechanism); not used for foreign-language strings (Spanish strings are rendered in upright Roboto, same as English); not used for legal disclaimers in the Privacy Policy body. The only allowed italic surface is the `text.code` style when Compose-Compiler-generated stack-trace symbols include italic glyphs (system-rendered, not NemoPill-authored).

## Spacing And Layout

- **Spacing scale** — 10 named tokens from `space.0` (0dp) through `space.12` (48dp) locked in Section 3 Design Tokens. The intermediate steps (`space.7 = 28dp`, `space.9 = 36dp`, `space.11 = 44dp`) are intentionally omitted to prevent ad-hoc "split the difference" decisions in Compose code; adding any of them is an ADR amendment in `_context/09_decision_log.md`, not an inline addition by a Developer. Compose code references the scale via `Theme.spacing.4` (or the equivalent accessor exposed in `:core::theme/Spacing.kt`) rather than literal `16.dp` values; Konsist asserts that no production composable references a literal `dp` value matching a named scale step (so `16.dp` in a composable is a violation if `space.4 = 16dp`). Exception: Compose internal modifiers like `Modifier.size(1.dp)` for hairline dividers and `Modifier.size(0.5.dp)` for sub-pixel separators are exempt since they don't correspond to a named scale step.

- **Spacing usage patterns** — three recurring patterns across NemoPill screens:
  - **Screen edge padding** = `space.4` (16dp) on left and right of every screen; `Scaffold` content lambda applies this as its outer padding.
  - **Inter-section vertical rhythm** = `space.6` (24dp) between major sections within a screen (e.g., between "Today's Doses" and "This Week's Adherence" cards on the home screen).
  - **Intra-section vertical rhythm** = `space.3` (12dp) between subsections / list rows within a section.

- **Grid system — no explicit column grid; edge-padded layouts only.** Every screen has `space.4` (16dp) edge padding applied by `Scaffold`'s content lambda; content fills the available width within those edges. No 12-column / 4-8-12 / responsive-column system is imposed. Rationale: NemoPill's four MVP screens are list-and-detail patterns rather than dashboard layouts; an explicit column grid would add wrapper-composable overhead without expressive payoff. Adaptive layouts at Medium and Expanded breakpoints (see Breakpoints below) are achieved by `Modifier.widthIn(max = 600.dp)` on body-text containers (per Section 5 Line length / measure) and by `SupportingPaneScaffold` for the Medication detail screen at Expanded breakpoint — not by a generic grid.

- **Breakpoints — Material 3 window-size classes via `currentWindowAdaptiveInfo()`.** Three named breakpoints consumed from M3's `androidx.compose.material3.adaptive` library:

| Window size class | Width range | Devices | NemoPill layout posture |
| --- | --- | --- | --- |
| `Compact` | < 600dp | Phone in portrait; foldable-folded | Single-pane stacked layout; full-width lists; primary nav via bottom `NavigationBar`; `Modifier.widthIn(max = 600.dp)` is a no-op at this width. |
| `Medium` | 600–839dp | Tablet in portrait; foldable partially unfolded | Single-pane stacked layout (same posture as Compact); body-text containers cap at 600dp and center horizontally; lists fill the available width; primary nav remains bottom `NavigationBar`. |
| `Expanded` | ≥ 840dp | Tablet in landscape; foldable fully unfolded | Two-pane `SupportingPaneScaffold` for the Medication-list-plus-Medication-detail pattern; today's-Dose list and Adherence history remain single-pane (their content is row-based, not detail-based); body-text containers cap at 600dp and center horizontally; primary nav via left-side `NavigationRail` rather than bottom `NavigationBar`. |

  The two snapshot form-factor breakpoints in `_context/05_engineering_quality_security_and_compliance.md` § Snapshot baseline map to these window-size classes as: **phone breakpoint = Compact** (representative width 360dp); **tablet breakpoint = Expanded** (representative width ≥ 840dp). The Medium breakpoint is not separately snapshotted because its layout posture matches Compact; foldable-folded ≈ Compact and foldable-unfolded ≈ Expanded per the `_context/05` foldable-subsumption rule. Specific Roborazzi capture configurations (exact dp dimensions, density bucket, font scale) are managed in `_context/12_environments_and_devops.md` § Section 4.

- **Layout primitives — core Compose / M3 primitives only.** NemoPill composables compose using the following primitives directly; no NemoPill-authored wrapper primitives (`NemoPillStack`, `NemoPillCluster`, etc.) are added to `:core::theme/`:

| Primitive | Source | NemoPill usage |
| --- | --- | --- |
| `Column`, `Row`, `Box` | `androidx.compose.foundation.layout` | Default linear and stacked layouts; consume `space.*` tokens via `Modifier.padding()` and `Arrangement.spacedBy(space.3)`. |
| `LazyColumn`, `LazyRow` | `androidx.compose.foundation.lazy` | Today's-Dose list, Adherence history (vertical), Medication list, the expand surface of the BR-010 batched-summary notification. |
| `FlowRow`, `FlowColumn` | `androidx.compose.foundation.layout` | Chip groups (e.g., Dose-time-of-day chip selector in the Medication editor); wraps to next line at narrow breakpoints. |
| `Spacer` | `androidx.compose.foundation.layout` | Explicit fixed-size gaps not better expressed as `Arrangement.spacedBy`; consumes `space.*` tokens via `Spacer(Modifier.height(space.4))`. |
| `Surface` | `androidx.compose.material3` | Backgrounds with `color.surface` / `color.surface.muted` and `elevation.*` shadows; card containers. |
| `Scaffold` | `androidx.compose.material3` | Every screen's outer container; provides `TopAppBar`, `BottomAppBar` / `NavigationBar`, `FloatingActionButton`, snackbar host slots; applies `space.4` edge padding to the content lambda. |
| `AlertDialog` | `androidx.compose.material3` | Hard-delete confirmation dialog; defensive Room-migration failure screen; permission-needed dialogs (`ExactAlarmPermissionRevoked`, `NotificationsPermissionRevoked`). |
| `ModalBottomSheet` | `androidx.compose.material3` | Medication editor (Add Medication, Edit Medication) — composes a multi-field form below a draggable handle without leaving the parent screen context. |
| `SupportingPaneScaffold` | `androidx.compose.material3.adaptive` | Expanded breakpoint two-pane layout for Medication-list-plus-Medication-detail; collapses to single-pane at Compact and Medium. |
| `NavigationBar`, `NavigationRail` | `androidx.compose.material3` | Bottom `NavigationBar` at Compact and Medium breakpoints; left-side `NavigationRail` at Expanded. |

  Custom layouts that combine primitives are allowed inside feature modules (e.g., a `DoseRow` composable inside `:adherence-tracking::presentation/`) but they are *composables*, not *layout primitives* — they do not get added to `:core::theme/` and they are not consumed by other modules. The cross-module composable surface is the M3 vocabulary above.

- **Touch target minimum — 48×48dp for every interactive element.** Every button, toggle, switch, checkbox, radio button, list-row tap target, icon button, inline action button (notification action), and link has a minimum touch target of 48dp wide × 48dp tall. Material 3 components ship with 48×48dp defaults; NemoPill composables that wrap M3 components must not shrink the touch target below 48×48dp via `Modifier.size()` or `Modifier.padding()` overrides. Rationale: file-03 one-handed-friendly environment constraint and novice-Patient persona favor stricter-than-WCAG-AA targets; M3's 48dp minimum is also the WCAG 2.5.5 (level AAA) target-size criterion. Snapshot tests verify per-component during M-001 implementation; Konsist asserts no production composable applies `Modifier.size(width.dp, height.dp)` to a `Button`, `IconButton`, `Checkbox`, `Switch`, `RadioButton`, or list-row root composable where either dimension is declared < 48dp.

- **Edge-padding exceptions** — three deliberate exceptions to the `space.4` (16dp) screen edge-padding rule:
  - **List rows in `LazyColumn`** extend to the screen edge horizontally so the tap target (and any background highlight or future swipe action) covers the full screen width; their internal content respects `space.4` padding on the left and right.
  - **Snackbar / toast surfaces** rendered via `Scaffold`'s snackbar host extend to the screen edge; their default M3 styling places `space.4` margin on left / right / bottom.
  - **Modal dialogs and bottom sheets** ignore the screen-edge-padding rule entirely; their own internal padding (`space.6` content padding for `AlertDialog`, `space.4` for `ModalBottomSheet`) governs.

## Iconography And Imagery

- **Icon library — Material Symbols Outlined (default) + Filled (selected-state).** Source: `androidx.compose.material:material-icons-extended` (Compose Material Symbols package). The dependency ships ~35,000 icons but the Compose Compiler tree-shakes unused ones at build time; per-icon APK cost is ~2KB for used icons only. NemoPill uses two paired weights:
  - **Outlined** — default visual weight for every icon in the at-rest state (toolbar action, list-row affordance, form-field leading icon, bottom-`NavigationBar` inactive tab, toggle OFF state, FAB at-rest). Lighter, more modern, M3-canonical.
  - **Filled** — used exclusively as the *selected-state pair* of an Outlined icon (bottom-`NavigationBar` active tab, toggle ON state, selected segmented-control item). Filled icons never appear standalone outside a selected-state context; using them otherwise dilutes the selected-state signal. Konsist asserts that every `Icons.Filled.*` reference in production composables is paired with a selected-state predicate.

- **Icon size tokens** — four sizes exposed via `:core::theme/IconSize.kt` (separate from `:core::theme/Spacing.kt` to keep Section 3's named-step rule scoped to spacing):

| Size | Token name | Use |
| --- | --- | --- |
| 20dp | `iconSize.inline` | Inline icons that sit within a `text.body` or `text.body.small` line (e.g., the warning icon inside a permission-needed body paragraph). |
| 24dp | `iconSize.default` | Default action-icon size; toolbar icons, list-row affordances, FAB icon glyph, bottom-`NavigationBar` icons, button leading icons. M3 baseline. |
| 32dp | `iconSize.large` | Prominent icons used inside cards or empty-state heading rows (when not part of the empty-state illustration). |
| 48dp | `iconSize.hero` | Hero icons used inside the defensive Room-migration failure screen and other one-off attention-required surfaces. Matches the 48dp touch-target minimum, so the icon doubles as its own tap affordance if interactive. |

  Konsist asserts no production composable applies a literal `dp` size to a `Material Symbols` icon that matches a named `iconSize` token. Stroke weight is the Material Symbols default (~2dp at 24dp icon size); NemoPill does not customize stroke weight via the variable-font axes (`wght`, `FILL`, `GRAD`, `opsz`). A future revision that wants to use the variable axes (e.g., to thin icons on dark theme for visual balance) is an ADR amendment.

- **Icon usage rule — icons + labels by default; icon-alone is permitted only for universal symbols.** The universal-symbol allowlist is closed at four entries:
  - `arrow_back` (`Icons.Outlined.ArrowBack`) — back navigation in `TopAppBar`. Accompanied by `contentDescription = "Back"` (localized) for TalkBack.
  - `close` (`Icons.Outlined.Close`) — close / dismiss a modal dialog, bottom sheet, or full-screen overlay. Accompanied by `contentDescription = "Close"` (localized).
  - `more_vert` (`Icons.Outlined.MoreVert`) — overflow menu trigger in `TopAppBar` or list-row. Accompanied by `contentDescription = "More options"` (localized).
  - `add` (`Icons.Outlined.Add`) — the FAB's primary glyph (Add Medication). Accompanied by `contentDescription = "Add medication"` (localized).

  Every other icon must be paired with a visible text label (M3 `NavigationBarItem` icon + label pattern; `Button` with leading icon + label; list-row leading icon + Medication name). Adding a new icon-alone affordance requires extending the allowlist via an ADR amendment in `_context/09_decision_log.md`; this gate exists to prevent silent erosion of the file-03 novice-Patient accessibility commitment. TalkBack reads the visible label for icon + label affordances; the `contentDescription` parameter is only required for the four icon-alone universal symbols above and for any future ADR-approved additions.

- **Imagery style — minimal flat-style illustrations for empty states only.** No photography, no 3D rendering, no generative imagery, no animated GIFs / Lottie. Each illustration is authored as a Compose `ImageVector` constant in `:core::theme/illustrations/EmptyStates.kt`. Style rules:
  - **Color palette** — fills use **Brand-4 (`#C4E2F5`)** via the illustration-scoped accessor `Theme.illustration.fill`; lines and details use `color.accent` (Brand-1 `#2C5EAD` light / Brand-3 `#4BB8FA` dark) via `Theme.illustration.line`. No other colors. The `Theme.illustration.*` namespace is intentionally segregated from the general-purpose color tokens — it exists only to give Brand-4 a usable handle in code without promoting it to a Section 3 token that could be misused outside the illustration context. Konsist asserts that `Theme.illustration.fill` is referenced only from composables under `:core::theme/illustrations/` or from empty-state composables that consume those illustrations.
  - **Geometry** — flat, no gradients, no shadows, no perspective. Single-layer composition; max ~4 distinct shapes per illustration. Vocabulary: stylized pill capsule, calendar grid, simple checkmark, abstract "nothing here yet" geometric arrangement (e.g., a thin-line empty container).
  - **Size** — ~120dp × ~120dp at Compact breakpoint; ~180dp × ~180dp at Expanded breakpoint; centered horizontally above the empty-state heading.
  - **Aspect ratio** — square (1:1) for simplicity and breakpoint-portability.
  - **Theme handling** — each `ImageVector` path is tinted at composition time via `Theme.illustration.fill` and `Theme.illustration.line`, so the same source renders correctly in light and dark themes without separate `drawable-night/` variants or duplicate Compose constants.
  - **Authoring milestone** — illustrations are authored at the M-006 empty-state implementation pass per the `_context/07` Milestone Register; no illustrations exist at file-11 contextualization time. Until M-006 lands, empty states render with the pattern below but without the illustration row (heading + body + action affordance only).

- **Empty-state and placeholder treatment — heading + body + action affordance + optional illustration.** Pattern composition, top-to-bottom:
  1. **Optional illustration row** (~120dp tall at Compact, ~180dp tall at Expanded). Present only after M-006 authors the illustrations; absent before then.
  2. **Heading** in `text.h2`, centered horizontally, `color.text.primary`. One short sentence: "No medications yet", "No doses scheduled today", "Adherence history will appear here".
  3. **Body** in `text.body`, centered horizontally, `color.text.secondary`. One-to-two sentences explaining what the user should do next or what to expect.
  4. **Action affordance** — primary `Button` if the empty state has a single "start here" action (e.g., "Add your first medication" on the empty Medication list; "Open settings" on a state caused by a permission revocation). Omitted when the empty state simply means "wait" (e.g., the empty Adherence history before any Doses are confirmed).

  Vertical centering: the composition centers vertically within the available screen content area below `TopAppBar` and above `NavigationBar` / `NavigationRail`, with a minimum `space.8` (32dp) top spacing. Horizontal padding: `space.6` (24dp) on left and right (one step inside the screen edge padding) to keep the body copy comfortable.

- **Four canonical NemoPill empty states (authored at M-006):**

| Surface | Heading (EN) | Body (EN) | Action |
| --- | --- | --- | --- |
| Empty Medication list (first launch; post-hard-delete) | "No medications yet" | "Tap the + button to add your first medication and set up reminders." | Primary `Button` "Add medication" (mirrors the FAB; reinforces the action affordance for novice Patients who may not associate the FAB with adding). |
| Empty today's-Dose list (Medications exist but none scheduled today) | "No doses scheduled today" | "Doses will appear here on the day they're scheduled. Check back tomorrow morning." | No action. |
| Empty Adherence history (Medications exist but no Confirmations yet) | "Adherence history will appear here" | "Once you've confirmed your first dose, your weekly adherence trend will appear in this view." | No action. |
| Permission-needed empty state (any screen blocked by `Result.Err.ExactAlarmPermissionRevoked` or `Result.Err.NotificationsPermissionRevoked`) | "Permission needed" | One-sentence explanation of the specific permission and what NemoPill cannot do without it (BR-004 reliability rationale). | Primary `Button` "Open settings" (deep-link to the Android Settings screen for the specific permission). |

  Spanish copy for each empty state is authored alongside the English copy at M-006 per the `_context/07_delivery_plan_and_milestones.md` Dependency And Blocker Register's short-bilingual-strings scope. The bilingual strings ship in `res/values/strings.xml` (EN) and `res/values-es/strings.xml` (ES); the `:notifications` and feature modules consume them via `stringResource(R.string.empty_medications_heading)` (no inline literal strings).

## Components

- **Table structure** — each row lists a component's *Variants* (named sub-types), *States* (the runtime conditions the variant occupies), and *Required Behaviors* (the contract every implementation must honor, including token references, accessibility requirements, and any NemoPill-specific overrides of M3 defaults). M3-grounded components reference the underlying Material 3 composable by name; NemoPill-specialized components are NemoPill-authored composables that live in `:core::theme/components/` (when cross-cutting) or in the relevant feature module's `presentation/` directory (when feature-scoped).

- **M3-vs.-NemoPill split** — fourteen rows are M3 primitives with NemoPill-specific tokenization and behavior rules; five rows (**Hard-delete confirmation dialog**, **Biometric-gate Settings group**, **Settings → Privacy Policy screen**, **Defensive Room-migration failure screen**, **Notification (inline action)**) are NemoPill-specialized components whose detailed contracts close out the Section 4 deferrals (vii) (viii) (ix) (x) (xi) (vi) from the T-002 packet.

| Component | Variants | States | Required Behaviors |
| --- | --- | --- | --- |
| `Button` | `primary`, `secondary`, `ghost`, `destructive` | default, focused, pressed, disabled, loading | M3 `Button` (`primary`), `OutlinedButton` (`secondary`), `TextButton` (`ghost`), `Button` with `color.danger` container (`destructive`); label always `text.h3` (weight 700); minimum touch target 48dp; leading icon + label pattern only (no icon-alone); focus ring uses `color.accent` per Section 4 contrast rules; loading state shows centered `CircularProgressIndicator` and disables interaction. |
| `TextField` | `outlined` (default), `filled` (Settings group rows) | default, focused, error, disabled | M3 `OutlinedTextField` / `TextField`; label sits *above* the field (not floating-label-inside) for novice-Patient legibility; helper text below field in `text.body.small`; error text replaces helper text and uses `color.danger`; focused state shows `color.accent` outline (1.5dp); tabular numerals applied via `text.body` for any numeric input (dose count, frequency); `imeAction = Done` on the last field of a form group. |
| `Checkbox` | `default` | unchecked, checked, disabled | M3 `Checkbox`; touch target 48×48dp via `Modifier.minimumInteractiveComponentSize()`; checked state uses `color.accent` fill; label sits to the right at `space.3` (12dp) gap; tap target extends to the whole row (label is tappable). |
| `Switch` | `default` | off, on, disabled | M3 `Switch`; touch target 48×48dp; on-state thumb uses `color.accent` fill; off-state thumb uses `color.text.secondary` fill on `color.border` track; tap target extends to row label. |
| `RadioButton` | `default` | unselected, selected, disabled | M3 `RadioButton`; touch target 48×48dp; selected state uses `color.accent` ring + dot; one selected button per group enforced by single-selection state holder; label tappable per `Checkbox` rule. |
| `Chip` | `assist` (input chip in Medication editor), `filter` (post-MVP candidate) | default, selected, disabled | M3 `AssistChip` / `FilterChip`; selected state uses `color.accent` outline + `color.surface.muted` background; touch target 48dp on the row containing the chip (chip itself may be smaller). |
| `AlertDialog` | `informational`, `destructive` | default | M3 `AlertDialog`; corner radius `radius.xl` (28dp); elevation `elevation.5` (12dp); content padding `space.6` (24dp); two-button row: primary + dismiss; focus moves to primary button on appearance; back-button press triggers dismiss; touch outside dismisses (except for the hard-delete dialog and the Room-migration failure screen, which are explicitly non-dismissible). |
| `ModalBottomSheet` | `default` | default, dragging, expanded | M3 `ModalBottomSheet`; corner radius `radius.xl` (28dp) on top corners only; drag handle on top center; back-button press collapses to dismiss; touch outside collapses to dismiss; primary use: Medication editor (Add / Edit). |
| `Snackbar` | `default`, `with action` | default | M3 `Snackbar` via `Scaffold`'s `SnackbarHost`; duration `Short` (~4s) for informational; `Long` (~10s) when paired with an action; never used to convey error state (errors use inline error text or a dedicated screen surface); copy ≤ 60 characters. |
| `Card` | `default`, `elevated` | default, pressed | M3 `Card` / `ElevatedCard`; corner radius `radius.md` (12dp); elevation `elevation.1` at-rest for default cards, `elevation.2` for elevated cards; pressed state uses `color.surface.muted` background overlay; tap target = the whole card. |
| `List row` | `today's-Dose row`, `Medication row`, `Adherence history row` | default, pressed, retroactive-badge, late-badge | NemoPill composable in `:medication-management::presentation/`, `:scheduling::presentation/`, `:adherence-tracking::presentation/` respectively; row layout = leading icon (`iconSize.default`) + primary text (`text.body`) + supporting text (`text.caption`) + trailing affordance (chevron, status chip, or status badge); minimum row height 56dp at Compact, 64dp at Expanded; full-width tap target; pressed state uses `color.surface.muted` background; retroactive Confirmations show a `color.info` "Logged later" chip; late Reminders fired in the BR-010 1-hour late window show a `color.warning` "Late" chip; missed Doses show a `color.danger` "Missed" chip. Swipe actions deferred post-MVP. |
| `TopAppBar` | `default`, `large` (post-MVP candidate) | default, scrolled-under | M3 `TopAppBar` / `LargeTopAppBar`; title in `text.h1` (`default`) or `text.display` (`large`); navigation icon is back arrow on detail screens, absent on top-level screens; up to 2 action icons via the icon-alone universal-symbol allowlist; `color.surface` background, `color.text.primary` foreground; scrolled-under state lifts to `elevation.2`. |
| `NavigationBar` / `NavigationRail` | `NavigationBar` at Compact / Medium, `NavigationRail` at Expanded | default, with-selection | M3 `NavigationBar` (bottom) / `NavigationRail` (left); 3 MVP destinations: Today, Adherence, Medications; Settings reached via a gear icon in the Today-screen `TopAppBar` (not a 4th nav destination — preserves novice-Patient nav focus on the primary loop); icon weight pairs Outlined-inactive / Filled-active per Section 7; label below icon in `text.caption`. |
| `FloatingActionButton` | `default` (Add Medication only) | default, pressed | M3 `FloatingActionButton`; glyph `Icons.Outlined.Add` at `iconSize.default`; `color.accent` container, `color.surface` foreground; `elevation.3` at-rest, `elevation.4` pressed; bottom-right placement at `space.4` from screen edge; visible only on the Medications screen at MVP. |
| **Hard-delete confirmation dialog** | (specialized `AlertDialog`) | default, checkbox-unchecked (button disabled), checkbox-checked (button enabled) | First tap on "Delete all data" in Settings opens this dialog. Composition top-to-bottom: hero icon `iconSize.hero` (48dp) in `color.danger` (`Icons.Outlined.DeleteForever`); `text.h2` heading "Delete all data?"; `text.body` body paragraph explaining "This will permanently delete every medication, dose, and adherence record on this device. **This cannot be undone.** There is no backup, no export, and no recovery."; below the body, an unchecked `Checkbox` row labeled "I understand this is irreversible"; below the checkbox row, a horizontal `Button` row: `secondary` variant "Cancel" (always enabled) + `destructive` variant "Delete all data" **disabled until the checkbox is checked**; dialog is **non-dismissible** by tap-outside or back-button (Patient must explicitly Cancel); content padding `space.6`; corner radius `radius.xl`; elevation `elevation.5`. The checkbox-gate is the **deliberate-confirmation interaction pattern**; softening this (default-checked, removed checkbox, button always enabled, dismissible-by-tap-outside) requires an ADR amendment with Security-role review per `_context/05` § Data Protection's no-silent-softening commitment. Konsist asserts that the destructive `Button` in this dialog is always paired with a `Checkbox`-gated `enabled` state. |
| **Biometric-gate Settings group** | (specialized form group composable in Settings) | toggle-off (timeout selector hidden), toggle-on (timeout selector visible) | Composition top-to-bottom inside a `Card` with `color.surface.muted` background: row heading "App lock" in `text.h3`; primary `Switch` row labeled "Require biometric or device passcode to open NemoPill" (default OFF per `_context/05` § Authentication); when the switch is ON, an inline timeout-selector group appears below the switch with `RadioButton` rows in a single-selection group: **"After 1 minute" / "After 5 minutes" (default selected when switch first turns ON) / "After 15 minutes" / "After 1 hour" / "Never re-prompt while app is open"**; selecting a timeout updates the persisted preference immediately (no Save button); appearance of the timeout selector when the switch flips on uses `motion.standard` per Section 9. Each row's touch target is 48dp; the whole row (label + control) is tappable. Underlying biometric prompt uses `androidx.biometric.BiometricPrompt` per `_context/05` § Authentication; if biometric is unavailable on the device, the prompt falls back to device passcode (PIN / pattern / password) via `BiometricPrompt.Builder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)`. |
| **Settings → Privacy Policy screen** | (specialized screen composable in `:app::presentation/`) | content-loaded, error-fallback (raw file missing — defensive only) | Composition top-to-bottom: `TopAppBar` with back arrow + title "Privacy Policy" (`text.h1`); below the bar, a sticky horizontal `Row` of TOC chips (`AssistChip` for each top-level Markdown `H1` heading) that scrolls horizontally if it overflows; below the TOC, the rendered Markdown body in a `LazyColumn` consuming Section 5 typography (`H1 → text.h1`, `H2 → text.h2`, `H3 → text.h3`, paragraph → `text.body`, code → `text.code`); body container applies `Modifier.widthIn(max = 600.dp)` and centers horizontally on Expanded breakpoint per Section 6. Tapping a TOC chip scrolls to the corresponding section via `LazyListState.animateScrollToItem()` with `motion.long` duration per Section 9. **Locale resolution**: at composition entry, read `LocalConfiguration.current.locales`; if first match is Spanish (`es-*`) AND `res/raw-es/privacy.md` exists → load it; otherwise load `res/raw/privacy.md`. Both files are APK-bundled per `_context/12` § Section 6; no network fetch ever occurs. If neither file exists (defensive — should be impossible given `_context/12`'s `copyPrivacyPolicyAssets` Gradle task), the error-fallback state renders "Privacy policy unavailable — please reinstall NemoPill" with no other actions. |
| **Defensive Room-migration failure screen** | (specialized full-screen composable, mounted before the normal navigation tree) | default | Replaces all normal app screen content after a failed migration is detected during `NemoPillDatabase` instantiation per `_context/12` § Section 5's defensive `try/catch` wrapper. Composition centered vertically on `color.surface`: hero icon `iconSize.hero` (48dp) in `color.warning` (`Icons.Outlined.SystemUpdate`); `space.6` vertical gap; `text.h1` heading "Update required" centered; `space.4` gap; `text.body` body paragraph centered with `Modifier.widthIn(max = 600.dp)`: "NemoPill needs to update its data store. The latest version of NemoPill on the Play Store includes this update. Please update NemoPill to continue."; `space.8` gap; primary `Button` `primary` variant labeled "Open Play Store" that fires `Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))` with browser fallback `https://play.google.com/store/apps/details?id=$packageName`; below the button at `space.3` gap, `text.body.small` supporting text in `color.text.secondary`: "If the problem persists, please reinstall NemoPill." Screen is **non-dismissible**: back-button is consumed (no-op); there is no `TopAppBar`; the screen is rendered before the rest of the navigation tree mounts so it is independent of any feature module. |
| **Notification (inline action)** | `reminder_on_time`, `reminder_late`, `reminder_missed`, `reminder_summary` | default | Android system `Notification` constructed via `NotificationCompat.Builder`; per-channel ID and importance pinned in `_context/06` § Section 2; for `reminder_on_time` and `reminder_late` channels, the notification includes two inline action buttons via `NotificationCompat.Action`: **"Take"** (primary; `PendingIntent` invokes `ConfirmDoseUseCase` with `taken` status) and **"Skip"** (secondary; `PendingIntent` invokes `ConfirmDoseUseCase` with `skipped` status); both `PendingIntent` instances use `FLAG_IMMUTABLE` per `_context/05` § OS permissions and `_context/06` § Trust Boundaries; tapping the notification body (outside the action buttons) opens NemoPill to the today's-Dose list scrolled to the relevant Dose row. The `reminder_missed` channel and `reminder_summary` channel do not include inline actions — `reminder_missed` is silent and informational per BR-010; `reminder_summary` opens to the today's-Dose list when tapped. Channel display-name and notification copy strings are authored in Section 10 Content And Voice. |

## Motion And Interaction

- **Motion principles — restrained and functional.** Motion in NemoPill communicates causality: where a screen came from (slide-in-from-end), where a dialog returns to (fade + scale-down), why the biometric Settings timeout selector appeared (slide-down from the switch row). Motion never exists for its own sake; decorative animation (bouncing pill icons, Adherence-streak celebrations, parallax scrolling, hero-element morphs) is out of scope at MVP. Rationale: the file-03 novice-Patient persona benefits from predictable, low-cognitive-load transitions; the file-03 glanceable + battery-tolerant environment constraints are degraded by long or complex animation; the `_context/05` § Snapshot baseline of 20 captures is more deterministic when transitions are short and use M3-default easings. A future revision that promotes NemoPill to more expressive motion is an ADR amendment in `_context/09_decision_log.md`.

- **Standard durations and easings — three duration tokens + three named easings, exposed via `:core::theme/Motion.kt`.**

| Token group | Token name | Value | Use |
| --- | --- | --- | --- |
| Duration | `motion.fast` | `100ms` | Instant-feedback responses — ripple onset, focus-ring fade-in, checkbox check / uncheck, switch thumb-position transition, radio-button dot transition. |
| Duration | `motion.standard` | `200ms` | Default for component-state changes — modal dialog appearance, dropdown reveal, biometric Settings timeout selector reveal (per Section 8), snackbar arrival / departure, `TopAppBar` scroll-under elevation lift. |
| Duration | `motion.long` | `400ms` | Screen-level transitions — programmatic `LazyListState.animateScrollToItem()` for the Privacy Policy TOC tap (per Section 8), Compose Navigation forward / back transitions, `SupportingPaneScaffold` two-pane reflow at Expanded breakpoint. |
| Easing | `easing.standard` | `FastOutSlowInEasing` | General in-out motion; the default for any animation that both enters and exits, like a dialog that mounts and unmounts. |
| Easing | `easing.enter` | `LinearOutSlowInEasing` | Elements entering the screen — the biometric timeout selector appearing, a snackbar arriving from off-screen, a screen sliding in. |
| Easing | `easing.exit` | `FastOutLinearInEasing` | Elements exiting the screen — a dialog dismissing, a snackbar leaving, a screen sliding out. |

  Compose code consumes these via `tween(durationMillis = Motion.standard, easing = Motion.easing.standard)` or `animateContentSize(animationSpec = tween(Motion.standard))`. Konsist asserts no production composable passes a literal `Int` duration to `tween()`, `spring()`, `snap()`, `animateContentSize()`, or similar `animationSpec` builders where the literal matches a named `motion.*` token value (so `tween(200)` is a violation when `motion.standard = 200ms`); the rule has the same edge cases as the Section 6 spacing-literal rule and is captured under the same arch-conformance suite.

- **Reduced-motion rule — honor Android's system reduced-motion setting fully, including duration scaling.** Implementation contract:
  - `:core::theme/Motion.kt` reads two system signals on app start and on every `Configuration` change: (i) whether the OS has animations enabled at all (via Compose's `LocalAccessibilityManager.current` canonical accessor — exact API surface confirmed at M-001 implementation time); (ii) the `ANIMATOR_DURATION_SCALE` float value from `Settings.Global.getFloat("animator_duration_scale", 1.0f)`.
  - Both are exposed via a `LocalReducedMotion` `CompositionLocal` with fields `shouldAnimate: Boolean` (the binary OS toggle) and `scale: Float` (the float multiplier, typically 1.0, 0.5, or 0.0).
  - Every NemoPill animation multiplies its declared duration by the scale: `effectiveDurationMillis = (Motion.standard * scale).coerceAtLeast(0).toInt()`. A Patient who has set the system animation scale to 0.5× receives NemoPill animations at half duration; a Patient who has set it to 0× receives no animation but the end state still applies (the dialog still mounts, the timeout selector still appears, the Privacy Policy still scrolls to the tapped section — just without animated transition).
  - When `shouldAnimate == false`, every motion duration collapses to 0ms regardless of the underlying scale. The animation still completes (the end state is reached immediately) so the UI never enters a partial-transition state.
  - The `LocalReducedMotion` value is keyed off the `Configuration` so Compose recomposes the affected subtree on system-setting change without requiring a process restart.
  - The Section 4 snapshot baseline of 20 captures is recorded with the default scale (1.0) and `shouldAnimate = true`; reduced-motion behavior is verified by a separate (non-baseline) Compose UI test that exercises a representative composable at `scale = 0.0` and asserts the end state matches the animated equivalent at `t = ∞`.

- **Implicit transition rules** locked here without further deliberation (M3 defaults at the Section 9 tokens; no NemoPill-specific judgment calls):
  - **Modal dialog mount / unmount** — fade + scale (0.85 → 1.0 on mount; 1.0 → 0.85 on unmount); duration `motion.standard`; easing `easing.standard`. Applies to `AlertDialog`, the hard-delete confirmation dialog, and the Room-migration failure screen's *first appearance* (subsequent screen state is static).
  - **Bottom sheet slide-up / slide-down** — translateY from full-height to anchor position; duration `motion.standard`; easing `easing.enter` on appearance, `easing.exit` on dismissal. Applies to `ModalBottomSheet` (Medication editor).
  - **Compose Navigation forward / back transitions** — horizontal slide; forward enter via `easing.enter` from-end; forward exit via `easing.exit` to-start; back enter via `easing.enter` from-start; back exit via `easing.exit` to-end; duration `motion.long`.
  - **Snackbar arrival / departure** — translateY from below-screen on arrival, to below-screen on departure; duration `motion.standard`; easing `easing.enter` on arrival, `easing.exit` on departure.
  - **List row press ripple** — M3 default ripple via `LocalRippleConfiguration`; ripple onset duration `motion.fast`, ripple fade duration `motion.standard`; uses `color.accent` with default M3 alpha (~12% on pressed).
  - **Checkbox / switch / radio state change** — M3 default state-change animation; duration `motion.fast` for the indicator transition.
  - **`TopAppBar` scroll-under elevation lift** — `elevation.0 → elevation.2` transition; duration `motion.standard`; easing `easing.standard`.
  - **Screen-level reduced-motion overrides** apply to every implicit transition above; when `LocalReducedMotion.shouldAnimate == false`, all of these collapse to 0ms instantly.

- **Interaction patterns not animated at MVP**: pull-to-refresh, drag-and-drop list reordering, swipe-to-delete on list rows, shared-element transitions across navigation destinations, animated illustrations in empty states, animated chart transitions on the Adherence history screen. Each is deferred to a post-MVP milestone and would require its own design + motion-token revisit. Adding any of them is an ADR amendment in `_context/09_decision_log.md`.

## Content And Voice

- **Tone — direct and warm.** NemoPill speaks plainly and supportively, without medical jargon and without performative cheerfulness. The Patient is a competent adult managing their own treatment; copy assumes this and avoids both clinical coldness ("Patient, your medication regimen requires confirmation") and infantilizing chirpiness ("Yay! You took your pill!"). When something goes wrong, the copy explains what happened in concrete terms and tells the Patient exactly what to do next.

- **Capitalization — sentence case throughout.** Buttons, headings, labels, list items, dialog titles, notification titles, and body copy all use sentence case. "Add medication", not "Add Medication". "Today's doses", not "Today's Doses". "Delete all data", not "Delete All Data". Exceptions: the brand name **NemoPill** is always capitalized as written regardless of position; proper nouns (medication brand names entered by the Patient) preserve whatever case the Patient typed; acronyms (TalkBack, USA, CCPA, LGPD) preserve their canonical case.

- **Punctuation in UI:**
  - **Buttons, chip labels, and inline labels** — no trailing period. "Delete all data", "Take", "Open settings", "Add medication".
  - **Body paragraphs and multi-sentence helper text** — trailing period on every sentence. "This cannot be undone. There is no backup, no export, and no recovery."
  - **Single-sentence headings and notification titles** — no trailing period unless interrogative. "Delete all data?" is allowed; "Delete all data." is not.
  - **Lists** — no terminal punctuation on bulleted items unless the item is a complete sentence.
  - **Oxford comma** — yes. "Medications, doses, and history."
  - **Em-dash** — occasional, no surrounding spaces. "This cannot be undone—there is no backup."
  - **Quotation marks** — straight double quotes (`"`) for inline quotes; never curly quotes (Roboto's smart-quote glyphs differ between text rendering passes and produce snapshot drift).
  - **Numbers** — numerals for everything. "16 medications", "8:00 AM", "78 %", "1 hour". Never spelled out in body copy.

- **Error message pattern — three-part: "What happened. Why. What to do next."** Each part is a separate sentence. The pattern maps to the closed `Result.Err` catalog from `_context/06_integrations_data_flow_and_boundaries.md` § Error Handling; every sub-type has a NemoPill-authored copy string in both English and Spanish. The no-Patient-data-in-failure-evidence rule from `_context/06` applies to log lines and (forthcoming) crash reports; Patient-visible error message strings shown in the UI reference concepts abstractly ("this dose", "this reminder") rather than naming specific drugs or schedules.

| `Result.Err` sub-type | EN copy | ES copy |
| --- | --- | --- |
| `ValidationFailed` (BR-002 / BR-003) | "We can't save this medication. Some details aren't valid. Check the highlighted fields and try again." | "No podemos guardar este medicamento. Algunos detalles no son válidos. Revise los campos resaltados y vuelva a intentarlo." |
| `RetroactiveWindowExpired` (BR-011) | "We can't log this dose now. The 24-hour window to log it retroactively has passed. You can still see it as missed in your history." | "No podemos registrar esta dosis ahora. El plazo de 24 horas para registrarla retroactivamente ha expirado. Aún puede verla como no tomada en su historial." |
| `ExactAlarmPermissionRevoked` (F-001 / F-011) | "Reminders can't be scheduled. NemoPill needs the exact-alarm permission to fire reminders at the right time. Open settings to grant the permission." | "No podemos programar recordatorios. NemoPill necesita el permiso de alarmas exactas para enviar recordatorios a tiempo. Abra los ajustes para conceder el permiso." |
| `NotificationsPermissionRevoked` (F-005) | "Reminders can't be shown. NemoPill needs notification permission to alert you when it's time for a dose. Open settings to grant the permission." | "No podemos mostrar recordatorios. NemoPill necesita el permiso de notificaciones para avisarle cuando sea hora de una dosis. Abra los ajustes para conceder el permiso." |
| `UnknownDose` (F-006 lookup) | "We can't find this dose. It may have been deleted or the schedule changed. Open today's doses to see what's currently scheduled." | "No podemos encontrar esta dosis. Es posible que haya sido eliminada o que el horario haya cambiado. Abra las dosis de hoy para ver lo que está programado actualmente." |
| `UnexpectedNotificationPayload` (F-006 defensive parse) | "Something went wrong with this reminder. NemoPill received unexpected data. Open today's doses to log this dose manually." | "Algo salió mal con este recordatorio. NemoPill recibió datos inesperados. Abra las dosis de hoy para registrar esta dosis manualmente." |
| `Unexpected` (catch-all) | "Something went wrong. NemoPill wasn't able to complete that action. Try again, or restart NemoPill if the problem continues." | "Algo salió mal. NemoPill no pudo completar esa acción. Vuelva a intentarlo, o reinicie NemoPill si el problema persiste." |

- **Localization-ready labeling:**
  - Every Patient-visible string lives in `res/values/strings.xml` (English) and `res/values-es/strings.xml` (Spanish). No inline literal strings in Compose code; Konsist asserts no `Text(...)` composable receives a literal `String` parameter (the parameter must be `stringResource(R.string.x)` or `pluralStringResource(R.plurals.x, count)`).
  - **Brand-locked strings (never translated):** the brand name "NemoPill". The medication names entered by the Patient are preserved as typed (not translated even if they happen to match a Spanish word).
  - **Plural and gender variations:** Android's `<plurals>` resource for counts ("1 reminder" vs. "3 reminders"); Spanish `<plurals>` rules differ from English and use the `androidx.core.os.LocaleListCompat` selector.
  - **Spanish formality register:** formal ("usted"). Every Spanish translation uses `usted`-conjugated verbs ("Tome", "Su medicación", "¿Está seguro?"), never `tú`-conjugated. A future revision that switches to `tú` requires an ADR and re-translation of every Spanish string.
  - **Long-form Privacy Policy:** English substance is authored below in this Section 10. Spanish translation of the long-form Privacy Policy body is M-006 close work per `_context/07_delivery_plan_and_milestones.md` Dependency And Blocker Register's Spanish-translation-capacity row. Short bilingual strings (notification copy, error messages, dialog copy, button labels, navigation labels, headings, empty states, settings labels) are authored bilingually in this section now.

### Notification channel display names and descriptions

Channel IDs are pinned in `_context/06_integrations_data_flow_and_boundaries.md` § Section 2; the strings below are the Patient-visible names and one-line descriptions that surface in Android's `Settings → Apps → NemoPill → Notifications` screen.

| Channel ID | EN display name | EN description | ES display name | ES description |
| --- | --- | --- | --- | --- |
| `reminder_on_time` | "Dose reminders" | "Alerts at the time each dose is scheduled." | "Recordatorios de dosis" | "Avisos a la hora programada para cada dosis." |
| `reminder_late` | "Late reminders" | "Alerts when a dose is overdue but still within the 1-hour window." | "Recordatorios tardíos" | "Avisos cuando una dosis está atrasada pero aún dentro del plazo de 1 hora." |
| `reminder_missed` | "Missed dose notes" | "Silent notes when a dose was missed." | "Notas de dosis no tomadas" | "Notas silenciosas cuando no se tomó una dosis." |
| `reminder_summary` | "Reminder summaries" | "Combined summary of multiple reminders received while the device was off." | "Resúmenes de recordatorios" | "Resumen combinado de varios recordatorios recibidos mientras el dispositivo estaba apagado." |

### BR-010 notification copy

Title and body per channel variant, with template placeholders `{medication-name}`, `{dose}`, `{time}`, and `{n}` resolved at notification-build time via the `:notifications` module's `NotificationFactory`. `{time}` is rendered using the Patient's device locale time format (`8:00 AM` in `en-US`; `08:00` in `es-MX` if the locale uses 24-hour format). `{n}` is rendered using Android `<plurals>` resources.

| Variant | EN title | EN body | ES title | ES body |
| --- | --- | --- | --- | --- |
| `reminder_on_time` | "Take {medication-name}" | "{dose} at {time}" | "Tome {medication-name}" | "{dose} a las {time}" |
| `reminder_late` | "Late: take {medication-name}" | "Scheduled for {time}, still within the 1-hour window." | "Atrasado: tome {medication-name}" | "Programado para las {time}, aún dentro del plazo de 1 hora." |
| `reminder_missed` | "Missed dose: {medication-name}" | "Scheduled for {time}. Tap to log retroactively if you took it." | "Dosis no tomada: {medication-name}" | "Programado para las {time}. Toque para registrar retroactivamente si la tomó." |
| `reminder_summary` | "{n} reminders while away" | "Tap to review." | "{n} recordatorios mientras estuvo ausente" | "Toque para revisar." |

Inline action button labels: **"Take"** / **"Skip"** (EN); **"Tomar"** / **"Saltar"** (ES). The `reminder_missed` and `reminder_summary` notifications do not include inline actions per BR-010.

### Hard-delete confirmation dialog copy

The structural composition is pinned in Section 8 (specialized `AlertDialog` with the checkbox-gated destructive button). The Patient-visible strings:

| Element | EN | ES |
| --- | --- | --- |
| Heading | "Delete all data?" | "¿Eliminar todos los datos?" |
| Body | "This will permanently delete every medication, dose, and adherence record on this device. **This cannot be undone.** There is no backup, no export, and no recovery." | "Esto eliminará permanentemente todos los medicamentos, dosis y registros de adherencia en este dispositivo. **No se puede deshacer.** No hay copia de seguridad, exportación ni recuperación." |
| Checkbox label | "I understand this is irreversible" | "Entiendo que esto es irreversible" |
| Cancel button | "Cancel" | "Cancelar" |
| Destructive button | "Delete all data" | "Eliminar todos los datos" |
| Success snackbar (post-delete) | "All data cleared." | "Todos los datos han sido eliminados." |

### Biometric-gate Settings group copy

The structural composition is pinned in Section 8 (switch + radio-button timeout selector). The Patient-visible strings:

| Element | EN | ES |
| --- | --- | --- |
| Group heading | "App lock" | "Bloqueo de la aplicación" |
| Switch label | "Require biometric or device passcode to open NemoPill" | "Requerir autenticación biométrica o contraseña del dispositivo para abrir NemoPill" |
| Timeout option 1 | "After 1 minute" | "Después de 1 minuto" |
| Timeout option 2 (default selected) | "After 5 minutes" | "Después de 5 minutos" |
| Timeout option 3 | "After 15 minutes" | "Después de 15 minutos" |
| Timeout option 4 | "After 1 hour" | "Después de 1 hora" |
| Timeout option 5 | "Never re-prompt while app is open" | "No volver a pedir mientras la aplicación esté abierta" |
| Biometric prompt subtitle | "Authenticate to continue using NemoPill" | "Autentíquese para continuar usando NemoPill" |
| Fallback-to-passcode subtitle | "Use your device passcode" | "Usar la contraseña del dispositivo" |

### Defensive Room-migration failure screen copy

| Element | EN | ES |
| --- | --- | --- |
| Heading | "Update required" | "Actualización requerida" |
| Body | "NemoPill needs to update its data store. The latest version of NemoPill on the Play Store includes this update. Please update NemoPill to continue." | "NemoPill necesita actualizar su almacén de datos. La última versión de NemoPill en Play Store incluye esta actualización. Por favor, actualice NemoPill para continuar." |
| Primary button | "Open Play Store" | "Abrir Play Store" |
| Supporting text | "If the problem persists, please reinstall NemoPill." | "Si el problema persiste, por favor reinstale NemoPill." |

### Empty-state copy

The four canonical empty states from Section 7, with Spanish translations:

| Empty state | EN heading | EN body | EN action | ES heading | ES body | ES action |
| --- | --- | --- | --- | --- | --- | --- |
| Empty Medication list (first launch; post-hard-delete) | "No medications yet" | "Tap the + button to add your first medication and set up reminders." | "Add medication" | "Aún no hay medicamentos" | "Toque el botón + para añadir su primer medicamento y configurar recordatorios." | "Añadir medicamento" |
| Empty today's-Dose list | "No doses scheduled today" | "Doses will appear here on the day they're scheduled. Check back tomorrow morning." | (no action) | "No hay dosis programadas hoy" | "Las dosis aparecerán aquí el día en que estén programadas. Vuelva a verificar mañana por la mañana." | (no action) |
| Empty Adherence history | "Adherence history will appear here" | "Once you've confirmed your first dose, your weekly adherence trend will appear in this view." | (no action) | "El historial de adherencia aparecerá aquí" | "Una vez que confirme su primera dosis, su tendencia semanal de adherencia aparecerá en esta vista." | (no action) |
| Permission-needed (exact-alarm) | "Permission needed" | "NemoPill needs the exact-alarm permission to fire reminders at the right time." | "Open settings" | "Permiso necesario" | "NemoPill necesita el permiso de alarmas exactas para enviar recordatorios a tiempo." | "Abrir ajustes" |
| Permission-needed (notifications) | "Permission needed" | "NemoPill needs notification permission to alert you when it's time for a dose." | "Open settings" | "Permiso necesario" | "NemoPill necesita el permiso de notificaciones para avisarle cuando sea hora de una dosis." | "Abrir ajustes" |

### Settings → Privacy Policy screen chrome

| Element | EN | ES |
| --- | --- | --- |
| Top app bar title | "Privacy Policy" | "Política de privacidad" |
| Error-fallback (raw file missing — defensive only) | "Privacy policy unavailable — please reinstall NemoPill." | "Política de privacidad no disponible — por favor reinstale NemoPill." |
| Settings entry-row label | "View Privacy Policy" | "Ver política de privacidad" |

### English Privacy Policy substance

The English Privacy Policy text below is the canonical substance for the file `legal/PRIVACY_POLICY.en.md` and the APK-bundled `res/raw/privacy.md` per `_context/12_environments_and_devops.md` § Section 6. The Spanish translation of this long-form body is M-006 close work per `_context/07_delivery_plan_and_milestones.md` Dependency And Blocker Register's Spanish-translation-capacity row. Each section header below maps to a top-level Markdown `H1` in the source file and to a TOC chip on the in-app Privacy Policy screen per Section 8.

#### About this policy

This Privacy Policy describes how NemoPill, an Android medication reminder app, handles your personal information. NemoPill is designed to operate entirely on your Android device. This policy applies to all versions of NemoPill distributed through Google Play. NemoPill is developed and maintained by Isidro Rodriguez, an independent developer.

#### What information NemoPill collects

NemoPill stores the information you enter into the app, on your device only. This includes:

- The medications you choose to track, including the name, dosage strength, form (tablet, capsule, etc.), and the dose quantity you take.
- The schedules you set, including frequency, time of day, and start and end dates.
- The dose confirmations you record — whether you took, skipped, or missed each dose, and the time you confirmed it.
- Your reminder permission states and notification preferences as configured in Android Settings.
- Your app lock preferences (whether the biometric-or-passcode gate is enabled, and the inactivity timeout you've chosen).

NemoPill does not require an account, an email address, a phone number, or any other personally identifying information to function.

#### What information NemoPill does NOT collect

NemoPill does not collect:

- Your name, age, gender, address, or contact details.
- Analytics events about how you use the app.
- Crash reports, usage statistics, or performance telemetry.
- Location data — NemoPill does not request the location permission.
- Your device identifier or advertising ID.
- Your contacts, photos, files, or any other personal data outside the medication information you choose to enter.

#### How your data is stored

All information you enter is stored locally in a database file inside NemoPill's private application directory on your device. This directory is sandboxed by Android — no other app can read or write to it without root privileges, which Android does not grant by default. The database is not backed up to Google Drive, Google One, or any other cloud service. NemoPill explicitly disables Android Auto Backup in its app manifest to prevent silent cloud transmission.

#### How your data is transmitted

NemoPill does not transmit any data off your device. NemoPill does not request the internet permission in its app manifest. NemoPill does not contain any network code, third-party SDKs, analytics libraries, or advertising libraries. NemoPill has no servers, no application programming interface (API), and no back-end. If a future version of NemoPill were ever to require network access, it would require a new Android permission grant from you, and you would receive a Play Store update notice explaining the change.

#### How your data is shared

NemoPill does not share, sell, or transmit your data to anyone. There are no third parties involved in NemoPill's operation. NemoPill does not have business partners, advertisers, or service providers that receive your data. NemoPill does not sell or rent your data — there is nothing to sell or rent, because no data leaves your device.

#### Your rights and choices

You have complete control over your NemoPill data:

- You can view and edit any medication, schedule, or dose record from within the app.
- You can delete any single medication or schedule, or archive a medication you've stopped taking.
- You can permanently delete all your NemoPill data via Settings → Delete all data. This action is irreversible and removes every medication, schedule, dose record, and preference from your device.
- Uninstalling NemoPill achieves the same result — all NemoPill data is removed from your device when the app is uninstalled, because Android automatically deletes an app's private data on uninstall and because NemoPill does not use Android Auto Backup.

Because NemoPill stores no data outside your device, there is no separate "data export" feature — your data is already, and only, on your device.

#### Data retention

NemoPill retains your data on your device only as long as you have NemoPill installed and have not used the "Delete all data" option. NemoPill does not enforce any retention limit on its own; your medication history, dose confirmations, and adherence records remain on your device until you choose to delete them or uninstall the app. There are no automatic deletions, expirations, or archives.

#### Children's privacy

NemoPill is designed for adults managing their own medication treatment. NemoPill does not knowingly collect information from children under 13, or under the equivalent minimum age in your jurisdiction. If you are a parent or guardian and you believe a child has used NemoPill, you can delete all their data using the "Delete all data" option in Settings.

#### Security

NemoPill relies on Android's built-in app sandboxing and on the device-level security you've configured (screen lock, biometric authentication, or device passcode). Within NemoPill, you can optionally enable a biometric-or-passcode gate via Settings → App lock for an additional layer of protection; this gate requires you to authenticate before opening the app. NemoPill's database is not separately encrypted at the file level; it relies on the same protection as every other private app data file on your device. If your device is unlocked and someone has physical access to it, they can open NemoPill and see your medication data unless the app lock is enabled.

#### Your rights under specific data protection laws

**California Consumer Privacy Act (CCPA) and California Privacy Rights Act (CPRA):** Because NemoPill does not collect personal information off your device and does not transmit, share, or sell data, the standard CCPA / CPRA rights are satisfied by the app's design: the right to know what information is collected is satisfied by the app's own visibility of your data; the right to delete is satisfied by Settings → Delete all data; the right to opt out of sale is satisfied by the fact that no data is sold; the right to non-discrimination applies because NemoPill does not differentiate service based on privacy choices, as no privacy choices are required to use the app.

**Latin American national data-protection laws (Mexico LFPDPPP, Argentina PDPL, Chile Ley 19.628, Colombia Ley 1581, Peru Ley 29733, Uruguay Ley 18.331, and equivalent laws in the distribution region):** NemoPill acts as both the data controller and the data processor for your data. Your data is processed solely to provide medication reminders. You have the right of access (your data is visible to you within the app at all times), the right of rectification (you can edit any data within the app), the right of cancellation (Settings → Delete all data, or uninstall the app), and the right of opposition (uninstall the app to stop processing). No international data transfer occurs because no data is transferred at all.

#### Changes to this policy

Changes to this Privacy Policy are recorded in the version history of the NemoPill source repository at `https://github.com/izirodriguez/NemoPill/blob/main/legal/PRIVACY_POLICY.en.md`. When a change affects how NemoPill handles your data, the in-app Privacy Policy screen displays the updated text after you install the relevant app update. Earlier versions of this policy remain available in the public repository version history.

#### Contact

If you have questions or concerns about this Privacy Policy or about NemoPill's data practices, please open an issue on the public NemoPill repository at `https://github.com/izirodriguez/NemoPill/issues`. Because GitHub Issues are publicly visible, please do not include personal medical information in your issue. If your concern requires sharing personal information, please indicate so in your issue and arrangements will be made to continue the conversation through a private channel.

### Spanish Privacy Policy translation (deferred to M-006)

The Spanish translation of the long-form Privacy Policy substance above (every section under "English Privacy Policy substance") is **deferred to M-006 close** per `_context/07_delivery_plan_and_milestones.md` Dependency And Blocker Register's Spanish-translation-capacity row. The M-006 task will translate the canonical English text above into `legal/PRIVACY_POLICY.es.md` and the APK-bundled `res/raw-es/privacy.md`. The translation must preserve every substantive claim, every right enumeration under CCPA / CPRA and the Latin American national data-protection laws section, and the GitHub Issues contact path; copy authoring is plain-language disclosure of facts already pinned in `_context/05_engineering_quality_security_and_compliance.md` and `_context/06_integrations_data_flow_and_boundaries.md`, not a legal-craft exercise. The Spanish translation uses the formal `usted` register per the localization-ready labeling rule above. Until M-006 lands, the in-app Privacy Policy screen's locale-resolution falls back to the English `res/raw/privacy.md` for Spanish-locale Patients (no error; just English content displayed); this defensive behavior is documented in Section 8's Settings → Privacy Policy screen row.

## Theming And Customization

- **Supported themes — light and dark only; no high-contrast variant.** Decision pinned in Section 4 Color System. The two themes are NemoPill-authored against the locked Section 3 design tokens; light theme uses light-value cells, dark theme uses dark-value cells, and `Theme.colors` resolves the active value at composition time. Rationale recap: Section 3 values already exceed WCAG AA across the board with primary text at AAA, removing the need for a separate high-contrast theme to serve low-vision Patients; the `_context/05_engineering_quality_security_and_compliance.md` § Snapshot baseline of 20 captures is calibrated against exactly two themes. Adding a third theme (high-contrast, brand variant, or seasonal) requires an ADR in `_context/09_decision_log.md` that explicitly addresses the snapshot baseline expansion to 32+ captures and the compositional cost across every component.

- **Theme switching mechanism — follows Android `Configuration.uiMode`.** Decision pinned in Section 4 Color System. NemoPill reads `Configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK` at composition root via Compose's `isSystemInDarkTheme()`; the result drives `Theme.colors` selection. There is **no in-app theme picker** — Patients adjust the active theme via the Android system dark-mode toggle (Settings → Display → Dark theme, or the quick-settings tile). Rationale: the OS-level toggle is the single source of truth and applies to every app the Patient runs; adding an in-app override would create a "manual override" state that diverges from system preference, doubles the snapshot matrix to handle manual-light-on-dark-system and manual-dark-on-light-system combinations, and contradicts the novice-Patient principle of not surfacing decisions that do not materially matter. System-level accessibility amplifiers (`Settings.Secure.HIGH_TEXT_CONTRAST_ENABLED` on Android 13+, font-scale, animation-scale, color inversion) are honored implicitly by Material 3 components without NemoPill-authored overrides.

- **Customizable surfaces — minimal.** The only Patient-customizable surfaces inside NemoPill are:

| Surface | Where customized | Affects |
| --- | --- | --- |
| Biometric / passcode app lock | NemoPill Settings → App lock (Section 8 biometric Settings group) | Whether NemoPill requires authentication on app open; the inactivity timeout window (1 / 5 / 15 / 60 minutes / Never re-prompt while app is open). Default: lock OFF. |
| Per-channel notification importance, sound, and vibration | NemoPill Settings → Notifications → [channel name] (deep-link to system per-channel notification settings via `Intent.ACTION_CHANNEL_NOTIFICATION_SETTINGS`) | Per-channel: alert importance (urgent / high / medium / low / none), sound, vibration, lock-screen visibility. NemoPill sets channel defaults at creation time per `_context/06_integrations_data_flow_and_boundaries.md` § Section 2; the Patient can adjust each channel independently. |

  The **Settings → Notifications** screen is a NemoPill-authored screen with four rows (one per channel ID); each row shows the channel display name authored in Section 10, a one-line current-state description read from `NotificationManager.getNotificationChannel(channelId).importance` and translated to the localized importance vocabulary, and a trailing chevron that fires the per-channel deep-link via:

```kotlin
val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
}
startActivity(intent)
```

  When the Patient returns to NemoPill after adjusting channel settings, the in-app Settings → Notifications screen re-reads the channel importance from `NotificationManager` and updates each row's description on resume; no manual refresh affordance is needed. The screen uses `Card` rows from Section 8 with the channel display name in `text.body`, the current-state description in `text.caption`, and an `Icons.Outlined.ChevronRight` affordance at `iconSize.default`.

  Per-medication and per-schedule data (Medication name, dosage, frequency, times of day) is editable by the Patient via the Medication editor `ModalBottomSheet` per Section 8, but that is *feature data*, not a *customization surface* in the design-system sense — it is listed here only to clarify the boundary so a future revision does not conflate the two.

- **Surfaces that are NOT customizable inside NemoPill.** Listed explicitly so a future revision does not accidentally promote one to customizable without an ADR:
  - Color palette, semantic role mapping, contrast ratios (Section 4 locked).
  - Typography family, type scale, weights, numeric rules, line-length measure (Section 5 locked).
  - Spacing scale, radius scale, elevation scale, z-index ramp (Section 3 locked).
  - Iconography library, icon size tokens, illustration accent colors (Section 7 locked).
  - Component variants, states, required behaviors (Section 8 locked).
  - Motion duration tokens, easings, reduced-motion behavior (Section 9 locked; the Patient adjusts reduced-motion via Android system settings, which NemoPill honors automatically).
  - Active theme (Section 4 + this section: follows OS).
  - Text content of any Patient-visible string, including notification copy and Privacy Policy substance (Section 10 locked; changes require an ADR per the Brand owner rule in Section 2 Applicability).
  - Notification channel **IDs** (pinned in `_context/06_integrations_data_flow_and_boundaries.md` § Section 2 — renaming an ID loses Patient customizations). Notification channel **importance / sound / vibration** is Patient-customizable per the row above; the channel **ID** is the contract.
  - System-level accessibility amplifiers (font scale, animation scale, color inversion, high-contrast text) — these are honored automatically by Material 3 components and Compose's `Density` system; NemoPill does not expose per-app overrides because the OS-level setting already applies app-wide.

## Design System Risks And Trade-Offs

### Accepted gaps

These are areas where the design system is intentionally minimal or opinionated. Each gap is a deliberate trade-off, not an oversight; reopening any of them requires an ADR in `_context/09_decision_log.md` with the reopen-trigger condition called out explicitly.

- **`color.border` fails WCAG 1.4.11 contrast for non-text UI components.** The light value `#D4D4D8` sits at 1.6:1 against `color.surface`; dark `#3F3F46` at 1.4:1. Both fail the WCAG 1.4.11 ≥ 3:1 floor. Compensating control documented in Section 4 Color System: no element relies on `color.border` alone to convey meaning — form-field inputs are paired with labels and a `color.accent` focus ring (which passes 1.4.11); list-row dividers are paired with `space.3` vertical spacing that itself communicates the row boundary; card borders are paired with `elevation.1` shadow that communicates the surface boundary. **Reopen-trigger:** a future component design that requires `color.border` to carry meaning standalone without spacing, focus ring, or elevation backup.

- **`color.info` light value `#0277BD` is brand-adjacent, not canonical brand-palette.** None of the four canonical brand colors (`#2C5EAD`, `#1591DC`, `#4BB8FA`, `#C4E2F5`) satisfy WCAG AA normal text on `color.surface` light; `#0277BD` is in the same blue family (~200° hue vs. the brand's ~210–220° range) and visually compatible but is not formally a brand color. **Reopen-trigger:** the brand palette is extended with a darker blue (e.g., `#1A6FB3`) that passes AA normal text on white, at which point `color.info` light can be migrated to the brand-extended color without re-baselining the snapshot suite (the contrast band is preserved).

- **Roboto font chosen over Atkinson Hyperlegible.** Roboto ships with every Android device (zero APK weight; predictable rendering; no font-loading edge cases) and is M3-canonical. Atkinson Hyperlegible is purpose-designed by the Braille Institute for low-vision readers and would more directly serve the file-03 low-light / glanceable environment constraint, at a cost of ~250KB APK weight plus design-system maintenance to keep its rendering consistent across the M-006 screens. **Reopen-trigger:** Patient surveys or accessibility reviews indicate Roboto is materially insufficient for the novice-Patient low-vision posture, or NemoPill's distribution scope expands to include populations with significantly higher low-vision prevalence than the current MVP audience.

- **No high-contrast theme.** Section 4 locked light + dark only. Section 3's color values exceed WCAG AA across the board, with primary text at AAA, so a third high-contrast theme is unnecessary for the file-03 novice-Patient posture. **Reopen-trigger:** Patient-reported low-vision accessibility issues that the OS-level `Settings.Secure.HIGH_TEXT_CONTRAST_ENABLED` amplifier does not adequately address. Adding a high-contrast theme would expand the `_context/05` § Snapshot baseline from 20 to 32+ captures and double the per-component maintenance cost.

- **No in-app theme picker.** Section 4 + Section 11 locked theme selection as a function of Android `Configuration.uiMode`; the OS-level system dark-mode toggle is the single source of truth. Adding an in-app override would create a "manual override" state that diverges from system preference and doubles the snapshot matrix. **Reopen-trigger:** Patient feedback that the OS dark-mode setting cannot adequately serve their preferred NemoPill viewing mode (e.g., always-dark-NemoPill on always-light-system).

- **Hard-delete deliberate-confirmation pattern requires three deliberate actions** (tap "Delete all data" in Settings → check the irreversibility-acknowledgment checkbox → tap the destructive button). Some Patients may find this overly cautious for a self-managed app. The friction is intentional and load-bearing on the `_context/05` § Data Protection irreversibility commitment. **Reopen-trigger:** softening the pattern (default-checked checkbox, removed checkbox, dismissible-by-tap-outside, single-tap "OK") requires an ADR amendment in `_context/09` with Security-role review per Section 8's lock.

- **Restrained / functional motion principle; no expressive animation at MVP.** Section 9 locked motion as restrained and functional; no decorative animation, no Adherence-streak celebrations, no bouncing pill icons. The file-03 novice-Patient persona and the file-03 glanceable + battery-tolerant constraints favor predictable, low-cognitive-load transitions; the `_context/05` § Snapshot baseline is more deterministic with short M3-default easings. **Reopen-trigger:** a Patient-engagement study or product strategy decision that promotes NemoPill from utility-app aesthetic to expressive consumer-health-app aesthetic.

- **No animated illustrations, no swipe actions on list rows, no pull-to-refresh, no shared-element transitions.** Section 9's "Interaction patterns not animated at MVP" list. Each pattern is deferred to post-MVP and would require its own design + motion-token revisit. **Reopen-trigger:** a specific post-MVP user-research finding that argues a deferred pattern materially improves the file-03 primary user journey (first-week onboarding loop) or steady-state daily loop. Adding any of them is an ADR amendment.

- **Spanish long-form Privacy Policy translation deferred to M-006.** Section 10 authored the English Privacy Policy substance in full; the Spanish translation of the long-form body is `_context/07` Dependency And Blocker Register's Spanish-translation-capacity row work. Until M-006 lands, Spanish-locale Patients see the English Privacy Policy via the defensive fallback in Section 8's Settings → Privacy Policy screen. Short bilingual strings (notification copy, error messages, dialog copy, button labels, navigation labels, headings, empty states, settings labels) are already authored bilingually in Section 10. **Reopen-trigger:** M-006 close, at which point the Spanish translation lands in `legal/PRIVACY_POLICY.es.md` and the APK-bundled `res/raw-es/privacy.md`.

- **Privacy Policy contact path is GitHub Issues only.** Public surface; non-technical Patients may find it intimidating to file an issue, and personal medical information cannot be shared in public issues (Section 10 § Contact documents the private-channel arrangement language). **Reopen-trigger:** Patient feedback indicating the GitHub Issues path is materially inaccessible, or regulatory guidance (CCPA / CPRA enforcement actions, LatAm authority advisories) requiring a private contact mechanism as default.

- **Konsist literal-token rules have evasion edge cases.** The four Konsist rules (no literal `dp` matching a named `space.*` step; no literal `dp` matching an `iconSize.*` token; no literal `Int` millisecond matching a `motion.*` duration; no literal `String` in `Text()` composable parameters) catch ~95% of accidental violations but a determined developer can write `15.dp + 1.dp` or `200 + 0` to evade. The rules are reasonable as guard-rails, not as absolute enforcement. **Reopen-trigger:** evidence that the evasion edge cases are being hit in practice during M-001 implementation, at which point either the rules are tightened (via more sophisticated AST analysis) or accepted as guard-rails-only with the gap formally documented.

### Planned hardening

These are items the design system acknowledges as future work without committing to a milestone. Each item names the precondition that would make it actionable.

- **Atkinson Hyperlegible font revisit.** If a future low-vision accessibility re-evaluation argues that Roboto is insufficient for the file-03 novice-Patient posture, the Section 5 font-family decision is the first thing to reopen. The Section 12 Konsist rule for typography would extend to enforce only the new family; APK weight would grow by ~250KB. Tied to the Roboto accepted-gap above.

- **`color.border` upgrade to passing WCAG 1.4.11 values.** If a future component requires `color.border` to carry meaning standalone (no spacing / focus-ring / elevation backup), the values bump to `#9CA3AF` light (3.0:1; borderline-pass) or `#71717A` light (4.5:1; comfortable pass). Trade-off: a darker border is more visually heavy and reads less "subtle". The snapshot baseline of 20 captures is partially invalidated — every component that renders a border is re-baselined.

- **`color.info` light brand-extended value.** A future revision can add a darker brand-extended blue (e.g., `#1A6FB3`) that passes WCAG AA normal text on `color.surface` light, allowing `color.info` light to migrate from `#0277BD` to a true brand-palette member without re-baselining the snapshot suite (contrast band preserved). Tied to the `color.info` light accepted-gap above.

- **M3-to-M2 reversal would invalidate the 20-capture snapshot baseline.** Section 2 Applicability and Section 4 Color System pinned Material 3 as the underlying token vocabulary. A future revision that picks Material 2 (or M4 when it lands) requires an ADR with an explicit consequences-on-snapshot-baseline section per Section 4's lock; every component's M3 underlying primitive changes, every snapshot re-baselines. **Precondition for action:** an ADR amendment in `_context/09`.

- **In-app theme picker addition.** If a future Patient survey demands a NemoPill-internal theme override, the Section 11 customizable-surfaces table grows by one row. The snapshot matrix expands to handle manual-override states (manual-light-on-dark-system, manual-dark-on-light-system) and the Settings → App lock screen gains a new section. Tied to the no-in-app-theme-picker accepted-gap above.

- **High-contrast theme addition.** Would expand the `_context/05` § Snapshot baseline from 20 to 32+ captures and double the per-component maintenance cost. **Precondition for action:** Patient-reported low-vision accessibility issues that the OS-level `Settings.Secure.HIGH_TEXT_CONTRAST_ENABLED` amplifier does not adequately address.

- **Empty-state illustration animations.** Section 7's flat-style illustrations are static `ImageVector` constants. A future revision could add subtle motion (a pulse on the pill capsule illustration, a gentle scale-in on appearance) using the Section 9 motion tokens. Tied to Section 9's deferred-animations list. **Precondition for action:** an ADR with motion-token consequences and a snapshot-baseline addendum.

- **Swipe-to-delete on list rows.** Section 8's list-row component would gain a swipe-action state and a confirmation-undo affordance. Requires file-04 / file-06 revisions for the gesture-handling boundary and likely a new motion token for swipe-velocity-based animation. **Precondition for action:** a Patient-research finding that swipe-to-delete materially improves the BR-007 (archive Medication) journey.

- **Pull-to-refresh on lists.** Currently rejected as unnecessary (NemoPill's lists auto-update via Compose state observation; there is no remote source to pull from). **Precondition for action:** an architectural change that introduces a remote source (which itself would require an ADR confronting the `_context/05` § Data Protection no-`INTERNET` rule).

- **Native-speaker Spanish secondary review for the long-form Privacy Policy.** `_context/07` Dependency And Blocker Register's Spanish-translation-capacity row notes this as a future-dependency-to-surface. M-006 will produce the first Spanish long-form translation; a separate review pass by a native Latin American Spanish speaker would catch idiom and cultural-fit issues that the formal-`usted` register convention cannot guarantee. **Precondition for action:** identifying a willing reviewer with regulatory-translation experience.

- **Privacy Policy alternative contact path.** Section 10 § Contact directs all questions to GitHub Issues. A future revision could add an email contact (`privacy@nemopill.app` if the domain is registered, or a personal email Isidro commits to monitoring) for non-technical Patients. Tied to the Privacy-Policy-contact accepted-gap above.

- **Plain-text Room storage re-evaluation at T-005 (file 13 threat model).** Explicitly deferred from `_context/12_environments_and_devops.md` § Section 10. T-005 must enumerate the adversaries the file-05 § Data Protection sub-rule (a) decision is calibrated against; if the threat-model exercise reveals adversaries the current decision does not cover, the follow-up is a `_context/05` revision packet. **Precondition for action:** T-005 contextualization session.

- **Universal-symbol icon-alone allowlist expansion.** Section 7's allowlist is closed at four entries (`arrow_back`, `close`, `more_vert`, `add`). A future component design that proposes a fifth icon-alone affordance (e.g., `search`, `edit`, `delete`, `settings`) requires an ADR amendment with a justification rooted in the file-03 novice-Patient persona and a snapshot-baseline addendum for the new icon's rendering. **Precondition for action:** a specific feature design that demonstrably benefits from icon-alone affordance.

- **Snapshot baseline expansion procedure documentation.** When Material version, theme count, breakpoint matrix, or screen count changes, the `_context/05` § Snapshot baseline of 20 captures changes correspondingly. The expansion procedure (decide new baseline → re-capture → commit → update file-05 documentation) is not formally documented as a procedure. **Precondition for action:** the first time any of those four parameters changes after MVP; the procedure is authored as part of that change's ADR.
