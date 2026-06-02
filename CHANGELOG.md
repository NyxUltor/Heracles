# 0.6 - 0.7

Added

4-tier fidelity system (Minimal/Balanced/Rich/Custom)
Elemental accent system (HELLFIRE, BLOOD & WINE, ABYSSAL SKY, BLACK GOLD, DEEP SEA, WHITE PHANTOM)
Dynamic usage-based accent reordering
Dual-layer radar chart (AVI Raw Volume + RVI Frame Intensity)
HerculesMathEngine with biomechanical load distribution (AVI/RVI formulas)
ExerciseProfile and StrengthHistory data models
Breakpoint system (COMPACT/EXPANDED) via CompositionLocal
Rich tier logger (glassmorphism, spring animations, particle burst, haptics)
Wireframe preview cards in Appearance screen
Curated scheme selector (Midnight, Arctic, Warm, Matrix)
Pre-built session fault-tolerant regex parser
Volume+bodyweight dual-axis scrollable bar/line chart
Collapsing header foundation
Dynamic K_t normalization for radar axes

Changed

Volume only counts completed sets
Duration removed from pre-built session templates
Timer display now HH:MM:SS
Balanced logger converted from WebView to native Compose
Theme system restructured — Minimal/Balanced/Rich use elemental accents, Custom uses mod system

Fixed

Scrubber triggering navigation drawer
Exercise rename broken in Balanced tier
Gray surface mismatch across layout layers
Start button resetting session state
Stale duration on session restore
Bodyweight field scrubber overflow (65587kg bug)
Double ++ on Add set button
Sessions tab label truncated to 'Ses'

Known Issues

Rich tier tab switching incomplete
Timer not unified across all tiers
Collapsing header not yet implemented
Native header not yet removed"