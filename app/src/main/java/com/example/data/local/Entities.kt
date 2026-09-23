package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "requests")
data class RequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: Long? = null,
    val name: String,
    val method: String = "GET",
    val url: String = "",
    val headersJson: String = "[]",
    val paramsJson: String = "[]",
    val bodyType: String = "NONE", // NONE, JSON, FORM_DATA, URL_ENCODED, RAW
    val bodyContent: String = "",
    val authType: String = "NONE", // NONE, BEARER, API_KEY, BASIC
    val authConfigJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "environments")
data class EnvironmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isSelected: Boolean = false,
    val variablesJson: String = "[]", // list of {key, value, isSecret}
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "request_history")
data class RequestHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val method: String,
    val url: String,
    val statusCode: Int,
    val statusText: String,
    val durationMs: Long,
    val responseSizeBytes: Long,
    val requestHeadersJson: String = "[]",
    val requestBody: String = "",
    val responseHeadersJson: String = "[]",
    val responseBody: String = "",
    val isSuccess: Boolean = true,
    val environmentName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "response_snapshots")
data class ResponseSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val method: String,
    val url: String,
    val statusCode: Int,
    val durationMs: Long,
    val responseBody: String,
    val responseHeadersJson: String = "[]",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "monitored_endpoints")
data class MonitoredEndpointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val method: String = "GET",
    val expectedStatus: Int = 200,
    val lastStatusCode: Int? = null,
    val lastLatencyMs: Long? = null,
    val lastCheckedTimestamp: Long? = null,
    val isPassing: Boolean? = null
)
