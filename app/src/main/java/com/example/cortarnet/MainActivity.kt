
package com.example.cortarnet

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    private val REQ_VPN = 100
    private val REQ_NOTIF = 101

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private val FONDO = Color.parseColor("#08050B")
    private val TARJETA = Color.parseColor("#120E18")
    private val BORDE = Color.parseColor("#2A2133")
    private val VERDE = Color.parseColor("#2ECC71")
    private val GRIS = Color.parseColor("#7B7585")

    private lateinit var orbe: OrbView
    private lateinit var puntoEstado: View
    private lateinit var textoEstado: TextView
    private lateinit var chip: TextView
    private lateinit var boton: Button

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun forma(color: Int, borde: Int, radio: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radio).toFloat()
            setStroke(dp(1), borde)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = FONDO
        window.navigationBarColor = FONDO

        val scroll = ScrollView(this).apply {
            setBackgroundColor(FONDO)
            isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(36), dp(16), dp(28))
        }
        scroll.addView(col)

        // Título
        col.addView(TextView(this).apply {
            text = "CortarNet"
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4), 0, 0, dp(16))
        })

        // Tarjeta 1: orbe + línea + estado
        val tarjeta1 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = forma(TARJETA, BORDE, 28)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        orbe = OrbView(this)
        tarjeta1.addView(orbe, LinearLayout.LayoutParams(MATCH, dp(280)))

        val pildora = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = forma(Color.parseColor("#0E0B13"), BORDE, 30)
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        puntoEstado = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(GRIS)
            }
        }
        pildora.addView(puntoEstado, LinearLayout.LayoutParams(dp(10), dp(10)))
        textoEstado = TextView(this).apply {
            text = "INACTIVO"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setPadding(dp(14), 0, 0, 0)
        }
        pildora.addView(textoEstado)
        tarjeta1.addView(pildora, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
        col.addView(tarjeta1, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(16) })

        // Tarjeta 2: SESSION + botón Activar
        val tarjeta2 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = forma(TARJETA, BORDE, 28)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val cabecera = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        cabecera.addView(TextView(this).apply {
            text = "SESSION"
            setTextColor(Color.parseColor("#D0CCD8"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }, LinearLayout.LayoutParams(0, WRAP, 1f))
        chip = TextView(this).apply {
            text = "INACTIVO"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(GRIS)
            background = forma(Color.parseColor("#0E0B13"), BORDE, 20)
            setPadding(dp(14), dp(7), dp(14), dp(7))
        }
        cabecera.addView(chip)
        tarjeta2.addView(cabecera, LinearLayout.LayoutParams(MATCH, WRAP))

        boton = Button(this).apply {
            text = "Activar"
            isAllCaps = false
            textSize = 20f
            setTextColor(Color.WHITE)
            stateListAnimator = null
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#4A5272"), Color.parseColor("#1F1E2B"))
            ).apply { cornerRadius = dp(22).toFloat() }
        }
        tarjeta2.addView(boton, LinearLayout.LayoutParams(MATCH, dp(64)).apply { topMargin = dp(18) })
        tarjeta2.addView(TextView(this).apply {
            text = "Necesita permiso para mostrarse sobre otras apps y permiso de VPN."
            setTextColor(GRIS)
            textSize = 12f
            setPadding(0, dp(12), 0, 0)
        })
        col.addView(tarjeta2, LinearLayout.LayoutParams(MATCH, WRAP))

        // Tarjeta 3: tamaño del botón flotante
        val prefs = getSharedPreferences("cortarnet", MODE_PRIVATE)
        val tamMin = 36
        val tamMax = 120
        val tamGuardado = prefs.getInt("tam", 58).coerceIn(tamMin, tamMax)

        val tarjeta3 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = forma(TARJETA, BORDE, 28)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val cabecera3 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        cabecera3.addView(TextView(this).apply {
            text = "TAMAÑO DEL BOTÓN"
            setTextColor(Color.parseColor("#D0CCD8"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }, LinearLayout.LayoutParams(0, WRAP, 1f))
        val etiquetaTam = TextView(this).apply {
            text = "$tamGuardado"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#D0CCD8"))
            background = forma(Color.parseColor("#0E0B13"), BORDE, 20)
            setPadding(dp(14), dp(7), dp(14), dp(7))
        }
        cabecera3.addView(etiquetaTam)
        tarjeta3.addView(cabecera3, LinearLayout.LayoutParams(MATCH, WRAP))

        val barra = SeekBar(this).apply {
            max = tamMax - tamMin
            progress = tamGuardado - tamMin
            progressTintList = ColorStateList.valueOf(Color.parseColor("#B04CFF"))
            thumbTintList = ColorStateList.valueOf(Color.parseColor("#E040FF"))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, valor: Int, delUsuario: Boolean) {
                    val nuevo = valor + tamMin
                    etiquetaTam.text = "$nuevo"
                    prefs.edit().putInt("tam", nuevo).apply()
                    FloatingService.actual?.cambiarTamano(nuevo)
                }

                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        tarjeta3.addView(barra, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(14) })
        tarjeta3.addView(TextView(this).apply {
            text = "Mueve la barra para hacer el botón flotante más pequeño o más grande."
            setTextColor(GRIS)
            textSize = 12f
            setPadding(0, dp(12), 0, 0)
        })
        col.addView(tarjeta3, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) })

        setContentView(scroll)

        boton.setOnClickListener {
            if (FloatingService.running) desactivar() else iniciar()
        }
        actualizarUI(FloatingService.running)
    }

    override fun onResume() {
        super.onResume()
        actualizarUI(FloatingService.running)
    }

    private fun actualizarUI(activo: Boolean) {
        val estado = if (activo) "ACTIVO" else "INACTIVO"
        textoEstado.text = estado
        chip.text = estado
        chip.setTextColor(if (activo) VERDE else GRIS)
        (puntoEstado.background as GradientDrawable).setColor(if (activo) VERDE else GRIS)
        boton.text = if (activo) "Desactivar" else "Activar"
        orbe.activo = activo
    }

    private fun iniciar() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Activa 'Mostrar sobre otras apps' y vuelve", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, REQ_VPN)
        } else {
            lanzarBoton()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN && resultCode == RESULT_OK) lanzarBoton()
    }

    private fun lanzarBoton() {
        startForegroundService(Intent(this, FloatingService::class.java))
        actualizarUI(true)
    }

    private fun desactivar() {
        stopService(Intent(this, FloatingService::class.java))
        startService(Intent(this, BlockVpnService::class.java).setAction(BlockVpnService.ACTION_STOP))
        actualizarUI(false)
    }
}

/** Orbe morado animado con anillos y una línea de estado rosa → verde. */
class OrbView(context: Context) : View(context) {

    var activo: Boolean = false
        set(valor) {
            field = valor
            invalidate()
        }

    private var fase = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hex = Path()
    private val ovalo = RectF()
    private val animador = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 9000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            fase = it.animatedValue as Float
            invalidate()
        }
    }

    private fun d(v: Float): Float = v * resources.displayMetrics.density

    private fun limpiar() {
        paint.reset()
        paint.isAntiAlias = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animador.start()
    }

    override fun onDetachedFromWindow() {
        animador.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.42f
        val r = min(w, h) * 0.20f
        val dosPi = (2.0 * PI).toFloat()
        val pulso = 1f + 0.05f * sin(fase * dosPi)

        // Brillo de fondo
        limpiar()
        val radioBrillo = r * 2.7f * pulso
        paint.shader = RadialGradient(
            cx, cy, radioBrillo,
            intArrayOf(Color.argb(if (activo) 150 else 95, 176, 0, 255), Color.argb(0, 176, 0, 255)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radioBrillo, paint)

        // Órbitas
        limpiar()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = d(1.6f)
        paint.color = Color.argb(170, 255, 205, 215)
        canvas.save()
        canvas.rotate(-25f + 360f * fase, cx, cy)
        ovalo.set(cx - r * 1.75f, cy - r * 0.8f, cx + r * 1.75f, cy + r * 0.8f)
        canvas.drawOval(ovalo, paint)
        canvas.restore()
        paint.color = Color.argb(200, 150, 60, 255)
        canvas.save()
        canvas.rotate(35f - 360f * fase, cx, cy)
        ovalo.set(cx - r * 1.55f, cy - r * 0.95f, cx + r * 1.55f, cy + r * 0.95f)
        canvas.drawOval(ovalo, paint)
        canvas.restore()

        // Esfera
        limpiar()
        paint.shader = RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.4f,
            intArrayOf(Color.parseColor("#3A1B2E"), Color.parseColor("#120912")),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, paint)
        limpiar()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = d(3f)
        paint.color = Color.argb(230, 233, 163, 168)
        canvas.drawCircle(cx, cy, r, paint)

        // Núcleo morado
        limpiar()
        val rn = r * 0.85f * pulso
        paint.shader = RadialGradient(
            cx, cy, rn,
            intArrayOf(Color.parseColor("#E040FF"), Color.parseColor("#7A12B8"), Color.argb(0, 90, 10, 140)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, rn, paint)

        // Anillo fino
        limpiar()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = d(1.2f)
        paint.color = Color.argb(130, 255, 255, 255)
        canvas.drawCircle(cx, cy, r * 0.62f, paint)

        // Hexágono blanco con punto
        hex.reset()
        val rh = r * 0.2f
        for (i in 0 until 6) {
            val ang = (PI / 3.0 * i - PI / 2.0).toFloat()
            val x = cx + rh * cos(ang)
            val y = cy + rh * sin(ang)
            if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
        }
        hex.close()
        limpiar()
        paint.color = Color.WHITE
        canvas.drawPath(hex, paint)
        paint.color = Color.parseColor("#7A12B8")
        canvas.drawCircle(cx, cy, r * 0.035f, paint)

        // Reflejo
        limpiar()
        val hx = cx - r * 0.55f
        val hy = cy - r * 0.6f
        paint.shader = RadialGradient(
            hx, hy, r * 0.2f,
            intArrayOf(Color.argb(220, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(hx, hy, r * 0.2f, paint)

        // Línea rosa → verde con punto que se mueve
        val ly = h * 0.87f
        val x0 = w * 0.04f
        val x1 = w * 0.96f
        limpiar()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = d(2f)
        paint.shader = LinearGradient(
            x0, 0f, x1, 0f,
            intArrayOf(
                Color.argb(0, 255, 107, 129), Color.argb(255, 255, 107, 129),
                Color.argb(255, 46, 204, 113), Color.argb(0, 46, 204, 113)
            ),
            floatArrayOf(0f, 0.3f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawLine(x0, ly, x1, ly, paint)

        val t = 0.5f - 0.5f * cos(fase * dosPi)
        val px = x0 + (x1 - x0) * t
        limpiar()
        paint.shader = RadialGradient(
            px, ly, d(12f),
            intArrayOf(Color.argb(230, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(px, ly, d(12f), paint)
        limpiar()
        paint.color = Color.WHITE
        canvas.drawCircle(px, ly, d(2.5f), paint)
    }
}
