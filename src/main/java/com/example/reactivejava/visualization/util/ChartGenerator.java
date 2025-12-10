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
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

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
}