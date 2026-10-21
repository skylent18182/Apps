package de.minephone.overlay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(0xFF0B1724.toInt())
        }
        val title = TextView(this).apply {
            text = "MinePhone Overlay"
            textSize = 26f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        val info = TextView(this).apply {
            text = "\nHandyfenster im Hochformat für Minecraft.\n\nHinweis: Android kann WhatsApp nicht einfach als echtes Fenster in das Overlay einbetten. Der Button öffnet die offizielle WhatsApp-App."
            textSize = 16f
            setTextColor(0xFFD4E1ED.toInt())
            gravity = Gravity.CENTER
        }
        val permission = Button(this).apply {
            text = "Über anderen Apps einblenden erlauben"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        }
        val start = Button(this).apply {
            text = "MinePhone-Fenster starten"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startService(Intent(this@MainActivity, PhoneOverlayService::class.java))
                    Toast.makeText(this@MainActivity, "MinePhone-Fenster gestartet", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Bitte zuerst die Overlay-Berechtigung erlauben.", Toast.LENGTH_LONG).show()
                }
            }
        }
        val stop = Button(this).apply {
            text = "Overlay schließen"
            setOnClickListener { stopService(Intent(this@MainActivity, PhoneOverlayService::class.java)) }
        }
        root.addView(title)
        root.addView(info)
        root.addView(permission)
        root.addView(start)
        root.addView(stop)
        setContentView(root)
    }
}
