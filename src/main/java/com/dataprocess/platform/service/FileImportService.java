package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.ParsedTable;
import com.dataprocess.platform.dto.UploadPreview;
import com.dataprocess.platform.repository.PlatformRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileImportService {
    private final DataPreviewService dataPreviewService;
    private final PlatformRepository repository;
    private final SystemLogService logService;
    private final Map<String, UploadPreview> previews = new ConcurrentHashMap<>();

    public FileImportService(DataPreviewService dataPreviewService, PlatformRepository repository, SystemLogService logService) {
        this.dataPreviewService = dataPreviewService;
        this.repository = repository;
        this.logService = logService;
    }

    public UploadPreview preview(MultipartFile file, Long sourceId, String businessType, String user) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("请选择需要上传的文件");
        }
        if (file.getSize() > 100L * 1024L * 1024L) {
            throw new IllegalArgumentException("文件大小不能超过 100MB");
        }
        String original = Path.of(file.getOriginalFilename() == null ? "upload.dat" : file.getOriginalFilename()).getFileName().toString();
        String token = UUID.randomUUID().toString().replace("-", "");
        Path target = Path.of("data", "uploads", token + "-" + original);
        Files.createDirectories(target.getParent());
        file.transferTo(target);
        ParsedTable table = dataPreviewService.parse(target);
        UploadPreview preview = new UploadPreview(token, original, sourceId, blankDefault(businessType), table);
        previews.put(token, preview);
        logService.log(user, "上传预览", original, "预览 " + table.totalRows() + " 行");
        return preview;
    }

    public long confirm(String token, String user) {
        UploadPreview preview = previews.remove(token);
        if (preview == null) {
            throw new IllegalArgumentException("预览已失效，请重新上传文件");
        }
        ParsedTable table = preview.table();
        long batchId = repository.addImportBatch(preview.sourceId(), preview.fileName(), fileType(preview.fileName()),
                blankDefault(preview.businessType()), table.totalRows(), table.headers().size(), "success", user);
        for (int i = 0; i < table.rows().size(); i++) {
            repository.addImportRecord(batchId, i + 1, table.rows().get(i));
        }
        logService.log(user, "确认导入", preview.fileName(), "批次#" + batchId + " 写入 " + table.totalRows() + " 行");
        return batchId;
    }

    private String blankDefault(String value) {
        return value == null || value.isBlank() ? "系统" : value;
    }

    private String fileType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        int index = lower.lastIndexOf('.');
        return index >= 0 ? lower.substring(index + 1) : "unknown";
    }
}
