package com.wbtracker.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WbNotificationHelper @Inject constructor() {

    companion object {
        const val CHANNEL_ID = "wb_price_drops"
        private const val NOTIFICATION_ID_OFFSET = 1000
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Снижение цены"
            val descriptionText = "Уведомления о снижении цены на отслеживаемые товары"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPriceDropNotification(
        context: Context,
        productTitle: String,
        newPrice: Double,
        productId: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // We assume MainActivity is in com.wbtracker.app
        val intent = Intent(context, Class.forName("com.wbtracker.app.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("productId", productId)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            productId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("\ud83d\udcb0 Цена снизилась!")
            .setContentText("$productTitle теперь стоит ${newPrice.toLong()} ₽")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify((productId % Int.MAX_VALUE).toInt() + NOTIFICATION_ID_OFFSET, builder.build())
        }
    }

    fun showGroupedPriceDropNotification(
        context: Context,
        drops: List<Pair<String, Double>> // Pair of productTitle to newPrice
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        if (drops.isEmpty()) return
        
        if (drops.size == 1) {
            val drop = drops.first()
            showPriceDropNotification(context, drop.first, drop.second, 0L)
            return
        }

        val intent = Intent(context, Class.forName("com.wbtracker.app.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            9999, // Static ID for grouped notification intent
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("Снижение цен на ${drops.size} товаров")
            
        drops.take(5).forEach { drop ->
            inboxStyle.addLine("${drop.first}: ${drop.second.toLong()} ₽")
        }
        if (drops.size > 5) {
            inboxStyle.setSummaryText("+ ещё ${drops.size - 5} товаров")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("\ud83d\udcb0 Снижение цен!")
            .setContentText("Цены на ${drops.size} товаров снизились")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_OFFSET - 1, builder.build())
        }
    }
}
