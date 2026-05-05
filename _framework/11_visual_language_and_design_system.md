# Visual Language And Design System

## How To Fill This File

- If the product has no user-facing visual surface, write `Not applicable` at the top of each section and explain why.
- Capture the rules a frontend agent needs to render correctly without inventing a system.
- Keep tokens and component names stable across files. Names defined here are the ones used in code.

## Applicability

- Visual surfaces in scope: `{{REQUIRED: web app, mobile app, marketing site, internal tool, email, PDF, embeddable widget, "Not applicable", etc.}}`
- Design system source of truth: `{{OPTIONAL: Figma file, internal package, third-party library, "starting from scratch"}}`
- Brand owner: `{{OPTIONAL: who approves visual changes}}`

## Design Tokens

| Token Group | Token Name | Value | Usage Notes |
| --- | --- | --- | --- |
| `Color` | `{{REQUIRED: e.g., color.surface.default}}` | `{{REQUIRED: hex / oklch / rgb}}` | `{{OPTIONAL: where it is allowed}}` |
| `Color` | `{{OPTIONAL: e.g., color.text.primary}}` | `{{OPTIONAL: value}}` | `{{OPTIONAL: contrast pair, WCAG ratio}}` |
| `Spacing` | `{{REQUIRED: e.g., space.4}}` | `{{REQUIRED: e.g., 16px}}` | `{{OPTIONAL: usage}}` |
| `Radius` | `{{OPTIONAL: e.g., radius.md}}` | `{{OPTIONAL: value}}` | `{{OPTIONAL: usage}}` |
| `Elevation` | `{{OPTIONAL: e.g., elevation.2}}` | `{{OPTIONAL: shadow string}}` | `{{OPTIONAL: usage}}` |
| `Z-index` | `{{OPTIONAL: e.g., z.modal}}` | `{{OPTIONAL: value}}` | `{{OPTIONAL: layering rule}}` |

## Color System

- Color roles (semantic): `{{REQUIRED: surface, surface-muted, text-primary, text-secondary, border, accent, success, warning, danger, info}}`
- Brand palette (raw): `{{OPTIONAL: brand color values, when each is used}}`
- Theming model: `{{REQUIRED: single theme, light/dark, multi-brand, user-customizable, etc.}}`
- Contrast rules: `{{REQUIRED: minimum contrast ratios per text size and per component state}}`

## Typography

- Font families: `{{REQUIRED: primary, secondary, monospace, fallback chain}}`
- Type scale: `{{REQUIRED: e.g., display, h1, h2, h3, body, body-sm, caption, code with sizes and line-heights}}`
- Weights in use: `{{OPTIONAL: e.g., 400, 500, 700}}`
- Numeric and tabular rules: `{{OPTIONAL: tabular numerals, currency formatting, decimal alignment}}`
- Line length / measure: `{{OPTIONAL: target characters per line for body copy}}`

## Spacing And Layout

- Spacing scale: `{{REQUIRED: base unit and scale, e.g., 4px base with 0/1/2/3/4/6/8/12/16}}`
- Grid system: `{{OPTIONAL: column count, gutter, breakpoints}}`
- Breakpoints: `{{OPTIONAL: mobile / tablet / desktop / wide values}}`
- Layout primitives: `{{OPTIONAL: stack, cluster, grid, sidebar, switcher, etc.}}`

## Iconography And Imagery

- Icon library: `{{OPTIONAL: source, size grid, stroke width, paired weights}}`
- Icon usage rule: `{{OPTIONAL: when to use icons alone vs icon + label}}`
- Imagery style: `{{OPTIONAL: photography, illustration, 3D, generative; aspect ratios; treatment}}`
- Empty-state and placeholder treatment: `{{OPTIONAL: pattern}}`

## Components

| Component | Variants | States | Required Behaviors |
| --- | --- | --- | --- |
| `{{REQUIRED: Button}}` | `{{REQUIRED: primary, secondary, ghost, destructive}}` | `{{REQUIRED: default, hover, focus, active, disabled, loading}}` | `{{REQUIRED: keyboard activation, loading lock, focus ring}}` |
| `{{OPTIONAL: Input}}` | `{{OPTIONAL: variants}}` | `{{OPTIONAL: states}}` | `{{OPTIONAL: behaviors}}` |
| `{{OPTIONAL: Modal}}` | `{{OPTIONAL: variants}}` | `{{OPTIONAL: states}}` | `{{OPTIONAL: focus trap, dismiss rules, scroll lock}}` |
| `{{OPTIONAL: Toast / Notification}}` | `{{OPTIONAL: variants}}` | `{{OPTIONAL: states}}` | `{{OPTIONAL: timing, accessibility}}` |
| `{{OPTIONAL: Form field group}}` | `{{OPTIONAL: variants}}` | `{{OPTIONAL: states}}` | `{{OPTIONAL: error placement, hint text}}` |

## Motion And Interaction

- Motion principles: `{{OPTIONAL: e.g., snappy, restrained; how motion communicates causality}}`
- Standard durations and easings: `{{OPTIONAL: e.g., fast 120ms, base 200ms, slow 320ms; easing tokens}}`
- Reduced-motion rule: `{{REQUIRED: how the system honors prefers-reduced-motion or "Not applicable"}}`

## Content And Voice

- Tone: `{{OPTIONAL: e.g., direct, warm, clinical}}`
- Capitalization: `{{OPTIONAL: sentence case vs title case for headings, buttons, labels}}`
- Punctuation in UI: `{{OPTIONAL: trailing periods in labels, oxford comma, em-dash usage}}`
- Error message pattern: `{{OPTIONAL: structure for user-facing errors}}`
- Localization ready: `{{OPTIONAL: which copy is translated, which is brand-locked}}`

## Theming And Customization

- Supported themes: `{{OPTIONAL: light, dark, high-contrast, brand variants}}`
- Theme switching mechanism: `{{OPTIONAL: how the user or system selects a theme}}`
- Customizable surfaces: `{{OPTIONAL: which tokens or components are end-user customizable}}`

## Design System Risks And Trade-Offs

- `{{OPTIONAL: areas where the system is intentionally minimal or opinionated}}`
- `{{OPTIONAL: known inconsistencies that will be normalized in a later milestone}}`
