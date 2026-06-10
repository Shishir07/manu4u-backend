package com.manu4u.tools.eval.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;

public class EvalReportWriter {

    private static final String OUTPUT_PATH = "target/eval-report.json";

    public static void write(EvalReport report) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try {
            File outputFile = new File(OUTPUT_PATH);
            outputFile.getParentFile().mkdirs();
            mapper.writeValue(outputFile, report);
            System.out.println("Eval report written to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write eval report: " + e.getMessage());
        }
    }
}
