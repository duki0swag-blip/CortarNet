package com.example.cortarnet

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQ_VPN = 100
    private val REQ_NOTIF = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        val info = TextView(this).apply {
            text = "1) Da permiso de mostrar sobre otras apps\n" +
                    "2) Acepta la VPN\n" +
                    "3) Aparecerá un botón flotante"
            textSize = 16f
        }
        val start = Button(this).apply { text = "Iniciar botón flotante" }
        val stop = Button(this).apply { text = "Cerrar botón flotante" }

        layout.addView(info)
        layout.addView(start)
        layout.addView(stop)
        setContentView(layout)

        start.setOnClickListener { iniciar() }
        stop.setOnClickListener {
            stopService(Intent(this, FloatingService::class.java))
            startService(Intent(this, BlockVpnService::class.java).setAction(BlockVpnService.ACTION_STOP))
        }
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
        Toast.makeText(this, "Botón flotante activo", Toast.LENGTH_SHORT).show()
    }
}
