1. Purpose

This document defines the UI architecture and design rules for BurnMate.

Goals:
	•	Maintain consistent UI design
	•	Ensure component reuse
	•	Prevent screen-level duplication
	•	Keep UI independent from domain logic

All UI work must follow this rulebook.

⸻

2. Design Language

Theme

Primary characteristics:
	•	Dark AMOLED-first UI
	•	Liquid glass card surfaces
	•	Cyan accent color
	•	Premium fitness dashboard feel

Guidelines:
	•	Avoid bright backgrounds
	•	Use high contrast typography
	•	Use glass surfaces only for cards and panels

⸻

3. Color System

Define all colors in the theme layer.

Example palette:

BackgroundPrimary   #0B0F14
BackgroundSecondary #121821
SurfaceGlass        #1B2430
AccentPrimary       #00D1FF
AccentSecondary     #5CE1E6
TextPrimary         #FFFFFF
TextSecondary       #9CA3AF
Divider             #2A3441
Error               #FF5A5F
Success             #10B981

Rules:
	•	Colors must never be hardcoded in composables
	•	Use theme tokens only

⸻

4. Typography

Typography hierarchy must remain consistent.

Usage	Style
Hero Metric	very large numeric display
Section Title	medium bold
Card Title	medium
Body	normal
Meta Label	small uppercase

Rules:
	•	Hero numbers should visually dominate the screen
	•	Avoid dense paragraphs
	•	Labels should be short

⸻

5. Spacing System

Use a consistent spacing scale.

4
8
12
16
24
32
40
48

Rules:
	•	Never use arbitrary spacing values
	•	Use spacing tokens

Example:

Spacing.Small = 8
Spacing.Medium = 16
Spacing.Large = 24


⸻

6. Layout Rules

Common screen structure:

Header
Hero Metric
Section
Cards
Section
Cards
Bottom Navigation

Guidelines:
	•	Use vertical stacking
	•	Avoid overcrowding
	•	Maintain visual breathing room

⸻

7. Glass Surface Rules

Glass cards must follow the same structure.

Properties:
	•	Rounded corners
	•	Subtle border
	•	Slight transparency
	•	Soft elevation

Glass cards should contain:

Title
Content
Optional icon
Optional action

Do not create multiple glass card variants unless necessary.

⸻

8. Atomic UI Architecture

All UI must follow atomic component methodology.

Project structure:

ui
 ├─ atoms
 ├─ molecules
 ├─ organisms
 ├─ screens
 ├─ theme
 ├─ components

Definitions:

Atoms

Smallest UI primitives.

Examples:

TextLabel
IconButton
DividerLine
MetricText
PrimaryButton


⸻

Molecules

Composed UI elements.

Examples:

SectionHeader
StatRow
InputField
DateSelector
MetricDisplay


⸻

Organisms

Complex reusable UI sections.

Examples:

HeroSummaryCard
WeightSummaryCard
DebtSummaryCard
ActionCardList
ChartContainer
BottomNavigationBar
AppHeader


⸻

Screens

Screens should only assemble organisms.

Examples:

DashboardScreen
DailyLogScreen
OnboardingScreen

Rules:
	•	Screens must stay thin
	•	Screens should contain no business logic

⸻

9. Component Reuse Policy

Reusable components must live in:

ui/components

Examples:

GlassCard
StatCard
ActionCard
SectionHeader
MetricDisplay
PrimaryActionButton
BottomNavigation

If a component appears in 2+ screens, it must be extracted.

⸻

10. ViewModel Architecture

UI must never talk directly to domain services.

Correct flow:

Composable
   ↓
ViewModel
   ↓
Domain / ReadModel

Rules:
	•	No domain imports inside UI layer
	•	UI must observe state only

⸻

11. Dashboard UI Rules

Dashboard must contain:
	•	Hero metric
	•	Today summary
	•	Weight summary
	•	Debt summary
	•	Quick actions
	•	Chart preview

Do not place full analytics on the dashboard.

⸻

12. Daily Logging UI Rules

Daily logging should include:
	•	Date selector
	•	Weight input
	•	Quick save action
	•	Entry history preview

The screen should remain simple and fast to use.

⸻

13. Navigation Rules

Navigation must use a single navigation host.

Expected screens:

Dashboard
Daily Log
History
Settings

Bottom navigation should control main sections.

⸻

14. Accessibility

Minimum standards:
	•	Tap targets ≥ 48dp
	•	Readable contrast
	•	Clear labels
	•	Avoid dense UI clusters

⸻

15. Forbidden Patterns

The following are not allowed:
	•	Hardcoded colors
	•	Hardcoded spacing
	•	Domain logic inside UI
	•	Duplicate UI components
	•	Business logic inside composables

⸻

16. Engineer Instructions

When implementing UI:
	1.	Follow this rulebook.
	2.	Use atomic components.
	3.	Extract reusable UI patterns.
	4.	Screens must remain thin.
	5.	Respect slice boundaries.

⸻

Recommended next step

After committing this file:

docs/ui/UI_RULEBOOK.md

Your SLICE-0007 Engineer prompt should include:

Follow docs/ui/UI_RULEBOOK.md.
Use atomic UI architecture.
Extract reusable UI components.
Do not duplicate UI patterns across screens.
