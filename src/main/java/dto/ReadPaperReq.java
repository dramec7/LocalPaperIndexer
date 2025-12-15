package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ReadPaperReq(
        @JsonProperty(required = true)
        @JsonPropertyDescription("PDF 文件的绝对路径，例如 /Users/admin/paper.pdf")
        String filePath,

        @JsonPropertyDescription("是否只返回摘要。如果为 true，只读取前 2000 字符。")
        boolean summaryOnly
) {}
