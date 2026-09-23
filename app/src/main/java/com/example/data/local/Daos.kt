package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY createdAt ASC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Query("SELECT * FROM requests WHERE collectionId = :collectionId ORDER BY updatedAt DESC")
    fun getRequestsByCollection(collectionId: Long): Flow<List<RequestEntity>>
}

@Dao
interface RequestDao {
    @Query("SELECT * FROM requests ORDER BY updatedAt DESC")
    fun getAllRequests(): Flow<List<RequestEntity>>

    @Query("SELECT * FROM requests WHERE id = :id")
    suspend fun getRequestById(id: Long): RequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: RequestEntity): Long

    @Update
    suspend fun updateRequest(request: RequestEntity)

    @Query("DELETE FROM requests WHERE id = :id")
    suspend fun deleteRequest(id: Long)

    @Query("DELETE FROM requests WHERE collectionId = :collectionId")
    suspend fun deleteRequestsByCollection(collectionId: Long)
}

@Dao
interface EnvironmentDao {
    @Query("SELECT * FROM environments ORDER BY createdAt ASC")
    fun getAllEnvironments(): Flow<List<EnvironmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnvironment(env: EnvironmentEntity): Long

    @Update
    suspend fun updateEnvironment(env: EnvironmentEntity)

    @Query("DELETE FROM environments WHERE id = :id")
    suspend fun deleteEnvironment(id: Long)

    @Query("UPDATE environments SET isSelected = 0")
    suspend fun deselectAll()

    @Query("UPDATE environments SET isSelected = 1 WHERE id = :id")
    suspend fun selectEnvironment(id: Long)

    @Query("SELECT * FROM environments WHERE isSelected = 1 LIMIT 1")
    fun getSelectedEnvironment(): Flow<EnvironmentEntity?>
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM request_history ORDER BY timestamp DESC LIMIT 100")
    fun getAllHistory(): Flow<List<RequestHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: RequestHistoryEntity): Long

    @Query("DELETE FROM request_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM request_history")
    suspend fun clearHistory()
}

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM response_snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<ResponseSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: ResponseSnapshotEntity): Long

    @Query("DELETE FROM response_snapshots WHERE id = :id")
    suspend fun deleteSnapshot(id: Long)
}

@Dao
interface MonitoredEndpointDao {
    @Query("SELECT * FROM monitored_endpoints ORDER BY id ASC")
    fun getAllMonitored(): Flow<List<MonitoredEndpointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonitored(endpoint: MonitoredEndpointEntity): Long

    @Update
    suspend fun updateMonitored(endpoint: MonitoredEndpointEntity)

    @Query("DELETE FROM monitored_endpoints WHERE id = :id")
    suspend fun deleteMonitored(id: Long)
}
