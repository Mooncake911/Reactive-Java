package com.example.reactivejava.visualization.util;
import com.example.reactivejava.visualization.model.JmhResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JmhResultParser {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<JmhResult> parse(String jsonFilePath) throws IOException {
        return mapper.readValue(new File(jsonFilePath), new TypeReference<List<JmhResult>>() {
        });
    }
}
