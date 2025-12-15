package com.example.reactivejava.visualization;

import com.example.reactivejava.visualization.visualizer.*;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        // --- ПУТИ К ВАШИМ ФАЙЛАМ ---
        String basePath = "data/benchmark_results/";

        // --- ПУТЬ ДЛЯ СОХРАНЕНИЯ ГРАФИКОВ ---
        String outputDir = "data/visualization_results/";
        new File(outputDir).mkdirs();

        try {
            System.out.println("\nЗапуск визуализации для Delay Impact...");
            DelayImpactVisualizer delayImpactViz = new DelayImpactVisualizer();
            String delayImpactJson = basePath + "delay_impact.json";
            delayImpactViz.run(delayImpactJson, outputDir);

            System.out.println("\nЗапуск визуализации для Batch Size Optimization...");
            String batchOptJson = basePath + "batch_size_optimization.json";
            BatchSizeOptimizationVisualizer batchOptViz = new BatchSizeOptimizationVisualizer();
            batchOptViz.run(batchOptJson, outputDir);

            System.out.println("\nЗапуск визуализации для Lab2 Benchmark...");
            String lab2Json = basePath + "lab2_benchmark.json";
            Lab2BenchmarkVisualizer lab2Viz = new Lab2BenchmarkVisualizer();
            lab2Viz.run(lab2Json, outputDir);

            System.out.println("\nЗапуск визуализации для Lab3 Benchmark...");
            String lab3Json = basePath + "lab3_benchmark.json";
            Lab3BenchmarkVisualizer lab3Viz = new Lab3BenchmarkVisualizer();
            lab3Viz.run(lab3Json, outputDir);

            System.out.println("\n--- Визуализация завершена! ---");
            System.out.println("Графики сохранены в: " + new File(outputDir).getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Ошибка при создании графиков: " + e.getMessage());
            e.printStackTrace();
        }
    }
}