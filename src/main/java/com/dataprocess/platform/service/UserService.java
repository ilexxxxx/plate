package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.OperationResult;
import com.dataprocess.platform.model.SysUser;
import com.dataprocess.platform.repository.PlatformRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final PlatformRepository repository;
    private final SystemLogService logService;

    public UserService(PlatformRepository repository, SystemLogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    public List<SysUser> listUsers() {
        return repository.listUsers();
    }

    public OperationResult resetPassword(SysUser operator, long userId, String password) {
        if (operator == null || !operator.admin()) {
            return OperationResult.fail("只有管理员可以重置用户密码");
        }
        if (password == null || password.isBlank()) {
            return OperationResult.fail("新密码不能为空");
        }
        repository.resetPassword(userId, password);
        logService.log(operator.username(), "重置密码", "用户#" + userId, "成功");
        return OperationResult.success("密码已重置");
    }
}
