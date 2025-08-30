package com.javaaidev.mcp.openapi

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.core.models.ParseOptions

object OpenAPIParser {
    fun parse(spec: String): OpenAPI {
        val parseOption = ParseOptions()
        parseOption.isFlatten = false
        parseOption.isResolveFully = true
        parseOption.isResolveRequestBody = true
        parseOption.isResolveResponses = true
        parseOption.isResolve = true
        parseOption.isFlattenComposedSchemas = false
        val result = OpenAPIParser().readLocation(spec, null, parseOption)
        return result?.openAPI
            ?: throw RuntimeException("OpenAPI spec parse error: ${result.messages}")
    }
}