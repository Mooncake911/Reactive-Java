package com.example.reactivejava.benchmark;

import domain.Device;
import domain.DeviceDelays;
import domain.DeviceGenerator;
import io.reactivex.rxjava3.core.Flowable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import statistics.DeviceStatistics;
import statistics.model.DeviceStats;
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
public class DelayImpactBenchmark {

    @Param({ "5000", "50000", "250000" })
    private int deviceCount;

    @Param({ "0", "1", "2" })
    private int delayMs;

    @Param({ "STATUS", "DEVICES_BY_TYPE" })
    private String statType;

    @Param({ "1024" })
    private int batchSize;

    private List<Device> devices;
    private DeviceGenerator generator;
    private DeviceStatistics statistics;

    @Setup(Level.Trial)
    public void setup() {
        DeviceDelays delays = delayMs > 0
                ? DeviceDelays.builder()
                        .idDelayMs(delayMs)
                        .nameDelayMs(delayMs)
                        .manufacturerDelayMs(delayMs)
                        .typeDelayMs(delayMs)
                        .locationDelayMs(delayMs)
                        .capabilitiesDelayMs(delayMs)
                        .statusDelayMs(delayMs)
                        .build()
                : DeviceDelays.empty();

        generator = new DeviceGenerator(delays);
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

//    @Benchmark
//    public void sequential(Blackhole bh) {
//        bh.consume(statistics.computeSequential(devices));
//    }
//
//    @Benchmark
//    public void standardCollectorsParallel(Blackhole bh) {
//        bh.consume(statistics.computeWithStandardCollectorsParallel(devices));
//    }
//
//    @Benchmark
//    public void customCollectorParallel(Blackhole bh) {
//        bh.consume(statistics.computeWithCustomCollectorParallel(devices));
//    }

    @Benchmark
    public void rxJavaObservable(Blackhole bh) {
        DeviceStats result = statistics.computeObservableSync(devices, batchSize);
        bh.consume(result);
    }

    @Benchmark
    public void rxJavaFlowable(Blackhole bh) {
        Flowable<Device> deviceFlow = DeviceStatistics.toFlowable(devices);
        DeviceStats result = statistics.computeFlowableSync(deviceFlow, batchSize);
        bh.consume(result);
    }

    @Benchmark
    public void rxJavaFlowableParallel(Blackhole bh) {
        Flowable<Device> deviceFlow = DeviceStatistics.toFlowable(devices);
        DeviceStats result = statistics.computeFlowableParallelSync(deviceFlow, batchSize, Runtime.getRuntime().availableProcessors());
        bh.consume(result);
    }
}