package com.example.reactivejava.benchmark;

import domain.Device;
import domain.DeviceDelays;
import domain.DeviceGenerator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import statistics.DeviceStatistics;
import statistics.model.StatType;
import statistics.model.StatsConfig;

import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@State(Scope.Benchmark)
public class BatchSizeOptimizationBenchmark {

    @Param({"5000", "50000", "250000"})
    private int deviceCount;

    @Param({"512", "1024", "2048", "4096"})
    private int batchSize;

    @Param({"STATUS", "DEVICES_BY_TYPE"})
    private String statType;

    private List<Device> devices;
    private DeviceGenerator generator;
    private DeviceStatistics statistics;

    @Setup(Level.Trial)
    public void setup() {
        generator = new DeviceGenerator(DeviceDelays.empty());
        devices = generator.randomDevices(deviceCount);
        StatsConfig config = StatsConfig.withStats(
                StatType.valueOf(statType)
        );
        statistics = new DeviceStatistics(config);
    }

    @Setup(Level.Invocation)
    public void resetCache() {
        generator.resetCache(devices);
    }

    @Benchmark
    public void standardCollectorsParallel(Blackhole bh) {
        bh.consume(statistics.computeWithStandardCollectorsParallel(devices, batchSize));
    }

    @Benchmark
    public void customCollectorParallel(Blackhole bh) {
        bh.consume(statistics.computeWithCustomCollectorParallel(devices, batchSize));
    }
}