# Design System Document: Vinilos

## 1. Overview & Creative North Star
**The Creative North Star: The Curated Sleeve**

This design system moves away from the "utility-first" look of standard music apps and leans into the tactile, high-end editorial feel of a luxury vinyl catalog. We are not building a database; we are building a digital gallery. The aesthetic is defined by **"Analog Precision"**—an intentional marriage of brutalist typography (Space Grotesk) and sophisticated tonal layering that mimics the physical act of flipping through records.

By utilizing extreme whitespace, intentional asymmetry, and a "No-Line" philosophy, we create a signature experience where the album art—the "Soul" of the app—is the primary driver of the interface.

## 2. Colors: The Monochromatic Depth
The palette is rooted in the "Noir" of a vinyl record. We use a high-contrast foundation of whites and deep charcoal grays to allow the vibrant colors of album sleeves to pop.

### Color Principles
*   **The "No-Line" Rule:** 1px solid borders are strictly prohibited for sectioning. Boundaries must be defined solely through background color shifts. For example, a `surface-container-low` section sits on a `surface` background to create a "pocket" without a hard line.
*   **Surface Hierarchy & Nesting:** Treat the UI as stacked sheets of fine paper.
    *   **Level 0 (Background):** `surface` (#f9f9f9) - The canvas.
    *   **Level 1 (Sections):** `surface-container-low` (#f2f4f4) - Subtle grouping.
    *   **Level 2 (Active Elements):** `surface-container-highest` (#dde4e5) - High visibility content.
*   **The "Glass & Gradient" Rule:** Use `surface-container-lowest` (#ffffff) at 80% opacity with a `20px` backdrop-blur for floating navigation bars or play-controls. This creates a "frosted glass" effect that keeps the layout integrated and airy.
*   **Signature Textures:** For primary CTAs (e.g., "Add to Collection"), use a subtle vertical gradient from `primary` (#5f5e5e) to `primary-dim` (#535252). This prevents the "flat-gray" look and adds a premium, satin-like finish.

## 3. Typography: Editorial Authority
We use a high-contrast type scale to create a "Magazine" feel.

*   **Display & Headlines (Space Grotesk):** This is our "Liner Note" font. It is wide, geometric, and carries an architectural weight. Use `display-lg` for artist names and `headline-sm` for album titles. Let the letter-spacing be slightly tight (-0.02em) on headlines to create a dense, authoritative block of text.
*   **Body & Labels (Manrope):** A modern, highly legible sans-serif for metadata. Use `body-md` for tracklists and `label-sm` (all caps, +0.05em tracking) for technical data like "RPM," "Weight," or "Release Date."
*   **Visual Hierarchy:** The massive gap between `display-lg` (3.5rem) and `body-sm` (0.75rem) is intentional. It creates "Dynamic Tension"—the core of high-end editorial design.

## 4. Elevation & Depth: Tonal Layering
Traditional Material shadows are too heavy for this "Clean White" aesthetic. We achieve depth through atmospheric light.

*   **The Layering Principle:** Instead of shadows, place a `surface-container-lowest` card on a `surface-container-low` background. This creates a soft, natural "lift."
*   **Ambient Shadows:** When an element must float (e.g., a "Now Playing" bar), use an extra-diffused shadow: `Y: 8px, Blur: 24px, Color: on-surface @ 4% opacity`. This mimics the soft shadow of a record sleeve lying on a white table.
*   **The "Ghost Border" Fallback:** If a border is required for accessibility, use the `outline-variant` token at 15% opacity. Never use 100% opaque borders; they break the "clean air" of the design.

## 5. Components: Precision Elements

### Cards & Lists (The "Gallery View")
*   **Constraint:** Forbid the use of divider lines.
*   **Implementation:** Separate album entries using the **Spacing Scale** (32px vertical gap). Use a subtle `surface-container-low` background for the "Currently Selected" record.
*   **The "Vinyl Pop-Out":** When a record is tapped, the album art should slightly shift 16px to the left, revealing a circular `primary` (dark gray) vinyl edge "sliding out" from the sleeve.

### Buttons
*   **Primary:** Filled with the "Satin" gradient (Primary to Primary-Dim). Roundedness: `lg` (0.5rem). No shadow.
*   **Secondary:** Ghost style. No border. `title-sm` text in `primary`.
*   **Tertiary/Utility:** `surface-container-high` background with `on-surface-variant` icons.

### Inputs & Search
*   **The Minimalist Bar:** Search bars should be `surface-container-lowest` with a "Ghost Border" (15% opacity `outline-variant`). Use `manrope` for placeholder text.

### High-End Detail: The Progress Bar
*   In the music player, the progress bar should not be a standard thick line. Use a 2px `outline-variant` background and a 2px `primary` active line. The "thumb" should be a 4px square, not a circle, to maintain the "Modern Android" aesthetic.

## 6. Do’s and Don’ts

### Do:
*   **DO** use asymmetric padding. For example, give a header 32px left padding but 16px right padding to create a "staggered" editorial flow.
*   **DO** use "Breathing Room." If you think there is enough whitespace, add 8dp more.
*   **DO** use the `full` (9999px) roundedness for small badges (e.g., "New Arrival") but keep cards at `lg` (0.5rem) for a structured feel.

### Don’t:
*   **DON'T** use 1px dividers to separate list items. Use white space.
*   **DON'T** use pure black (#000000). Always use the `on-background` (#2d3435) to keep the dark tones sophisticated and "ink-like."
*   **DON'T** use standard Material "elevated" cards with heavy shadows. They will make the app look like a generic template. Stick to tonal layering.