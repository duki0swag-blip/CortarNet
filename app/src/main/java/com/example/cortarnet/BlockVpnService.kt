package com.example.cortarnet

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class BlockVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.example.cortarnet.START"
        const val ACTION_STOP = "com.example.cortarnet.STOP"
    }

    private var tunnel: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent?.action == ACTION_STOP) {
            detener()
            START_NOT_STICKY
        } else {
            iniciar()
            START_STICKY
        }
    }

    private fun iniciar() {
        if (tunnel != null) return
        tunnel = Builder()
            .setSession("CortarNet")
            .addAddress("10.0.0.2", 32)
            .addAddress("fd00::2", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .establish()
    }

    private fun detener() {
        tunnel?.close()
        tunnel = null
        stopSelf()
    }

    override fun onRevoke() {
        detener()
        super.onRevoke()
    }

    override fun onDestroy() {
        tunnel?.close()
        tunnel = null
        super.onDestroy()
    }
}
