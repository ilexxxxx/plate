package com.dataprocess.platform.service;

import com.dataprocess.platform.model.DataSourceInfo;
import com.dataprocess.platform.repository.PlatformRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DataSourceService {
    private static final List<String> STATUSES = List.of("在线", "待导入", "模拟", "已刷新");

    private final PlatformRepository repository;
    private final SystemLogService logService;

    public DataSourceService(PlatformRepository repository, SystemLogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    public List<DataSourceInfo> list() {
        return repository.listDataSources();
    }

    public void add(String name, String type, String connInfo, String user) {
        String status = "Excel".equalsIgnoreCase(type) || "CSV".equalsIgnoreCase(type) ? "待导入" : "模拟";
        repository.addDataSource(name, type, connInfo, status);
        logService.log(user, "新增数据源", name, "保存成功");
    }

    public void refresh(long id, String user) {
        String status = STATUSES.get(ThreadLocalRandom.current().nextInt(STATUSES.size()));
        repository.updateDataSourceStatus(id, status);
        logService.log(user, "刷新状态", "数据源#" + id, status);
    }
}
