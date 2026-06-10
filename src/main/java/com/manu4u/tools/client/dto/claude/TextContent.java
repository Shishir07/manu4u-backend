package com.manu4u.tools.client.dto.claude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TextContent extends ContentBlock {
    private String text;

    public TextContent(String text) {
        this.text = text;
        super.setType("text");
    }
}
