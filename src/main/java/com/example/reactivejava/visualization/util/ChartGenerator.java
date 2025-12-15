package com.example.reactivejava.visualization.util;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ChartGenerator {

    /**
     * Создает и сохраняет гистограмму (Bar Chart).
     *
     * @param title             Заголовок графика
     * @param categoryAxisLabel Название оси X
     * @param valueAxisLabel    Название оси Y
     * @param dataset           Данные
     * @param useLogAxis        Использовать ли логарифмическую шкалу для Y
     * @param filePath          Путь для сохранения файла
     */
    public static void createAndSaveBarChart(String title, String categoryAxisLabel, String valueAxisLabel,
            CategoryDataset dataset, boolean useLogAxis, String filePath) throws IOException {

        JFreeChart barChart = ChartFactory.createBarChart(
                title,
                categoryAxisLabel,
                valueAxisLabel,
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

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(
                CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));

        // --- ОБНОВЛЕННАЯ ЛОГИКА (ВЕРСИЯ 3) ---
        if (useLogAxis) {
            try {
                // 1. Пытаемся создать логарифмическую ось
                LogarithmicAxis rangeAxis = new LogarithmicAxis(valueAxisLabel);
                rangeAxis.setAllowNegativesFlag(false);
                // 2. УДАЛЕНО: rangeAxis.setLowerBound(1E-6);
                // Это было причиной проблемы на графиках с большими значениями.

                // 3. Пытаемся применить ось.
                // Эта строка вызовет сбой, если данные <= 0 (например, для "COUNT")
                plot.setRangeAxis(rangeAxis);

            } catch (RuntimeException e) {
                // 4. Если сбой...
                if (e.getMessage() != null && e.getMessage().contains("Values less than or equal to zero")) {
                    // ...откатываемся к обычной (линейной) шкале
                    // System.err.println("ПРЕДУПРЕЖДЕНИЕ: Не удалось использовать логарифмическую
                    // шкалу для графика '" + title + "'.");
                    // System.err.println("Причина: Данные слишком близки к 0. Переключение на
                    // линейную шкалу.");
                    plot.setRangeAxis(new NumberAxis(valueAxisLabel));
                } else {
                    // Пробрасываем другие, непредвиденные ошибки
                    throw e;
                }
            }
        }
        // --- КОНЕЦ ОБНОВЛЕННОЙ ЛОГИКИ ---

        int width = 1280;
        int height = 720;

        // Создаем директории, если их нет
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Сохраняем график (ошибка конфигурации оси уже обработана)
        ChartUtils.saveChartAsPNG(file, barChart, width, height);

        System.out.println("График сохранен в: " + filePath);
    }

    /**
     * Создает и сохраняет линейный график (Line Chart) с отметками пересечений.
     *
     * @param title         Заголовок графика
     * @param xAxisLabel    Название оси X
     * @param yAxisLabel    Название оси Y
     * @param dataset       Данные (XYSeriesCollection)
     * @param intersections Список точек пересечения кривых
     * @param filePath      Путь для сохранения файла
     */
    public static void createAndSaveLineChart(String title, String xAxisLabel, String yAxisLabel,
            XYSeriesCollection dataset, List<?> intersections, String filePath) throws IOException {

        JFreeChart lineChart = ChartFactory.createXYLineChart(
                title,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        XYPlot plot = lineChart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

        // Configure line renderer
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Set different colors for each series
        Color[] colors = {
                new Color(31, 119, 180), // Blue
                new Color(255, 127, 14), // Orange
                new Color(44, 160, 44), // Green
                new Color(214, 39, 40), // Red
                new Color(148, 103, 189), // Purple
                new Color(140, 86, 75) // Brown
        };

        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesStroke(i, new BasicStroke(2.5f));
            renderer.setSeriesPaint(i, colors[i % colors.length]);
            renderer.setSeriesShapesVisible(i, true);
            renderer.setSeriesShape(i, new Ellipse2D.Double(-3, -3, 6, 6));
        }

        plot.setRenderer(renderer);

        // Add intersection markers if provided
        if (intersections != null && !intersections.isEmpty()) {
            // Create a separate renderer for intersection points
            XYLineAndShapeRenderer intersectionRenderer = new XYLineAndShapeRenderer(false, true);
            intersectionRenderer.setSeriesPaint(0, Color.RED);
            intersectionRenderer.setSeriesShape(0, new Ellipse2D.Double(-5, -5, 10, 10));
            intersectionRenderer.setSeriesShapesFilled(0, false);
            intersectionRenderer.setSeriesStroke(0, new BasicStroke(2.0f));

            // Create a dataset for intersection points
            XYSeriesCollection intersectionDataset = new XYSeriesCollection();
            org.jfree.data.xy.XYSeries intersectionSeries = new org.jfree.data.xy.XYSeries("Intersections");

            for (Object obj : intersections) {
                try {
                    // Use reflection to get x and y values
                    double x = (double) obj.getClass().getField("x").get(obj);
                    double y = (double) obj.getClass().getField("y").get(obj);
                    intersectionSeries.add(x, y);
                } catch (Exception e) {
                    // Skip if reflection fails
                }
            }

            if (intersectionSeries.getItemCount() > 0) {
                intersectionDataset.addSeries(intersectionSeries);
                plot.setDataset(1, intersectionDataset);
                plot.setRenderer(1, intersectionRenderer);
            }
        }

        int width = 1280;
        int height = 720;

        // Create directories if they don't exist
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Save the chart
        ChartUtils.saveChartAsPNG(file, lineChart, width, height);

        System.out.println("График сохранен в: " + filePath);
    }
}