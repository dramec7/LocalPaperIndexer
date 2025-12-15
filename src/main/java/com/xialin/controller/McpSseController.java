package com.xialin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xialin.exception.McpErrorCode;
import com.xialin.exception.McpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.xialin.service.PaperService;
import com.xialin.tools.McpTool;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class McpSseController {

    private static final Logger log = LoggerFactory.getLogger(McpSseController.class);

    @Autowired
    private PaperService paperService;

    @Autowired
    private ObjectMapper mapper;

    // 用来存储当前的连接 如果是多用户的话，需要用 Map 存储
    private SseEmitter activeEmitter;

    // 工具注册表
    private final Map<String, McpTool> toolRegistry;

    public McpSseController(List<McpTool> tools) {
        this.toolRegistry = tools.stream()
                .collect(Collectors.toMap(McpTool::getName, tool -> tool));

        log.info("Loaded {} com.xialin.tools: {}", tools.size(), toolRegistry.keySet());
    }


    // 建立 SSE 连接通道，produces 指定响应类型为 SSE
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSseConnection() {

        this.activeEmitter = new SseEmitter(3600_000L);

        // 连接建立后，发送 endpoint 地址给 Client
        try {
            activeEmitter.send(SseEmitter.event().name("endpoint").data("/messages"));
            log.info("Endpoint sent to client");
        } catch (Exception e) {
            log.info("Fail to send endpoint to client: {}", e.getMessage());
        }

        return activeEmitter;
    }

    // 接收消息通道 开始握手
    @PostMapping("/messages")
    public void handleMessage(@RequestBody String jsonRpcBody) {
        log.info("Received request: {}", jsonRpcBody);
        JsonNode id = null;

        try {
            // 解析请求
            JsonNode request;
            try {
                request = mapper.readTree(jsonRpcBody);
            } catch (Exception e) {
                // json 解析失败
                sendError(null, McpErrorCode.PARSE_ERROR.getCode(), "Parse error: Invalid JSON");
                return;
            }

            id = request.get("id");
            String method = request.path("method").asText();

            // 校验参数 防止 method 为空
            if (method.isEmpty()) {
                sendError(id, McpErrorCode.INVALID_REQUEST.getCode(), "Invalid Request: Method is missing");
                return;
            }

            // 执行业务逻辑
            Object result = processRequest(method, request.get("params"));

            // 发送成功响应
            if (id != null && !id.isNull()) {
                ObjectNode response = mapper.createObjectNode();
                response.put("jsonrpc", "2.0");
                response.set("id", id);
                response.putPOJO("result", result);

                if (activeEmitter != null) {
                    activeEmitter.send(SseEmitter.event().name("message").data(response.toString()));
                    log.info("Sent response for id: {}", id);
                }
            } else {
                log.info("Processed notification: {}, no response sent.", method);
            }

        } catch (McpException e) {
            // 捕获业务层明确抛出的错误 (比如：文件不存在，参数错误)
            log.warn("Business error: {}", e.getMessage());
            sendError(id, e.getCode(), e.getMessage());

        } catch (Exception e) {
            // 捕获未知的系统级错误 (比如：空指针，PDFBox崩溃)
            log.error("System error: {}", e.getMessage());
            sendError(id, -McpErrorCode.INTERNAL_ERROR.getCode(), "Internal error: " + e.getMessage());

        }
    }

    private void sendResponse(JsonNode id, Object result) {
        if (activeEmitter == null) {
            log.info("SSE link does not exist");
            return;
        }

        try {
            ObjectNode response = mapper.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.set("id", id);
            response.putPOJO("result", result);

            activeEmitter.send(SseEmitter.event().name("message").data(response.toString()));
            log.info("Sent result for id: {}", id);
        } catch (IOException e) {
            log.error("Failed to send response via SSE", e);
        }
    }

    private void sendError(JsonNode id, int code, String message) {
        if (activeEmitter == null) {
            log.info("SSE link does not exist");
            return;
        }

        try {
            ObjectNode response = mapper.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.set("id", id);

            ObjectNode errorNode = response.putObject("error");
            errorNode.put("code", code);
            errorNode.put("message", message);

            activeEmitter.send(SseEmitter.event().name("message").data(response.toString()));
            log.info("Sent error for id: {}, code: {}", id, code);
        } catch (IOException e) {
            log.error("Failed to send error via SSE", e);
        }
    }

    private Object processRequest(String method, JsonNode params) {

        switch (method) {
            // 握手阶段
            case "initialize":
                return handleInitialize(params);

            // client确认握手完成，无需返回
            case "notifications/initialized":
                return null;

            // 告诉client有什么工具
            case "tools/list":
                return handleListTools();

            // 调用工具
            case "tools/call":
                return handleCallTool(params);

            case "ping":
                return "pong";

            default:
                throw new McpException(McpErrorCode.METHOD_NOT_FOUND, "Method not found: " + method);
        }
    }

    private ObjectNode handleInitialize(JsonNode params) {
        log.info("Client initializing: {}", params.path("clientInfo").toString());

        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");

        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", "spring-boot-paper-sse-server");
        serverInfo.put("version", "1.0.0");

        ObjectNode capabilities = result.putObject("capabilities");
        // 声明支持 com.xialin.tools，且支持通知
        capabilities.putObject("tools");
        capabilities.putObject("logging"); // 如果你想支持远程日志

        return result;
    }

    private ObjectNode handleListTools() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode toolsArray = result.putArray("tools");

        for (McpTool tool : toolRegistry.values()) {
            ObjectNode toolNode = toolsArray.addObject();
            toolNode.put("name", tool.getName());
            toolNode.put("description", tool.getDescription());
            toolNode.set("inputSchema", tool.getInputSchema());
        }

        return result;
    }

    private Object handleCallTool(JsonNode params) {
        String toolName = params.get("name").asText();
        JsonNode arguments = params.get("arguments");

        // 查找
        McpTool tool = toolRegistry.get(toolName);
        if (tool == null) {
            throw new McpException(McpErrorCode.TOOL_NOT_FOUND, "Tool not found: " + toolName);
        }

        try {
            // 执行
            Object resultData = tool.execute(arguments);

            // 统一包装结果
            return wrapToolResult(resultData.toString());
        } catch (Exception e) {
            throw new McpException(McpErrorCode.TOOL_EXECUTION_FAILED, "Tool execution failed: " + e.getMessage());
        }
    }

    private ObjectNode wrapToolResult(String textContent) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode content = result.putArray("content");
        ObjectNode textNode = content.addObject();
        textNode.put("type", "text");
        textNode.put("text", textContent);
        return result;
    }
}