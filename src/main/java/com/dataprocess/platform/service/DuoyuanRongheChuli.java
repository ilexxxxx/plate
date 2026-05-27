package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.LedgerCard;
import com.dataprocess.platform.dto.ParsedTable;
import com.dataprocess.platform.dto.QualityCheckItem;
import com.dataprocess.platform.dto.StatisticsSummary;
import com.dataprocess.platform.model.FusionRecord;
import com.dataprocess.platform.model.StatReport;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class DuoyuanRongheChuli {
    private final DataPreviewService dataPreviewService;
    private final FusionService fusionService;
    private final StatisticsService statisticsService;
    private final QualityCheckService qualityCheckService;
    private final LedgerService ledgerService;
    private final ReportExportService reportExportService;

    public DuoyuanRongheChuli(DataPreviewService dataPreviewService,
                              FusionService fusionService,
                              StatisticsService statisticsService,
                              QualityCheckService qualityCheckService,
                              LedgerService ledgerService,
                              ReportExportService reportExportService) {
        this.dataPreviewService = dataPreviewService;
        this.fusionService = fusionService;
        this.statisticsService = statisticsService;
        this.qualityCheckService = qualityCheckService;
        this.ledgerService = ledgerService;
        this.reportExportService = reportExportService;
    }

    public ParsedTable chuliExcelDaoru(String filePath, String tableName) throws IOException {
        return dataPreviewService.parse(Path.of(filePath));
    }

    public FusionRecord zhixingQingxi(long batchId, String user) {
        return fusionService.run(batchId, user);
    }

    public StatisticsSummary zhixingTongji(Long batchId) {
        return statisticsService.summary(batchId);
    }

    public List<QualityCheckItem> zhixingJianyan(Long batchId, String user) {
        return qualityCheckService.runFullCheck(batchId, user);
    }

    public List<LedgerCard> chaxunTaizhang() {
        return ledgerService.cards();
    }

    public StatReport daochuBaobao(Long batchId, String scope, String format, String user) throws IOException {
        return reportExportService.export(batchId, scope, format, user);
    }
}
