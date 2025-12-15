package tools;

import com.fasterxml.jackson.databind.JsonNode;

public interface McpTool {
    // 工具名称
    String getName();

    // 工具描述
    String getDescription();

    // 参数定义的 Schema
    JsonNode getInputSchema();

    // 具体的执行逻辑
    Object execute(JsonNode arguments);
}