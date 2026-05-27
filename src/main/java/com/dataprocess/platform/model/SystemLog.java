package com.dataprocess.platform.model;

public record SystemLog(
        Long id,
        String user,
        String action,
        String target,
        String result,
        String logTime
) {
}
