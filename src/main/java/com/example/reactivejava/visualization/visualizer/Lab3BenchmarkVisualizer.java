package com.example.reactivejava.visualization.visualizer;

import com.example.reactivejava.visualization.model.*;
import com.example.reactivejava.visualization.util.*;

import org.jfree.data.category.DefaultCategoryDataset;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Lab3BenchmarkVisualizer {

    public void run(String jsonFilePath, String outputDir) throws IOException {
        List<JmhResult> results = JmhResultParser.parse(jsonFilePath);

        // 1. Группируем по statType
        Map<String, List<JmhResult>> byStatType = results.stream()
                .collect(Collectors.groupingBy(r -> r.getParam("statType")));

        for (Map.Entry<String, List<JmhResult>> statEntry : byStatType.entrySet()) {
            String statType = statEntry.getKey();

            // 2. Группируем по delayMs
            Map<String, List<JmhResult>> byDelay = statEntry.getValue().stream()
                    .collect(Collectors.groupingBy(r -> r.getParam("delayMs")));

            for (Map.Entry<String, List<JmhResult>> delayEntry : byDelay.entrySet()) {
                String delayMs = delayEntry.getKey();

                // 3. Группируем по deviceCount
                Map<String, List<JmhResult>> byDeviceCount = delayEntry.getValue().stream()
                        .collect(Collectors.groupingBy(r -> r.getParam("deviceCount")));

                for (Map.Entry<String, List<JmhResult>> deviceEntry : byDeviceCount.entrySet()) {
                    String deviceCount = deviceEntry.getKey();
                    List<JmhResult> groupResults = deviceEntry.getValue();

                    // 4. Ищем результат стандартного параллельного стрима как базовый для сравнения
                    // double baselineScore = groupResults.stream()
                    // .filter(r -> r.getShortMethodName().equals("standardCollectorsParallel"))
                    // .mapToDouble(r -> r.primaryMetric.score)
                    // .findFirst()
                    // .orElse(0.0);

                    DefaultCategoryDataset timeDataset = new DefaultCategoryDataset();
                    String yAxisLabel = "ms/op";

                    // 5. Заполняем наборы данных
                    for (JmhResult r : groupResults) {
                        String method = r.getShortMethodName();
                        // Упрощаем названия методов для графика
                        String displayMethod = simplifyMethodName(method);

                        double score = r.primaryMetric.score;
                        yAxisLabel = "Time (" + r.primaryMetric.scoreUnit + ")";
                        timeDataset.addValue(score, "Time", displayMethod);
                    }

                    // 6. Создаем и сохраняем графики
                    String safeStatType = statType.replaceAll("[^a-zA-Z0-9_]", "");
                    String safeDelay = delayMs.replaceAll("[^a-zA-Z0-9_]", "");
                    String safeDeviceCount = deviceCount.replaceAll("[^a-zA-Z0-9_]", "");

                    // График времени
                    String timeTitle = String.format("Lab3: %s - RxJava vs Streams (Delay: %s ms, Devices: %s) - Time",
                            statType, delayMs, deviceCount);
                    String timeFilePath = Paths.get(outputDir, "Lab3Benchmark", safeStatType, "dev_" + safeDeviceCount,
                            "delay_" + safeDelay + "ms_time.png").toString();
                    ChartGenerator.createAndSaveBarChart(timeTitle, "Method", yAxisLabel, timeDataset, true,
                            timeFilePath);
                }
            }
        }
    }

    private String simplifyMethodName(String original) {
        return switch (original) {
            case "standardCollectorsParallel" -> "Stream (Std)";
            case "customCollectorParallel" -> "Stream (Custom)";
            case "rxJavaObservable" -> "Rx Observable";
            case "rxJavaFlowable" -> "Rx Flowable";
            case "rxJavaFlowableParallel" -> "Rx Flowable (Parallel)";
            default -> original;
        };
    }
}
