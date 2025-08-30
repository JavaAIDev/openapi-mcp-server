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

data class McpTool(
    val tool: Tool,
    val handler: suspend (CallToolRequest) -> CallToolResult,
)

object McpToolHelper {
    private val httpClient = HttpClient {
        defaultRequest {
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

    fun toTools(openAPI: OpenAPI): List<McpTool> {
        val serverUrl = openAPI.servers.first().url
        val components = openAPI.components?.schemas
        return openAPI.paths?.entries?.flatMap { entry ->
            val path = entry.key
            listOfNotNull(
                entry.value.get?.let { toTool(serverUrl, it, "GET", path, components) },
                entry.value.post?.let { toTool(serverUrl, it, "POST", path, components) },
                entry.value.put?.let { toTool(serverUrl, it, "PUT", path, components) },
                entry.value.delete?.let { toTool(serverUrl, it, "DELETE", path, components) },
                entry.value.patch?.let { toTool(serverUrl, it, "PATCH", path, components) },
            )
        } ?: listOf()
    }

    fun toTool(
        serverUrl: String,
        operation: Operation,
        httpMethod: String,
        path: String,
        components: Map<String, Schema<*>>?
    ): McpTool {
        val name = operation.operationId ?: "${httpMethod}_$path"
        val description = operation.description ?: (operation.summary ?: "")
        val (parameters, requiredParams) = operationParameters(operation, components)
        val requestBody = operationRequestBody(operation, components)
        val responseBody = operationResponseBody(operation, components)
        val toolAnnotations = if (httpMethod == "GET")
            ToolAnnotations(
                operation.operationId,
                readOnlyHint = true,
                destructiveHint = false,
                openWorldHint = false
            ) else ToolAnnotations(operation.operationId, openWorldHint = false)
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
            name, description, toolInput, responseBody?.let { Tool.Output(it) }, toolAnnotations
        )
        val urlTemplate = serverUrl.removeSuffix("/") + "/" + path.removePrefix("/")
        return McpTool(tool) { request ->
            callApi(urlTemplate, httpMethod, request)
        }
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