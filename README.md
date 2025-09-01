# OpenAPI MCP Server

Convert OpenAPI spec to MCP server, OpenAPI operations as tools

## CLI Options

When running the MCP server, an OpenAPI spec is required. It can be either a file path or URL.

The MCP server also provides options to filter operations to be converted as MCP tools. Filter conditions of the same type are combined using `OR`, while filter conditions of different types are combined using 'AND'.

Currently supported filter conditions:

| Condition    | CLI option               | Example                       |
|--------------|--------------------------|-------------------------------|
| HTTP method  | `--include-http-method`  | `--include-http-method=GET`   |
| Path         | `--include-path`         | `--include-path=/holidays`    |
| Operation Id | `--include-operation-id` | `--include-operation-id=Root` |
| Tag          | `--include-tag`          | `--include-tag=info`          |

You can run the JAR file to see the CLI help.

```
Usage: openapi-mcp [-hV] [--include-http-method=<includeHttpMethods>[,
                   <includeHttpMethods>...]]...
                   [--include-operation-id=<includeOperationIds>[,
                   <includeOperationIds>...]]... [--include-path=<includePaths>
                   [,<includePaths>...]]... [--include-tag=<includeTags>[,
                   <includeTags>...]]... <openapiSpec>
Run OpenAPI MCP server
      <openapiSpec>   File path or URL of OpenAPI spec
  -h, --help          Show this help message and exit.
      --include-http-method=<includeHttpMethods>[,<includeHttpMethods>...]
                      Include operations with HTTP methods (comma separated)
      --include-operation-id=<includeOperationIds>[,<includeOperationIds>...]
                      Include operations with id (comma separated)
      --include-path=<includePaths>[,<includePaths>...]
                      Include operations with paths (comma separated)
      --include-tag=<includeTags>[,<includeTags>...]
                      Include operations with tags (comma separated)
  -V, --version       Print version information and exit.
```

## How to Run

### Container

Use Docker or Podman to run the container.

```sh
podman run -i ghcr.io/javaaidev/openapi-mcp-server https://api.apis.guru/v2/specs/canada-holidays.ca/1.8.0/openapi.json
```
