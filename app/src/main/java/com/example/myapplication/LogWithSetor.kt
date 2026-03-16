package com.example.myapplication

import androidx.room.Embedded
import androidx.room.Relation

data class LogWithSetor(
    @Embedded val log: LogEvento,
    @Relation(
        parentColumn = "setorId",
        entityColumn = "id"
    )
    val setor: Setor
)
