package com.example.myapplication

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SetorDao {
    @Query("SELECT * FROM setores ORDER BY nome ASC")
    fun getAllSetores(): Flow<List<Setor>>

    @Query("SELECT * FROM setores")
    suspend fun getAllSetoresSync(): List<Setor>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setor: Setor): Long

    @Update
    suspend fun update(setor: Setor)

    @Delete
    suspend fun delete(setor: Setor)

    @Insert
    suspend fun insertLog(log: LogEvento)

    @Query("SELECT * FROM logs_eventos WHERE setorId = :id ORDER BY timestamp DESC LIMIT 50")
    fun getLogsForSetor(id: Int): Flow<List<LogEvento>>

    @Query("SELECT * FROM logs_eventos WHERE setorId = :id ORDER BY timestamp DESC")
    suspend fun getAllLogsForSetorSync(id: Int): List<LogEvento>

    @Transaction
    @Query("SELECT * FROM logs_eventos ORDER BY timestamp DESC")
    fun getAllLogsWithSetor(): Flow<List<LogWithSetor>>
}
