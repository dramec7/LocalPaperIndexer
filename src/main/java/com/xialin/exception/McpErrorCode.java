package com.xialin.exception;


public enum McpErrorCode {

    // 标准 JSON-RPC 错误
    PARSE_ERROR(-32700, "Parse error"),
    INVALID_REQUEST(-32600, "Invalid Request"),
    METHOD_NOT_FOUND(-32601, "Method not found"),
    INVALID_PARAMS(-32602, "Invalid params"),
    INTERNAL_ERROR(-32603, "Internal error"),

    // 自定义业务错误 (-32000 到 -32099)
    TOOL_NOT_FOUND(-32000, "Tool not found"),
    FILE_NOT_FOUND(-32001, "File or directory not found"),
    PERMISSION_DENIED(-32002, "Permission denied"),
    READ_TIMEOUT(-32003, "Operation timed out"),
    TOOL_EXECUTION_FAILED(-32004, "Tool execution failed");

    private final int code;
    private final String message;

    McpErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
