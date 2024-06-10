package com.provoly.ref.abac;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import com.provoly.common.Constant;
import com.provoly.common.abac.AbacRuleType;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.error.ProvolyNotFoundException;
import com.provoly.ref.entity.EntityIdRepository;
import com.provoly.ref.model.OClass;
import com.provoly.ref.searchrequest.AttributeCondition;
import com.provoly.ref.searchrequest.ComposedCondition;
import com.provoly.ref.searchrequest.Condition;

import org.jboss.logging.Logger;

@ApplicationScoped
public class AbacService {

    private Logger log;
    private EntityIdRepository entityIdRepository;
    private EntityManager em;

    AbacService(Logger log, EntityManager em, EntityIdRepository entityIdRepository) {
        this.log = log;
        this.em = em;
        this.entityIdRepository = entityIdRepository;
    }

    @Transactional
    public Collection<AbacRule> getAllForClass(UUID oClassId) {
        log.infof("get All rules for class %s", oClassId);
        OClass oClass = entityIdRepository.getLinkedById(oClassId, OClass.class);

        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(AbacRule.class);
        var root = q.from(AbacRule.class);
        var isAttributeType = cb.equal(root.get(AbacRule_.O_CLASS), oClass);
        var isMetadataType = cb.equal(root.get(AbacRule_.TYPE), AbacRuleType.METADATA);

        q.where(cb.or(isAttributeType, isMetadataType));
        return em.createQuery(q).getResultList();
    }

    @Transactional
    public Collection<AbacRule> getActiveForClass(UUID oClassId) {
        log.infof("get All actives rules for class %s", oClassId);
        Collection<AbacRule> getAll = getAllForClass(oClassId);
        return getAll.stream().filter(AbacRule::isActive).collect(Collectors.toList());
    }

    @Transactional
    public void save(AbacRule rule) {
        log.infof("Save Abac rule with type : %s", rule.getType());
        Set<UUID> oClassIds = new HashSet<>();
        checkSameTypeAndOClass(rule.getType(), rule.getCondition(), oClassIds);
        if (!oClassIds.isEmpty()) {
            UUID id = oClassIds.iterator().next();
            log.infof("Rule type is ATTRIBUTE, set oClass with id %s", id);
            rule.setoClass(entityIdRepository.findById(id, OClass.class));
        }
        var old = em.find(AbacRule.class, rule.getId());
        if (old != null) {
            em.remove(old.getCondition()); // FIXME : Condition mapping has been changed to onetoone. This seems no longer necessary
            log.debugf("Deleted old condition=[%s]", old.getCondition());
        }
        entityIdRepository.saveEntity(rule);
    }

    private void checkSameTypeAndOClass(AbacRuleType ruleType, Condition condition, Set<UUID> oClassIds) {
        switch (condition.type) {
            case AND, OR:
                ComposedCondition composedCondition = (ComposedCondition) condition;
                log.debugf("condition %s loop over composed", condition.type.name());
                for (Condition inner : composedCondition.composed) {
                    checkSameTypeAndOClass(ruleType, inner, oClassIds);
                }
                break;
            case ATTRIBUTE:
                if (ruleType != AbacRuleType.ATTRIBUTE) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "RuleType " + ruleType + " does not allowed Attribute condition");
                }
                OClass oClass = ((AttributeCondition) condition).getAttribute().getOclass();
                oClassIds.add(oClass.getId());
                if (oClassIds.size() > 1) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "Attributes are not on same OClass");
                }
                break;

            case METADATA:
                if (ruleType != AbacRuleType.METADATA) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "RuleType " + ruleType + " does not allowed Metadata condition");
                }
                break;
            case TRUE:
                break;
            default:
                throw new UnsupportedOperationException("Unknown condition type " + condition.type);
        }
    }

    @Transactional
    public void deleteRule(UUID id) {
        log.infof("delete rule with id %s", id);
        entityIdRepository.removeEntity(id, AbacRule.class);
    }

    @Transactional
    public void saveContextVariable(ContextVariable abacVariableContext) {
        em.merge(abacVariableContext);
    }

    @Transactional
    public Collection<ContextVariable> getAllContextVariable() {
        var q = em.getCriteriaBuilder().createQuery(ContextVariable.class);
        q.select(q.from(ContextVariable.class));
        return em.createQuery(q).getResultList();
    }

    @Transactional
    public ContextVariable getContextVariable(String name) throws BusinessException {
        var abacContextVariable = em.find(ContextVariable.class, name);
        if (abacContextVariable == null) {
            throw new ProvolyNotFoundException(ContextVariable.class, "name", name);
        }
        return abacContextVariable;
    }

    @Transactional
    public void deleteContextVariable(String name) throws BusinessException {
        for (AbacRule rule : getAllRules()) {
            if (rule.getPredicate().getValue().contains(Constant.VARIABLE_PREFIX + "." + name)) {
                throw new BusinessException(ErrorCode.NOT_MODIFIABLE, "this context is used in rule " + rule.getId());
            }
        }
        em.remove(getContextVariable(name));
    }

    public Collection<AbacRule> getAllRules() {
        return entityIdRepository.getAll(AbacRule.class);
    }

    public AbacRule getRule(UUID ruleId) {
        return entityIdRepository.getById(ruleId, AbacRule.class);
    }

}