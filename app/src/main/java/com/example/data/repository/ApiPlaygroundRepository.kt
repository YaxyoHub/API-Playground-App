package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.network.HttpClientEngine
import com.example.network.PostmanImporter
import com.example.util.JsonHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ApiPlaygroundRepository(
    private val database: AppDatabase,
    private val httpEngine: HttpClientEngine
) {
    // Collections
    fun getAllCollections(): Flow<List<CollectionEntity>> = database.collectionDao().getAllCollections()
    fun getRequestsByCollection(collectionId: Long): Flow<List<RequestEntity>> =
        database.collectionDao().getRequestsByCollection(collectionId)
    suspend fun saveCollection(collection: CollectionEntity): Long = database.collectionDao().insertCollection(collection)
    suspend fun deleteCollection(id: Long) {
        database.requestDao().deleteRequestsByCollection(id)
        database.collectionDao().deleteCollection(id)
    }

    // Requests
    fun getAllRequests(): Flow<List<RequestEntity>> = database.requestDao().getAllRequests()
    suspend fun getRequestById(id: Long): RequestEntity? = database.requestDao().getRequestById(id)
    suspend fun saveRequest(request: RequestEntity): Long = database.requestDao().insertRequest(request)
    suspend fun deleteRequest(id: Long) = database.requestDao().deleteRequest(id)

    // Environments
    fun getAllEnvironments(): Flow<List<EnvironmentEntity>> = database.environmentDao().getAllEnvironments()
    fun getSelectedEnvironment(): Flow<EnvironmentEntity?> = database.environmentDao().getSelectedEnvironment()
    suspend fun selectEnvironment(id: Long) {
        database.environmentDao().deselectAll()
        database.environmentDao().selectEnvironment(id)
    }
    suspend fun saveEnvironment(env: EnvironmentEntity): Long = database.environmentDao().insertEnvironment(env)
    suspend fun deleteEnvironment(id: Long) = database.environmentDao().deleteEnvironment(id)

    // History
    fun getAllHistory(): Flow<List<RequestHistoryEntity>> = database.historyDao().getAllHistory()
    suspend fun deleteHistory(id: Long) = database.historyDao().deleteHistory(id)
    suspend fun clearHistory() = database.historyDao().clearHistory()

    // Snapshots
    fun getAllSnapshots(): Flow<List<ResponseSnapshotEntity>> = database.snapshotDao().getAllSnapshots()
    suspend fun saveSnapshot(snapshot: ResponseSnapshotEntity): Long = database.snapshotDao().insertSnapshot(snapshot)
    suspend fun deleteSnapshot(id: Long) = database.snapshotDao().deleteSnapshot(id)

    // Monitored Endpoints
    fun getAllMonitored(): Flow<List<MonitoredEndpointEntity>> = database.monitoredEndpointDao().getAllMonitored()
    suspend fun saveMonitored(endpoint: MonitoredEndpointEntity): Long = database.monitoredEndpointDao().insertMonitored(endpoint)
    suspend fun deleteMonitored(id: Long) = database.monitoredEndpointDao().deleteMonitored(id)
    suspend fun updateMonitored(endpoint: MonitoredEndpointEntity) = database.monitoredEndpointDao().updateMonitored(endpoint)

    // Network Request Execution
    suspend fun executeRequest(
        method: String,
        url: String,
        params: List<KeyValuePair>,
        headers: List<KeyValuePair>,
        bodyType: RequestBodyType,
        bodyContent: String,
        authType: AuthType,
        authConfig: AuthConfig
    ): NetworkResponse {
        val activeEnv = database.environmentDao().getSelectedEnvironment().firstOrNull()
        val variables = mutableMapOf<String, String>()
        if (activeEnv != null) {
            val pairs = JsonHelper.parseKeyValuePairs(activeEnv.variablesJson)
            pairs.forEach { p ->
                if (p.key.isNotBlank()) {
                    variables[p.key] = p.value
                }
            }
        }

        val response = httpEngine.execute(
            method = method,
            urlTemplate = url,
            params = params,
            headers = headers,
            bodyType = bodyType,
            bodyContent = bodyContent,
            authType = authType,
            authConfig = authConfig,
            variables = variables
        )

        // Record into history
        val historyEntry = RequestHistoryEntity(
            method = method,
            url = response.resolvedUrl.ifBlank { url },
            statusCode = response.statusCode,
            statusText = response.statusText,
            durationMs = response.durationMs,
            responseSizeBytes = response.sizeBytes,
            requestHeadersJson = JsonHelper.serializeKeyValuePairs(headers),
            requestBody = bodyContent,
            responseHeadersJson = org.json.JSONArray().apply {
                response.headers.forEach { h ->
                    put(org.json.JSONObject().apply {
                        put("key", h.first)
                        put("value", h.second)
                    })
                }
            }.toString(),
            responseBody = if (response.isSuccess) response.body else (response.errorDetails ?: response.body),
            isSuccess = response.isSuccess,
            environmentName = activeEnv?.name ?: "No Environment",
            timestamp = System.currentTimeMillis()
        )
        database.historyDao().insertHistory(historyEntry)

        return response
    }

    // Monitored Endpoints Health Check
    suspend fun pingMonitoredEndpoint(endpoint: MonitoredEndpointEntity): MonitoredEndpointEntity {
        val activeEnv = database.environmentDao().getSelectedEnvironment().firstOrNull()
        val variables = mutableMapOf<String, String>()
        if (activeEnv != null) {
            val pairs = JsonHelper.parseKeyValuePairs(activeEnv.variablesJson)
            pairs.forEach { p -> if (p.key.isNotBlank()) variables[p.key] = p.value }
        }

        val resp = httpEngine.execute(
            method = endpoint.method,
            urlTemplate = endpoint.url,
            params = emptyList(),
            headers = emptyList(),
            bodyType = RequestBodyType.NONE,
            bodyContent = "",
            authType = AuthType.NONE,
            authConfig = AuthConfig(),
            variables = variables
        )

        val isPassing = resp.statusCode == endpoint.expectedStatus
        val updated = endpoint.copy(
            lastStatusCode = resp.statusCode,
            lastLatencyMs = resp.durationMs,
            lastCheckedTimestamp = System.currentTimeMillis(),
            isPassing = isPassing
        )
        database.monitoredEndpointDao().updateMonitored(updated)
        return updated
    }

    // Postman Import
    suspend fun importPostmanCollection(jsonStr: String): Pair<CollectionEntity, Int> {
        val parsed = PostmanImporter.parse(jsonStr)
        val colId = database.collectionDao().insertCollection(parsed.collection)
        var count = 0
        parsed.requests.forEach { req ->
            database.requestDao().insertRequest(req.copy(collectionId = colId))
            count++
        }
        return Pair(parsed.collection.copy(id = colId), count)
    }

    // Postman Export
    suspend fun exportCollection(collectionId: Long): String {
        val collections = database.collectionDao().getAllCollections().firstOrNull() ?: emptyList()
        val col = collections.find { it.id == collectionId }
            ?: CollectionEntity(name = "Exported Collection")
        val requests = database.collectionDao().getRequestsByCollection(collectionId).firstOrNull() ?: emptyList()
        return PostmanImporter.exportToPostmanJson(col, requests)
    }
}
