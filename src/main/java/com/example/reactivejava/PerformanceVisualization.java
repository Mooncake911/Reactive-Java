package com.example.reactivejava;

import domain.Device;
import domain.DeviceDelays;
import domain.DeviceGenerator;
import statistics.DeviceStatistics;
import statistics.model.DeviceStats;
import statistics.model.StatType;
import statistics.model.StatsConfig;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Визуализация производительности трёх методов вычисления:
 * - computeSequential
 * - computeWithStandardCollectors
 * - computeWithCustomCollector
 *
 * Измеряет время выполнения для размеров коллекции: 5000, 50000, 250000
 * Содержит фазу прогрева JVM и выполняет множественные прогоны для точности
 */
public class PerformanceVisualization {

    // Модель для хранения результатов измерений
    static class PerformanceResult {
        String methodName;
        int dataSize;
        long executionTimeMs;

        PerformanceResult(String methodName, int dataSize, long executionTimeMs) {
            this.methodName = methodName;
            this.dataSize = dataSize;
            this.executionTimeMs = executionTimeMs;
        }
    }

    public static void main(String[] args) throws IOException {
        int[] sizes = { 5000, 50000, 250000 };
        List<PerformanceResult> results = new ArrayList<>();

        DeviceDelays deviceDelays = DeviceDelays.builder()
                .statusDelayMs(0)
                .build();
        DeviceGenerator generator = new DeviceGenerator(deviceDelays);

        StatsConfig statsConfig = StatsConfig.withStats(StatType.COVERAGE_VOLUME);
        DeviceStatistics deviceStats = new DeviceStatistics(statsConfig);

        System.out.println("=== Измерение производительности ===\n");

        // --- ФАЗА ПРОГРЕВА (WARM-UP) ---
        System.out.println("Фаза прогрева JVM...");
        System.out.println("Выполняем каждый метод 5 раз на 10000 элементах...");

        List<Device> warmUpDevices = generator.randomDevices(10000);

        // Прогоняем каждый метод несколько раз для прогрева JIT-компилятора
        for (int i = 0; i < 5; i++) {
            generator.resetCache(warmUpDevices);
            deviceStats.computeSequential(warmUpDevices);

            generator.resetCache(warmUpDevices);
            deviceStats.computeWithStandardCollectors(warmUpDevices);

            generator.resetCache(warmUpDevices);
            deviceStats.computeWithCustomCollector(warmUpDevices);

            if ((i + 1) % 2 == 0) {
                System.out.println("  Прогрев: выполнено " + (i + 1) + " / 5 итераций");
            }
        }
        System.out.println("Прогрев завершен.\n");

        // --- ОСНОВНЫЕ ИЗМЕРЕНИЯ ---
        for (int size : sizes) {
            List<Device> devices = generator.randomDevices(size);
            System.out.printf("=== Количество устройств: %d ===%n", size);

            // Для каждого размера выполняем несколько прогонов и берем среднее
            long[] sequentialTimes = new long[5];
            long[] standardTimes = new long[5];
            long[] customTimes = new long[5];

            for (int run = 0; run < 5; run++) {
                System.out.printf("  Прогон %d:%n", run + 1);

                // Меняем порядок выполнения методов для каждого прогона
                // чтобы избежать преимуществ от кэширования

                switch (run % 3) {
                    case 0:
                        // Порядок: Sequential -> Standard -> Custom
                        sequentialTimes[run] = measureMethod("Sequential",
                                () -> deviceStats.computeSequential(devices), generator, devices);
                        standardTimes[run] = measureMethod("Standard Collectors",
                                () -> deviceStats.computeWithStandardCollectors(devices), generator, devices);
                        customTimes[run] = measureMethod("Custom Collector",
                                () -> deviceStats.computeWithCustomCollector(devices), generator, devices);
                        break;

                    case 1:
                        // Порядок: Standard -> Custom -> Sequential
                        standardTimes[run] = measureMethod("Standard Collectors",
                                () -> deviceStats.computeWithStandardCollectors(devices), generator, devices);
                        customTimes[run] = measureMethod("Custom Collector",
                                () -> deviceStats.computeWithCustomCollector(devices), generator, devices);
                        sequentialTimes[run] = measureMethod("Sequential",
                                () -> deviceStats.computeSequential(devices), generator, devices);
                        break;

                    case 2:
                        // Порядок: Custom -> Sequential -> Standard
                        customTimes[run] = measureMethod("Custom Collector",
                                () -> deviceStats.computeWithCustomCollector(devices), generator, devices);
                        sequentialTimes[run] = measureMethod("Sequential",
                                () -> deviceStats.computeSequential(devices), generator, devices);
                        standardTimes[run] = measureMethod("Standard Collectors",
                                () -> deviceStats.computeWithStandardCollectors(devices), generator, devices);
                        break;
                }

                System.out.println();
            }

            // Вычисляем среднее время для каждого метода (исключая первый прогон как возможный выброс)
            long avgSequential = calculateAverage(sequentialTimes, 1);
            long avgStandard = calculateAverage(standardTimes, 1);
            long avgCustom = calculateAverage(customTimes, 1);

            // Добавляем средние результаты
            results.add(new PerformanceResult("Sequential", size, avgSequential));
            results.add(new PerformanceResult("Standard Collectors", size, avgStandard));
            results.add(new PerformanceResult("Custom Collector", size, avgCustom));

            System.out.printf("  Средние результаты (последние 4 прогона):%n");
            System.out.printf("    Sequential: %d ms%n", avgSequential);
            System.out.printf("    Standard Collectors: %d ms%n", avgStandard);
            System.out.printf("    Custom Collector: %d ms%n", avgCustom);

            // Выводим все измерения для прозрачности
            System.out.printf("    Все измерения Sequential: ");
            for (int i = 0; i < sequentialTimes.length; i++) {
                System.out.printf("%d%s", sequentialTimes[i], i < sequentialTimes.length - 1 ? ", " : "");
            }
            System.out.println();

            System.out.printf("    Все измерения Standard: ");
            for (int i = 0; i < standardTimes.length; i++) {
                System.out.printf("%d%s", standardTimes[i], i < standardTimes.length - 1 ? ", " : "");
            }
            System.out.println();

            System.out.printf("    Все измерения Custom: ");
            for (int i = 0; i < customTimes.length; i++) {
                System.out.printf("%d%s", customTimes[i], i < customTimes.length - 1 ? ", " : "");
            }
            System.out.println();

            System.out.println();
        }

        // Создание графиков
        String outputDir = "performance_charts";
        new File(outputDir).mkdirs();

        createExecutionTimeChart(results, outputDir);
        createMethodComparisonChart(results, outputDir);
        createSpeedupChart(results, outputDir);

        System.out.println("=== Графики сохранены в директории: " + outputDir + " ===");
        System.out.println("=== Измерения завершены ===");
    }

    /**
     * Измеряет время выполнения одного метода
     */
    private static long measureMethod(String methodName, Runnable computation,
                                      DeviceGenerator generator, List<Device> devices) {
        // Сбрасываем кэш перед измерением
        generator.resetCache(devices);

        // Небольшая дополнительная "прогревочная" итерация для этого метода
        computation.run();

        // Основное измерение
        generator.resetCache(devices);
        Instant start = Instant.now();
        computation.run();
        Instant end = Instant.now();

        long time = Duration.between(start, end).toMillis();
        System.out.printf("    %s: %d ms%n", methodName, time);
        return time;
    }

    /**
     * Вычисляет среднее значение массива long, начиная с указанного индекса
     * @param values массив значений
     * @param startIndex с какого индекса начинать вычисление среднего (чтобы исключить первые прогоны)
     * @return среднее значение
     */
    private static long calculateAverage(long[] values, int startIndex) {
        if (startIndex >= values.length) {
            return values[0];
        }

        long sum = 0;
        int count = 0;
        for (int i = startIndex; i < values.length; i++) {
            sum += values[i];
            count++;
        }
        return count > 0 ? sum / count : 0;
    }

    /**
     * Вычисляет медиану массива long
     */
    private static long calculateMedian(long[] values) {
        long[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        int middle = sorted.length / 2;
        if (sorted.length % 2 == 0) {
            return (sorted[middle - 1] + sorted[middle]) / 2;
        } else {
            return sorted[middle];
        }
    }

    /**
     * График 1: Время выполнения по размерам данных
     * Ось X - размер данных, Серии - методы
     */
    private static void createExecutionTimeChart(List<PerformanceResult> results, String outputDir)
            throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (PerformanceResult result : results) {
            dataset.addValue(
                    result.executionTimeMs,
                    result.methodName, // Серия (легенда)
                    String.valueOf(result.dataSize) // Категория (ось X)
            );
        }

        String title = "Время выполнения методов вычисления (среднее по 4 прогонам)";
        String xAxisLabel = "Количество элементов";
        String yAxisLabel = "Время выполнения (мс)";
        String filePath = outputDir + "/execution_time.png";

        saveBarChart(title, xAxisLabel, yAxisLabel, dataset, filePath);
    }

    /**
     * График 2: Сравнение методов
     * Ось X - методы, Серии - размеры данных
     */
    private static void createMethodComparisonChart(List<PerformanceResult> results, String outputDir)
            throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (PerformanceResult result : results) {
            dataset.addValue(
                    result.executionTimeMs,
                    String.valueOf(result.dataSize), // Серия (легенда)
                    result.methodName // Категория (ось X)
            );
        }

        String title = "Сравнение методов по времени выполнения";
        String xAxisLabel = "Метод";
        String yAxisLabel = "Время выполнения (мс)";
        String filePath = outputDir + "/method_comparison.png";

        saveBarChart(title, xAxisLabel, yAxisLabel, dataset, filePath);
    }

    /**
     * График 3: Ускорение относительно Sequential
     * Ось X - методы, Серии - размеры данных
     */
    private static void createSpeedupChart(List<PerformanceResult> results, String outputDir) throws IOException {
        // Группируем результаты по размеру данных
        Map<Integer, Map<String, Long>> resultsBySize = new HashMap<>();
        for (PerformanceResult result : results) {
            resultsBySize.putIfAbsent(result.dataSize, new HashMap<>());
            resultsBySize.get(result.dataSize).put(result.methodName, result.executionTimeMs);
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Map.Entry<Integer, Map<String, Long>> entry : resultsBySize.entrySet()) {
            int dataSize = entry.getKey();
            Map<String, Long> methods = entry.getValue();

            Long sequentialTime = methods.get("Sequential");
            if (sequentialTime == null || sequentialTime == 0) {
                continue;
            }

            // Вычисляем ускорение для остальных методов
            for (Map.Entry<String, Long> methodEntry : methods.entrySet()) {
                String methodName = methodEntry.getKey();
                if (!methodName.equals("Sequential")) {
                    double speedup = (double) sequentialTime / methodEntry.getValue();
                    dataset.addValue(
                            speedup,
                            String.valueOf(dataSize), // Серия (легенда)
                            methodName // Категория (ось X)
                    );
                }
            }
        }

        String title = "Ускорение относительно последовательного метода";
        String xAxisLabel = "Метод";
        String yAxisLabel = "Ускорение (раз)";
        String filePath = outputDir + "/speedup.png";

        saveBarChart(title, xAxisLabel, yAxisLabel, dataset, filePath);
    }

    /**
     * График 4: Абсолютное время выполнения в логарифмической шкале
     */
    private static void createLogScaleChart(List<PerformanceResult> results, String outputDir) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (PerformanceResult result : results) {
            dataset.addValue(
                    Math.log10(result.executionTimeMs + 1), // +1 чтобы избежать log(0)
                    result.methodName,
                    String.valueOf(result.dataSize)
            );
        }

        String title = "Логарифмическая шкала времени выполнения";
        String xAxisLabel = "Количество элементов";
        String yAxisLabel = "log10(время выполнения, мс)";
        String filePath = outputDir + "/log_scale.png";

        saveBarChart(title, xAxisLabel, yAxisLabel, dataset, filePath);
    }

    /**
     * Вспомогательный метод для создания и сохранения гистограммы
     */
    private static void saveBarChart(String title, String xAxisLabel, String yAxisLabel,
                                     DefaultCategoryDataset dataset, String filePath) throws IOException {

        JFreeChart barChart = ChartFactory.createBarChart(
                title,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        CategoryPlot plot = barChart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);

        // Настраиваем цвета для разных методов
        if (dataset.getRowCount() > 0) {
            for (int i = 0; i < dataset.getRowCount(); i++) {
                String seriesName = (String) dataset.getRowKey(i);
                if (seriesName.contains("Sequential")) {
                    renderer.setSeriesPaint(i, new Color(70, 130, 180)); // SteelBlue
                } else if (seriesName.contains("Standard")) {
                    renderer.setSeriesPaint(i, new Color(50, 205, 50)); // LimeGreen
                } else if (seriesName.contains("Custom")) {
                    renderer.setSeriesPaint(i, new Color(220, 20, 60)); // Crimson
                }
            }
        }

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(
                CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));

        int width = 1280;
        int height = 720;

        File file = new File(filePath);
        ChartUtils.saveChartAsPNG(file, barChart, width, height);

        System.out.println("График сохранён: " + filePath);
    }
}