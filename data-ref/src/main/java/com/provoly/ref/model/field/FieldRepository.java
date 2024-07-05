package com.provoly.ref.model.field;

import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import com.provoly.common.model.field.FieldDto;
import com.provoly.ref.entity.EntityId_;
import com.provoly.ref.entity.EntityNamed_;
import com.provoly.ref.model.AttributeDef_;
import com.provoly.ref.model.OClass;
import com.provoly.ref.model.OClass_;

@ApplicationScoped
@Transactional
public class FieldRepository {
    private EntityManager em;

    public FieldRepository(EntityManager em) {
        this.em = em;
    }

    public Collection<Field> getFieldForClass(UUID id) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Field.class);
        var classRoot = q.from(OClass.class);
        var fieldRoot = classRoot.join(OClass_.attributes).join(AttributeDef_.field);
        q.select(fieldRoot).distinct(true).where(cb.equal(classRoot.get(OClass_.id), id));
        return em.createQuery(q).getResultList();
    }

    public boolean fieldExists(UUID fieldId) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var root = q.from(Field.class);
        q.select(cb.count(root));
        q.where(cb.equal(root.get(Field_.id), fieldId));
        return em.createQuery(q).getSingleResult() > 0;
    }

    public boolean nameAlreadyExists(FieldDto fieldDto) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var metadataRoot = q.from(Field.class);
        q.select(cb.count(metadataRoot));
        q.where(
                cb.equal(metadataRoot.get(EntityId_.id), fieldDto.getId()),
                cb.equal(metadataRoot.get(EntityNamed_.name), fieldDto.getName()));
        return em.createQuery(q).getSingleResult() > 0;
    }
}
