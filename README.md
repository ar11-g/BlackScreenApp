# Black Screen

Fullscreen black overlay app. Enable from the app; disable by holding
**Volume Down for 10 seconds** while the overlay is showing.

## What it does

- `VolumeKeyAccessibilityService` draws the black overlay itself, using
  `TYPE_ACCESSIBILITY_OVERLAY` (not `TYPE_APPLICATION_OVERLAY`). This window
  type sits higher in Android's z-order — it also covers the screen-pinning
  "to unpin, hold back + overview" hint and most system toasts, which a
  standard app overlay can't reach. This is the same technique screen dimmer
  apps like Darker use, and it doesn't require any foreground service.
- The same service intercepts Volume Down (10-second hold dismisses the
  overlay) and Volume Up (always swallowed while the overlay is active, so
  it can't pop the system volume slider on top of it) — but only while the
  overlay is showing. Normal volume behaviour is untouched otherwise.



## First-run setup on the phone

1. Open the app.
2. Tap **Grant Overlay Permission** → allow "Display over other apps".
3. Tap **Enable Accessibility Service** → find "Black Screen" in the list →
   turn it on (Android will show a warning dialog about accessibility
   services reading input; this is expected — that's how the volume-hold
   detection works).
4. Back in the app, tap **Enable Black Overlay**.
5. To exit: hold Volume Down for a full 10 seconds.

## Signing note

The release build is signed with the debug keystore for simplicity, so the
GitHub Actions output installs directly without extra signing steps. This is
fine for personal sideloading; it is **not** suitable for Play Store
distribution.
