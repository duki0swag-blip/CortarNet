package com.example.cortarnet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private var button: Button? = null
    private var activo = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        crearNotificacion()
        crearBoton()
    }

    private fun crearNotificacion() {
        val canal = "cortarnet"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(canal, "CortarNet", NotificationManager.IMPORTANCE_LOW)
        )
        val notif = Notification.Builder(this, canal)
            .setContentTitle("CortarNet activo")
            .setContentText("Botón flotante en pantalla")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        startForeground(1, notif)
    }

    private fun crearBoton() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        val btn = Button(this)
        actualizarAspecto(btn)

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        btn.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = e.rawX
                    touchY = e.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (e.rawX - touchX).toInt()
                    params.y = startY + (e.rawY - touchY).toInt()
                    wm.updateViewLayout(v, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (abs(e.rawX - touchX) < 10 && abs(e.rawY - touchY) < 10) alternar()
                    true
                }
                else -> false
            }
        }

        button = btn
        wm.addView(btn, params)
    }

    private fun alternar() {
        activo = !activo
        val intent = Intent(this, BlockVpnService::class.java)
        intent.action = if (activo) BlockVpnService.ACTION_START else BlockVpnService.ACTION_STOP
        startService(intent)
        button?.let { actualizarAspecto(it) }
    }

    private fun actualizarAspecto(b: Button) {
        if (activo) {
            b.text = "SIN NET\n(ON)"
            b.setBackgroundColor(Color.RED)
        } else {
            b.text = "CON NET\n(OFF)"
            b.setBackgroundColor(Color.parseColor("#2E7D32"))
        }
        b.setTextColor(Color.WHITE)
    }

    override fun onDestroy() {
        button?.let { wm.removeView(it) }
        button = null
        super.onDestroy()
    }
}
