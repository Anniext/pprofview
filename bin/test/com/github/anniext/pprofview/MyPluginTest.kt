package com.github.anniext.pprofview

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * pprofview 插件测试
 */
@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    /**
     * 基本测试 - 验证插件可以加载
     */
    fun testPluginLoaded() {
        assertNotNull(project)
        assertTrue(project.isInitialized)
    }

    override fun getTestDataPath() = "src/test/testData"
}
