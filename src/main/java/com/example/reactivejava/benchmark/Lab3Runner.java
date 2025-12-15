package com.example.reactivejava.benchmark;

import org.openjdk.jmh.Main;

import java.io.File;
import java.util.logging.Logger;

public class Lab3Runner {
    private static final Logger logger = Logger.getLogger(Lab3Runner.class.getName());

    public static void main(String[] args) throws Exception {
        logger.info("Starting Device Statistics Lab3 Benchmark...");

        String resultsFile = "data/benchmark_results/lab3_benchmark.json";

        new File("data/benchmark_results").mkdirs();

        String[] jmhArgs = {
                "-rf", "json",
                "-rff", resultsFile,
                Lab3Benchmark.class.getName()
        };

        logger.info("Results will be saved to: " + resultsFile);

        Main.main(jmhArgs);

        logger.info("Lab3 Benchmark completed!");
    }
}