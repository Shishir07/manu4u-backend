package com.manu4u.tools.client.dto.claude;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolResult extends ContentBlock {
    @JsonProperty("tool_use_id")
    private String toolUseId;
    private Object content;
    @JsonProperty("is_error")
    private Boolean isError = false;

    public ToolResult(String toolUseId, Object content, Boolean isError) {
        this.toolUseId = toolUseId;
        this.content = content;
        this.isError = isError != null ? isError : false;
        super.setType("tool_result");
    }
}
