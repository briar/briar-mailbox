package org.briarproject.mailbox.core.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import org.briarproject.mailbox.core.DaggerTestComponent
import org.briarproject.mailbox.core.TestComponent
import org.briarproject.mailbox.core.TestModule
import org.briarproject.mailbox.core.TestUtils.getNewRandomContact
import org.briarproject.mailbox.core.TestUtils.getNewRandomId
import org.briarproject.mailbox.core.contacts.Contact
import org.briarproject.mailbox.core.server.WebServerManager.Companion.PORT
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.io.TempDir
import java.io.File

@TestInstance(Lifecycle.PER_CLASS)
abstract class IntegrationTest(private val installJsonFeature: Boolean = true) {

    protected lateinit var testComponent: TestComponent
    protected val db by lazy { testComponent.getDatabase() }
    private val lifecycleManager by lazy { testComponent.getLifecycleManager() }
    protected val metadataManager by lazy { testComponent.getMetadataManager() }
    protected val httpClient = HttpClient(CIO) {
        expectSuccess = false // prevents exceptions on non-success responses
        if (installJsonFeature) {
            install(JsonFeature) {
                serializer = JacksonSerializer()
            }
        }
    }
    protected val baseUrl = "http://127.0.0.1:$PORT"

    protected val ownerToken = getNewRandomId()
    protected val token = getNewRandomId()
    protected val id = getNewRandomId()
    protected val contact1 = getNewRandomContact()
    protected val contact2 = getNewRandomContact()

    @BeforeAll
    fun setUp(@TempDir tempDir: File) {
        testComponent = DaggerTestComponent.builder().testModule(TestModule(tempDir)).build()
        testComponent.injectCoreEagerSingletons()
        lifecycleManager.startServices()
        lifecycleManager.waitForStartup()
    }

    @AfterAll
    fun tearDown() {
        lifecycleManager.stopServices()
        lifecycleManager.waitForShutdown()
    }

    protected fun addOwnerToken() {
        testComponent.getSetupManager().setToken(null, ownerToken)
    }

    protected fun addContact(c: Contact) {
        val db = testComponent.getDatabase()
        db.write { txn ->
            db.addContact(txn, c)
        }
    }

    protected fun HttpRequestBuilder.authenticateWithToken(t: String) {
        headers {
            @Suppress("EXPERIMENTAL_API_USAGE_FUTURE_ERROR")
            append(HttpHeaders.Authorization, "Bearer $t")
        }
    }

}
