package com.example

import com.example.data.model.KeyValuePair
import com.example.network.PostmanImporter
import com.example.util.JsonHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ApiPlaygroundCoreTest {

    @Test
    fun testVariableResolution() {
        val template = "{{baseUrl}}/users/{{userId}}?api_key={{apiKey}}"
        val vars = mapOf(
            "baseUrl" to "https://api.github.com",
            "userId" to "42",
            "apiKey" to "secret_abc"
        )
        val resolved = JsonHelper.resolveVariables(template, vars)
        assertEquals("https://api.github.com/users/42?api_key=secret_abc", resolved)
    }

    @Test
    fun testJsonFormatting() {
        val compact = "{\"name\":\"Alice\",\"age\":30,\"skills\":[\"Kotlin\",\"Compose\"]}"
        val formatted = JsonHelper.prettyPrintJson(compact)
        assertTrue(formatted.contains("\n"))
        assertTrue(formatted.contains("\"name\": \"Alice\""))
    }

    @Test
    fun testKeyValueSerialization() {
        val list = listOf(
            KeyValuePair(key = "Authorization", value = "Bearer token123", enabled = true, isSecret = true),
            KeyValuePair(key = "Accept", value = "application/json", enabled = true, isSecret = false)
        )
        val serialized = JsonHelper.serializeKeyValuePairs(list)
        val deserialized = JsonHelper.parseKeyValuePairs(serialized)
        assertEquals(2, deserialized.size)
        assertEquals("Authorization", deserialized[0].key)
        assertTrue(deserialized[0].isSecret)
        assertEquals("application/json", deserialized[1].value)
    }

    @Test
    fun testPostmanImportAndExport() {
        val samplePostmanJson = """
        {
          "info": {
            "name": "E-Commerce API",
            "description": "Products & Orders"
          },
          "item": [
            {
              "name": "Get Products",
              "request": {
                "method": "GET",
                "url": "https://fakestoreapi.com/products",
                "header": [
                  { "key": "Accept", "value": "application/json" }
                ]
              }
            }
          ]
        }
        """.trimIndent()

        val parsed = PostmanImporter.parse(samplePostmanJson)
        assertEquals("E-Commerce API", parsed.collection.name)
        assertEquals(1, parsed.requests.size)
        assertEquals("Get Products", parsed.requests[0].name)
        assertEquals("GET", parsed.requests[0].method)
        assertEquals("https://fakestoreapi.com/products", parsed.requests[0].url)

        val exportedJson = PostmanImporter.exportToPostmanJson(parsed.collection, parsed.requests)
        assertTrue(exportedJson.contains("E-Commerce API"))
        assertTrue(exportedJson.contains("Get Products"))
        assertTrue(exportedJson.contains("fakestoreapi.com"))
    }
}
