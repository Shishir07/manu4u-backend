package com.manu4u.tools.client.dto.claude;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextContent.class, name = "text"),
    @JsonSubTypes.Type(value = ToolUse.class, name = "tool_use"),
    @JsonSubTypes.Type(value = ToolResult.class, name = "tool_result")
})
public abstract class ContentBlock {
    private String type;
}
