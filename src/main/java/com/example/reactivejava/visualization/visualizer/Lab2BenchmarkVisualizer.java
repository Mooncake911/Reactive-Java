package com.example.reactivejava.visualization.visualizer;

import com.example.reactivejava.visualization.model.*;
import com.example.reactivejava.visualization.util.*;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Lab2BenchmarkVisualizer {

    public void run(String inputPath, String outputDir) throws IOException {
        List<JmhResult> results = new ArrayList<>();
        java.io.File input = new java.io.File(inputPath);

        if (input.isDirectory()) {
            java.io.File[] files = input.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (java.io.File file : files) {
                    try {
                        results.addAll(JmhResultParser.parse(file.getAbsolutePath()));
                    } catch (Exception e) {
                        System.err.println("Error parsing file " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        } else {
            results.addAll(JmhResultParser.parse(inputPath));
        }

        // Group by statType (STATUS, DEVICES_BY_TYPE)
        Map<String, List<JmhResult>> byStatType = results.stream()
                .collect(Collectors.groupingBy(r -> r.getParam("statType")));

        for (Map.Entry<String, List<JmhResult>> statEntry : byStatType.entrySet()) {
            String statType = statEntry.getKey();

            // Create charts for standard collectors (sequential vs parallel)
            createComparisonCharts(statEntry.getValue(), statType, "standard",
                    "standardCollectors", "standardCollectorsParallel", outputDir);

            // Create charts for custom collector (sequential vs parallel)
            createComparisonCharts(statEntry.getValue(), statType, "custom",
                    "customCollector", "customCollectorParallel", outputDir);
        }
    }

    /**
     * Creates comparison charts for sequential vs parallel methods
     * Format: TYPE_METHOD_DELAY (e.g., STATUS_standard_0ms.png)
     */
    private void createComparisonCharts(List<JmhResult> results, String statType,
            String methodLabel, String sequentialMethod,
            String parallelMethod, String outputDir) throws IOException {

        // Group by delayMs - we'll create one chart per delay value
        Map<String, List<JmhResult>> byDelay = results.stream()
                .collect(Collectors.groupingBy(r -> r.getParam("delayMs")));

        for (Map.Entry<String, List<JmhResult>> delayEntry : byDelay.entrySet()) {
            String delayMs = delayEntry.getKey();

            // Filter results for sequential and parallel methods
            List<JmhResult> sequentialResults = delayEntry.getValue().stream()
                    .filter(r -> r.getShortMethodName().equals(sequentialMethod))
                    .sorted(Comparator.comparingInt(r -> Integer.parseInt(r.getParam("deviceCount"))))
                    .collect(Collectors.toList());

            List<JmhResult> parallelResults = delayEntry.getValue().stream()
                    .filter(r -> r.getShortMethodName().equals(parallelMethod))
                    .sorted(Comparator.comparingInt(r -> Integer.parseInt(r.getParam("deviceCount"))))
                    .collect(Collectors.toList());

            if (sequentialResults.isEmpty() || parallelResults.isEmpty()) {
                System.out.println(String.format("  ПРОПУЩЕНО: %s результаты пустые",
                        sequentialResults.isEmpty() ? "Sequential" : "Parallel"));
                continue;
            }

            // Create dataset with two series: sequential and parallel
            XYSeriesCollection dataset = new XYSeriesCollection();
            String yAxisLabel = "Time (ms/op)";

            // Sequential series
            XYSeries sequentialSeries = new XYSeries("Sequential");
            for (JmhResult r : sequentialResults) {
                int deviceCount = Integer.parseInt(r.getParam("deviceCount"));
                double score = r.primaryMetric.score;
                sequentialSeries.add(deviceCount, score);
                yAxisLabel = "Time (" + r.primaryMetric.scoreUnit + ")";
            }
            dataset.addSeries(sequentialSeries);

            // Parallel series
            XYSeries parallelSeries = new XYSeries("Parallel");
            for (JmhResult r : parallelResults) {
                int deviceCount = Integer.parseInt(r.getParam("deviceCount"));
                double score = r.primaryMetric.score;
                parallelSeries.add(deviceCount, score);
            }
            dataset.addSeries(parallelSeries);

            // Find intersections
            List<IntersectionPoint> intersections = findIntersectionsBetweenSeries(sequentialSeries, parallelSeries);

            // Create chart title
            String title = String.format("Lab2: %s - %s (Delay: %s ms)\nSequential vs Parallel",
                    statType, methodLabel, delayMs);

            String safeStatType = statType.replaceAll("[^a-zA-Z0-9_]", "");
            String safeMethodLabel = methodLabel.replaceAll("[^a-zA-Z0-9_]", "");
            String safeDelay = delayMs.replaceAll("[^a-zA-Z0-9_]", "");

            String fileName = String.format("%s_%sms.png", safeMethodLabel, safeDelay);
            String filePath = Paths.get(outputDir, "Lab2Benchmark", safeStatType, fileName).toString();

            // Ensure directory exists
            new java.io.File(Paths.get(outputDir, "Lab2Benchmark", safeStatType).toString()).mkdirs();

            // Create and save line chart
            ChartGenerator.createAndSaveLineChart(title, "Device Count", yAxisLabel,
                    dataset, intersections, filePath);
        }
    }

    /**
     * Finds intersection points between two series using linear interpolation
     */
    private List<IntersectionPoint> findIntersectionsBetweenSeries(XYSeries series1, XYSeries series2) {
        List<IntersectionPoint> intersections = new ArrayList<>();

        // Get all x values that exist in both series
        Set<Double> xValues1 = new TreeSet<>();
        Set<Double> xValues2 = new TreeSet<>();

        for (int i = 0; i < series1.getItemCount(); i++) {
            xValues1.add(series1.getX(i).doubleValue());
        }
        for (int i = 0; i < series2.getItemCount(); i++) {
            xValues2.add(series2.getX(i).doubleValue());
        }

        Set<Double> commonX = new TreeSet<>(xValues1);
        commonX.retainAll(xValues2);

        List<Double> sortedX = new ArrayList<>(commonX);

        // Check for intersections between consecutive points
        for (int i = 0; i < sortedX.size() - 1; i++) {
            double x1 = sortedX.get(i);
            double x2 = sortedX.get(i + 1);

            double y1_s1 = getYValue(series1, x1);
            double y2_s1 = getYValue(series1, x2);
            double y1_s2 = getYValue(series2, x1);
            double y2_s2 = getYValue(series2, x2);

            // Check if lines cross between these points
            if ((y1_s1 - y1_s2) * (y2_s1 - y2_s2) < 0) {
                // Lines cross - calculate intersection point using linear interpolation
                double xIntersect = x1 + (x2 - x1) * Math.abs(y1_s1 - y1_s2) /
                        (Math.abs(y1_s1 - y1_s2) + Math.abs(y2_s1 - y2_s2));
                double yIntersect = y1_s1 + (y2_s1 - y1_s1) * (xIntersect - x1) / (x2 - x1);

                intersections.add(new IntersectionPoint(xIntersect, yIntersect,
                        series1.getKey().toString(), series2.getKey().toString()));
            }
        }

        return intersections;
    }

    private double getYValue(XYSeries series, double x) {
        for (int i = 0; i < series.getItemCount(); i++) {
            if (Math.abs(series.getX(i).doubleValue() - x) < 0.001) {
                return series.getY(i).doubleValue();
            }
        }
        return 0.0;
    }

    /**
     * Helper class to represent an intersection point
     */
    public static class IntersectionPoint {
        public final double x;
        public final double y;
        public final String series1Name;
        public final String series2Name;

        public IntersectionPoint(double x, double y, String series1Name, String series2Name) {
            this.x = x;
            this.y = y;
            this.series1Name = series1Name;
            this.series2Name = series2Name;
        }
    }
}
