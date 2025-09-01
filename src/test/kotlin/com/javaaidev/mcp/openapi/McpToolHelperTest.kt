package com.javaaidev.mcp.openapi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class McpToolHelperTest {
    @Test
    fun testParsePetStoreAPI() {
        val openAPI = OpenAPIParser.parse("https://petstore.swagger.io/v2/swagger.json")
        val tools = McpToolHelper.toTools(openAPI, null)
        assertTrue(tools.isNotEmpty())
    }

    @Test
    fun testParseCanadaHolidaysAPI() {
        val openAPI =
            OpenAPIParser.parse("https://api.apis.guru/v2/specs/canada-holidays.ca/1.8.0/openapi.json")
        val tools = McpToolHelper.toTools(openAPI, null)
        assertTrue(tools.isNotEmpty())
    }

    @Test
    fun testOperationFilter() {
        val openAPI =
            OpenAPIParser.parse("https://api.apis.guru/v2/specs/canada-holidays.ca/1.8.0/openapi.json")
        var tools = McpToolHelper.toTools(
            openAPI, OpenAPIOperationFilter(
                listOf("Root")
            )
        )
        assertEquals(1, tools.size)
        tools = McpToolHelper.toTools(
            openAPI, OpenAPIOperationFilter(
                httpMethods = listOf("POST")
            )
        )
        assertEquals(0, tools.size)
        tools = McpToolHelper.toTools(
            openAPI, OpenAPIOperationFilter(
                paths = listOf("/api/v1/holidays")
            )
        )
        assertEquals(1, tools.size)
        tools = McpToolHelper.toTools(
            openAPI, OpenAPIOperationFilter(
                listOf("Root"),
                httpMethods = listOf("GET")
            )
        )
        assertEquals(1, tools.size)
        tools = McpToolHelper.toTools(
            openAPI, OpenAPIOperationFilter(
                tags = listOf("holidays")
            )
        )
        assertEquals(2, tools.size)
    }
}