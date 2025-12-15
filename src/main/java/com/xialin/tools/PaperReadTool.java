package com.xialin.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xialin.dto.ReadPaperReq;
import org.springframework.beans.factory.annotation.Autowired;
import com.xialin.schema.SchemaHelper;
import com.xialin.service.PaperService;
import org.springframework.stereotype.Component;

@Component
public class PaperReadTool implements McpTool {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PaperService paperService;

    @Autowired
    private SchemaHelper schemaHelper;

    @Override
    public String getName() {
        return "read_paper";
    }

    @Override
    public String getDescription() {
        return "用于读取本地 PDF 文件的纯文本内容。适用于论文分析、文档总结等场景。";
    }

    @Override
    public JsonNode getInputSchema() {
        return schemaHelper.generate(ReadPaperReq.class);
    }

    @Override
    public Object execute(JsonNode arguments) {
        ReadPaperReq req = mapper.convertValue(arguments, ReadPaperReq.class);
        return paperService.readPdfContent(req.filePath());
    }


}
