package com.example.network

import com.example.data.local.CollectionEntity
import com.example.data.local.RequestEntity
import com.example.util.JsonHelper
import org.json.JSONArray
import org.json.JSONObject

object PostmanImporter {

    data class ParsedPostmanCollection(
        val collection: CollectionEntity,
        val requests: List<RequestEntity>
    )

    fun parse(jsonStr: String): ParsedPostmanCollection {
        val root = JSONObject(jsonStr)
        val infoObj = root.optJSONObject("info")
        val colName = infoObj?.optString("name", "Imported Collection") ?: "Imported Collection"
        val colDesc = infoObj?.optString("description", "") ?: ""

        val collection = CollectionEntity(name = colName, description = colDesc)
        val requests = mutableListOf<RequestEntity>()

        val items = root.optJSONArray("item")
        if (items != null) {
            extractItems(items, requests)
        }

        return ParsedPostmanCollection(collection, requests)
    }

    private fun extractItems(items: JSONArray, requests: MutableList<RequestEntity>) {
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val subItems = item.optJSONArray("item")
            if (subItems != null) {
                // It's a folder, recurse
                extractItems(subItems, requests)
            } else {
                val reqObj = item.optJSONObject("request") ?: continue
                val name = item.optString("name", "Untitled Request")
                val method = reqObj.optString("method", "GET").uppercase()

                // Parse URL
                var url = ""
                val urlObj = reqObj.opt("url")
                if (urlObj is String) {
                    url = urlObj
                } else if (urlObj is JSONObject) {
                    url = urlObj.optString("raw", "")
                }

                // Parse Headers
                val headerArr = reqObj.optJSONArray("header")
                val headersList = mutableListOf<com.example.data.model.KeyValuePair>()
                if (headerArr != null) {
                    for (h in 0 until headerArr.length()) {
                        val hObj = headerArr.optJSONObject(h) ?: continue
                        headersList.add(
                            com.example.data.model.KeyValuePair(
                                key = hObj.optString("key", ""),
                                value = hObj.optString("value", ""),
                                enabled = !hObj.optBoolean("disabled", false)
                            )
                        )
                    }
                }

                // Parse Body
                var bodyType = "NONE"
                var bodyContent = ""
                val bodyObj = reqObj.optJSONObject("body")
                if (bodyObj != null) {
                    val mode = bodyObj.optString("mode", "")
                    if (mode == "raw") {
                        bodyType = "JSON"
                        bodyContent = bodyObj.optString("raw", "")
                    } else if (mode == "urlencoded" || mode == "formdata") {
                        bodyType = "FORM_DATA"
                        val formArr = bodyObj.optJSONArray(mode)
                        val pairs = mutableListOf<com.example.data.model.KeyValuePair>()
                        if (formArr != null) {
                            for (f in 0 until formArr.length()) {
                                val fObj = formArr.optJSONObject(f) ?: continue
                                pairs.add(
                                    com.example.data.model.KeyValuePair(
                                        key = fObj.optString("key", ""),
                                        value = fObj.optString("value", ""),
                                        enabled = !fObj.optBoolean("disabled", false)
                                    )
                                )
                            }
                        }
                        bodyContent = JsonHelper.serializeKeyValuePairs(pairs)
                    }
                }

                requests.add(
                    RequestEntity(
                        name = name,
                        method = method,
                        url = url,
                        headersJson = JsonHelper.serializeKeyValuePairs(headersList),
                        bodyType = bodyType,
                        bodyContent = bodyContent
                    )
                )
            }
        }
    }

    fun exportToPostmanJson(collection: CollectionEntity, requests: List<RequestEntity>): String {
        val root = JSONObject()
        val info = JSONObject().apply {
            put("name", collection.name)
            put("description", collection.description)
            put("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json")
        }
        root.put("info", info)

        val itemsArr = JSONArray()
        for (req in requests) {
            val itemObj = JSONObject()
            itemObj.put("name", req.name)

            val reqObj = JSONObject()
            reqObj.put("method", req.method)
            reqObj.put("url", JSONObject().apply {
                put("raw", req.url)
            })

            // Headers
            val headers = JsonHelper.parseKeyValuePairs(req.headersJson)
            val headerArr = JSONArray()
            headers.filter { it.enabled && it.key.isNotBlank() }.forEach { h ->
                headerArr.put(JSONObject().apply {
                    put("key", h.key)
                    put("value", h.value)
                })
            }
            reqObj.put("header", headerArr)

            // Body
            if (req.bodyContent.isNotBlank() && req.method in listOf("POST", "PUT", "PATCH")) {
                val bodyObj = JSONObject()
                bodyObj.put("mode", "raw")
                bodyObj.put("raw", req.bodyContent)
                reqObj.put("body", bodyObj)
            }

            itemObj.put("request", reqObj)
            itemsArr.put(itemObj)
        }

        root.put("item", itemsArr)
        return root.toString(2)
    }
}
