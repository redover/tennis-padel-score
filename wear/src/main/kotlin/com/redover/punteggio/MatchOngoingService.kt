package com.redover.punteggio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

/**
 * Registra la partita come "attività in corso" di Wear OS, come fanno le app di allenamento.
 * Così quando lo schermo si spegne e si riaccende l'orologio torna a Punteggio invece che al
 * quadrante, e sul quadrante compare un'icona per rientrare nell'app.
 */
class MatchOngoingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Partita in corso", NotificationManager.IMPORTANCE_DEFAULT),
        )

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ongoing)
            .setContentTitle("Punteggio")
            .setContentText("Partita in corso")
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openApp)

        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, notification)
            .setStaticIcon(R.drawable.ic_ongoing)
            .setTouchIntent(openApp)
            .setStatus(Status.Builder().addTemplate("Partita in corso").build())
            .build()
            .apply(applicationContext)

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        return START_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "match"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            context.startForegroundService(Intent(context, MatchOngoingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MatchOngoingService::class.java))
        }
    }
}
