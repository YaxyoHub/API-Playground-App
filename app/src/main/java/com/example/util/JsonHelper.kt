package com.example.util

import com.example.data.model.AuthConfig
import com.example.data.model.KeyValuePair
import org.json.JSONArray
import org.json.JSONObject

object JsonHelper {

    fun parseKeyValuePairs(json: String?): List<KeyValuePair> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<KeyValuePair>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    KeyValuePair(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        key = obj.optString("key", ""),
                        value = obj.optString("value", ""),
                        enabled = obj.optBoolean("enabled", true),
                        isSecret = obj.optBoolean("isSecret", false)
                    )
                )
            }
        } catch (e: Exception) {
            // ignore malformed
        }
        return list
    }

    fun serializeKeyValuePairs(pairs: List<KeyValuePair>): String {
        val jsonArray = JSONArray()
        pairs.forEach { pair ->
            val obj = JSONObject().apply {
                put("id", pair.id)
                put("key", pair.key)
                put("value", pair.value)
                put("enabled", pair.enabled)
                put("isSecret", pair.isSecret)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun parseAuthConfig(json: String?): AuthConfig {
        if (json.isNullOrBlank()) return AuthConfig()
        return try {
            val obj = JSONObject(json)
            AuthConfig(
                bearerToken = obj.optString("bearerToken", ""),
                apiKeyName = obj.optString("apiKeyName", "x-api-key"),
                apiKeyValue = obj.optString("apiKeyValue", ""),
                apiKeyAddTo = obj.optString("apiKeyAddTo", "HEADER"),
                basicUsername = obj.optString("basicUsername", ""),
                basicPassword = obj.optString("basicPassword", "")
            )
        } catch (e: Exception) {
            AuthConfig()
        }
    }

    fun serializeAuthConfig(config: AuthConfig): String {
        return JSONObject().apply {
            put("bearerToken", config.bearerToken)
            put("apiKeyName", config.apiKeyName)
            put("apiKeyValue", config.apiKeyValue)
            put("apiKeyAddTo", config.apiKeyAddTo)
            put("basicUsername", config.basicUsername)
            put("basicPassword", config.basicPassword)
        }.toString()
    }

    fun prettyPrintJson(raw: String): String {
        val trimmed = raw.trim()
        return try {
            if (trimmed.startsWith("{")) {
                JSONObject(trimmed).toString(2)
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed).toString(2)
            } else {
                raw
            }
        } catch (e: Exception) {
            raw
        }
    }

    fun resolveVariables(template: String, variables: Map<String, String>): String {
        var result = template
        variables.forEach { (k, v) ->
            result = result.replace("{{$k}}", v)
        }
        return result
    }
}
