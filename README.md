## 🚀 Java Spring Boot MCP Server (SSE Edition)

这是一个标准的 MCP 服务端实现，旨在让 LLM 能够通过标准 HTTP 协议访问本地资源。本项目目前专注于 PDF 文档处理，赋予 AI 读取和索引本地论文的能力。

## 🛠 技术栈

语言: Java 17+

框架: Spring Boot 3.3.x (Web MVC)

工具库: Jackson (JSON), Apache PDFBox (PDF)

辅助库: Victools (JSON Schema Generator)

构建工具: Maven

## 🔌 客户端配置
支持所有兼容 MCP 协议的客户端。

**VS Code Cline / Claude Desktop 配置**

编辑你的 MCP 配置文件：

```JSON
{
  "mcpServers": {
    "java-paper-agent": {
      "url": "http://localhost:8080/sse",
      "transport": "sse"
    }
  }
}
```

## 📜 许可证
MIT License
