package com.example.reactivejava.benchmark;

import org.openjdk.jmh.Main;

import java.io.File;
import java.util.logging.Logger;

public class DelayImpactRunner {
    private static final Logger logger = Logger.getLogger(DelayImpactRunner.class.getName());

    public static void main(String[] args) throws Exception {
        logger.info("Starting Device Statistics Delay Impact Benchmark...");

        String resultsFile = "data/benchmark_results/delay_impact.json";

        new File("data/benchmark_results").mkdirs();

        String[] jmhArgs = {
                "-rf", "json",
                "-rff", resultsFile,
                DelayImpactBenchmark.class.getName()
        };

        logger.info("Results will be saved to: " + resultsFile);

        Main.main(jmhArgs);

        logger.info("Delay Impact Benchmark completed!");
    }
}