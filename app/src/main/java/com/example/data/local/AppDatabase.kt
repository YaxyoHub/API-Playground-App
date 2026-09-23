package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CollectionEntity::class,
        RequestEntity::class,
        EnvironmentEntity::class,
        RequestHistoryEntity::class,
        ResponseSnapshotEntity::class,
        MonitoredEndpointEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun requestDao(): RequestDao
    abstract fun environmentDao(): EnvironmentDao
    abstract fun historyDao(): HistoryDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun monitoredEndpointDao(): MonitoredEndpointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "api_playground.db"
                ).addCallback(DatabaseCallback(scope)).build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val envDao = database.environmentDao()
            val colDao = database.collectionDao()
            val reqDao = database.requestDao()
            val monDao = database.monitoredEndpointDao()

            // 1. Environments
            val devEnvId = envDao.insertEnvironment(
                EnvironmentEntity(
                    name = "Production",
                    isSelected = true,
                    variablesJson = """[{"key":"baseUrl","value":"https://jsonplaceholder.typicode.com","isSecret":false},{"key":"apiKey","value":"prod_key_9942a","isSecret":true},{"key":"userId","value":"1","isSecret":false}]"""
                )
            )
            envDao.insertEnvironment(
                EnvironmentEntity(
                    name = "Staging",
                    isSelected = false,
                    variablesJson = """[{"key":"baseUrl","value":"https://httpbin.org","isSecret":false},{"key":"apiKey","value":"stage_key_demo","isSecret":true}]"""
                )
            )
            envDao.insertEnvironment(
                EnvironmentEntity(
                    name = "Local Dev",
                    isSelected = false,
                    variablesJson = """[{"key":"baseUrl","value":"http://10.0.2.2:8000","isSecret":false},{"key":"apiKey","value":"dev_secret_local","isSecret":true}]"""
                )
            )

            // 2. Collections & Requests
            val col1Id = colDao.insertCollection(
                CollectionEntity(
                    name = "JSONPlaceholder",
                    description = "Popular free fake REST API for prototyping"
                )
            )
            reqDao.insertRequest(
                RequestEntity(
                    collectionId = col1Id,
                    name = "Get All Posts",
                    method = "GET",
                    url = "{{baseUrl}}/posts",
                    headersJson = """[{"key":"Accept","value":"application/json","enabled":true}]""",
                    paramsJson = """[{"key":"_limit","value":"5","enabled":true}]"""
                )
            )
            reqDao.insertRequest(
                RequestEntity(
                    collectionId = col1Id,
                    name = "Get Post by ID",
                    method = "GET",
                    url = "{{baseUrl}}/posts/{{userId}}",
                    headersJson = """[{"key":"Accept","value":"application/json","enabled":true}]"""
                )
            )
            reqDao.insertRequest(
                RequestEntity(
                    collectionId = col1Id,
                    name = "Create New Post",
                    method = "POST",
                    url = "{{baseUrl}}/posts",
                    headersJson = """[{"key":"Content-Type","value":"application/json","enabled":true}]""",
                    bodyType = "JSON",
                    bodyContent = """{\n  "title": "Debugging on mobile",\n  "body": "Testing endpoints directly from API Playground",\n  "userId": 1\n}"""
                )
            )
            reqDao.insertRequest(
                RequestEntity(
                    collectionId = col1Id,
                    name = "Delete Post",
                    method = "DELETE",
                    url = "{{baseUrl}}/posts/1"
                )
            )

            val col2Id = colDao.insertCollection(
                CollectionEntity(
                    name = "HTTPBin Echo & Test",
                    description = "HTTP request & response testing service"
                )
            )
            reqDao.insertRequest(
                RequestEntity(
                    collectionId = col2Id,
                    name = "Echo Headers & IP",
                    method = "GET",
                    url = "https://httpbin.org/get",
                    headersJson = """[{"key":"User-Agent","value":"API-Playground-Mobile/1.0","enabled":true}]""",
                    paramsJson = """[{"key":"platform","value":"android","enabled":true}]"""
                )
            )
            reqDao.insertRequest(
                RequestEntity(
                    collectionId = col2Id,
                    name = "Test 418 Teapot",
                    method = "GET",
                    url = "https://httpbin.org/status/418"
                )
            )
            reqDao.insertRequest(
                RequestEntity(
                    collectionId = col2Id,
                    name = "Delayed Response (1s)",
                    method = "GET",
                    url = "https://httpbin.org/delay/1"
                )
            )

            // 3. Monitored Endpoints for Status Page
            monDao.insertMonitored(
                MonitoredEndpointEntity(
                    name = "JSONPlaceholder API",
                    url = "https://jsonplaceholder.typicode.com/posts/1",
                    method = "GET",
                    expectedStatus = 200
                )
            )
            monDao.insertMonitored(
                MonitoredEndpointEntity(
                    name = "HTTPBin Echo Service",
                    url = "https://httpbin.org/get",
                    method = "GET",
                    expectedStatus = 200
                )
            )
            monDao.insertMonitored(
                MonitoredEndpointEntity(
                    name = "Public DNS / Cloudflare Status",
                    url = "https://cloudflare.com/cdn-cgi/trace",
                    method = "GET",
                    expectedStatus = 200
                )
            )
        }
    }
}
