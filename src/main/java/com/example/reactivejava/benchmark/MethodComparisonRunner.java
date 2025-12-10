package com.example.reactivejava.benchmark;

import org.openjdk.jmh.Main;

import java.io.File;
import java.util.logging.Logger;

public class MethodComparisonRunner {
    private static final Logger logger = Logger.getLogger(MethodComparisonRunner.class.getName());

    public static void main(String[] args) throws Exception {
        logger.info("Starting Device Statistics Method Comparison Benchmark...");

        String resultsFile = "data/benchmark_results/method_comparison.json";

        new File("data/benchmark_results").mkdirs();

        String[] jmhArgs = {
                "-rf", "json",
                "-rff", resultsFile,
                MethodComparisonBenchmark.class.getName()
        };

        logger.info("Results will be saved to: " + resultsFile);

        Main.main(jmhArgs);

        logger.info("Method Comparison Benchmark completed!");
    }
}