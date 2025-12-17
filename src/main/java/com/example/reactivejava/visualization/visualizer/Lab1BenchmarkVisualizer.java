package com.example.reactivejava.visualization.visualizer;

import com.example.reactivejava.visualization.model.JmhResult;
import com.example.reactivejava.visualization.util.JmhResultParser;
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
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Lab1BenchmarkVisualizer {

    public void run(String inputPath, String outputDir) throws IOException {
        List<JmhResult> results = JmhResultParser.parse(inputPath);

        // Group by statType
        Map<String, List<JmhResult>> byStatType = results.stream()
                .collect(Collectors.groupingBy(r -> r.getParam("statType")));

        for (Map.Entry<String, List<JmhResult>> entry : byStatType.entrySet()) {
            String statType = entry.getKey();
            List<JmhResult> statResults = entry.getValue();
            processStatType(statType, statResults, outputDir);
        }
    }

    private void processStatType(String statType, List<JmhResult> results, String outputDir) {
        // Map: Size -> Dataset
        Map<Integer, DefaultCategoryDataset> datasetsBySize = new HashMap<>();

        // Group by size
        Map<String, List<JmhResult>> bySize = results.stream()
                .collect(Collectors.groupingBy(r -> r.getParam("deviceCount")));

        // Sort sizes to process in order
        List<Integer> sortedSizes = bySize.keySet().stream()
                .map(Integer::parseInt)
                .sorted()
                .collect(Collectors.toList());

        for (int size : sortedSizes) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            List<JmhResult> sizeResults = bySize.get(String.valueOf(size));

            // Fill dataset
            addValueToDataset(dataset, sizeResults, "Sequential", "Sequential");
            addValueToDataset(dataset, sizeResults, "Stream API (Std)", "Stream (Std)");
            addValueToDataset(dataset, sizeResults, "Stream API (Custom)", "Stream (Custom)");

            datasetsBySize.put(size, dataset);
        }

        saveCompositeChart(statType, sortedSizes, datasetsBySize, outputDir);
    }

    private void addValueToDataset(DefaultCategoryDataset dataset, List<JmhResult> results, String displayName,
            String label) {
        results.stream()
                .filter(r -> displayName.equals(r.getParam("displayName")))
                .findFirst()
                .ifPresent(r -> dataset.addValue(r.primaryMetric.score, "Time", label));
    }

    private void saveCompositeChart(String statType, List<Integer> sizes, Map<Integer, DefaultCategoryDataset> datasets,
            String outputDir) {
        try {
            List<BufferedImage> charts = new ArrayList<>();
            int totalWidth = 0;
            int maxHeight = 0;

            for (int size : sizes) {
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
            String title = "Benchmark Result: " + statType;
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, (totalWidth - fm.stringWidth(title)) / 2, 35);

            int currentX = 0;
            for (BufferedImage chart : charts) {
                g.drawImage(chart, currentX, headerHeight, null);
                currentX += chart.getWidth();
            }

            g.dispose();

            // Safe filename
            String safeStatType = statType.replaceAll("[^a-zA-Z0-9_]", "");
            File outputFile = new File(outputDir + File.separator + "Lab1Benchmark", safeStatType + ".png");

            // Ensure dir exists
            outputFile.getParentFile().mkdirs();

            ImageIO.write(combined, "PNG", outputFile);
            System.out.println("Generated chart: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Ошибка при сохранении графика: " + e.getMessage());
        }
    }

    private BufferedImage createChartImage(int size, DefaultCategoryDataset dataset) {
        String title = String.format("Size: %,d", size);

        JFreeChart chart = ChartFactory.createBarChart(
                title,
                "",
                "Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        NumberAxis rangeAxis = getNumberAxis(chart);
        rangeAxis.setAutoRangeIncludesZero(true);

        return chart.createBufferedImage(400, 300);
    }

    private NumberAxis getNumberAxis(JFreeChart chart) {
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
