package com.manu4u.tools.client.dto.claude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String role; // "user" or "assistant"
    private Object content; // Can be String or List<ContentBlock>
}
