package com.manu4u.tools.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manu4u.tools.eval.model.EvalTestCase;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class EvalDatasetLoader {

    private static final String DATASET_PATH = "eval/eval-dataset.json";

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EvalDataset {
        private String version;
        private List<EvalTestCase> testCases;
    }

    public static List<EvalTestCase> load() {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (InputStream is = EvalDatasetLoader.class.getClassLoader().getResourceAsStream(DATASET_PATH)) {
            if (is == null) {
                throw new IllegalStateException("Dataset not found on classpath: " + DATASET_PATH);
            }
            EvalDataset dataset = mapper.readValue(is, EvalDataset.class);
            System.out.println("Loaded eval dataset v" + dataset.getVersion() +
                    " with " + dataset.getTestCases().size() + " test cases");
            return dataset.getTestCases();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load eval dataset: " + e.getMessage(), e);
        }
    }
}
