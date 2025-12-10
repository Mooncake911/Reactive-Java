package com.example.reactivejava.visualization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrimaryMetric {
    public double score;
    public String scoreUnit;
}