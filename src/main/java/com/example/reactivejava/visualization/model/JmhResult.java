package com.example.reactivejava.visualization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JmhResult {
    public String benchmark;
    public Map<String, String> params;
    public PrimaryMetric primaryMetric;

    // Геттер для получения короткого имени метода (гибкость!)
    public String getShortMethodName() {
        if (benchmark == null) return "unknown";
        int lastDot = benchmark.lastIndexOf('.');
        return (lastDot == -1) ? benchmark : benchmark.substring(lastDot + 1);
    }

    // Геттер для параметра (удобство)
    public String getParam(String key) {
        return params.getOrDefault(key, "N/A");
    }
}