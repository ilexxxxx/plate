package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.OperationResult;
import com.dataprocess.platform.model.SysUser;
import com.dataprocess.platform.repository.PlatformRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final PlatformRepository repository;
    private final SystemLogService logService;

    public AuthService(PlatformRepository repository, SystemLogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    public OperationResult login(String username, String password) {
        return repository.findUser(username)
                .filter(user -> "enabled".equalsIgnoreCase(user.status()))
                .filter(user -> user.password().equals(password))
                .map(user -> {
                    logService.log(username, "登录", "账号", "成功");
                    return OperationResult.success("登录成功");
                })
                .orElseGet(() -> {
                    logService.log(username, "登录", "账号", "失败");
                    return OperationResult.fail("用户名或密码不正确");
                });
    }

    public OperationResult register(String username, String password, String confirmPassword, boolean agreed) {
        if (username == null || username.isBlank()) {
            return OperationResult.fail("用户名不能为空");
        }
        if (password == null || password.isBlank()) {
            return OperationResult.fail("密码不能为空");
        }
        if (!password.equals(confirmPassword)) {
            return OperationResult.fail("两次输入的密码不一致");
        }
        if (!agreed) {
            return OperationResult.fail("请先同意服务条款与隐私政策");
        }
        if (repository.findUser(username).isPresent()) {
            return OperationResult.fail("用户名已存在");
        }
        repository.createUser(username, password, "user");
        logService.log(username, "注册", "账号", "成功");
        return OperationResult.success("注册成功，请登录");
    }

    public SysUser user(String username) {
        return repository.findUser(username).orElseThrow();
    }
}
