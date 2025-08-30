package com.javaaidev.mcp.openapi

import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess

enum class TransportType {
    stdio, httpSse, streamableHttp
}

@CommandLine.Command(
    name = "openapi-mcp",
    mixinStandardHelpOptions = true,
    version = ["0.1.0"],
    description = ["Run OpenAPI MCP server"],
)
class Cli : Callable<Int> {
    @CommandLine.Parameters(index = "0", description = ["File path or URL of OpenAPI spec"])
    lateinit var openapiSpec: String

    @CommandLine.Option(
        names = ["--transport"],
        defaultValue = "stdio",
        description = [$$"MCP transport type. Valid values: ${COMPLETION-CANDIDATES}"],
    )
    var transportType: TransportType = TransportType.stdio

    override fun call(): Int {
        McpServer.start(openapiSpec)
        return 0
    }
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(Cli()).execute(*args))
}