package com.provoly.virt.search.mono;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.el.ExpressionFactory;
import jakarta.el.StandardELContext;
import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.abac.ContextVariableDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.GeoFormat;
import com.provoly.common.search.AttributeConditionDto;
import com.provoly.common.search.ComposedConditionDto;
import com.provoly.common.search.ConditionDto;
import com.provoly.common.search.MetadataConditionDto;
import com.provoly.virt.GeoHolder;

import io.quarkus.runtime.annotations.RegisterForReflection;

import org.jboss.logging.Logger;

import com.sun.el.ExpressionFactoryImpl;

@ApplicationScoped
@RegisterForReflection(targets = { ExpressionFactoryImpl.class, String.class })
public class PredicateEvaluator {
    private Logger log;
    private AbacContext abacContext;

    public PredicateEvaluator(Logger log, AbacContext abacContext) {
        this.log = log;
        this.abacContext = abacContext;
    }

    public <T> T evaluate(String value, Class<T> clazz, Collection<ContextVariableDto> contextVariableDtos) {
        if (value == null)
            return null;

        log.tracef("Evaluate value %s", value);
        ExpressionFactory factory = ExpressionFactory.newInstance();
        StandardELContext context = new StandardELContext(factory);
        context.getVariableMapper().setVariable("user", factory.createValueExpression(abacContext.getUser(), User.class));
        context.getVariableMapper().setVariable("request",
                factory.createValueExpression(abacContext.getRequest(), ProvolyRequest.class));

        var contextVariables = contextVariableDtos
                .stream()
                .collect(Collectors.toMap(c -> c.name, c -> c.value));

        context.getVariableMapper().setVariable("var",
                factory.createValueExpression(contextVariables, Map.class));

        var expression = factory.createValueExpression(context, value, clazz);
        var result = expression.getValue(context);
        if (!clazz.isInstance(result)) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Predicate evaluator expecting a result of type %s but result is of type %s"
                            .formatted(clazz, result.getClass()));
        }
        return clazz.cast(result);
    }

    public void evaluate(ConditionDto condition, Collection<ContextVariableDto> contextVariableDtos) {
        log.tracef("Evaluate condition %s", condition.type);
        switch (condition.type) {
            case OR, AND -> evaluate((ComposedConditionDto) condition, contextVariableDtos);
            case ATTRIBUTE -> evaluate((AttributeConditionDto) condition, contextVariableDtos);
            case METADATA -> evaluate((MetadataConditionDto) condition, contextVariableDtos);
            case TRUE -> {
            }
            default -> throw new IllegalStateException("Unknown Condition type " + condition.type);
        }
    }

    private void evaluate(ComposedConditionDto condition, Collection<ContextVariableDto> contextVariableDtos) {
        log.tracef("Evaluate composed condition with size : %s", condition.composed.size());
        condition.composed
                .forEach(c -> evaluate(c, contextVariableDtos));
    }

    private void evaluate(AttributeConditionDto condition, Collection<ContextVariableDto> contextVariableDtos) {
        log.trace("Evaluate attribute condition");
        new GeoHolder(condition.getValue(), GeoFormat.GEO_JSON);
        condition.setValue(evaluate(condition.getValue(), String.class, contextVariableDtos));
        condition.setLocation(evaluate(condition.getLocation(), String.class, contextVariableDtos));
    }

    private void evaluate(MetadataConditionDto condition, Collection<ContextVariableDto> contextVariableDtos) {
        log.trace("Evaluate metadata condition");
        String evaluated = evaluate(condition.getValue(), String.class, contextVariableDtos);
        condition.setValue(evaluated);
    }

}
