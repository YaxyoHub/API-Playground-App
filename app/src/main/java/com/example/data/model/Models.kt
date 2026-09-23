package com.example.data.model

data class KeyValuePair(
    val id: String = java.util.UUID.randomUUID().toString(),
    val key: String = "",
    val value: String = "",
    val enabled: Boolean = true,
    val isSecret: Boolean = false
)

enum class HttpMethod(val colorHex: Long) {
    GET(0xFF10B981),      // Green
    POST(0xFF3B82F6),     // Blue
    PUT(0xFFF59E0B),      // Amber
    PATCH(0xFF8B5CF6),    // Purple
    DELETE(0xFFEF4444),   // Red
    HEAD(0xFF06B6D4),     // Cyan
    OPTIONS(0xFF6B7280)   // Gray
}

enum class RequestBodyType {
    NONE,
    JSON,
    URL_ENCODED,
    FORM_DATA,
    RAW
}

enum class AuthType {
    NONE,
    BEARER,
    API_KEY,
    BASIC
}

data class AuthConfig(
    val bearerToken: String = "",
    val apiKeyName: String = "x-api-key",
    val apiKeyValue: String = "",
    val apiKeyAddTo: String = "HEADER", // HEADER or QUERY
    val basicUsername: String = "",
    val basicPassword: String = ""
)

data class NetworkResponse(
    val statusCode: Int,
    val statusText: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val headers: List<Pair<String, String>>,
    val body: String,
    val isSuccess: Boolean,
    val errorDetails: String? = null,
    val resolvedUrl: String = ""
)
