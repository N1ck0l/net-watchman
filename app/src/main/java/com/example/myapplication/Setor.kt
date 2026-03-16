package com.example.myapplication

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "setores")
data class Setor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val ip: String,
    val categoria: String = "Geral",
    val macAddress: String? = null,
    var isOnline: Boolean = false,
    var ultimaVerificacao: Long = 0L,
    var latencia: Long = -1L,
    var isNotificationEnabled: Boolean = true,
    var uptimePercent: Double = 100.0
)

@Entity(
    tableName = "logs_eventos",
    foreignKeys = [
        ForeignKey(
            entity = Setor::class,
            parentColumns = ["id"],
            childColumns = ["setorId"],
            onDelete = ForeignKey.CASCADE // Limpa os logs se o setor for excluído
        )
    ]
)
data class LogEvento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val setorId: Int,
    val timestamp: Long,
    val isOnline: Boolean,
    val latencia: Long = -1L // Novo campo para o gráfico
)
