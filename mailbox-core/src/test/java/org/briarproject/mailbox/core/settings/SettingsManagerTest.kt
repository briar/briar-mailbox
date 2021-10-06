package org.briarproject.mailbox.core.settings

import org.briarproject.mailbox.core.DaggerTestComponent
import org.briarproject.mailbox.core.TestComponent
import org.briarproject.mailbox.core.TestModule
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

@TestInstance(Lifecycle.PER_CLASS)
class SettingsManagerTest {

    private lateinit var testComponent: TestComponent

    @BeforeAll
    fun setUp(@TempDir tempDir: File) {
        testComponent = DaggerTestComponent.builder().testModule(TestModule(tempDir)).build()
        testComponent.injectCoreEagerSingletons()
        testComponent.getDatabase().open(null)
    }

    @Test
    @Throws(java.lang.Exception::class)
    open fun testMergeSettings() {
        val before = Settings()
        before["foo"] = "bar"
        before["baz"] = "bam"
        val update = Settings()
        update["baz"] = "qux"
        val merged = Settings()
        merged["foo"] = "bar"
        merged["baz"] = "qux"

        val sm: SettingsManager = testComponent.getSettingsManager()

        // store 'before'
        sm.mergeSettings(before, "namespace")
        assertEquals(before, sm.getSettings("namespace"))

        // merge 'update'
        sm.mergeSettings(update, "namespace")
        assertEquals(merged, sm.getSettings("namespace"))
    }

}
