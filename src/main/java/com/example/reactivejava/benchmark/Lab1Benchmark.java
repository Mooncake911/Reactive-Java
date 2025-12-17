package com.example.reactivejava.benchmark;

import com.example.reactivejava.visualization.model.JmhResult;
import com.example.reactivejava.visualization.model.PrimaryMetric;
import domain.Device;
import domain.DeviceDelays;
import domain.DeviceGenerator;
import statistics.DeviceStatistics;
import statistics.model.DeviceStats;
import statistics.model.StatType;
import statistics.model.StatsConfig;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Lab1Benchmark {

    public static final int[] COLLECTION_SIZES = { 5_000, 50_000, 250_000 };

    public static final StatType[] TYPES_TO_TEST = {
            StatType.COUNT,
            StatType.ONLINE_COUNT,
            StatType.BATTERY,
            StatType.SIGNAL,
            StatType.HEARTBEAT,
            StatType.STATUS,
            StatType.COVERAGE_VOLUME,
            StatType.DEVICES_BY_TYPE,
            StatType.DEVICES_BY_MANUFACTURER,
            StatType.DEVICES_BY_CAPABILITIES,
    };

    private static final int WARMUP_ITERATIONS = 10;
    private static final int MEASURE_ITERATIONS = 20;

    private record BenchmarkScenario(
            String displayName,
            BiFunction<DeviceStatistics, List<Device>, DeviceStats> execution
    ) {}

    private static final List<BenchmarkScenario> SCENARIOS = List.of(
            new BenchmarkScenario("Sequential", DeviceStatistics::computeSequential),
            new BenchmarkScenario("Stream API (Std)", DeviceStatistics::computeWithStandardCollectors),
            new BenchmarkScenario("Stream API (Custom)", DeviceStatistics::computeWithCustomCollector)
    );

    public static List<JmhResult> runBenchmarks() {
        List<JmhResult> results = new ArrayList<>();
        DeviceGenerator generator = new DeviceGenerator(DeviceDelays.empty());

        for (int size : COLLECTION_SIZES) {
            List<Device> devices = generator.randomDevices(size);

            for (StatType type : TYPES_TO_TEST) {
                StatsConfig statsConfig = StatsConfig.withStats(type);
                DeviceStatistics statistics = new DeviceStatistics(statsConfig);

                // Цикл по сценариям
                for (BenchmarkScenario scenario : SCENARIOS) {
                    // Подготовка лямбды для конкретного сценария
                    java.util.function.Function<List<Device>, DeviceStats> methodToTest =
                            data -> scenario.execution.apply(statistics, data);

                    // 1. Изолированный прогрев (Warmup) конкретного метода
                    warmup(devices, methodToTest);

                    // 2. Сброс кэша/состояния генератора перед основным замером
                    generator.resetCache(devices);

                    // 3. Замер (Measure)
                    double avgTime = measure(devices, methodToTest);

                    results.add(createResult(type, size, scenario, avgTime));
                }
            }
        }
        return results;
    }

    private static double measure(List<Device> data, java.util.function.Function<List<Device>, DeviceStats> method) {
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        long totalTimeNs = 0;
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long start = System.nanoTime();
            DeviceStats result = method.apply(data);
            long end = System.nanoTime();

            if (result == null) throw new RuntimeException("Result shouldn't be null");
            totalTimeNs += (end - start);
        }
        return (double) totalTimeNs / MEASURE_ITERATIONS / 1_000_000.0;
    }

    private static void warmup(List<Device> data, java.util.function.Function<List<Device>, DeviceStats> method) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            method.apply(data);
        }
    }

    private static JmhResult createResult(StatType type, int size, BenchmarkScenario scenario, double score) {
        JmhResult result = new JmhResult();

        String safeName = scenario.displayName().replaceAll("\\s+", "_");
        result.benchmark = Lab1Benchmark.class.getName() + "." + safeName;

        result.params = new HashMap<>();
        result.params.put("statType", type.name());
        result.params.put("deviceCount", String.valueOf(size));
        result.params.put("displayName", scenario.displayName());

        result.primaryMetric = new PrimaryMetric();
        result.primaryMetric.score = score;
        result.primaryMetric.scoreUnit = "ms";

        return result;
    }

    public static void printSummary(List<JmhResult> results) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("BENCHMARK SUMMARY");
        System.out.println("=".repeat(80));

        Map<String, List<JmhResult>> byType = results.stream()
                .collect(Collectors.groupingBy(r -> r.getParam("statType")));

        for (StatType type : TYPES_TO_TEST) {
            String typeName = type.name();
            if (!byType.containsKey(typeName)) continue;

            System.out.println("\n>>> " + typeName);
            System.out.printf("%-10s | %-25s | %-15s%n", "Size", "Method", "Avg Time");
            System.out.println("-".repeat(60));

            List<JmhResult> typeResults = byType.get(typeName);

            typeResults.sort(
                    Comparator.comparingInt((JmhResult r) -> Integer.parseInt(r.getParam("deviceCount")))
                            .thenComparing(r -> r.getParam("displayName"))
            );

            for (JmhResult r : typeResults) {
                int size = Integer.parseInt(r.getParam("deviceCount"));
                String methodName = r.getParam("displayName");
                double score = r.primaryMetric.score;

                System.out.printf(Locale.US, "%,10d | %-25s | %10.4f ms%n", size, methodName, score);
            }
        }
        System.out.println("=".repeat(80));
    }
}