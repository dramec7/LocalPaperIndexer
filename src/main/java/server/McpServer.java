package server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import service.PaperService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class McpServer {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static PaperService paperService;

    public static void main(String[] args) {

        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(java.util.logging.Level.OFF);
        paperService = new PaperService();

        System.err.println("MCP Server started...");
        System.err.flush();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    handleMessage(line);
                } catch (Exception e) {
                    System.err.println("Error handling message: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleMessage(String line) throws Exception {
        JsonNode request = mapper.readTree(line);
        System.err.println("Received request: " + request.toString());

        // JSON-RPC 的关键字段
        JsonNode idNode = request.get("id");
        String method = request.get("method").asText();

        // 如果没有 ID，通常是通知（Notification），我们可以忽略或仅记录
//        Object id = idNode != null ? idNode.asInt() : null;
        JsonNode id = idNode;

        System.err.println("Received method: " + method); // Debug log

        // 2. 路由分发
        switch (method) {
            case "initialize":
                handleInitialize(id);
                break;
            case "tools/list":
                handleListTools(id);
                break;
            case "tools/call":
                handleCallTool(id, request.get("params"));
                break;
            case "notifications/initialized":
                // 客户端确认初始化完成，无需响应
                break;
//            case "ping":
//                sendResult(id, "pong");
//                break;
            default:
                // 未知方法，生产环境应该返回 Method Not Found 错误
                System.err.println("Unknown method: " + method);
        }
    }

    /**
     * Step 1：握手
     * 客户端发送 initialize，服务器返回自己的名字和版本
     */
    private static void handleInitialize(JsonNode id) {
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05"); // MCP 协议版本

        ObjectNode serverInfo = result.putObject("server");
        serverInfo.put("name", "java-local-paper-indexer-server");
        serverInfo.put("version", "1.0.0");

        ObjectNode capabilities = result.putObject("capabilities");
        capabilities.putObject("tools"); // 声明我有 Tool 能力

        sendResponse(id, result);
    }

    /**
     * Step 2：告诉客户端我有哪些工具
     */
    private static void handleListTools(JsonNode id) {
        ObjectNode result = mapper.createObjectNode();
        var toolsArray = result.putArray("tools");

        // --- 定义 read_paper 工具 ---
        ObjectNode readTool = toolsArray.addObject();
        readTool.put("name", "read_paper");
        readTool.put("description", "读取指定 PDF 文件的全部文本内容");

        // 定义参数 Schema (JSON Schema draft 7)
        ObjectNode inputSchema = readTool.putObject("inputSchema");
        inputSchema.put("type", "object");
        ObjectNode properties = inputSchema.putObject("properties");
        properties.putObject("filePath")
                .put("type", "string")
                .put("description", "PDF 文件的绝对路径");
        inputSchema.putArray("required").add("filePath");

        sendResponse(id, result);
    }

    /**
     * 第三步：执行工具调用
     */
    private static void handleCallTool(JsonNode id, JsonNode params) {
        String name = params.get("name").asText();
        JsonNode arguments = params.get("arguments");

        if ("read_paper".equals(name)) {
            try {
                String filePath = arguments.get("filePath").asText();

                // 调用你的业务逻辑
                String content = paperService.readPdfContent(filePath);

                // 构建成功响应
                ObjectNode result = mapper.createObjectNode();
                var contentArray = result.putArray("content");
                ObjectNode textNode = contentArray.addObject();
                textNode.put("type", "text");
                textNode.put("text", content);

                sendResponse(id, result);
            } catch (Exception e) {
                // 这里应该捕获你的 McpException 并返回 JSON-RPC Error
                // 为了演示简单，这里先返回一个错误文本
                sendError(id, -32000, e.getMessage());
            }
        } else {
            sendError(id, -32601, "Tool not found: " + name);
        }
    }

    // === 4. 辅助发送方法 ===


    private static void sendResponse(JsonNode id, Object result) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        // 直接把原来的 ID 节点塞回去，不管它是 Int 还是 String
        if (id != null) {
            response.set("id", id);
        } else {
            response.putNull("id");
        }
        response.putPOJO("result", result);
        printJson(response);
    }

    private static void sendError(JsonNode id, int code, String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.set("id", id);
        } else {
            response.putPOJO("id", id);
        }

        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);

        printJson(response);
    }

    private static void printJson(ObjectNode node) {
        try {
            // 1. 先把要发的 JSON 转换成字符串
            String json = mapper.writeValueAsString(node);

            // 2. [调试关键] 先偷偷告诉 Inspector 我们打算发什么 (通过 stderr)
            // 这样我们就能在 Notification 里看到生成的 JSON 对不对
            System.err.println("DEBUG: Trying to send JSON: " + json);
            System.err.flush();

            // 3. 正式发送到 stdout
            System.out.write((json + "\n").getBytes("UTF-8"));
            System.out.flush(); // 再次确保刷新

            // 4. [调试关键] 告诉我们发送动作完成了
            System.err.println("DEBUG: Sent successfully.");
            System.err.flush();

        } catch (Throwable e) { // 改成 Throwable 以捕获 NoClassDefFoundError
            // 5. 如果报错，必须把错误信息吼出来
            System.err.println("CRITICAL ERROR inside printJson:");
            e.printStackTrace(); // 这会打印到 stderr
            System.err.flush();
        }
    }
}
