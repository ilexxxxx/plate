package com.dataprocess.platform.model;

public record Notice(
        Long id,
        String title,
        String content,
        String type,
        String publishTime
) {
}
