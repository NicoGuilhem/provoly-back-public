package com.provoly.common.dataset;

import java.time.Instant;

public record DatasetVersionInformationsDto(String producer, Instant productionDate, String additionalInformation) {
    public DatasetVersionInformationsDto(String producer, Instant productionDate) {
        this(producer, productionDate, null);
    }
}
