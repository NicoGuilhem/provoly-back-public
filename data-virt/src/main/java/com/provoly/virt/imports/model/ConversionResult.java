package com.provoly.virt.imports.model;

import java.util.List;

import com.provoly.common.imports.ExtractedMessage;

public record ConversionResult(ItemRecord record, List<ExtractedMessage> messages) {
}
