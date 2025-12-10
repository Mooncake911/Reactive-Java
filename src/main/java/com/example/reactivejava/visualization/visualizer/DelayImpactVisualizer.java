package com.example.reactivejava.visualization.visualizer;

import com.example.reactivejava.visualization.model.*;
import com.example.reactivejava.visualization.util.*;

import org.jfree.data.category.DefaultCategoryDataset;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DelayImpactVisualizer {

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

                // 3. НОВАЯ ГРУППИРОВКА: по deviceCount
                Map<String, List<JmhResult>> byDeviceCount = delayEntry.getValue().stream()
                        .collect(Collectors.groupingBy(r -> r.getParam("deviceCount")));

                for (Map.Entry<String, List<JmhResult>> deviceEntry : byDeviceCount.entrySet()) {
                    String deviceCount = deviceEntry.getKey();
                    List<JmhResult> groupResults = deviceEntry.getValue();

                    // 4. Ищем "базовый" (sequential) результат ДЛЯ ЭТОЙ ГРУППЫ
                    double baselineScore = groupResults.stream()
                            .filter(r -> r.getShortMethodName().equals("sequential"))
                            .mapToDouble(r -> r.primaryMetric.score)
                            .findFirst()
                            .orElse(0.0);

                    DefaultCategoryDataset timeDataset = new DefaultCategoryDataset();
                    DefaultCategoryDataset speedupDataset = new DefaultCategoryDataset();
                    String yAxisLabel = "ms/op";

                    // 5. Заполняем наборы данных (Ось X = method)
                    for (JmhResult r : groupResults) {
                        String method = r.getShortMethodName();
                        double score = r.primaryMetric.score;
                        yAxisLabel = "Time (" + r.primaryMetric.scoreUnit + ")";
                        timeDataset.addValue(score, "Time", method);

                        if (baselineScore > 0 && !method.equals("sequential")) {
                            double speedup = baselineScore / score;
                            speedupDataset.addValue(speedup, "Speedup", method);
                        }
                    }

                    // 6. Создаем и сохраняем графики
                    String safeStatType = statType.replaceAll("[^a-zA-Z0-9_]", "");
                    String safeDelay = delayMs.replaceAll("[^a-zA-Z0-9_]", "");
                    String safeDeviceCount = deviceCount.replaceAll("[^a-zA-Z0-9_]", "");

                    // График времени
                    String timeTitle = String.format("Delay Impact: %s (Delay: %s ms, Devices: %s) - Time", statType,
                            delayMs, deviceCount);
                    // New path:
                    // {outputDir}/DelayImpact/{statType}/dev_{deviceCount}/delay_{delayMs}ms_time.png
                    String timeFilePath = Paths.get(outputDir, "DelayImpact", safeStatType, "dev_" + safeDeviceCount,
                            "delay_" + safeDelay + "ms_time.png").toString();
                    ChartGenerator.createAndSaveBarChart(timeTitle, "Method", yAxisLabel, timeDataset, true,
                            timeFilePath);

                    // График ускорения
                    if (speedupDataset.getColumnCount() > 0) {
                        String speedupTitle = String.format("Delay Impact: %s (Delay: %s ms, Devices: %s) - Speedup",
                                statType, delayMs, deviceCount);
                        // New path:
                        // {outputDir}/DelayImpact/{statType}/dev_{deviceCount}/delay_{delayMs}ms_speedup.png
                        String speedupFilePath = Paths.get(outputDir, "DelayImpact", safeStatType,
                                "dev_" + safeDeviceCount, "delay_" + safeDelay + "ms_speedup.png").toString();
                        ChartGenerator.createAndSaveBarChart(speedupTitle, "Method", "Speedup (X times)",
                                speedupDataset, false, speedupFilePath);
                    }
                }
            }
        }
    }
}