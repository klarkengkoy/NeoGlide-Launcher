# Project Plan: Pxl Launcher Neo (Pixel-First Lightweight Edition)

## Status Legend
- `[ ]` **Not Started**: Task defined but no code written.
- `[/]` **In Progress**: Actively being implemented.
- `[~]` **For Checking**: Implemented by AI; awaiting user verification.
- `[x]` **Completed**: Implementation verified and finalized.

## Priority Legend
- `[P0]` **Critical**: High-impact bugs or essential interaction fixes.
- `[P1]` **High**: Core launcher features (Drag-and-drop, Folders).
- `[P2]` **Medium**: Advanced customization and structural improvements.
- `[P3]` **Low**: Visual polish, non-essential gestures, and compliance.

Pxl Launcher Neo is a modern, ultra-lightweight Android launcher specifically optimized for Google Pixel users.
 It fills the organizational gap in the stock Pixel experience by adding intelligent app categorization and organization without compromising the clean, "stock" Android aesthetic or battery life.

## Project Brief

Designed as the definitive "Pixel Launcher Plus," PxlLauncher prioritizes responsiveness, low resource usage, and clean organization. It adds powerful categorization that Google missed, making it the fastest way to navigate large app libraries on Pixel devices.

### Features
*   **Stock-Plus Home Screen**: A clean, Pixel-inspired interface with adaptive icons and an ultra-minimalist layout.
*   **Pixel-Optimized Drawer**: Intelligent categorization (Communication, Social, Games, etc.) added to the familiar Pixel drawer layout, including a high-speed "scrubbing" selector.
*   **Incremental Package Management**: Efficient background syncing that only processes changes (installs/uninstalls), preserving battery life.
*   **Instant Universal Search**: Unified local and web search with "Top Hit" logic for immediate app launching.
*   **Deep Theming Engine**: Native integration with Material You dynamic color schemes.
*   **Adaptive Icon support**: Explicit handling of Adaptive Icons for a perfect, consistent look on every app.

### High-Level Technical Stack
*   **Language**: Kotlin (100%)
*   **UI Framework**: Jetpack Compose with Material 3
*   **Architecture**: MVVM with Clean Architecture
*   **Reactive Programming**: Kotlin Coroutines and Flow
*   **Persistence**: Room (KSP) with efficient diffing logic
*   **Image Loading**: Coil (Compose-optimized)

## Implementation Steps

### Task_1_Core_Logic_and_Data: Set up the core architecture including Hilt, Room, and a repository to fetch and categorize apps.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - [x] Hilt configured for singleton scoped repositories.
  - [x] Room database with `AppEntity` and `AppDao`.
  - [x] Incremental sync logic for package changes.

### Task_2_Theming_and_Home_Screen: Implement Material 3 dynamic colors, edge-to-edge display, and Home screen UI.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - [x] Dynamic color support (Android 12+).
  - [x] True edge-to-edge handling with window insets.
  - [x] Adaptive icon rendering with circular masks.

### Task_3_App_Drawer_and_Universal_Search: Develop the Smart Categorized App Drawer and Universal Search bar.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - [x] Responsive drawer with "Bottom-Anchored" layout (A-Z standard flow, anchored to bottom for ergonomics).
  - [x] Dynamic category rail indentation with pixel-perfect measurement logic.
  - [x] Redesigned header with integrated category title and quick-action icons (Search, Play Store, Pxl Settings, System Settings).
  - [x] Premium "Zoom-from-Icon" search bar animation with 400ms duration.
  - [x] Local app search with debounced web suggestions and instant launching.

### Task_4_Refinement_and_Optimization: Optimize icon handling, context menus, and overall system integration.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - [x] Long-press menus for App Info and Uninstall.
  - [x] Asynchronous icon fetching.
  - [x] BroadcastReceiver for real-time package updates.

### Task_5_Pixel_Polish_and_Performance: Refine scrubbing logic, implement "Top Hit" search, and add the Pixel-style Home Header.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - [x] **Stable Category Scrubber:** Improved vertical zone calculation for zero-jitter sliding.
  - [x] **Top Hit Search:** Pressing Enter in search launches the first result immediately.
  - [x] **Thumb-Friendly Ergonomics:** Bottom-anchored grid layout that keeps apps within thumb reach while maintaining standard A-Z order.
  - [x] **Intelligent Default Dock:** Robust detection of browser, dialer, messages, and camera.
  - [x] **Material You Mono Icons:** Refined monochromatic icon support with proper safe-zone scaling (32dp) and fallback logic.
  - [x] **App Name Readability:** Implemented white text with deep drop shadows for home screen labels to ensure visibility on all wallpapers.

### Task_6_Architectural_Refinement: Consolidate UI components and optimize performance.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - [x] **Component Centralization:** Created `AppItem.kt` to unify app rendering across Home and Drawer.
  - [x] **Code Cleanup:** Removed unused imports and redundant UI logic.
  - [x] **Performance:** Minimized recompositions by lifting state and using stable data models.

### Task_7_Release_Readiness [P2]: Implement core features for Play Store compliance and user expectations.
- **Status:** FOR_CHECKING
- **Acceptance Criteria:**
  - [x] **Self-Exclusion:** Launcher hides its own icon from the app drawer and home screen.
  - [x] **Default Launcher Prompt:** Dismissible dialog for setting as default launcher.
  - [~] [P2] **Hidden Apps:** Support for hiding apps from the drawer with a toggleable "Hidden" category.
  - [x] **Wallpaper Picker:** Long-press home screen to change wallpaper (system integration).
  - [~] [P2] **Settings Refinement:** Modern Bottom Sheet UI with quick actions (shortcuts, folders, categories) and a "Show all settings" expansion pill.
  - [x] **App Shortcuts:** Integrated `LauncherApps` shortcuts with icons into the long-press context menu.
  - [x] **Home Screen Context Menu:** Long-press home screen to access wallpapers and settings.

### Task_8_Premium_Polish: Refine animations and performance.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - [x] **Advanced Transitions:** Improved "Zoom-from-Icon" app opening animation by centering the scale-up on the icon.
  - [x] **Baseline Profiles:** Configured R8 and Baseline Profiles for zero-lag startup.

### Task_9_Launcher_Basics: Implement essential home screen functionality.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - [x] **Widget Hosting:** Ability to pick, bind, and display system widgets on the home screen.
  - [x] **Widget Persistence:** Save and restore widget configurations across restarts.
  - [x] **Widget Interaction:** Support resizing, moving, and removing widgets.
  - [x] **Home Context Menu Update:** Add "Widgets" option to the home screen long-press menu.

### Task_10_Play_Store_Readiness [P2/P3]: Final polish for Google Play Store submission.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - [x] **Modern Splash Screen**: Implement official Android Splash Screen API with brand-aligned animations.
  - [x] **Predictive Search**: "Recently Used" app suggestions at the top of the search interface.
  - [ ] [P3] **Legal & Compliance (Low Priority)**: "About" section in settings with Privacy Policy, versioning, and OSS licenses.
  - [x] **Icon Pack Support**: Initial implementation for standard third-party icon packs (Architecture).
  - [ ] [P2] **Drag-and-Drop Categorization**: Long-press an app and drag it to a category icon to move it.
  - [x] **Notification Dots**: Lifecycle-aware notification listeners with Material You colored badges.
  - [x] **Dark Mode & Dynamic Color**: Full adaptive UI support for Material You theming and Dark Mode.
    - [x] Fixed search result text elements not adapting to dark mode.
  - [x] [P0] **Build Performance**: Enabled configuration caching and optimized JVM memory to fix build-time warnings and stutter.
  - [ ] [P1] **SDK 37 Migration**: Update `compileSdk` and `targetSdk` to 37 and address any new API changes or deprecations.

### Task_11_Settings_Expansion [P2/P3]: Expand Pxl Settings with high-utility configurations.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - [x] **Home Customization**: Grid size (4x5, 5x5, 6x6).
  - [x] **Search Options**: Toggle between local-only and web-enabled search.
  - [x] **Lock Layout**: Option to prevent accidental icon/widget movement.
  - [ ] [P2] **Drawer Layout**: Toggle for "Bottom-Anchored" vs "Top-Anchored" app grid.
  - [ ] [P2] **Drawer Gravity**: Decoupled horizontal anchoring (Left/Right) for the bottom-anchored drawer.
  - [/] [P2] **Gestures**: Double tap to sleep and Swipe down for notifications.
  - [x] **Troubleshooting**: Added Clear Icon Cache and Reset Layout utility actions.

### Task_12_Home_Screen_Repositioning [P1]: Implement drag-and-drop repositioning for home screen elements.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - [/] [P1] **Icon Repositioning**: Long-press and drag icons to move them within the home screen grid.
  - [ ] [P1] **Drop Target Visuals**: Highlight valid drop zones during icon/widget drag.
  - [ ] [P1] **Auto-Rearrange**: Nearby icons shift to make room for dragged elements.

### Task_13_UI_Bugs_and_Polish [P0]: Address reported UI issues and interaction bugs.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - [x] [P0] **Interaction Feedback Position**: Fix issue where tapping at the top of an item shows feedback at the bottom.
  - [x] **Home Screen Rounding**: Further refine Home screen elements to remove remaining "sharpness".
  - [x] [P0] **Widget Interaction Polish**: Fixed "app opening" on widget release and accidental dock clicks during edit mode.
  - [x] [P0] **Adaptive Grid**: Optimized grid math to fill screen on high-aspect ratio devices (Pixel 9 Pro).
  - [x] [P0] **Default Dock Apps**: Fixed incorrect default dock apps and order (Browser, Phone, Messages, Camera) on Pixel devices.

### Task_14_Widget_Refinement [P2]: Advanced widget management and grid optimization.
- **Status:** NOT_STARTED
- **Acceptance Criteria:**
  - [ ] [P2] **Z-Ordering**: Add "Move to Front" and "Move to Back" options for overlapping widgets in the context menu.
  - [ ] [P2] **Finer Grid**: Increase grid resolution (sub-grid lines) to allow for more precise widget placement.
  - [x] **Proportional Handles**: Widget resize handles scale proportionally to the widget's edge length.

### Task_15_Modular_Dock [P2]: Transform the dock into a flexible widget-like container.
- **Status:** NOT_STARTED
- **Acceptance Criteria:**
  - [ ] [P2] **Dynamic Resize**: Allow the dock to be resized or moved like a widget.
  - [ ] [P2] **Custom Slot Count**: Allow users to add or remove app slots from the dock.
  - [ ] [P2] **Interchangeability**: Allow standard widgets to be placed in the dock area and vice versa.

### Task_16_Folder_Support [P1]: Implement Home Screen folders for icon organization.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - [/] [P1] **Folder Creation**: Dragging one icon over another on the home screen creates a folder.
  - [ ] [P1] **Folder Expansion**: Pixel-style rounded expansion animation when tapping a folder.
  - [ ] [P1] **Folder Management**: Ability to name folders and drag items in/out.

### Task_17_System_Polish_and_Haptics [P3]: Deep integration and tactile refinements.
- **Status:** NOT_STARTED
- **Acceptance Criteria:**
  - [ ] [P3] **Haptic Feedback Engine**: Context-aware vibrations for scrubbing, dragging, and long-pressing.
  - [ ] [P3] **Dynamic Icons**: Real-time clock and calendar updates on supported app icons.
  - [ ] [P3] **Search Glow**: Material You colored radiant glow transition when opening search.

## Post-Launch Roadmap

### Free Tier Improvements
*   **Manual Reordering**: Drag-and-drop apps within the drawer to custom positions (enabled via "Manual Sort" in Settings).
*   **Wallpaper Shortcut**: System integration in settings.
*   **Performance Monitoring**: Integration of Firebase Crashlytics for stability tracking.

### Premium Features (Paid)
*   **Page Manager**: Advanced UI for reordering and managing multiple home screen pages.
*   **Category Management**: UI for adding, renaming, and reordering custom categories.
*   **Biometric App Lock**: Secure hidden/private apps with Fingerprint or PIN.
*   **Advanced Theming**: "Forced Mono" engine for legacy icons and custom icon masking.
*   **Widget Stacking**: Vertical stacking and swiping for home screen widgets.
*   **Cloud Sync**: Google Drive integration for layout and configuration backups.

## Architectural Decisions
- **Theming & Accessibility**: All UI components MUST use semantic colors from `MaterialTheme.colorScheme` (e.g., `onSurface`, `surfaceVariant`). Hardcoded colors (hex or `Color.White/Black`) are prohibited to ensure automatic support for Dark Mode and Material You dynamic theming.
- **Global Skills**: Moved the `.agents` directory to `C:\Dev\Android\.agents` to enable global skill sharing across all Android projects.
- **Incremental Sync**: Optimized `AppRepository` to perform differential updates instead of full database wipes to preserve metadata like `lastUsedTime`.
- **UI Consistency**: Moved `AppItem` and `SearchAppItem` to a shared components file to ensure visual and functional parity between Home and Drawer.
- **Contrast System**: Forced white text with black shadows for Home screen labels to solve legibility issues on custom wallpapers.
- **Themed Icons**: Decided against "forcing" monochrome on legacy apps to preserve visual clarity; using official system layers only.
- **Minimalism**: Removed the static Home Header (clock/date) to maximize space for user widgets and maintain a "Pixel-First" clean aesthetic.
- **Navigation**: Using `SharedTransitionScope` for seamless app opening animations.
- **Drawer Ergonomics**: Switched to a "Bottom-Anchored" layout where apps flow A-Z from the top of the grid, but the entire grid remains anchored to the bottom for thumb accessibility when not full. Replaced "Bottom-Priority" terminology for clarity.
