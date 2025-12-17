package com.example.reactivejava.benchmark;

import com.example.reactivejava.visualization.model.JmhResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Lab1Runner {
    private static final Logger logger = Logger.getLogger(Lab1Runner.class.getName());

    public static void main(String[] args) {
        logger.info("Starting Device Statistics Lab1 Benchmark (Runner)...");

        try {
            List<JmhResult> results = Lab1Benchmark.runBenchmarks();
            Lab1Benchmark.printSummary(results);
            saveResults(results);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Benchmark failed unexpectedly", e);
        }
    }

    private static void saveResults(List<JmhResult> results) {
        Path dirPath = Path.of("data", "benchmark_results");
        Path filePath = dirPath.resolve("lab1_benchmark.json");

        try {
            Files.createDirectories(dirPath);

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(filePath.toFile(), results);

            logger.info("All benchmarks completed! Results saved to: " + filePath.toAbsolutePath());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save benchmark results to JSON", e);
        }
    }
}