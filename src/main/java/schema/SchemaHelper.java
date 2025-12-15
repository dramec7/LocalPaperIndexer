package schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import org.springframework.stereotype.Component;

@Component
public class SchemaHelper {

    private final SchemaGenerator generator;

    public SchemaHelper() {
        // 初始化配置构建器
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2019_09,
                OptionPreset.PLAIN_JSON
        );

        // 启用 Jackson 模块，否则victools 默认的生成器会忽略你的 @JsonPropertyDescription 注解
        configBuilder.with(new JacksonModule());


        // 构建生成器实例
        this.generator = new SchemaGenerator(configBuilder.build());
    }

    // 给一个Class 输出JsonNode 即schema
    public JsonNode generate(Class<?> clazz) {
        return generator.generateSchema(clazz);
    }
}
