package com.ayaan.blackscreen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var grantOverlayButton: Button
    private lateinit var grantAccessibilityButton: Button
    private lateinit var enableOverlayButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        grantOverlayButton = findViewById(R.id.grantOverlayButton)
        grantAccessibilityButton = findViewById(R.id.grantAccessibilityButton)
        enableOverlayButton = findViewById(R.id.enableOverlayButton)

        grantOverlayButton.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        grantAccessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        enableOverlayButton.setOnClickListener {
            val shown = VolumeKeyAccessibilityService.requestShowOverlay()
            if (!shown) {
                statusText.text = "Accessibility service not running yet — toggle it off and on in Settings, then try again."
                return@setOnClickListener
            }
            // Push the app to the background so the overlay isn't just sitting
            // behind the activity window.
            moveTaskToBack(true)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAccessibility = isAccessibilityServiceEnabled()

        grantOverlayButton.isEnabled = !hasOverlay
        grantAccessibilityButton.isEnabled = !hasAccessibility
        enableOverlayButton.isEnabled = hasOverlay && hasAccessibility

        statusText.text = when {
            !hasOverlay -> "Step 1: grant overlay permission"
            !hasAccessibility -> "Step 2: enable the accessibility service"
            else -> "Ready. Tap Enable to go black."
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = "$packageName/${VolumeKeyAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }
}
