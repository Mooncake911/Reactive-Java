package com.example.reactivejava;

import domain.Device;
import statistics.model.DeviceStats;

import domain.DeviceDelays;
import domain.DeviceGenerator;

import io.reactivex.rxjava3.core.Flowable;
import statistics.DeviceStatistics;
import statistics.model.StatType;
import statistics.model.StatsConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        Instant start;
        Instant end;

        int[] sizes = { 5000, 50000, 250000 };
        int batchSize = 1024;

        DeviceDelays deviceDelays = DeviceDelays.builder()
                .statusDelayMs(0)
                .build();
        DeviceGenerator generator = new DeviceGenerator(deviceDelays);

        StatsConfig statsConfig = StatsConfig.withStats(StatType.STATUS);
        DeviceStatistics deviceStats = new DeviceStatistics(statsConfig);
        System.out.println(statsConfig.getEnabledStats());

        for (int size : sizes) {
            List<Device> devices = generator.randomDevices(size);

            System.out.printf("\nКоличество устройств: %d\n", size);
            System.out.println("==============================");

            // ---- Loop ----
            generator.resetCache(devices);
            start = Instant.now();
            DeviceStats sequentialStats = deviceStats.computeSequential(devices);
            end = Instant.now();
            System.out.printf("Sequential: %d ms -> %s%n", Duration.between(start, end).toMillis(), sequentialStats);

            // ---- Stream API ----
            generator.resetCache(devices);
            start = Instant.now();
            DeviceStats standardCollectorsStats = deviceStats.computeWithStandardCollectors(devices);
            end = Instant.now();
            System.out.printf("Standard Collectors: %d ms -> %s%n", Duration.between(start, end).toMillis(),
                    standardCollectorsStats);

            generator.resetCache(devices);
            start = Instant.now();
            DeviceStats standardCollectorsParallelStats = deviceStats.computeWithStandardCollectorsParallel(devices);
            end = Instant.now();
            System.out.printf("Standard Collectors Parallel: %d ms -> %s%n", Duration.between(start, end).toMillis(),
                    standardCollectorsParallelStats);

            generator.resetCache(devices);
            start = Instant.now();
            DeviceStats standardCollectorsParallelStatsWithSpliterator = deviceStats
                    .computeWithStandardCollectorsParallel(devices, batchSize);
            end = Instant.now();
            System.out.printf("Standard Collectors Parallel (Custom Spliterator, batch=%d): %d ms -> %s%n", batchSize,
                    Duration.between(start, end).toMillis(), standardCollectorsParallelStatsWithSpliterator);

            // ---- Custom Collector ----
            generator.resetCache(devices);
            start = Instant.now();
            DeviceStats customCollectorStats = deviceStats.computeWithCustomCollector(devices);
            end = Instant.now();
            System.out.printf("Custom Collector: %d ms -> %s%n", Duration.between(start, end).toMillis(),
                    customCollectorStats);

            generator.resetCache(devices);
            start = Instant.now();
            DeviceStats customCollectorParallelStats = deviceStats.computeWithCustomCollectorParallel(devices);
            end = Instant.now();
            System.out.printf("Custom Collector Parallel: %d ms -> %s%n", Duration.between(start, end).toMillis(),
                    customCollectorParallelStats);

            generator.resetCache(devices);
            start = Instant.now();
            DeviceStats customCollectorParallelStatsWithSpliterator = deviceStats
                    .computeWithCustomCollectorParallel(devices, batchSize);
            end = Instant.now();
            System.out.printf("Custom Collector Parallel (Custom Spliterator, batch=%d): %d ms -> %s%n", batchSize,
                    Duration.between(start, end).toMillis(), customCollectorParallelStatsWithSpliterator);

            // ---- RxJava ----
            generator.resetCache(devices);
            start = Instant.now();
            DeviceStats rxJavaObservableStats = deviceStats.computeObservableSync(devices, batchSize);
            end = Instant.now();
            System.out.printf("RxJava Observable: %d ms -> %s%n", Duration.between(start, end).toMillis(),
                    rxJavaObservableStats);

            generator.resetCache(devices);
            start = Instant.now();
            Flowable<Device> deviceFlow1 = DeviceStatistics.toFlowable(devices);
            DeviceStats rxJavaFlowableStats = deviceStats.computeFlowableSync(deviceFlow1, batchSize);
            end = Instant.now();
            System.out.printf("RxJava Flowable: %d ms -> %s%n", Duration.between(start, end).toMillis(),
                    rxJavaFlowableStats);

            generator.resetCache(devices);
            start = Instant.now();
            Flowable<Device> deviceFlow2 = DeviceStatistics.toFlowable(devices);
            int parallelism = Runtime.getRuntime().availableProcessors();
            DeviceStats rxJavaFlowableParallelStats = deviceStats.computeFlowableParallelSync(deviceFlow2, batchSize,
                    parallelism);
            end = Instant.now();
            System.out.printf("RxJava Flowable Parallel: %d ms -> %s%n", Duration.between(start, end).toMillis(),
                    rxJavaFlowableParallelStats);

            generator.resetCache(devices);
            start = Instant.now();
            Flowable<Device> deviceFlow3 = DeviceStatistics.toAsyncFlowable(devices);
            DeviceStats rxJavaCustomSubscriberStats = deviceStats.computeWithCustomSubscriber(deviceFlow3, batchSize);
            end = Instant.now();
            System.out.printf("RxJava Custom Subscriber: %d ms -> %s%n", Duration.between(start, end).toMillis(),
                    rxJavaCustomSubscriberStats);

            System.out.println("==============================");
        }
    }
}
