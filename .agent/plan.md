# Project Plan: Pxl Launcher Neo (Pixel-First Lightweight Edition)

## Status Legend
- **Not Started**: Feature defined but no code written.
- **Pending**: Dependency or sub-feature awaiting logic.
- **In Progress**: Actively being implemented.
- **For Checking**: Implemented by AI; awaiting user verification.
- **Completed**: Implementation verified and finalized by the user.

## Priority Legend
- `[P0]` **Critical**: High-impact bugs or essential interaction fixes.
- `[P1]` **High**: Core launcher features (Drag-and-drop, Folders).
- `[P2]` **Medium**: Advanced customization and structural improvements.
- `[P3]` **Low**: Visual polish, non-essential gestures, and compliance.

Pxl Launcher Neo is a modern, ultra-lightweight Android launcher specifically optimized for Google Pixel users.
It fills the organizational gap in the stock Pixel experience by adding intelligent app categorization and organization without compromising the clean, "stock" Android aesthetic or battery life.

## Project Brief

Designed as the definitive "Pixel Launcher Plus," PxlLauncher prioritizes responsiveness, low resource usage, and clean organization. It adds powerful categorization that Google missed, making it the fastest way to navigate large app libraries on Pixel devices.

## Features and Subfeatures

### Home Screen Experience
*   **Completed** | **Visual Foundation**
    - **Completed** | Material 3 dynamic color support.
    - **Completed** | True edge-to-edge handling.
    - **Completed** | White text with deep drop shadows for readability.
*   **Completed** | **Interaction & Gestures**
    - **Completed** | Smooth "Zoom-from-Icon" transitions.
    - **Completed** | Fixed interaction feedback positions.
    - **Completed** | Adaptive grid math for high-aspect ratio devices.
*   **Completed** | **Layout Customization**
    - **Completed** | Icon repositioning (long-press and drag)
    - **Completed** | Drop target visuals (Ghost Target for Icons/Widgets)
    - **Completed** | Precise snapping math (Fix "jumping" on long-press)
    - **Completed** | Universal Collision Avoidance (No displacement policy)
*   **In Progress** | **App & Folder Management**
    - **Completed** | Folder Creation logic (Dragging icon over icon)
    - **Completed** | Folder Preview UI (2x2 mini-grid)
    - **Completed** | Folder Expansion & Animation
    - **Completed** | Folder Naming and Management
    - **Completed** | Home Screen Cleanup (Remove from Home / Home-to-Hide sync)
    - **Completed** | Add app to existing folder via drag-and-drop
    - **Completed** | Folder Context Menu (Edit Name, Remove Folder)
    - **Completed** | Drag app out of folder
    - **Completed** | Auto-dissolve folders (1 or 0 apps)
    - **Pending** | `[P2]` Auto-rearrange icons
*   **Not Started** | **Advanced Layout Management**
    - **Not Started** | Page Manager: UI for reordering/managing multiple pages.
    - **Not Started** | Widget Stacking: Vertical stacking and swiping for widgets.
    - **Not Started** | Cloud Sync: Google Drive integration for layout backups.

### App Drawer & Universal Search
*   **Completed** | **Smart Organization**
    - **Completed** | Bottom-anchored grid for ergonomics.
    - **Completed** | Stable category rail with scrubber.
    - **Completed** | Dynamic category rail indentation.
*   **Completed** | **Universal Search Bar**
    - **Completed** | Animated search bar (Zoom-from-Icon).
    - **Completed** | Local search with Enter-to-launch (Top Hit).
    - **Completed** | "Recently Used" app suggestions
*   **Pending** | **Categorization & Sorting**
    - **Pending** | `[P2]` Drag-and-drop categorization
    - **Pending** | `[P2]` Drawer layout toggle (Bottom vs Top-anchored)
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
*   **In Progress** | **Expansion Options**
    - **Not Started** | `[P2]` Home customization (Grid sizes).
    - **Completed** | Search options toggle.
    - **Completed** | App names visibility (Label modes).
    - **Not Started** | `[P3]` Wallpaper Shortcut: Direct system integration in settings.

### Visual Polish & System Integration
*   **In Progress** | **Theming & Compliance**
    - **Completed** | Official Android Splash Screen API.
    - **Completed** | Notification dots with dynamic color.
    - **Pending** | `[P3]` Legal & Compliance (About section).
    - **Completed** | SDK 37 & Tooling Migration.
    - **Not Started** | Advanced Theming: "Forced Mono" engine and custom masking.
*   **Pending** | **Tactile Feedback**
    - **Pending** | `[P3]` Haptic Feedback Engine.
    - **Pending** | `[P3]` Dynamic Clock/Calendar icons.

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
