package com.manu4u.tools.model.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {
    @NotBlank(message = "Question cannot be empty")
    private String question;
    
    private String sessionId; // Optional, for future conversation memory
}
