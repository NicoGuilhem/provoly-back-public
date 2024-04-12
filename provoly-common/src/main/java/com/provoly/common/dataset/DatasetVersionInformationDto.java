package com.provoly.common.dataset;

import java.time.Instant;

public record DatasetVersionInformationDto(String producer, Instant productionDate, String additionalInformation) {
    public DatasetVersionInformationDto(String producer, Instant productionDate) {
        this(producer, productionDate, null);
    }
}
