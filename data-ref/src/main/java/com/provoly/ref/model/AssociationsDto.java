package com.provoly.ref.model;

import java.util.List;

public record AssociationsDto(List<AssociationDto> associations, boolean usedElsewhere) {
}
