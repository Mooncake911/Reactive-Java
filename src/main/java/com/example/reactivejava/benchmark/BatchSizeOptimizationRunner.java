package com.example.reactivejava.benchmark;

import org.openjdk.jmh.Main;

import java.io.File;
import java.util.logging.Logger;

public class BatchSizeOptimizationRunner {
    private static final Logger logger = Logger.getLogger(BatchSizeOptimizationRunner.class.getName());

    public static void main(String[] args) throws Exception {
        logger.info("Starting Device Statistics Batch Size Optimization Benchmark...");

        String resultsFile = "data/benchmark_results/batch_size_optimization.json";

        new File("data/benchmark_results").mkdirs();

        String[] jmhArgs = {
                "-rf", "json",
                "-rff", resultsFile,
                BatchSizeOptimizationBenchmark.class.getName()
        };

        logger.info("Results will be saved to: " + resultsFile);

        Main.main(jmhArgs);

        logger.info("Batch Size Optimization Benchmark completed!");
    }
}