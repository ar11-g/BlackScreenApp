package com.ayaan.blackscreen

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class VolumeKeyAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var holdRunnable: Runnable? = null
    private var overlayView: View? = null

    private val HOLD_DURATION_MS = 10_000L

    companion object {
        @Volatile
        private var instance: VolumeKeyAccessibilityService? = null

        @Volatile
        var isOverlayActive: Boolean = false
            private set

        /** Called from MainActivity. Returns false if the service isn't running yet. */
        fun requestShowOverlay(): Boolean {
            val svc = instance ?: return false
            svc.showOverlay()
            return true
        }

        fun requestHideOverlay() {
            instance?.hideOverlay()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isOverlayActive) return false

        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (holdRunnable == null) {
                            val runnable = Runnable { hideOverlay() }
                            holdRunnable = runnable
                            handler.postDelayed(runnable, HOLD_DURATION_MS)
                        }
                    }
                    KeyEvent.ACTION_UP -> {
                        holdRunnable?.let { handler.removeCallbacks(it) }
                        holdRunnable = null
                    }
                }
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> true
            else -> false
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val view = View(this).apply { setBackgroundColor(Color.BLACK) }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.OPAQUE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Dim to the lowest possible level while the overlay is up. This is
        // a per-window override — Android applies it only while this window
        // is on screen and restores the previous brightness automatically
        // when it's removed. No WRITE_SETTINGS permission needed.
        params.screenBrightness = 0f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Modern API: actually hides the status bar (and with it the
            // location/mic privacy dot), rather than just the legacy
            // systemUiVisibility flags which One UI can ignore.
            view.windowInsetsController?.let { applyHiddenBars(it) }
            // The system likes to re-show bars on its own (new toast,
            // notification, etc). Re-hide every time insets change.
            view.setOnApplyWindowInsetsListener { v, insets ->
                v.windowInsetsController?.let { applyHiddenBars(it) }
                insets
            }
        } else {
            @Suppress("DEPRECATION")
            view.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.addView(view, params)
        overlayView = view
        isOverlayActive = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Re-apply once more after attach, since the controller is only
            // reliably available once the view has a window.
            view.post { view.windowInsetsController?.let { applyHiddenBars(it) } }
        }
    }

    private fun applyHiddenBars(controller: WindowInsetsController) {
        controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun hideOverlay() {
        holdRunnable = null
        overlayView?.let {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.removeView(it)
            overlayView = null
        }
        isOverlayActive = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used; this service only cares about key events and the overlay.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        instance = null
    }
}
