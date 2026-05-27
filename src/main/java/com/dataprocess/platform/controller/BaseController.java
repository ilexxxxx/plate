package com.dataprocess.platform.controller;

import com.dataprocess.platform.model.SysUser;
import jakarta.servlet.http.HttpSession;

abstract class BaseController {
    protected SysUser currentUser(HttpSession session) {
        Object user = session.getAttribute("loginUser");
        return user instanceof SysUser sysUser ? sysUser : null;
    }

    protected String currentUsername(HttpSession session) {
        SysUser user = currentUser(session);
        return user == null ? "system" : user.username();
    }
}
