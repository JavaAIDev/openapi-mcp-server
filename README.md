# OpenAPI MCP Server

Convert OpenAPI spec to MCP server, OpenAPI operations as tools.

This MCP server uses `stdio` transport.

## CLI Options

When running the MCP server, an OpenAPI spec is required. It can be either a file path or URL.

The MCP server also provides options to filter operations to be converted as MCP tools. Filter conditions of the same type are combined using `OR`, while filter conditions of different types are combined using `AND`.

Currently supported filter conditions:

| Condition    | CLI option               | Example                       |
|--------------|--------------------------|-------------------------------|
| HTTP method  | `--include-http-method`  | `--include-http-method=GET`   |
| Path         | `--include-path`         | `--include-path=/holidays`    |
| Operation Id | `--include-operation-id` | `--include-operation-id=Root` |
| Tag          | `--include-tag`          | `--include-tag=info`          |

The HTTP requests sent to the API can be customized by specifying query parameters and headers.

| Option      | CLI option               | Example                     |
|-------------|--------------------------|-----------------------------|
| Query Param | `--query-param`          | `--query-param=k1=v1,k2=v2` |
| Header      | `--header`               | `--header=h1=v1,h2=v2`      |

You can run the JAR file to see the CLI help.

```
Usage: openapi-mcp [-hV] [--header=<headers>[,<headers>...]]...
                   [--include-http-method=<includeHttpMethods>[,
                   <includeHttpMethods>...]]...
                   [--include-operation-id=<includeOperationIds>[,
                   <includeOperationIds>...]]... [--include-path=<includePaths>
                   [,<includePaths>...]]... [--include-tag=<includeTags>[,
                   <includeTags>...]]... [--query-param=<queryParams>[,
                   <queryParams>...]]... <openapiSpec>
Run OpenAPI MCP server
      <openapiSpec>   File path or URL of OpenAPI spec
  -h, --help          Show this help message and exit.
      --header=<headers>[,<headers>...]
                      Headers (comma separated with format a=b)
      --include-http-method=<includeHttpMethods>[,<includeHttpMethods>...]
                      Include operations with HTTP methods (comma separated)
      --include-operation-id=<includeOperationIds>[,<includeOperationIds>...]
                      Include operations with id (comma separated)
      --include-path=<includePaths>[,<includePaths>...]
                      Include operations with paths (comma separated)
      --include-tag=<includeTags>[,<includeTags>...]
                      Include operations with tags (comma separated)
      --query-param=<queryParams>[,<queryParams>...]
                      Query params (comma separated with format a=b)
  -V, --version       Print version information and exit.
```

## How to Run

### Standalone JAR file

Download latest [release JAR file](https://github.com/JavaAIDev/openapi-mcp-server/releases) and run it using `java -jar`.

```sh
java -jar openapi-mcp-server-0.1.4-all.jar https://api.apis.guru/v2/specs/exchangerate-api.com/4/openapi.json
```

### Container

Use Docker or Podman to run the container.

```sh
docker run -i ghcr.io/javaaidev/openapi-mcp-server https://api.apis.guru/v2/specs/exchangerate-api.com/4/openapi.json
```
