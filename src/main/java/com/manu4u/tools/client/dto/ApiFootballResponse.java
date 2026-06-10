package com.manu4u.tools.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFootballResponse<T> {
    private String get;
    private Map<String, String> parameters;
    private Object errors; // Can be List<String> or Map<String, String>
    private Integer results;
    private Paging paging;
    private List<T> response;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Paging {
        private Integer current;
        private Integer total;
    }

    public boolean hasErrors() {
        if (errors == null) {
            return false;
        }
        if (errors instanceof List) {
            return !((List<?>) errors).isEmpty();
        }
        if (errors instanceof Map) {
            return !((Map<?, ?>) errors).isEmpty();
        }
        return false;
    }

    public String getErrorMessage() {
        if (errors == null) {
            return null;
        }
        if (errors instanceof List) {
            List<?> errorList = (List<?>) errors;
            return errorList.isEmpty() ? null : errorList.toString();
        }
        if (errors instanceof Map) {
            Map<?, ?> errorMap = (Map<?, ?>) errors;
            return errorMap.isEmpty() ? null : errorMap.toString();
        }
        return errors.toString();
    }
}
