package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MonitoramentoWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.setorDao()
        val setores = dao.getAllSetoresSync()

        for (setor in setores) {
            val result = NetworkUtils.checkStatusWithLatency(setor.ip)
            
            // Sempre insere log para alimentar o gráfico de latência
            dao.insertLog(LogEvento(
                setorId = setor.id,
                timestamp = System.currentTimeMillis(),
                isOnline = result.isOnline,
                latencia = result.latency
            ))

            // Notifica apenas se mudou de Online -> Offline
            if (!result.isOnline && setor.isOnline && setor.isNotificationEnabled) {
                showNotification(setor.nome)
            }

            // Melhoria: Usar todos os logs para um cálculo de uptime real
            val uptime = calculateNewUptime(dao, setor.id)
            dao.update(setor.copy(
                isOnline = result.isOnline,
                latencia = result.latency,
                ultimaVerificacao = System.currentTimeMillis(),
                uptimePercent = uptime
            ))
        }
        Result.success()
    }

    private suspend fun calculateNewUptime(dao: SetorDao, id: Int): Double {
        // Agora usamos a função Sync que não tem limite de 50 registros
        val logs = dao.getAllLogsForSetorSync(id)
        if (logs.isEmpty()) return 100.0
        
        val totalTime = System.currentTimeMillis() - logs.last().timestamp
        if (totalTime <= 0) return 100.0
        
        var onlineDuration = 0L
        var lastTime = System.currentTimeMillis()
        
        // Percorre os logs calculando os intervalos em que esteve online
        for (log in logs) {
            if (log.isOnline) {
                onlineDuration += (lastTime - log.timestamp)
            }
            lastTime = log.timestamp
        }

        return (onlineDuration.toDouble() / totalTime.toDouble()) * 100.0
    }

    private fun showNotification(nomeSetor: String) {
        val channelId = "monitoramento_rede_critico"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alertas Críticos de Rede", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifica quando um dispositivo monitorado fica offline"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("⚠️ Dispositivo Offline")
            .setContentText("O setor $nomeSetor perdeu a conexão!")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(nomeSetor.hashCode(), notification)
    }
}
