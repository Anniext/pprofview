package com.github.anniext.pprofview.parser

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * pprof 文本解析器测试
 */
class PprofTextParserTest : BasePlatformTestCase() {
    
    fun testParseSimpleReport() {
        val text = """
            File: test
            Type: cpu
            Time: Nov 20, 2025 at 10:00am (CST)
            Duration: 10s
            Showing nodes accounting for 20s, 100% of 20s total
                  flat  flat%   sum%        cum   cum%
                10.50s 52.50% 52.50%     10.50s 52.50%  main.fibonacci
                 5.25s 26.25% 78.75%      5.25s 26.25%  runtime.mallocgc
                 2.50s 12.50% 91.25%      2.50s 12.50%  runtime.scanobject
                 1.75s  8.75%   100%      1.75s  8.75%  runtime.memmove
        """.trimIndent()
        
        val parser = PprofTextParser()
        val report = parser.parse(text)
        
        assertEquals(4, report.entries.size)
        assertEquals("cpu", report.unit)
        
        val firstEntry = report.entries[0]
        assertEquals("main.fibonacci", firstEntry.functionName)
        assertEquals(52.50, firstEntry.flatPercent, 0.01)
        assertEquals(52.50, firstEntry.cumPercent, 0.01)
    }
    
    fun testParseEmptyReport() {
        val text = ""
        
        val parser = PprofTextParser()
        val report = parser.parse(text)
        
        assertTrue(report.entries.isEmpty())
        assertEquals(0L, report.totalSamples)
    }
    
    fun testParseWithMemoryUnits() {
        val text = """
            Type: alloc_space
                  flat  flat%   sum%        cum   cum%
              512.50MB 50.00% 50.00%   512.50MB 50.00%  main.allocate
              256.25MB 25.00% 75.00%   256.25MB 25.00%  runtime.makeslice
              128.13MB 12.50% 87.50%   128.13MB 12.50%  runtime.newobject
              128.12MB 12.50%   100%   128.12MB 12.50%  runtime.mallocgc
        """.trimIndent()
        
        val parser = PprofTextParser()
        val report = parser.parse(text)
        
        assertEquals(4, report.entries.size)
        assertEquals("alloc_space", report.unit)
        
        val firstEntry = report.entries[0]
        assertEquals("main.allocate", firstEntry.functionName)
        assertTrue(firstEntry.flat > 0)
    }
}
