package com.dataprocess.platform.model;

public record SysUser(
        Long id,
        String username,
        String password,
        String role,
        String status,
        String createTime,
        String updateTime
) {
    public boolean admin() {
        return "admin".equalsIgnoreCase(role);
    }
}
