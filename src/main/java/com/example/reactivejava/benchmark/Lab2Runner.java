package com.example.reactivejava.benchmark;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.util.logging.Logger;

public class Lab2Runner {
    private static final Logger logger = Logger.getLogger(Lab2Runner.class.getName());

    public static void main(String[] args) throws Exception {
        logger.info("Starting Device Statistics Lab2 Benchmark (Full Suite)...");

        String resultsFile = "data/benchmark_results/lab2_benchmark.json";
        new File("data/benchmark_results").mkdirs();

        Options opt = new OptionsBuilder()
                .include(Lab2BenchmarkNoDelay.class.getSimpleName())
                .include(Lab2BenchmarkDelayed.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .result(resultsFile)
                .build();

        logger.info("Running benchmarks: NoDelay and Delayed");
        new Runner(opt).run();

        logger.info("All benchmarks completed! Results saved to: " + resultsFile);
    }
}