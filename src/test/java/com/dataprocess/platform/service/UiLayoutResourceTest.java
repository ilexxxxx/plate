package com.dataprocess.platform.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UiLayoutResourceTest {

    @Test
    void statisticsChartsUseStableCanvasFrames() throws Exception {
        String statistics = Files.readString(Path.of("src/main/resources/templates/statistics.html"), StandardCharsets.UTF_8);
        String css = Files.readString(Path.of("src/main/resources/static/css/app.css"), StandardCharsets.UTF_8);

        assertThat(statistics).contains("chart-canvas-frame");
        assertThat(css).contains(".chart-canvas-frame");
        assertThat(css).contains("height: 240px");
    }

    @Test
    void checkboxAndActionLayoutsAreCompact() throws Exception {
        String css = Files.readString(Path.of("src/main/resources/static/css/app.css"), StandardCharsets.UTF_8);

        assertThat(css).contains("input[type=\"checkbox\"]");
        assertThat(css).contains("inline-size: 20px");
        assertThat(css).contains(".action-row");
        assertThat(css).contains(".rule-list .btn");
    }
}
