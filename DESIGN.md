# Smart Watch Monitoring System — Design System

> **DESIGN.md** for the Smart Watch Patient Monitoring Platform.
> This document defines the complete visual language so any AI agent can generate consistent UI.
>
> **Inspiration**: Airbnb (friendly warmth) + Sentry (monitoring dashboards) + Revolut (precision data)

---

## 1. Visual Theme & Atmosphere

**Mood**: Professional yet approachable. A medical monitoring system that feels trustworthy and calm — not cold or clinical, but not playful either.

**Density**: Data-rich but breathable. Dashboards pack information without visual noise. White space is used deliberately to separate concerns.

**Philosophy**:
- **Clarity first** — health data and alerts must be instantly scannable
- **Warm professionalism** — soft surfaces and rounded corners remove the "sterile hospital" feel
- **Precision where it matters** — numbers, timestamps, and status indicators are crisp and exact
- **Progressive disclosure** — show summary on cards, detail on click/expansion

---

## 2. Color Palette & Roles

### Primary — Medical Trust Blue

| Token | Hex | Role |
|-------|-----|------|
| `--color-primary` | `#2563EB` | Primary actions, active states, links |
| `--color-primary-light` | `#DBEAFE` | Primary backgrounds, hover fills |
| `--color-primary-dark` | `#1D4ED8` | Primary hover, pressed states |
| `--color-primary-subtle` | `#EFF6FF` | Subtle primary tint backgrounds |

### Accent — Warm Coral (Airbnb-inspired)

| Token | Hex | Role |
|-------|-----|------|
| `--color-accent` | `#FF6B6B` | CTAs, important notifications, warm highlights |
| `--color-accent-light` | `#FFF0F0` | Accent backgrounds, alert highlights |
| `--color-accent-dark` | `#E85555` | Accent hover states |

### Semantic — Status & Monitoring

| Token | Hex | Role |
|-------|-----|------|
| `--color-success` | `#10B981` | Online status, normal readings, completed |
| `--color-success-bg` | `#ECFDF5` | Success background |
| `--color-warning` | `#F59E0B` | Low battery, pending alerts, caution |
| `--color-warning-bg` | `#FFFBEB` | Warning background |
| `--color-danger` | `#EF4444` | Critical alerts, SOS, offline devices |
| `--color-danger-bg` | `#FEF2F2` | Danger background |
| `--color-info` | `#3B82F6` | Informational, tips, neutral alerts |
| `--color-info-bg` | `#EFF6FF` | Info background |

### Health Metric Colors (Sentry-inspired data palette)

| Token | Hex | Role |
|-------|-----|------|
| `--color-heart-rate` | `#EF4444` | Heart rate metrics, pulse indicators |
| `--color-blood-pressure` | `#8B5CF6` | Blood pressure metrics |
| `--color-blood-oxygen` | `#3B82F6` | Blood oxygen (SpO2) metrics |
| `--color-body-temp` | `#F59E0B` | Body temperature metrics |
| `--color-steps` | `#10B981` | Step count, activity metrics |

### Neutrals (Revolut-inspired precision)

| Token | Hex | Role |
|-------|-----|------|
| `--color-bg` | `#F8FAFC` | Page background |
| `--color-surface` | `#FFFFFF` | Card/panel surfaces |
| `--color-surface-raised` | `#F1F5F9` | Elevated surfaces, hovered rows |
| `--color-border` | `#E2E8F0` | Default borders, dividers |
| `--color-border-strong` | `#CBD5E1` | Emphasized borders |
| `--color-text-primary` | `#1E293B` | Body text, headings |
| `--color-text-secondary` | `#64748B` | Labels, descriptions |
| `--color-text-muted` | `#94A3B8` | Placeholders, disabled text |
| `--color-text-inverse` | `#FFFFFF` | Text on dark/primary backgrounds |

---

## 3. Typography Rules

### Font Stack

```css
--font-sans: "Inter", "PingFang SC", "Microsoft YaHei", system-ui, -apple-system, sans-serif;
--font-mono: "JetBrains Mono", "Fira Code", "Source Code Pro", monospace;
```

**Rationale**: Inter for excellent readability and clear numerals (tabular figures for data). PingFang SC for native Chinese rendering on macOS; Microsoft YaHei for Windows fallback. JetBrains Mono for device IDs and technical data.

### Type Scale

| Level | Size | Weight | Line Height | Usage |
|-------|------|--------|-------------|-------|
| `--text-display` | 40px | 700 | 1.2 | Hero numbers, KPIs |
| `--text-h1` | 32px | 700 | 1.3 | Page titles |
| `--text-h2` | 24px | 600 | 1.3 | Section headings |
| `--text-h3` | 20px | 600 | 1.4 | Card titles, sub-sections |
| `--text-h4` | 16px | 600 | 1.5 | Component headings |
| `--text-body` | 14px | 400 | 1.6 | Body text, descriptions |
| `--text-small` | 12px | 400 | 1.5 | Labels, meta text, captions |
| `--text-micro` | 11px | 500 | 1.4 | Badges, status tags |

### Number Display Rules

- **Data numbers** (counts, measurements): Use `font-variant-numeric: tabular-nums` for alignment
- **Device IMEIs**: Use `--font-mono`, letter-spacing `0.05em`
- **Timestamps**: Use `--font-mono` for time portions, `--font-sans` for relative labels ("3分钟前")
- **Health metrics**: Display size with colored unit (e.g., **72** bpm, **120/80** mmHg)

### Chinese Text Rules

- Always ensure `PingFang SC` / `Microsoft YaHei` in font stack
- Use `--text-body` (14px) minimum for Chinese — 12px Chinese is hard to read
- Line height for Chinese text should be ≥1.6 for comfortable reading

---

## 4. Component Stylings

### Buttons

| Variant | Background | Text | Border | Radius | Usage |
|---------|-----------|------|--------|--------|-------|
| **Primary** | `--color-accent` `#FF6B6B` | `--color-text-inverse` | none | 12px | Main CTA (login, handle alarm) |
| **Secondary** | `--color-surface` | `--color-primary` | `--color-primary` 1px | 12px | Alternative actions |
| **Ghost** | transparent | `--color-text-secondary` | none | 8px | Tertiary, inline actions |
| **Danger** | `--color-danger` | `--color-text-inverse` | none | 12px | Destructive actions |
| **Icon** | transparent | `--color-text-secondary` | none | 8px | Toolbar actions |

**States**:
- Hover: Background darkens by 8%, slight `translateY(-1px)` transform
- Active: Background darkens by 12%, `translateY(0)`
- Disabled: `opacity: 0.4`, no pointer events
- Loading: Show spinner, text fades to 50%

**Sizes**:
- Small: 28px height, 12px padding-x, `--text-small`
- Medium: 36px height, 16px padding-x, `--text-body`
- Large: 44px height, 24px padding-x, `--text-h4`

### Cards

```css
background: var(--color-surface);
border-radius: 16px;
border: 1px solid var(--color-border);
box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
padding: 24px;
```

**Card variants**:
- **Stats card**: Icon left, numbers right, subtle background tint for icon area
- **Alarm card**: Left color stripe (4px, severity-colored), expandable detail section
- **Patient card**: Avatar circle, name, ward/bed, status dot, battery bar
- **Map card**: Full-bleed map with overlay tools (zoom, reset, layer toggle)

### Tags & Badges

- **Shape**: Pill (`border-radius: 20px`)
- **Alarm type tags**: Color-coded by severity
  - Critical/SOS: `--color-danger` bg with white text
  - Warning: `--color-warning` bg with dark text
  - Info: `--color-info-bg` bg with `--color-info` text
- **Status tags**:
  - Online/Active: Green dot + "在线"
  - Offline: Gray dot + "离线"
  - Alarming: Red pulsing dot + "报警"

### Tables

```css
/* Table styling */
--table-header-bg: var(--color-surface-raised);
--table-header-text: var(--color-text-secondary);
--table-row-hover: var(--color-surface-raised);
--table-border: var(--color-border);
--table-stripe: var(--color-bg);
```

- Zebra striping on alternating rows
- Sticky header for scrollable tables
- Row click → highlight with `--color-primary-light` background
- Sortable columns show arrow indicator in header

### Forms & Inputs

```css
border-radius: 12px;
border: 1px solid var(--color-border);
padding: 10px 16px;
font-size: var(--text-body);
```

**States**:
- Focus: Border becomes `--color-primary`, with 3px `--color-primary-light` ring
- Error: Border becomes `--color-danger`, error message below in `--color-danger`
- Disabled: Background `--color-surface-raised`, text `--color-text-muted`
- Search: Prefix icon, clearable, rounded-full (24px radius)

### Alarm List Item

```
┌─┬─────────────────────────────────────────────────┐
│█│  SOS报警        张三  内科一病区    10:32:15      │
│█│  北京市朝阳区...                     [未处理]     │
└─┴─────────────────────────────────────────────────┘
  ▲ 4px color stripe: red=critical, amber=warning, blue=info
```

### Health Metric Display

```
  ♥ 心率          SpO2          🌡 体温
   72             98%           36.5
   bpm            %             °C
   ▲ 正常         ▲ 正常        ▲ 正常
```

- Metric value: `--text-display` (40px), weight 700, metric color
- Unit: `--text-small`, `--color-text-muted`
- Status arrow + label: Green/yellow/red per normal/warning/abnormal

### Device Status Indicator

- **Online**: 8px green circle with subtle pulse animation (`box-shadow` breathe)
- **Offline**: 8px gray circle
- **Warning**: 8px amber circle
- **SOS**: 8px red circle with urgent pulse animation (faster, larger glow)

### Map Components

- Full-bleed within card container
- Overlay toolbar: white rounded pills with subtle shadow
- Markers: Custom SVG (device icon), color-coded by status
- Geofence zones: Semi-transparent fills with dashed borders
- Patient cluster: Circle with count number

---

## 5. Layout Principles

### Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| `--space-1` | 4px | Tight: icon gaps, inline spacing |
| `--space-2` | 8px | Compact: related elements, chip gaps |
| `--space-3` | 12px | Standard: form field gaps, list items |
| `--space-4` | 16px | Comfortable: card internal padding |
| `--space-5` | 24px | Generous: section gaps, card padding |
| `--space-6` | 32px | Spacious: major section breaks |
| `--space-7` | 48px | Large: page section separations |
| `--space-8` | 64px | Extra: top-level layout gaps |

### Grid

- **Desktop** (≥1280px): 12-column grid, 24px gutter, max-width 1440px
- **Tablet** (768–1279px): 8-column grid, 16px gutter
- **Mobile** (<768px): 4-column grid, 16px gutter, single column content

### Page Layout

```
┌──────────────────────────────────────────────────┐
│  Sidebar (240px)  │  Content Area                │
│                   │  ┌─────────────────────────┐ │
│  ┌─────────┐      │  │  Alarm Banner (if any)  │ │
│  │ Logo    │      │  └─────────────────────────┘ │
│  ├─────────┤      │  ┌───┐ ┌───┐ ┌───┐ ┌───┐   │
│  │ 仪表盘  │      │  │S1 │ │S2 │ │S3 │ │S4 │   │
│  │ 设备    │      │  └───┘ └───┘ └───┘ └───┘   │
│  │ 病人    │      │  ┌──────────┐ ┌─────────┐  │
│  │ 报警    │      │  │  Map     │ │ Alarms  │  │
│  │ 围栏    │      │  │          │ │         │  │
│  │ 历史    │      │  └──────────┘ └─────────┘  │
│  ├─────────┤      │                              │
│  │ 用户    │      │                              │
│  └─────────┘      │                              │
└──────────────────────────────────────────────────┘
```

- Sidebar: Collapsible (icon-only at 64px width)
- Content: Scrollable, padded `--space-6` on sides
- Cards: Fill grid cells with consistent `--space-4` gap

---

## 6. Depth & Elevation

| Level | Shadow | Usage |
|-------|--------|-------|
| `--elevation-0` | none | Page background, flat surfaces |
| `--elevation-1` | `0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)` | Cards, list items |
| `--elevation-2` | `0 4px 6px rgba(0,0,0,0.05), 0 2px 4px rgba(0,0,0,0.04)` | Hovered cards, dropdowns |
| `--elevation-3` | `0 10px 15px rgba(0,0,0,0.07), 0 4px 6px rgba(0,0,0,0.04)` | Popovers, tooltips |
| `--elevation-4` | `0 20px 25px rgba(0,0,0,0.08), 0 8px 10px rgba(0,0,0,0.04)` | Modals, drawers |
| `--elevation-5` | `0 25px 50px rgba(0,0,0,0.12)` | Toast notifications, full-screen overlays |

**Surface hierarchy**:
- Background (`--color-bg`): No elevation
- Cards (`--color-surface`): Elevation 1
- Raised elements (dropdowns, popovers): Elevation 2-3
- Overlays (modals, drawers): Elevation 4

---

## 7. Do's and Don'ts

### Do

- ✅ Use color stripe indicators on alarm items for instant severity recognition
- ✅ Show real-time status with animated dots (pulse for online, static for offline)
- ✅ Use tabular figures for all numerical data (counts, metrics, timestamps)
- ✅ Display health metrics with their specific metric color (red=heart, violet=BP, blue=SpO2)
- ✅ Provide clear Chinese labels alongside icons — don't rely on icons alone
- ✅ Use progressive disclosure: summary in cards, details on expansion
- ✅ Make alarm banners impossible to miss (coral accent, high contrast)
- ✅ Support keyboard navigation for alarm handling workflows
- ✅ Use 14px minimum for Chinese text; 12px only for English labels/badges

### Don't

- ❌ Don't use pure black (`#000000`) for text — use `--color-text-primary` (`#1E293B`)
- ❌ Don't show raw timestamps without relative time ("3分钟前" is better than "2026-04-12T10:32:15")
- ❌ Don't use red/green as the only indicator — include icons and labels for color-blind users
- ❌ Don't put more than 4 stat cards in a single row on desktop
- ❌ Don't use alert/danger colors for decorative purposes — reserve them for actual alerts
- ❌ Don't stack modals on top of modals — use side drawers for drill-down
- ❌ Don't auto-play alarm sounds without a toggle — provide visual + audio options
- ❌ Don't truncate patient names or device IMEIs without tooltip — show full on hover

---

## 8. Responsive Behavior

### Breakpoints

| Name | Min-width | Columns | Sidebar |
|------|-----------|---------|---------|
| Mobile | 0 | 4 | Hidden (hamburger menu) |
| Tablet | 768px | 8 | Collapsed (icon-only) |
| Desktop | 1280px | 12 | Expanded (240px) |
| Wide | 1536px | 12 + margins | Expanded |

### Responsive Adaptations

| Component | Desktop | Tablet | Mobile |
|-----------|---------|--------|--------|
| Stats cards | 4 across | 2 across | 1 across |
| Alarm list | Side-by-side with detail | Full width list | Full width list |
| Map | 60% width, alongside list | Full width, above list | Full width, collapsible |
| Patient table | Full columns | Scroll horizontally | Card layout |
| Navigation | Sidebar | Collapsed sidebar | Bottom tab bar |
| Health metrics | Horizontal row | 2x2 grid | Stacked cards |

### Touch Targets

- Minimum touch target: 44x44px (WCAG 2.5.5)
- Button padding increased on mobile (+8px vertical)
- Alarm item tap targets: full-width rows with 56px minimum height
- Map zoom buttons: 44x44px with 8px gap

---

## 9. Agent Prompt Guide

### Quick Color Reference

```
Primary blue:  #2563EB    (trust, navigation, links)
Warm coral:    #FF6B6B    (CTAs, urgent actions)
Success green: #10B981    (online, normal, completed)
Warning amber: #F59E0B    (caution, low battery)
Danger red:    #EF4444    (critical, SOS, offline)
Background:    #F8FAFC    (cool white)
Surface:       #FFFFFF    (cards, panels)
Text:          #1E293B    (primary), #64748B (secondary)
```

### Ready-to-Use Prompts

**Dashboard page**:
> Build a monitoring dashboard with 4 stat cards (patients, alarms, devices, online rate), a leaflet map showing patient locations with colored markers, a recent alarm list with severity stripes, and a ward overview section. Use the DESIGN.md color palette and component styles.

**Alarm management page**:
> Build an alarm management page with a filterable alarm list (status, type, time range), alarm detail panel on the right, and batch action buttons. Each alarm item shows a left color stripe, alarm type tag, patient info, location, and time. Use tabular figures for timestamps.

**Patient detail page**:
> Build a patient detail page with a patient info header (name, ward, bed, diagnosis), health metrics cards (heart rate, blood pressure, SpO2, temperature with sparklines), device status section, and location history on a map. Use health metric colors for each type.

**Login page**:
> Build a split-screen login page. Left side: login form with rounded inputs, coral primary button, "remember me" checkbox. Right side: branded illustration area with gradient. Use warm, friendly styling — not corporate.

### Element Plus Theme Override

When using Element Plus, override these CSS variables to match this design system:

```css
:root {
  --el-color-primary: #2563EB;
  --el-color-primary-light-3: #5B8DEF;
  --el-color-primary-light-5: #92B5F5;
  --el-color-primary-light-7: #DBEAFE;
  --el-color-primary-light-9: #EFF6FF;
  --el-color-primary-dark-2: #1D4ED8;
  --el-color-danger: #EF4444;
  --el-color-warning: #F59E0B;
  --el-color-success: #10B981;
  --el-color-info: #3B82F6;
  --el-border-radius-base: 12px;
  --el-border-radius-small: 8px;
  --el-border-radius-round: 20px;
  --el-font-family: "Inter", "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  --el-font-size-base: 14px;
  --el-bg-color: #F8FAFC;
  --el-bg-color-overlay: #FFFFFF;
  --el-text-color-primary: #1E293B;
  --el-text-color-regular: #64748B;
  --el-border-color: #E2E8F0;
}
```
