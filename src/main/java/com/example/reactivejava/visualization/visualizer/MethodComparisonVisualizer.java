package com.example.reactivejava.visualization.visualizer;

import com.example.reactivejava.visualization.model.*;
import com.example.reactivejava.visualization.util.*;

import org.jfree.data.category.DefaultCategoryDataset;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MethodComparisonVisualizer {

    public void run(String jsonFilePath, String outputDir) throws IOException {
        List<JmhResult> results = JmhResultParser.parse(jsonFilePath);

        // 1. Группируем по statType
        Map<String, List<JmhResult>> byStatType = results.stream()
                .collect(Collectors.groupingBy(r -> r.getParam("statType")));

        for (Map.Entry<String, List<JmhResult>> statEntry : byStatType.entrySet()) {
            String statType = statEntry.getKey();
            List<JmhResult> statResults = statEntry.getValue();

            // 2. Подготовка данных
            // Находим baseline (sequential) для каждого deviceCount
            Map<String, Double> baselinesByDevice = statResults.stream()
                    .filter(r -> r.getShortMethodName().equals("sequential"))
                    .collect(Collectors.toMap(
                            r -> r.getParam("deviceCount"),
                            r -> r.primaryMetric.score,
                            (v1, v2) -> v1 // на случай дубликатов
                    ));

            // Сортируем результаты: сначала по имени метода, потом по количеству устройств
            // (числовому)
            statResults.sort((r1, r2) -> {
                int methodCompare = r1.getShortMethodName().compareTo(r2.getShortMethodName());
                if (methodCompare != 0)
                    return methodCompare;

                return compareDeviceCounts(r1.getParam("deviceCount"), r2.getParam("deviceCount"));
            });

            DefaultCategoryDataset timeDataset = new DefaultCategoryDataset();
            DefaultCategoryDataset speedupDataset = new DefaultCategoryDataset();
            String yAxisLabel = "ms/op"; // Значение по умолчанию

            // 3. Заполняем наборы данных
            // Ось X (Category) = Method
            // Легенда (Series) = Device Count
            for (JmhResult r : statResults) {
                String method = r.getShortMethodName();
                String deviceCount = r.getParam("deviceCount");
                double score = r.primaryMetric.score;
                yAxisLabel = "Time (" + r.primaryMetric.scoreUnit + ")";

                // График 1: Время
                timeDataset.addValue(score, deviceCount, method);

                // График 2: Ускорение
                // Считаем ускорение относительно sequential ТОГО ЖЕ deviceCount
                Double baseline = baselinesByDevice.get(deviceCount);
                if (baseline != null && baseline > 0 && !method.equals("sequential")) {
                    double speedup = baseline / score;
                    speedupDataset.addValue(speedup, deviceCount, method);
                }
            }

            // 4. Создаем и сохраняем графики
            String safeStatType = statType.replaceAll("[^a-zA-Z0-9_]", "");

            // График времени
            String timeTitle = String.format("Method Comparison: %s - Time", statType);
            String timeFilePath = Paths
                    .get(outputDir, "MethodComparison", safeStatType, "time.png")
                    .toString();
            ChartGenerator.createAndSaveBarChart(timeTitle, "Method", yAxisLabel, timeDataset, true, timeFilePath);

            // График ускорения
            if (speedupDataset.getColumnCount() > 0) {
                String speedupTitle = String.format("Method Comparison: %s - Speedup vs Sequential", statType);
                String speedupFilePath = Paths
                        .get(outputDir, "MethodComparison", safeStatType, "speedup.png")
                        .toString();
                ChartGenerator.createAndSaveBarChart(speedupTitle, "Method", "Speedup (X times)", speedupDataset,
                        false, speedupFilePath);
            }
        }
    }

    private int compareDeviceCounts(String s1, String s2) {
        try {
            return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
        } catch (NumberFormatException e) {
            return s1.compareTo(s2);
        }
    }
}