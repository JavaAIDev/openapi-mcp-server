package com.javaaidev.mcp.openapi

import kotlin.test.Test
import kotlin.test.assertTrue

class McpToolHelperTest {
    @Test
    fun testParsePetStoreAPI() {
        val openAPI = OpenAPIParser.parse("https://petstore.swagger.io/v2/swagger.json")
        val tools = McpToolHelper.toTools(openAPI)
        assertTrue(tools.isNotEmpty())
    }

    @Test
    fun testParseCanadaHolidaysAPI() {
        val openAPI =
            OpenAPIParser.parse("https://api.apis.guru/v2/specs/canada-holidays.ca/1.8.0/openapi.json")
        val tools = McpToolHelper.toTools(openAPI)
        assertTrue(tools.isNotEmpty())
    }
}