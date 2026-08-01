# Architecture Decision Record (ADR): Compose Preview Capture Strategy

- **Status**: Accepted & Frozen
- **Date**: 2026-08-01
- **Subsystem**: Orbit Browser Decoupled Tab Preview Subsystem

---

## 1. Context

Orbit Browser requires a decoupled, non-intrusive preview subsystem capable of capturing scaled thumbnail snapshots of active browser tabs. Browser tabs may host native WebViews (`OBWebView`) or Jetpack Compose UI screens (`HomeScreen`, `Settings`, `Bookmarks`, `History`, `Downloads`, `PasswordVault`, `NewsHub`).

To maintain consistent browser performance and prevent UI thread stutters, preview generation must execute off the main thread, utilize configurable scheduling policies (`SchedulePolicy`), and remain completely decoupled from tab management logic.

---

## 2. Problem Statement

Selecting a bitmap capture mechanism for Jetpack Compose screens must satisfy strict performance, rendering fidelity, and memory safety requirements:

1. **Rendering Fidelity**: Must accurately capture frosted glass blurs, custom AGSL shaders, weather material effects, dynamic themes, and vector graphics.
2. **Memory Safety**: Must avoid memory leaks when screens are unmounted or destroyed.
3. **Compatibility**: Must provide broad compatibility across supported Android API levels (API 26 through 35+).
4. **Non-Blocking**: Must not block the UI thread during scrolling, animations, or compositing.

---

## 3. Solution & Selected Approach

We selected **`LocalView.current` + `View.draw(Canvas)` scaling** implemented in `ComposePreviewProvider`:

- **Mechanism**: Obtains the root `AndroidOwner` View via `LocalView.current` held in a `WeakReference<View>`. Draws the view hierarchy onto an `ARGB_8888` Canvas scaled to max width 600px on `Dispatchers.Main`.
- **Scheduling**: Dispatched via `PreviewManager` under `SchedulePolicy.Debounced(PreviewTimingDefaults.COMPOSE_SETTLE_DELAY_MS)`.

---

## 4. Alternatives Considered & Rejection Rationale

1. **`PixelCopy` (Surface API)**:
   - *Rejected*: Requires active hardware window surface. Fails on unmounted sub-views and forces display V-Sync synchronization (~16ms block), causing micro-stutters during user interactions.
2. **`GraphicsLayer.toImageBitmap()`**:
   - *Rejected*: Requires Android 12+ (API 31+). Restricted compatibility on Android 8.0 through 11 devices, and omits parent window overlays.
3. **`RenderNode` Hardware Canvas**:
   - *Rejected*: Requires API 29+ and produces `HardwareBuffer` objects that cannot be safely managed inside a standard JVM `LruCache` without native heap leakage risk.
4. **Off-screen Compose Sub-composition**:
   - *Rejected*: Creates duplicate component state, re-executes ViewModel side-effects, and fails to sample screen backdrop buffers for frosted glass blurs.

---

## 5. Trade-offs & Future Considerations

- **Trade-off**: `View.draw(Canvas)` pass executes on `Dispatchers.Main` for ~1.5ms to 2.5ms. This minor overhead is mitigated via `SchedulePolicy.Debounced` conflation.
- **Developer Note / Future Validation**: Validate preview correctness after future weather particle effects, advanced AGSL shaders, and animated glass materials are implemented.
