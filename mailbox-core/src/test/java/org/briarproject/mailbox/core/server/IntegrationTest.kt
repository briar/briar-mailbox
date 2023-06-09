package org.briarproject.mailbox.core.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.jackson
import org.briarproject.mailbox.core.DaggerTestComponent
import org.briarproject.mailbox.core.TestComponent
import org.briarproject.mailbox.core.TestModule
import org.briarproject.mailbox.core.TestUtils.getNewRandomContact
import org.briarproject.mailbox.core.TestUtils.getNewRandomId
import org.briarproject.mailbox.core.contacts.Contact
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SUCCESS
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(Lifecycle.PER_CLASS)
abstract class IntegrationTest(private val installJsonFeature: Boolean = true) {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(IntegrationTest::class.java)
    }

    protected lateinit var testComponent: TestComponent
    protected val db by lazy { testComponent.getDatabase() }
    private val lifecycleManager by lazy { testComponent.getLifecycleManager() }
    protected val setupManager by lazy { testComponent.getSetupManager() }
    protected val metadataManager by lazy { testComponent.getMetadataManager() }
    protected val wipeManager by lazy { testComponent.getWipeManager() }
    protected val httpClient = HttpClient(CIO) {
        expectSuccess = false // prevents exceptions on non-success responses
        if (installJsonFeature) {
            install(ContentNegotiation) {
                jackson()
            }
        }
        install(Logging) {
            level = LogLevel.ALL
        }
    }
    protected lateinit var baseUrl: String
        private set

    protected val ownerToken = getNewRandomId()
    protected val token = getNewRandomId()
    protected val contact1 = getNewRandomContact()
    protected val contact2 = getNewRandomContact()
    protected var tempDir: File? = null

    @Volatile
    protected var exceptionInBackgroundThread: Throwable? = null

    init {
        // Ensure exceptions thrown on worker threads cause tests to fail
        val fail = Thread.UncaughtExceptionHandler { _: Thread?, throwable: Throwable ->
            LOG.warn("Caught unhandled exception", throwable)
            exceptionInBackgroundThread = throwable
        }
        Thread.setDefaultUncaughtExceptionHandler(fail)
    }

    @BeforeAll
    fun setUp(@TempDir tempDir: File) {
        this.tempDir = tempDir
        testComponent = DaggerTestComponent.builder().testModule(TestModule(tempDir)).build()
        testComponent.injectCoreEagerSingletons()
        assertFalse(setupManager.hasDb)
        assertEquals(SUCCESS, lifecycleManager.startServices())
        lifecycleManager.waitForStartup()
        baseUrl = "http://127.0.0.1:${testComponent.getWebServerManager().port}"
    }

    @AfterAll
    fun tearDown() {
        lifecycleManager.stopServices(false)
        lifecycleManager.waitForShutdown()
    }

    @BeforeEach
    open fun beforeEach() {
        exceptionInBackgroundThread = null
        // need to reopen database here because we're closing it after each test
        db.open(null)
        db.read { txn ->
            // clears [metadataManager.ownerConnectionTime]
            metadataManager.onDatabaseOpened(txn)
        }
        assertTrue(setupManager.hasDb)
    }

    @AfterEach
    open fun afterEach() {
        afterEach(true)
    }

    fun afterEach(wipe: Boolean) {
        if (wipe) {
            wipeManager.wipeDatabaseAndFiles()
            assertFalse(setupManager.hasDb)
        }

        if (exceptionInBackgroundThread != null) {
            fail(
                "background thread has thrown an exception unexpectedly",
                exceptionInBackgroundThread
            )
        }
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
