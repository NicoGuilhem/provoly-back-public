package com.provoly.ref.link;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.model.AttributeDef;
import com.provoly.ref.relation.RelationType;

@ApplicationScoped
public class LinkService {

    private EntityIdRepository entityIdRepository;
    private EntityManager em;

    LinkService(EntityIdRepository entityIdRepository, EntityManager em) {
        this.entityIdRepository = entityIdRepository;
        this.em = em;
    }

    @Transactional
    public void save(Link link) {
        checkLinkAlreadyExists(link);
        entityIdRepository.saveEntity(link);
    }

    @Transactional
    public Link getById(UUID id) {
        return entityIdRepository.getById(id, Link.class);
    }

    @Transactional
    public Collection<Link> getAll() {
        return entityIdRepository.getAll(Link.class);
    }

    @Transactional
    public void delete(UUID id) {
        entityIdRepository.removeEntity(id, Link.class);
    }

    @Transactional
    public int countLinkByRelationType(RelationType relationType) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Long.class);
        var root = q.from(Link.class);

        q.select(cb.count(root)).where(cb.equal(root.get(Link_.relationType), relationType));
        return em.createQuery(q).getSingleResult().intValue();
    }

    @Transactional
    public Collection<Link> getAllLinksByAttribute(AttributeDef attributeDef) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Link.class);
        var root = q.from(Link.class);

        Predicate sourcePredicate = cb.equal(root.get(Link_.ATTRIBUTE_SOURCE), attributeDef);
        Predicate destinationPredicate = cb.equal(root.get(Link_.ATTRIBUTE_DESTINATION), attributeDef);

        q.where(cb.or(sourcePredicate, destinationPredicate));
        return em.createQuery(q).getResultList();
    }

    private void checkLinkAlreadyExists(Link link) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Link.class);
        var root = q.from(Link.class);

        Predicate sourcePredicate = cb.equal(root.get(Link_.ATTRIBUTE_SOURCE), link.getAttributeSource());
        Predicate destinationPredicate = cb.equal(root.get(Link_.ATTRIBUTE_DESTINATION), link.getAttributeDestination());

        q.where(sourcePredicate, destinationPredicate);

        var links = em.createQuery(q).getResultList();

        if (!links.isEmpty()) {
            var existingLink = links.get(0);
            if (!existingLink.equals(link)) { // it's not an update
                String message = MessageFormat.format("Link from {0} to {1} already exists",
                        link.getAttributeSource(), link.getAttributeDestination());
                throw new BusinessException(ErrorCode.ID_ALREADY_USED, message);
            }
        }
    }

}
