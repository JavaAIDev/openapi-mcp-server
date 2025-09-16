package com.javaaidev.mcp.openapi

import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@CommandLine.Command(
    name = "openapi-mcp",
    mixinStandardHelpOptions = true,
    version = ["0.1.3"],
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

    override fun call(): Int {
        McpServer.start(
            openapiSpec,
            OpenAPIOperationFilter(
                includeOperationIds,
                includeHttpMethods,
                includePaths,
                includeTags
            )
        )
        return 0
    }
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(Cli()).execute(*args))
}