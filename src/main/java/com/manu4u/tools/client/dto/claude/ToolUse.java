package com.manu4u.tools.client.dto.claude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolUse extends ContentBlock {
    private String id;
    private String name;
    private Map<String, Object> input;

    public ToolUse(String id, String name, Map<String, Object> input) {
        this.id = id;
        this.name = name;
        this.input = input;
        super.setType("tool_use");
    }
}
