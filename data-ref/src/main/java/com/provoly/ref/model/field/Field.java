package com.provoly.ref.model.field;

import java.util.UUID;

import jakarta.persistence.*;

import com.provoly.common.model.Type;
import com.provoly.ref.entity.EntitySlug;

import org.hibernate.annotations.DiscriminatorFormula;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case " +
        "when type in ('INTEGER','LONG') then 'NUMERIC' " +
        "when type = 'INSTANT' then 'DATE' " +
        "when type = 'DECIMAL' then 'DECIMAL' " +
        "when type in ('POINT', 'MULTIPOINT', 'LINESTRING', 'MULTILINESTRING', 'POLYGON', 'MULTIPOLYGON') then 'GEO' " +
        "else 'DEFAULT' end")
@DiscriminatorValue("DEFAULT")
public class Field extends EntitySlug {
    @Enumerated(EnumType.STRING)
    private Type type;

    protected Field() {
        super();
    }

    protected Field(UUID id) {
        super(id);
    }

    public Field(UUID id, String name, String slug, Type type) {
        super(id);
        this.slug = slug;
        this.name = name;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
