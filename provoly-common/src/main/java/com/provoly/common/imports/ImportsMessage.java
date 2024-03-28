package com.provoly.common.imports;

import java.util.List;
import java.util.UUID;

public record ImportsMessage(UUID datasetVersionId, String recordId, List<ExtractedMessage> messages) {
}
