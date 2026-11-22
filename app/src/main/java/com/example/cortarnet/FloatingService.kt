package com.example.cortarnet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

class FloatingService : Service() {

    companion object {
        @Volatile var running = false
        @Volatile var bloqueando = false
        @Volatile var actual: FloatingService? = null
    }

    private lateinit var wm: WindowManager
    private var vista: BotonFlotanteView? = null
    private var params: WindowManager.LayoutParams? = null
    private var activo = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        actual = this
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
        val tamDp = getSharedPreferences("cortarnet", MODE_PRIVATE).getInt("tam", 58)

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
            alpha = 0.95f
        }
        params = p

        val btn = BotonFlotanteView(this, tamDp)

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        btn.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = p.x
                    startY = p.y
                    touchX = e.rawX
                    touchY = e.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = startX + (e.rawX - touchX).toInt()
                    p.y = startY + (e.rawY - touchY).toInt()
                    wm.updateViewLayout(v, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (abs(e.rawX - touchX) < 10 && abs(e.rawY - touchY) < 10) alternar()
                    true
                }
                else -> false
            }
        }

        vista = btn
        wm.addView(btn, p)
    }

    fun cambiarTamano(dp: Int) {
        val v = vista ?: return
        val p = params ?: return
        v.cambiarTamano(dp)
        try {
            wm.updateViewLayout(v, p)
        } catch (e: Exception) {
        }
    }

    private fun alternar() {
        activo = !activo
        bloqueando = activo
        val intent = Intent(this, BlockVpnService::class.java)
        intent.action = if (activo) BlockVpnService.ACTION_START else BlockVpnService.ACTION_STOP
        startService(intent)
        vista?.activo = activo
    }

    override fun onDestroy() {
        running = false
        bloqueando = false
        actual = null
        vista?.let {
            try {
                wm.removeView(it)
            } catch (e: Exception) {
            }
        }
        vista = null
        try {
            startService(Intent(this, BlockVpnService::class.java).setAction(BlockVpnService.ACTION_STOP))
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}

/** Botón flotante: cuadrado oscuro con esquinas redondeadas y un play blanco. */
class BotonFlotanteView(context: Context, tamDp: Int) : View(context) {

    var activo: Boolean = false
        set(valor) {
            field = valor
            invalidate()
        }

    private val fondo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val play = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val stop = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val triangulo = Path()
    private var tam = (tamDp * resources.displayMetrics.density).toInt()

    fun cambiarTamano(tamDp: Int) {
        tam = (tamDp * resources.displayMetrics.density).toInt()
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(tam, tam)
    }

    override fun onDraw(canvas: Canvas) {
        val s = width.toFloat()
        // Gris oscuro = listo (con internet). Rojo oscuro = bloqueando (sin internet).
        fondo.color = if (activo) Color.parseColor("#7A1F1F") else Color.parseColor("#2F2F2F")
        canvas.drawRoundRect(0f, 0f, s, s, s * 0.26f, s * 0.26f, fondo)

        if (activo) {
            canvas.drawRoundRect(s * 0.30f, s * 0.30f, s * 0.70f, s * 0.70f, s * 0.08f, s * 0.08f, stop)
        } else {
            play.pathEffect = CornerPathEffect(s * 0.09f)
            triangulo.reset()
            triangulo.moveTo(s * 0.33f, s * 0.24f)
            triangulo.lineTo(s * 0.33f, s * 0.76f)
            triangulo.lineTo(s * 0.80f, s * 0.50f)
            triangulo.close()
            canvas.drawPath(triangulo, play)
        }
    }
}
