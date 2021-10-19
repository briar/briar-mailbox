package org.briarproject.mailbox.core.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.briarproject.mailbox.core.DaggerTestComponent
import org.briarproject.mailbox.core.TestComponent
import org.briarproject.mailbox.core.TestModule
import org.briarproject.mailbox.core.server.WebServerManager.Companion.PORT
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.io.TempDir
import java.io.File

@TestInstance(Lifecycle.PER_CLASS)
abstract class IntegrationTest {

    protected lateinit var testComponent: TestComponent
    private val lifecycleManager by lazy { testComponent.getLifecycleManager() }
    protected val httpClient = HttpClient(CIO) {
        expectSuccess = false // prevents exceptions on non-success responses
    }
    protected val baseUrl = "http://127.0.0.1:$PORT"

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

}
