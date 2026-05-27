package com.dataprocess.platform.service;

import com.dataprocess.platform.dto.LedgerCard;
import com.dataprocess.platform.model.ImportBatch;
import com.dataprocess.platform.repository.PlatformRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LedgerService {
    private final PlatformRepository repository;

    public LedgerService(PlatformRepository repository) {
        this.repository = repository;
    }

    public List<LedgerCard> cards() {
        return repository.ledgerCards();
    }

    public List<ImportBatch> batches(String businessType, String keyword) {
        return repository.listBatches(businessType, keyword);
    }
}
