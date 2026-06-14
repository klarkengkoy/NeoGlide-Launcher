# Project Plan: NeoGlide Launcher (Smart Organizer)

## Status Legend
- **Not Started**: Feature defined but no code written.
- **Pending**: Dependency or sub-feature awaiting logic.
- **In Progress**: Actively being implemented.
- **For Checking**: Implemented by AI; awaiting user verification.
- **Completed**: Implementation verified and finalized by the user.

## Priority Legend
- `[P0]` **Critical**: High-impact bugs or essential interaction fixes.
- `[P1]` **High**: Core launcher features (Drag-and-drop, Folders).
- `[P2]` **Medium**: Advanced customization and structural improvements (V1).
- `[P3]` **Low**: Visual polish, non-essential gestures, and post-V1 features.

NeoGlide Launcher is a smart, clean, and lightweight Android launcher optimized for Pixel-style aesthetics and ergonomics.
It fills the organizational gap in the stock experience by adding intelligent app categorization and "Glide" navigation without compromising battery life.

## Project Brief

Designed as a refined "Pixel Launcher Plus," NeoGlide Launcher prioritizes responsiveness, low resource usage, and clean organization. It adds powerful "Glide" categorization, making it the fastest way to navigate large app libraries.

## Features and Subfeatures

### Home Screen Experience
*   **Completed** | **Visual Foundation**
    - **Completed** | Material 3 dynamic color support.
    - **Completed** | True edge-to-edge handling.
    - **Completed** | White text with deep drop shadows for readability.
    - **Completed** | Larger App Icons (56dp) & Labels (13sp).
*   **Completed** | **Interaction & Gestures**
    - **Completed** | Smooth "Zoom-from-Icon" transitions.
    - **Completed** | Fixed interaction feedback positions.
    - **Completed** | Adaptive grid math for high-aspect ratio devices.
*   **Completed** | **Layout Customization**
    - **Completed** | Icon repositioning (long-press and drag)
    - **Completed** | Drop target visuals (Ghost Target for Icons/Widgets)
    - **Completed** | Precise snapping math (Fix "jumping" on long-press)
    - **Completed** | **Bit-Perfect Drag Initialization**: Resolved "jump to top-left" via formula mirroring.
    - **Completed** | **Unified Overlay Drag System**: Unified repositioning and folder drag-out for zero-drift sync.
    - **Completed** | **Visual Collision Feedback**: Soft red indicator for obstructed drop zones.
    - **Completed** | Universal Collision Avoidance (No displacement policy)
    - **Completed** | **5-Column Grid**: Transitioned to 5-column layout for standard Pixel ergonomics.
*   **Completed** | **App & Folder Management**
    - **Completed** | Folder Creation logic (Dragging icon over icon)
    - **Completed** | Folder Preview UI (2x2 mini-grid)
    - **Completed** | Folder Expansion & Animation
    - **Completed** | Folder Naming and Management
    - **Completed** | Home Screen Cleanup (Remove from Home / Home-to-Hide sync)
    - **Completed** | Add app to existing folder via drag-and-drop
    - **Completed** | Folder Context Menu (Edit Name, Remove Folder)
    - **Completed** | Drag app out of folder
    - **Completed** | Auto-dissolve folders (1 or 0 apps)
    - **Completed** | Folders in App Drawer Categories (Merge & Rendering)
    - **Completed** | App Drawer Folder Enhancements:
        - **Completed** | Dissolve & Contextual Categorization.
        - **Completed** | Multi-select "Move apps here" with sectioned picker.
        - **Completed** | Drag-and-drop into folders with visual feedback.
        - **Completed** | Drag folders to category rail with mini-grid preview.
        - **Completed** | Seamless "Drag-out to Escape" from expanded folders.
        - **Completed** | Persistent renaming with keyboard Done support.
        - **Completed** | Text-only Options menu with sorted category relocation.
*   **Not Started** | **Advanced Layout Management**
    - **Not Started** | `[P3]` Page Manager: UI for reordering/managing multiple pages.
    - **Not Started** | `[P3]` Widget Stacking: Vertical stacking and swiping for widgets.
    - **Not Started** | `[P3]` Cloud Sync: Google Drive integration for layout backups.

*   **Completed** | **App Drawer & Universal Search**
*   **Completed** | **Performance & Smoothness**
    - **Completed** | **Ultra-High-Speed Rail Gliding**: Achieved 120fps "painted-on" feel during rapid Category Rail scrubbing.
    - **Completed** | **Fluid Ghost Target**: Animated grid highlight for smooth, non-jumpy drag-and-drop.
    - **Completed** | **Reactive Hover Feedback**: Magnetic feel where target apps shrink (0.8x) and folders expand (1.2x) during drag.
    - **Completed** | **ViewModel-Driven Layout**: Off-thread grid math (Corner Priority sorting) to eliminate re-composition stutters.
    - **Completed** | **Direct Icon Rendering**: Coil bypass for cached icons using custom `Painter` for instant frame updates.
    - **Completed** | **Adaptive Warm-up**: Prioritized icon loading (Visible -> Immediate Glide -> Full Library) to fill cache silently.
*   **Completed** | **Smart Organization**
    - **Completed** | Bottom-anchored grid for ergonomics.
    - **Completed** | Stable category rail with scrubber.
    - **Completed** | Dynamic category rail indentation.
    - **Completed** | Semantic Gravity: Vertical (Top/Bottom) and Horizontal (Left/Right) anchoring logic.
*   **Completed** | **Universal Search Bar**
    - **Completed** | Animated search bar (Zoom-from-Icon).
    - **Completed** | Local search with Enter-to-launch (Top Hit).
    - **Completed** | "Recently Used" app suggestions
*   **Completed** | **Categorization & Sorting**
    - **Completed** | `[P2]` Drag-and-drop categorization
    - **Not Started** | `[P2]` Manual Reordering: Custom sorting within the drawer.
    - **Not Started** | `[P2]` Category Management: UI for custom categories (add/rename/reorder).

### Widget Support
*   **Completed** | **Essential Widget Hosting**
    - **Completed** | Widget picking, binding, and display.
    - **Completed** | Persistence across restarts.
    - **Completed** | Functional context menu with "Open App" and "Remove".
    - **Completed** | Filtered context menu for Internal Widgets (Remove "Open App")
    - **Completed** | Specific toast feedback for fixed-dimension widgets (Dock).
    - **Completed** | Normalized Dock with unified database persistence.
    - **Completed** | Adaptive Floating Placement (device-agnostic bottom calibration).
    - **Completed** | Seamless long-press-to-drag and resize shadows.
    - **Completed** | **Responsive Widget Hosting**: Implemented `updateAppWidgetSize` for real-time layout "transformation" and adaptive content.
    - **Completed** | **NeoGlide Widget Picker**: Custom 2-step selection UI with high-fidelity, center-aligned previews, global search (by label & app), and widget count badges.
    - **Completed** | **Configuration & Binding Fix**: Resolved "Permission Denial" and implemented mandatory system binding flow for custom hosts.

### Security & Privacy
*   **Completed** | **App Protection**
    - **Completed** | Hidden apps support (Core logic & Drawer visibility).
    - **Completed** | Direct Hidden Apps Management (Bypassed Vault UI).
    - **Completed** | Improved Hidden Apps Picker (A-Z/Recent sorting, Sectioned list).
    - **Completed** | Biometric App Lock: Secure hidden apps with Fingerprint/PIN.
*   **Completed** | **System Hygiene**
    - **Completed** | Self-exclusion of launcher from drawer.
    - **Completed** | Default launcher prompt dialog.

### Settings & Customization
*   **Completed** | **Settings Experience**
    - **Completed** | Modern Bottom Sheet UI for quick settings.
    - **Completed** | Troubleshooting tools (Clear Cache/Reset Layout).
    - **Completed** | Search options toggle.
    - **Completed** | App names visibility (Label modes).
*   **Completed** | **Expansion Options**
    - **Completed** | Side Anchor: UI for side-anchored drawer grid (now implemented as Top/Bottom + Left/Right gravity).
    - **Not Started** | `[P2]` Sorting: UI for sorting apps in the drawer.
    - **Not Started** | `[P2]` Home Grid: UI for customizing home screen grid sizes.
    - **Not Started** | `[P3]` Wallpaper: Direct system integration for wallpaper changes.
    - **Not Started** | `[P2]` Appearance: Custom colors and monochrome icon settings.
    - **Not Started** | `[P3]` Backup (Handoff API): Save or restore launcher layout.
    - **Not Started** | `[P3]` Premium features: Support the developer and unlock advanced options.
    - **Completed** | `[P3]` Troubleshooting: Tools to reset layout or clear icon cache.
    - **Not Started** | `[P3]` Review NeoGlide: Link to Play Store for feedback.
    - **Not Started** | `[P2]` About NeoGlide: Version information and legal details.

### Visual Polish & System Integration
*   **In Progress** | **Theming & Compliance**
    - **Completed** | Official Android Splash Screen API.
    - **Completed** | Adaptive icon & splash screen support.
    - **Completed** | Notification dots with dynamic color.
    - **Completed** | Native Dynamic Icons (LauncherApps refactor).
    - **Pending** | `[P2]` Legal & Compliance (About section).
    - **Completed** | SDK 37 & Tooling Migration.
    - **Not Started** | `[P3]` Advanced Theming: "Forced Mono" engine and custom masking.
*   **Completed** | **Tactile Feedback**
    - **Completed** | Haptic Feedback Engine.
    - **Completed** | Dynamic Clock/Calendar icons.
*   **Completed** | **Drawer Improvements**
    - **Completed** | `[P2]` Category Rail Refinement: Improve touch mapping and state persistence for the category rail.

## AI Instructions
*   **Verification**: The AI must NEVER mark a task as `Completed`. Only the user can verify and authorize the transition from `For Checking` to `Completed`.
*   *   **Progression**: Tasks should only advance by one level per interaction (e.g., `Not Started` -> `Pending`, `Pending` -> `In Progress`, `In Progress` -> `For Checking`).
*   *   **Documentation**: Always update the `plan.md` before starting work on a new state transition.

## Architectural Decisions
- **Theming**: Strict use of semantic colors from `MaterialTheme.colorScheme`.
- **Global Skills**: Shared `.agents` directory for cross-project intelligence, located at `C:\Dev\Android\.agents`.
- **Incremental Sync**: Differential repository updates to preserve metadata.
- **UI Consistency**: Centralized `AppItem` for Home and Drawer parity.
- **Minimalism**: Focus on widgets; no redundant static headers.
- **Drawer Ergonomics**: Bottom-anchored layout for better one-handed use.
- **Widget Normalization**: All core components (Dock) are standard `WidgetEntity` entries.
- **Placeholder Calibration**: Use special row values (99.5f) to trigger one-time physical coordinate discovery.
- **Semantic Gravity**: Use invisible spacers in the `LazyVerticalGrid` to implement non-standard fill patterns (e.g., Bottom-Right) without breaking standard grid alignment or coordinate math.
- **Enum-Driven Settings**: Moved complex layout toggles (Anchor, Category Bar, Sorting) to type-safe enums and `SelectionDialog` menus for better scalability and descriptive guidance.
- **Modular Descriptions**: Relocated technical descriptions from the main settings list into individual dialogs to maintain a clean Material 3 "Settings" surface.
- **Project Rebranding**: Completed full transition from "Pxl Launcher Neo" to **NeoGlide Launcher: Smart Organizer**, including package cleanup, metadata refactoring, and Firebase migration.
