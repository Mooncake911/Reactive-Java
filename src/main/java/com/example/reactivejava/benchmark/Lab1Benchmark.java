package com.example.reactivejava.benchmark;

import domain.Device;
import domain.DeviceDelays;
import domain.DeviceGenerator;
import statistics.DeviceStatistics;
import statistics.model.DeviceStats;
import statistics.model.StatType;
import statistics.model.StatsConfig;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Lab1Benchmark {

    private static final int[] COLLECTION_SIZES = {5000, 50000, 250000};

    private static final StatType[] TYPES_TO_TEST = {
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

    private static final int WARMUP_ITERATIONS = 20;
    private static final int MEASURE_ITERATIONS = 20;

    private static final String CHARTS_DIR = "data/visualization_results/Lab1Benchmark";

    public static void main(String[] args) {
        new File(CHARTS_DIR).mkdirs();

        DeviceDelays deviceDelays = DeviceDelays.empty();
        DeviceGenerator generator = new DeviceGenerator(deviceDelays);

        for (StatType type : TYPES_TO_TEST) {
            runBenchmarkForType(type, generator);
            System.out.println("\n" + "=".repeat(80) + "\n");
        }

        System.out.println("Графики сохранены в папку: " + new File(CHARTS_DIR).getAbsolutePath());
    }

    private static void runBenchmarkForType(StatType type, DeviceGenerator generator) {
        StatsConfig statsConfig = StatsConfig.withStats(type);
        DeviceStatistics statistics = new DeviceStatistics(statsConfig);

        System.out.println(">>> ТЕСТИРОВАНИЕ ТИПА: " + type);
        System.out.printf("%-10s | %-25s | %-15s%n", "Размер", "Метод", "Среднее время");
        System.out.println("-".repeat(60));

        Map<Integer, DefaultCategoryDataset> datasetsBySize = new HashMap<>();

        for (int size : COLLECTION_SIZES) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            List<Device> devices = generator.randomDevices(size);

            warmup(devices, statistics);

            // 1. Sequential
            generator.resetCache(devices);
            double timeSequential = measureAndPrint(size, "1. Sequential", devices, statistics::computeSequential);
            dataset.addValue(timeSequential, "Time", "Sequential");

            // 2. Stream Std
            generator.resetCache(devices);
            double timeStd = measureAndPrint(size, "2. Stream API (Std)", devices, statistics::computeWithStandardCollectors);
            dataset.addValue(timeStd, "Time", "Stream (Std)");

            // 3. Stream Custom
            generator.resetCache(devices);
            double timeCustom = measureAndPrint(size, "3. Stream API (Custom)", devices, statistics::computeWithCustomCollector);
            dataset.addValue(timeCustom, "Time", "Stream (Custom)");

            datasetsBySize.put(size, dataset);
            System.out.println("-".repeat(60));
        }

        saveCompositeChart(type, datasetsBySize);
    }

    private static double measureAndPrint(int size, String methodName, List<Device> data, Function<List<Device>, DeviceStats> method) {
        System.gc();
        try {
            Thread.sleep(100); // Даем время GC успокоиться
        } catch (InterruptedException ignored) {}

        long totalTimeNs = 0;

        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            long start = System.nanoTime();
            DeviceStats result = method.apply(data);
            long end = System.nanoTime();

            totalTimeNs += (end - start);

            if (result == null) throw new RuntimeException("Result shouldn't be null");
        }

        double averageTimeMs = (double) totalTimeNs / MEASURE_ITERATIONS / 1_000_000.0;
        System.out.printf("%,10d | %-25s | %10.4f ms%n", size, methodName, averageTimeMs);

        return averageTimeMs;
    }

    private static void warmup(List<Device> data, DeviceStatistics statistics) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            statistics.computeSequential(data);
            statistics.computeWithStandardCollectors(data);
            statistics.computeWithCustomCollector(data);
        }
    }

    private static void saveCompositeChart(StatType type, Map<Integer, DefaultCategoryDataset> datasets) {
        try {
            List<BufferedImage> charts = new ArrayList<>();
            int totalWidth = 0;
            int maxHeight = 0;

            for (int size : COLLECTION_SIZES) {
                if (datasets.containsKey(size)) {
                    BufferedImage chartImage = createChartImage(size, datasets.get(size));
                    charts.add(chartImage);

                    totalWidth += chartImage.getWidth();
                    maxHeight = Math.max(maxHeight, chartImage.getHeight());
                }
            }

            int headerHeight = 50;
            int totalHeight = maxHeight + headerHeight;

            BufferedImage combined = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = combined.createGraphics();

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, totalWidth, totalHeight);

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            String title = "Benchmark Result: " + type.name();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, (totalWidth - fm.stringWidth(title)) / 2, 35);

            int currentX = 0;
            for (BufferedImage chart : charts) {
                g.drawImage(chart, currentX, headerHeight, null);
                currentX += chart.getWidth();
            }

            g.dispose();

            File outputFile = new File(CHARTS_DIR, "Bench_" + type.name() + ".png");
            ImageIO.write(combined, "PNG", outputFile);

        } catch (IOException e) {
            System.err.println("Ошибка при сохранении графика: " + e.getMessage());
        }
    }

    private static BufferedImage createChartImage(int size, DefaultCategoryDataset dataset) {
        String title = String.format("Size: %,d", size);

        JFreeChart chart = ChartFactory.createBarChart(
                title,
                "",
                "Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        NumberAxis rangeAxis = getNumberAxis(chart);
        rangeAxis.setAutoRangeIncludesZero(true);

        return chart.createBufferedImage(400, 300);
    }

    private static NumberAxis getNumberAxis(JFreeChart chart) {
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinePaint(Color.lightGray);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(true);

        renderer.setSeriesPaint(0, new Color(79, 129, 189));
        renderer.setSeriesPaint(1, new Color(192, 80, 77));
        renderer.setSeriesPaint(2, new Color(155, 187, 89));

        return (NumberAxis) plot.getRangeAxis();
    }
}