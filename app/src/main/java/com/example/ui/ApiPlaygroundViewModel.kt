package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.ApiPlaygroundRepository
import com.example.network.HttpClientEngine
import com.example.util.JsonHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RequestBuilderUiState(
    val currentRequestId: Long? = null,
    val requestName: String = "Untitled Request",
    val method: String = "GET",
    val url: String = "https://jsonplaceholder.typicode.com/posts/1",
    val params: List<KeyValuePair> = emptyList(),
    val headers: List<KeyValuePair> = listOf(
        KeyValuePair(key = "Accept", value = "application/json", enabled = true)
    ),
    val bodyType: RequestBodyType = RequestBodyType.NONE,
    val bodyContent: String = "{\n  \"title\": \"foo\",\n  \"body\": \"bar\",\n  \"userId\": 1\n}",
    val authType: AuthType = AuthType.NONE,
    val authConfig: AuthConfig = AuthConfig(),
    val isLoading: Boolean = false,
    val latestResponse: NetworkResponse? = null,
    val selectedReqTab: Int = 0, // 0: Params, 1: Headers, 2: Body, 3: Auth
    val selectedResTab: Int = 0  // 0: Body, 1: Headers, 2: Raw
)

class ApiPlaygroundViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ApiPlaygroundRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = ApiPlaygroundRepository(db, HttpClientEngine())
    }

    // --- Request Builder State ---
    private val _builderState = MutableStateFlow(RequestBuilderUiState())
    val builderState: StateFlow<RequestBuilderUiState> = _builderState.asStateFlow()

    // --- Environments Flow ---
    val environments: StateFlow<List<EnvironmentEntity>> = repository.getAllEnvironments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedEnvironment: StateFlow<EnvironmentEntity?> = repository.getSelectedEnvironment()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Collections Flow ---
    val collections: StateFlow<List<CollectionEntity>> = repository.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRequests: StateFlow<List<RequestEntity>> = repository.getAllRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- History Flow ---
    val historyList: StateFlow<List<RequestHistoryEntity>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Snapshots Flow ---
    val snapshots: StateFlow<List<ResponseSnapshotEntity>> = repository.getAllSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Monitored Endpoints ---
    val monitoredEndpoints: StateFlow<List<MonitoredEndpointEntity>> = repository.getAllMonitored()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Comparison State ---
    private val _compareItemA = MutableStateFlow<RequestHistoryEntity?>(null)
    val compareItemA: StateFlow<RequestHistoryEntity?> = _compareItemA.asStateFlow()

    private val _compareItemB = MutableStateFlow<RequestHistoryEntity?>(null)
    val compareItemB: StateFlow<RequestHistoryEntity?> = _compareItemB.asStateFlow()

    // --- Vault State ---
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    // --- Theme (Light / Dark Mode) ---
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
    }

    // --- Localization (Uzbek, Russian, English) ---
    private val _currentLanguage = MutableStateFlow(com.example.util.localization.AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<com.example.util.localization.AppLanguage> = _currentLanguage.asStateFlow()

    val strings: StateFlow<com.example.util.localization.AppStrings> = _currentLanguage
        .map { com.example.util.localization.getStrings(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.example.util.localization.EnglishStrings)

    fun setLanguage(language: com.example.util.localization.AppLanguage) {
        _currentLanguage.value = language
        emitToast(
            when (language) {
                com.example.util.localization.AppLanguage.ENGLISH -> "Language changed to English"
                com.example.util.localization.AppLanguage.RUSSIAN -> "Язык переключен на Русский"
                com.example.util.localization.AppLanguage.UZBEK -> "Til O'zbekchaga o'zgartirildi"
            }
        )
    }

    private val _isPingingAll = MutableStateFlow(false)
    val isPingingAll: StateFlow<Boolean> = _isPingingAll.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // --- Builder Actions ---
    fun updateMethod(method: String) {
        _builderState.update {
            val newBodyType = if (method in listOf("POST", "PUT", "PATCH") && it.bodyType == RequestBodyType.NONE) {
                RequestBodyType.JSON
            } else it.bodyType
            it.copy(method = method, bodyType = newBodyType)
        }
    }

    fun updateUrl(url: String) {
        _builderState.update { it.copy(url = url) }
    }

    fun updateRequestName(name: String) {
        _builderState.update { it.copy(requestName = name) }
    }

    fun updateParams(params: List<KeyValuePair>) {
        _builderState.update { it.copy(params = params) }
    }

    fun updateHeaders(headers: List<KeyValuePair>) {
        _builderState.update { it.copy(headers = headers) }
    }

    fun updateBodyType(bodyType: RequestBodyType) {
        _builderState.update { it.copy(bodyType = bodyType) }
    }

    fun updateBodyContent(content: String) {
        _builderState.update { it.copy(bodyContent = content) }
    }

    fun formatJsonBody() {
        _builderState.update {
            it.copy(bodyContent = JsonHelper.prettyPrintJson(it.bodyContent))
        }
    }

    fun updateAuthType(authType: AuthType) {
        _builderState.update { it.copy(authType = authType) }
    }

    fun updateAuthConfig(config: AuthConfig) {
        _builderState.update { it.copy(authConfig = config) }
    }

    fun selectReqTab(tab: Int) {
        _builderState.update { it.copy(selectedReqTab = tab) }
    }

    fun selectResTab(tab: Int) {
        _builderState.update { it.copy(selectedResTab = tab) }
    }

    fun sendRequest() {
        val s = _builderState.value
        if (s.url.isBlank()) {
            emitToast("Please enter an endpoint URL")
            return
        }

        viewModelScope.launch {
            _builderState.update { it.copy(isLoading = true) }
            val response = repository.executeRequest(
                method = s.method,
                url = s.url,
                params = s.params,
                headers = s.headers,
                bodyType = s.bodyType,
                bodyContent = s.bodyContent,
                authType = s.authType,
                authConfig = s.authConfig
            )
            _builderState.update {
                it.copy(
                    isLoading = false,
                    latestResponse = response
                )
            }
        }
    }

    fun loadRequestIntoBuilder(request: RequestEntity) {
        _builderState.update {
            it.copy(
                currentRequestId = request.id,
                requestName = request.name,
                method = request.method,
                url = request.url,
                params = JsonHelper.parseKeyValuePairs(request.paramsJson),
                headers = JsonHelper.parseKeyValuePairs(request.headersJson),
                bodyType = try { RequestBodyType.valueOf(request.bodyType) } catch (e: Exception) { RequestBodyType.NONE },
                bodyContent = request.bodyContent,
                authType = try { AuthType.valueOf(request.authType) } catch (e: Exception) { AuthType.NONE },
                authConfig = JsonHelper.parseAuthConfig(request.authConfigJson),
                latestResponse = null
            )
        }
        emitToast("Loaded '${request.name}'")
    }

    fun loadHistoryIntoBuilder(history: RequestHistoryEntity) {
        _builderState.update {
            it.copy(
                currentRequestId = null,
                requestName = "${history.method} ${history.url.take(30)}",
                method = history.method,
                url = history.url,
                headers = JsonHelper.parseKeyValuePairs(history.requestHeadersJson),
                bodyType = if (history.requestBody.isNotBlank()) RequestBodyType.JSON else RequestBodyType.NONE,
                bodyContent = history.requestBody,
                latestResponse = NetworkResponse(
                    statusCode = history.statusCode,
                    statusText = history.statusText,
                    durationMs = history.durationMs,
                    sizeBytes = history.responseSizeBytes,
                    headers = emptyList(),
                    body = history.responseBody,
                    isSuccess = history.isSuccess,
                    resolvedUrl = history.url
                )
            )
        }
        emitToast("Loaded request from history")
    }

    fun saveCurrentRequest(collectionId: Long?, name: String) {
        val s = _builderState.value
        viewModelScope.launch {
            val entity = RequestEntity(
                id = s.currentRequestId ?: 0,
                collectionId = collectionId,
                name = name.ifBlank { s.requestName },
                method = s.method,
                url = s.url,
                paramsJson = JsonHelper.serializeKeyValuePairs(s.params),
                headersJson = JsonHelper.serializeKeyValuePairs(s.headers),
                bodyType = s.bodyType.name,
                bodyContent = s.bodyContent,
                authType = s.authType.name,
                authConfigJson = JsonHelper.serializeAuthConfig(s.authConfig),
                updatedAt = System.currentTimeMillis()
            )
            val newId = repository.saveRequest(entity)
            _builderState.update { it.copy(currentRequestId = newId, requestName = entity.name) }
            emitToast("Saved to collection!")
        }
    }

    // --- Environments ---
    fun selectEnvironment(envId: Long) {
        viewModelScope.launch {
            repository.selectEnvironment(envId)
            emitToast("Active environment updated")
        }
    }

    fun saveEnvironment(id: Long, name: String, variables: List<KeyValuePair>) {
        viewModelScope.launch {
            val entity = EnvironmentEntity(
                id = id,
                name = name,
                variablesJson = JsonHelper.serializeKeyValuePairs(variables)
            )
            repository.saveEnvironment(entity)
            emitToast("Saved environment '$name'")
        }
    }

    fun deleteEnvironment(id: Long) {
        viewModelScope.launch {
            repository.deleteEnvironment(id)
            emitToast("Deleted environment")
        }
    }

    fun unlockVault() {
        _isVaultUnlocked.value = true
        emitToast("Vault unlocked! Secrets revealed.")
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        emitToast("Vault locked.")
    }

    // --- Collections ---
    fun createCollection(name: String, description: String) {
        viewModelScope.launch {
            repository.saveCollection(CollectionEntity(name = name, description = description))
            emitToast("Collection '$name' created")
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            repository.deleteCollection(id)
            emitToast("Collection deleted")
        }
    }

    fun deleteRequest(id: Long) {
        viewModelScope.launch {
            repository.deleteRequest(id)
            emitToast("Request removed")
        }
    }

    fun importPostman(json: String) {
        viewModelScope.launch {
            try {
                val (col, count) = repository.importPostmanCollection(json)
                emitToast("Imported '${col.name}' with $count requests!")
            } catch (e: Exception) {
                emitToast("Import failed: ${e.localizedMessage ?: "Invalid JSON"}")
            }
        }
    }

    suspend fun exportPostmanCollection(collectionId: Long): String {
        return repository.exportCollection(collectionId)
    }

    // --- History & Snapshots ---
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            emitToast("History cleared")
        }
    }

    fun setCompareItemA(item: RequestHistoryEntity?) {
        _compareItemA.value = item
    }

    fun setCompareItemB(item: RequestHistoryEntity?) {
        _compareItemB.value = item
    }

    fun saveSnapshot(title: String, response: NetworkResponse) {
        viewModelScope.launch {
            val snapshot = ResponseSnapshotEntity(
                title = title,
                method = _builderState.value.method,
                url = _builderState.value.url,
                statusCode = response.statusCode,
                durationMs = response.durationMs,
                responseBody = response.body
            )
            repository.saveSnapshot(snapshot)
            emitToast("Snapshot '$title' saved!")
        }
    }

    fun deleteSnapshot(id: Long) {
        viewModelScope.launch {
            repository.deleteSnapshot(id)
            emitToast("Snapshot removed")
        }
    }

    // --- Status Page / Monitored Endpoints ---
    fun pingAllMonitored() {
        val list = monitoredEndpoints.value
        if (list.isEmpty()) return
        viewModelScope.launch {
            _isPingingAll.value = true
            list.forEach { endpoint ->
                repository.pingMonitoredEndpoint(endpoint)
            }
            _isPingingAll.value = false
            emitToast("Health check complete for ${list.size} endpoints")
        }
    }

    fun pingSingleEndpoint(endpoint: MonitoredEndpointEntity) {
        viewModelScope.launch {
            repository.pingMonitoredEndpoint(endpoint)
            emitToast("Checked ${endpoint.name}")
        }
    }

    fun addMonitoredEndpoint(name: String, url: String, expectedStatus: Int) {
        addMonitoredEndpoint(name, url, "GET", expectedStatus)
    }

    fun addMonitoredEndpoint(name: String, url: String, method: String, expectedStatus: Int) {
        viewModelScope.launch {
            repository.saveMonitored(
                MonitoredEndpointEntity(
                    name = name,
                    url = url,
                    method = method,
                    expectedStatus = expectedStatus
                )
            )
            emitToast("Endpoint added to health monitor")
        }
    }

    fun deleteMonitored(id: Long) {
        viewModelScope.launch {
            repository.deleteMonitored(id)
            emitToast("Endpoint removed from monitor")
        }
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastMessage.emit(msg)
        }
    }
}
