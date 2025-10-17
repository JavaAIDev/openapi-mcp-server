package com.javaaidev.mcp.openapi

import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@CommandLine.Command(
    name = "openapi-mcp",
    mixinStandardHelpOptions = true,
    version = ["0.1.4"],
    description = ["Run OpenAPI MCP server"],
)
class Cli : Callable<Int> {
    @CommandLine.Parameters(index = "0", description = ["File path or URL of OpenAPI spec"])
    lateinit var openapiSpec: String

    @CommandLine.Option(
        names = ["--include-operation-id"],
        split = ",",
        description = ["Include operations with id (comma separated)"]
    )
    private val includeOperationIds: List<String>? = null

    @CommandLine.Option(
        names = ["--include-http-method"],
        split = ",",
        description = ["Include operations with HTTP methods (comma separated)"]
    )
    private val includeHttpMethods: List<String>? = null

    @CommandLine.Option(
        names = ["--include-path"],
        split = ",",
        description = ["Include operations with paths (comma separated)"]
    )
    private val includePaths: List<String>? = null

    @CommandLine.Option(
        names = ["--include-tag"],
        split = ",",
        description = ["Include operations with tags (comma separated)"]
    )
    private val includeTags: List<String>? = null

    @CommandLine.Option(
        names = ["--header"],
        split = ",",
        description = ["Headers (comma separated with format a=b)"]
    )
    private val headers: List<String>? = null

    @CommandLine.Option(
        names = ["--query-param"],
        split = ",",
        description = ["Query params (comma separated with format a=b)"]
    )
    private val queryParams: List<String>? = null

    override fun call(): Int {
        McpServer.start(
            openapiSpec,
            OpenAPIOperationFilter(
                includeOperationIds,
                includeHttpMethods,
                includePaths,
                includeTags
            ),
            parseToMap(queryParams),
            parseToMap(headers),
        )
        return 0
    }

    private fun parseToMap(values: List<String>?) = values?.map {
        it.split("=")
    }?.filter { it.size == 2 }?.associate { it[0] to it[1] }
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(Cli()).execute(*args))
}