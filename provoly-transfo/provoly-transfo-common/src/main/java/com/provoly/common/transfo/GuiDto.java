package com.provoly.common.transfo;

import java.io.Serializable;
import java.util.Collection;

public record GuiDto(Collection<Integer> pos, Collection<Integer> size) implements Serializable {
}
