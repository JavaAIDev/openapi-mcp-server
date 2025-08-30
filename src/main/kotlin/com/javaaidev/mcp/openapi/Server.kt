package com.javaaidev.mcp.openapi

import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("McpServer")

object McpServer {

    fun start(openapiSpec: String, openAPIOperationFilter: OpenAPIOperationFilter? = null) {
        logger.info("Parse OpenAPI spec {} ", openapiSpec)
        val openAPI = OpenAPIParser.parse(openapiSpec)

        val server = Server(
            Implementation(
                name = openAPI.info?.title ?: "openapi-mcp-server",
                version = openAPI.info?.version ?: "1.0.0"
            ),
            ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true),
                    logging = null,
                )
            )
        )

        McpToolHelper.toTools(openAPI, openAPIOperationFilter).forEach { tool ->
            server.addTool(tool.tool, tool.handler)
        }

        val transport = StdioServerTransport(
            System.`in`.asInput(),
            System.out.asSink().buffered()
        )

        runBlocking {
            server.connect(transport)
            val done = Job()
            server.onClose {
                done.complete()
            }
            done.join()
        }
    }
}