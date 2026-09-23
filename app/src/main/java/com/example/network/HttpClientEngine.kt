package com.example.network

import android.util.Base64
import com.example.data.model.*
import com.example.util.JsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpClientEngine {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun execute(
        method: String,
        urlTemplate: String,
        params: List<KeyValuePair>,
        headers: List<KeyValuePair>,
        bodyType: RequestBodyType,
        bodyContent: String,
        authType: AuthType,
        authConfig: AuthConfig,
        variables: Map<String, String>
    ): NetworkResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. Resolve variables in URL
        var resolvedUrl = JsonHelper.resolveVariables(urlTemplate.trim(), variables)
        if (!resolvedUrl.startsWith("http://") && !resolvedUrl.startsWith("https://")) {
            resolvedUrl = "https://$resolvedUrl"
        }

        // 2. Build URL with query params
        val httpUrlBuilder = resolvedUrl.toHttpUrlOrNull()?.newBuilder()
        if (httpUrlBuilder == null) {
            return@withContext NetworkResponse(
                statusCode = 0,
                statusText = "Invalid URL",
                durationMs = 0,
                sizeBytes = 0,
                headers = emptyList(),
                body = "",
                isSuccess = false,
                errorDetails = "Could not parse '$resolvedUrl' as a valid HTTP/HTTPS URL. Please check the scheme and host format.",
                resolvedUrl = resolvedUrl
            )
        }

        // Add enabled query params
        params.filter { it.enabled && it.key.isNotBlank() }.forEach { param ->
            val resolvedKey = JsonHelper.resolveVariables(param.key, variables)
            val resolvedVal = JsonHelper.resolveVariables(param.value, variables)
            httpUrlBuilder.addQueryParameter(resolvedKey, resolvedVal)
        }

        // Add API key query param if selected
        if (authType == AuthType.API_KEY && authConfig.apiKeyAddTo == "QUERY" && authConfig.apiKeyName.isNotBlank()) {
            val resolvedKey = JsonHelper.resolveVariables(authConfig.apiKeyName, variables)
            val resolvedVal = JsonHelper.resolveVariables(authConfig.apiKeyValue, variables)
            httpUrlBuilder.addQueryParameter(resolvedKey, resolvedVal)
        }

        val finalHttpUrl = httpUrlBuilder.build()
        val requestBuilder = Request.Builder().url(finalHttpUrl)

        // 3. Add Headers
        headers.filter { it.enabled && it.key.isNotBlank() }.forEach { header ->
            val resolvedKey = JsonHelper.resolveVariables(header.key, variables)
            val resolvedVal = JsonHelper.resolveVariables(header.value, variables)
            requestBuilder.addHeader(resolvedKey, resolvedVal)
        }

        // Add Auth Headers
        when (authType) {
            AuthType.BEARER -> {
                if (authConfig.bearerToken.isNotBlank()) {
                    val resolvedToken = JsonHelper.resolveVariables(authConfig.bearerToken, variables)
                    requestBuilder.header("Authorization", "Bearer $resolvedToken")
                }
            }
            AuthType.BASIC -> {
                val user = JsonHelper.resolveVariables(authConfig.basicUsername, variables)
                val pass = JsonHelper.resolveVariables(authConfig.basicPassword, variables)
                val credentials = "$user:$pass"
                val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
                requestBuilder.header("Authorization", "Basic $encoded")
            }
            AuthType.API_KEY -> {
                if (authConfig.apiKeyAddTo == "HEADER" && authConfig.apiKeyName.isNotBlank()) {
                    val resolvedKey = JsonHelper.resolveVariables(authConfig.apiKeyName, variables)
                    val resolvedVal = JsonHelper.resolveVariables(authConfig.apiKeyValue, variables)
                    requestBuilder.header(resolvedKey, resolvedVal)
                }
            }
            AuthType.NONE -> {}
        }

        // 4. Request Body
        val requestBody = when {
            method in listOf("GET", "HEAD", "OPTIONS") -> null
            bodyType == RequestBodyType.JSON -> {
                val resolvedBody = JsonHelper.resolveVariables(bodyContent, variables)
                resolvedBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            }
            bodyType == RequestBodyType.URL_ENCODED -> {
                val pairs = JsonHelper.parseKeyValuePairs(bodyContent)
                val formBuilder = FormBody.Builder()
                pairs.filter { it.enabled && it.key.isNotBlank() }.forEach { p ->
                    formBuilder.add(
                        JsonHelper.resolveVariables(p.key, variables),
                        JsonHelper.resolveVariables(p.value, variables)
                    )
                }
                formBuilder.build()
            }
            bodyType == RequestBodyType.FORM_DATA -> {
                val pairs = JsonHelper.parseKeyValuePairs(bodyContent)
                val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                pairs.filter { it.enabled && it.key.isNotBlank() }.forEach { p ->
                    multipartBuilder.addFormDataPart(
                        JsonHelper.resolveVariables(p.key, variables),
                        JsonHelper.resolveVariables(p.value, variables)
                    )
                }
                multipartBuilder.build()
            }
            bodyType == RequestBodyType.RAW -> {
                val resolvedBody = JsonHelper.resolveVariables(bodyContent, variables)
                resolvedBody.toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())
            }
            else -> {
                // NONE or empty body for POST/PUT/DELETE
                "".toRequestBody(null)
            }
        }

        requestBuilder.method(method, requestBody)

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            val duration = System.currentTimeMillis() - startTime
            val rawBody = response.body?.string() ?: ""
            val sizeBytes = rawBody.toByteArray().size.toLong()

            val headerList = mutableListOf<Pair<String, String>>()
            for (i in 0 until response.headers.size) {
                headerList.add(Pair(response.headers.name(i), response.headers.value(i)))
            }

            val prettyBody = JsonHelper.prettyPrintJson(rawBody)

            NetworkResponse(
                statusCode = response.code,
                statusText = response.message.ifBlank { if (response.isSuccessful) "OK" else "Error" },
                durationMs = duration,
                sizeBytes = sizeBytes,
                headers = headerList,
                body = prettyBody,
                isSuccess = response.isSuccessful,
                resolvedUrl = finalHttpUrl.toString()
            )
        } catch (e: IOException) {
            val duration = System.currentTimeMillis() - startTime
            NetworkResponse(
                statusCode = 0,
                statusText = "Network Error",
                durationMs = duration,
                sizeBytes = 0,
                headers = emptyList(),
                body = "",
                isSuccess = false,
                errorDetails = e.localizedMessage ?: "Failed to connect to host",
                resolvedUrl = finalHttpUrl.toString()
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            NetworkResponse(
                statusCode = 0,
                statusText = "Execution Error",
                durationMs = duration,
                sizeBytes = 0,
                headers = emptyList(),
                body = "",
                isSuccess = false,
                errorDetails = e.message ?: "An unexpected error occurred",
                resolvedUrl = finalHttpUrl.toString()
            )
        }
    }
}
