package com.example.reactivejava.visualization.visualizer;

import com.example.reactivejava.visualization.model.*;
import com.example.reactivejava.visualization.util.*;

import org.jfree.data.category.DefaultCategoryDataset;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BatchSizeOptimizationVisualizer {

    public void run(String jsonFilePath, String outputDir) throws IOException {
        List<JmhResult> results = JmhResultParser.parse(jsonFilePath);

        // 1. Группируем по statType
        Map<String, List<JmhResult>> byStatType = results.stream()
                .collect(Collectors.groupingBy(r -> r.getParam("statType")));

        for (Map.Entry<String, List<JmhResult>> statEntry : byStatType.entrySet()) {
            String statType = statEntry.getKey();

            // 2. Группируем по deviceCount
            Map<String, List<JmhResult>> byDeviceCount = statEntry.getValue().stream()
                    .collect(Collectors.groupingBy(r -> r.getParam("deviceCount")));

            for (Map.Entry<String, List<JmhResult>> deviceEntry : byDeviceCount.entrySet()) {
                String deviceCount = deviceEntry.getKey();

                // 3. НОВАЯ ГРУППИРОВКА: по method
                Map<String, List<JmhResult>> byMethod = deviceEntry.getValue().stream()
                        .collect(Collectors.groupingBy(JmhResult::getShortMethodName));

                for (Map.Entry<String, List<JmhResult>> methodEntry : byMethod.entrySet()) {
                    String method = methodEntry.getKey();
                    List<JmhResult> groupResults = methodEntry.getValue();

                    DefaultCategoryDataset timeDataset = new DefaultCategoryDataset();
                    String yAxisLabel = "ms/op";

                    // 4. Заполняем набор данных (Ось X = batchSize)
                    for (JmhResult r : groupResults) {
                        String batchSize = r.getParam("batchSize");
                        timeDataset.addValue(r.primaryMetric.score, "Time", batchSize);
                        yAxisLabel = "Time (" + r.primaryMetric.scoreUnit + ")";
                    }

                    // 5. Создаем и сохраняем график
                    String safeStatType = statType.replaceAll("[^a-zA-Z0-9_]", "");
                    String safeMethod = method.replaceAll("[^a-zA-Z0-9_]", "");
                    String safeDeviceCount = deviceCount.replaceAll("[^a-zA-Z0-9_]", "");

                    String title = String.format("Batch Size Opt: %s (Method: %s, Devices: %s) - Time", statType,
                            method, deviceCount);
                    // New path structure:
                    // {outputDir}/BatchSizeOptimization/{statType}/dev_{deviceCount}/{method}.png
                    String filePath = Paths.get(outputDir, "BatchSizeOptimization", safeStatType,
                            "dev_" + safeDeviceCount, safeMethod + ".png").toString();

                    ChartGenerator.createAndSaveBarChart(title, "Batch Size", yAxisLabel, timeDataset, true, filePath);
                }
            }
        }
    }
}