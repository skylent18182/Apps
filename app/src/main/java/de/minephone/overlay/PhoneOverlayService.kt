package de.minephone.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class PhoneOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var phone: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (phone == null) showPhone()
        return START_NOT_STICKY
    }

    private fun showPhone() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 18, 14, 14)
            background = GradientDrawable().apply {
                setColor(0xFF07131E.toInt())
                cornerRadius = 42f
                setStroke(3, 0xFF34495B.toInt())
            }
        }
        val top = TextView(this).apply {
            text = "●  MinePhone                                  ━  ▰"
            textSize = 12f
            setTextColor(0xFFBBD0E1.toInt())
            setPadding(6, 0, 6, 12)
        }
        val title = TextView(this).apply {
            text = "MinePhone"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 10, 0, 16)
        }
        val subtitle = TextView(this).apply {
            text = "Minecraft overlay"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(0xFF8EA9BE.toInt())
        }
        val open = Button(this).apply {
            text = "WhatsApp öffnen"
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launch)
                    Toast.makeText(this@PhoneOverlayService, "WhatsApp wird in seiner eigenen App geöffnet.", Toast.LENGTH_LONG).show()
                } else {
                    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.whatsapp.com/android"))
                    web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(web)
                }
            }
        }
        val note = TextView(this).apply {
            text = "Android lässt die echte WhatsApp-Oberfläche nicht einfach in dieses Fenster einbetten. Zum Chatten öffnet sich WhatsApp separat."
            textSize = 12f
            setTextColor(0xFFD4E1ED.toInt())
            setPadding(6, 8, 6, 12)
        }
        val close = Button(this).apply {
            text = "Handy schließen  ✕"
            setOnClickListener { stopSelf() }
        }
        shell.addView(top)
        shell.addView(title)
        shell.addView(subtitle)
        shell.addView(open)
        shell.addView(note)
        shell.addView(close)

        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.78f).toInt().coerceAtMost(440),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }
        // Drag by touching the header. FLAG_NOT_FOCUSABLE keeps Minecraft touch usable,
        // but means controls in the overlay may not receive input reliably on every device.
        top.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x; initialY = params.y
                        touchX = event.rawX; touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - touchX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        try { wm.updateViewLayout(shell, params) } catch (_: Exception) {}
                        return true
                    }
                }
                return true
            }
        })
        phone = shell
        wm.addView(shell, params)
    }

    override fun onDestroy() {
        phone?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        phone = null
        super.onDestroy()
    }
}
