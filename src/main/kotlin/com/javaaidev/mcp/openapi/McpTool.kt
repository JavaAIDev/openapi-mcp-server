package com.javaaidev.mcp.openapi

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import kotlinx.serialization.json.*
import org.apache.commons.lang3.StringUtils
import java.util.function.Supplier

data class OpenAPIOperation(
    val operation: Operation,
    val httpMethod: String,
    val path: String,
)

data class OpenAPIOperationFilter(
    val operationIds: List<String>? = null,
    val httpMethods: List<String>? = null,
    val paths: List<String>? = null,
    val tags: List<String>? = null,
) {
    fun match(operation: OpenAPIOperation): Boolean {
        val checkers = mutableListOf<Supplier<Boolean>>()
        operationIds?.let {
            checkers.add {
                StringUtils.isNotBlank(operation.operation.operationId)
                        && it.contains(operation.operation.operationId)
            }
        }
        httpMethods?.let {
            checkers.add {
                it.any { method ->
                    method.equals(operation.httpMethod, true)
                }
            }
        }
        paths?.let {
            checkers.add {
                it.any { path ->
                    path.equals(operation.path, true)
                }
            }
        }
        tags?.let {
            checkers.add {
                !operation.operation.tags.isNullOrEmpty() &&
                        it.intersect(operation.operation.tags).isNotEmpty()
            }
        }
        return checkers.all { it.get() }
    }
}

data class McpTool(
    val tool: Tool,
    val handler: suspend (CallToolRequest) -> CallToolResult,
)

object McpToolHelper {
    private val httpClient = HttpClient {
        defaultRequest {
            headers {
                append(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36"
                )
            }
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
    }

    fun toTools(
        openAPI: OpenAPI,
        openAPIOperationFilter: OpenAPIOperationFilter? = null
    ): List<McpTool> {
        logger.info("Create tools with filter {}", openAPIOperationFilter)
        val serverUrl = openAPI.servers.first().url
        val components = openAPI.components?.schemas
        val operations = openAPI.paths?.entries?.flatMap { entry ->
            val path = entry.key
            listOfNotNull(
                entry.value.get?.let { OpenAPIOperation(it, "GET", path) },
                entry.value.post?.let { OpenAPIOperation(it, "POST", path) },
                entry.value.put?.let { OpenAPIOperation(it, "PUT", path) },
                entry.value.delete?.let { OpenAPIOperation(it, "DELETE", path) },
                entry.value.patch?.let { OpenAPIOperation(it, "PATCH", path) },
            )
        }?.also {
            logger.info("Found {} operations", it.size)
        }?.filter { operation -> openAPIOperationFilter?.match(operation) ?: true }?.also {
            logger.info("Use {} operations after filtering", it.size)
        }
        return operations?.map {
            toTool(serverUrl, it.operation, it.httpMethod, it.path, components)
        } ?: listOf()
    }

    fun toTool(
        serverUrl: String,
        operation: Operation,
        httpMethod: String,
        path: String,
        components: Map<String, Schema<*>>?
    ): McpTool {
        val name = sanitizeToolName(operation.operationId ?: "${httpMethod}_$path")
        val description = operation.description ?: (operation.summary ?: "")
        val (parameters, requiredParams) = operationParameters(operation, components)
        val requestBody = operationRequestBody(operation, components)
        val responseBody = operationResponseBody(operation, components)
        val toolAnnotations = if (httpMethod == "GET")
            ToolAnnotations(
                name,
                readOnlyHint = true,
                destructiveHint = false,
            ) else ToolAnnotations(name)
        val toolInput =
            if (parameters?.isNotEmpty() == true && requestBody?.isNotEmpty() == true) {
                Tool.Input(
                    JsonObject(
                        mapOf(
                            "parameters" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("object"),
                                    "properties" to parameters,
                                    "required" to JsonArray((requiredParams ?: listOf()).map {
                                        JsonPrimitive(it)
                                    })
                                )
                            ),
                            "requestBody" to requestBody,
                        )
                    )
                )
            } else if (parameters?.isNotEmpty() == true) {
                Tool.Input(parameters, requiredParams)
            } else if (requestBody?.isNotEmpty() == true) {
                Tool.Input(
                    requestBody["properties"] as? JsonObject ?: JsonObject(mapOf()),
                    (requestBody["required"] as? JsonArray)?.toList()
                        ?.map { it.jsonPrimitive.content })
            } else {
                Tool.Input()
            }
        val tool = Tool(
            name,
            name,
            description,
            toolInput,
            responseBody?.let { Tool.Output(it) },
            toolAnnotations
        )
        val urlTemplate = serverUrl.removeSuffix("/") + "/" + path.removePrefix("/")
        return McpTool(tool) { request ->
            callApi(urlTemplate, httpMethod, request)
        }
    }

    private fun sanitizeToolName(input: String): String {
        return input
            .replace(Regex("[^A-Za-z0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
    }


    private suspend fun callApi(
        urlTemplate: String,
        httpMethod: String,
        request: CallToolRequest
    ): CallToolResult {
        return httpClient.request {
            url(
                parseUrl(expandUriTemplate(urlTemplate, request.arguments))
                    ?: throw RuntimeException("Invalid url")
            )
            method = when (httpMethod) {
                "PUT" -> HttpMethod.Put
                "POST" -> HttpMethod.Post
                "PATCH" -> HttpMethod.Patch
                "DELETE" -> HttpMethod.Delete
                else -> HttpMethod.Get
            }
        }.bodyAsText().let { text ->
            CallToolResult(listOf(TextContent(text)))
        }
    }

    private fun operationParameters(
        operation: Operation,
        components: Map<String, Schema<*>>?
    ): Pair<JsonObject?, List<String>?> {
        val parameters = operation.parameters?.filter { parameter ->
            setOf("query", "path").contains(parameter.`in`)
        }
        val required = parameters?.filter { it.required }?.map { it.name }
        return (parameters?.associate { parameter ->
            parameter.name to schemaToJsonObject(parameter.schema, components, parameter)
        }?.let {
            JsonObject(it)
        } to required)
    }

    private fun operationRequestBody(operation: Operation, components: Map<String, Schema<*>>?) =
        operation.requestBody?.content?.let {
            schemaFromJsonContent(it, components)
        }

    private fun operationResponseBody(operation: Operation, components: Map<String, Schema<*>>?) =
        operation.responses?.let { responses ->
            responses.entries.firstOrNull { entry -> entry.key >= "200" && entry.key < "300" }?.value?.content?.let { content ->
                schemaFromJsonContent(content, components)
            }
        }

    private fun schemaFromJsonContent(content: Content, components: Map<String, Schema<*>>?) =
        content.entries.firstOrNull { entry -> entry.key.contains("json") }?.value?.schema?.let {
            schemaToJsonObject(it, components)
        }

    private fun schemaToJsonObject(
        schema: Schema<*>,
        components: Map<String, Schema<*>>?,
        parameter: Parameter? = null,
    ): JsonObject {
        return schemaToJsonObjectInternal(mutableListOf(), schema, components, parameter)
    }

    private fun schemaToJsonObjectInternal(
        seen: MutableList<Schema<*>>,
        schema: Schema<*>,
        components: Map<String, Schema<*>>?,
        parameter: Parameter? = null,
    ): JsonObject {
        if (seen.contains(schema)) {
            return JsonObject(mapOf())
        }
        seen.add(schema)
        if (schema.`$ref`?.isNotBlank() == true) {
            val component = schema.`$ref`.substringAfterLast("/")
            components?.get(component)
                ?.let { schema ->
                    return schemaToJsonObjectInternal(seen, schema, components)
                }
        }

        val content = mutableMapOf<String, JsonElement>()
        (schema.description ?: parameter?.description)?.let {
            content["description"] = JsonPrimitive(it)
        }

        content["type"] = JsonPrimitive(schema.type)
        schema.format?.let {
            content["format"] = JsonPrimitive(it)
        }

        when (schema.type) {
            "string" -> {
                if (schema is StringSchema) {
                    schema.enum?.let {
                        content["enum"] = JsonArray(it.map { v -> JsonPrimitive(v) })
                    }
                }
            }

            "number", "integer" -> {
                schema.minimum?.let {
                    content["minimum"] = JsonPrimitive(it)
                }
                schema.maximum?.let {
                    content["maximum"] = JsonPrimitive(it)
                }
            }

            "boolean" -> {

            }

            "object" -> {
                val properties = mutableMapOf<String, JsonElement>()
                schema.properties?.forEach { (name, propertySchema) ->
                    properties[name] = schemaToJsonObjectInternal(seen, propertySchema, components)
                }
                content["properties"] = JsonObject(properties)
            }

            "array" -> {
                schema.items?.let {
                    content["items"] = schemaToJsonObjectInternal(seen, it, components)
                }
            }
        }
        return JsonObject(content)
    }

    private fun expandUriTemplate(template: String, values: JsonObject): String {
        var result = template
        values.forEach { (key, value) ->
            if (value is JsonPrimitive) {
                result = result.replace("{$key}", value.contentOrNull ?: "")
            }
        }
        return result
    }
}