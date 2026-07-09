package com.ayaan.blackscreen

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
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

        // Volume Down: 10-second hold dismisses the overlay.
        // Volume Up: always swallowed while active, so it can't pop the
        // system volume slider on top of the overlay.
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

        view.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.addView(view, params)
        overlayView = view
        isOverlayActive = true
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
