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
@Fork(value = 1, jvmArgs = { "-Xms2G", "-Xmx2G" })
@State(Scope.Benchmark)
public class Lab2BenchmarkNoDelay {

    @Param({ "1000", "2500", "5000", "10000" })
    private int deviceCount;

    @Param({ "0" })
    private int delayMs;

    @Param({ "STATUS", "DEVICES_BY_TYPE" })
    private String statType;

    private List<Device> devices;
    private DeviceGenerator generator;
    private DeviceStatistics statistics;

    @Setup(Level.Trial)
    public void setup() {
        DeviceDelays delays = DeviceDelays.empty();
        generator = new DeviceGenerator(delays);
        devices = generator.randomDevices(deviceCount);
        statistics = new DeviceStatistics(StatsConfig.withStats(StatType.valueOf(statType)));
    }

    @Setup(Level.Invocation)
    public void resetCache() {
        generator.resetCache(devices);
    }

    @Benchmark
    public void standardCollectors(Blackhole bh) {
        bh.consume(statistics.computeWithStandardCollectors(devices));
    }

    @Benchmark
    public void customCollector(Blackhole bh) {
        bh.consume(statistics.computeWithCustomCollector(devices));
    }

    @Benchmark
    public void standardCollectorsParallel(Blackhole bh) {
        bh.consume(statistics.computeWithStandardCollectorsParallel(devices));
    }

    @Benchmark
    public void customCollectorParallel(Blackhole bh) {
        bh.consume(statistics.computeWithCustomCollectorParallel(devices));
    }
}
