package com.provoly.virt.search.mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.StandardELContext;
import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.common.abac.ContextVariableDto;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.search.*;

import io.quarkus.runtime.annotations.RegisterForReflection;

import org.jboss.logging.Logger;

import com.sun.el.ExpressionFactoryImpl;

@ApplicationScoped
@RegisterForReflection(targets = { ExpressionFactoryImpl.class, String.class })
public class PredicateEvaluator {
    private Logger log;
    private AbacContext abacContext;
    private ExpressionFactory elFactory;

    public PredicateEvaluator(Logger log, AbacContext abacContext) {
        this.log = log;
        this.abacContext = abacContext;
        this.elFactory = ExpressionFactory.newInstance();
    }

    /**
     * Evaluate an expression language string which result should be a Boolean (used to evaluate ABAC rule predicate)
     *
     * @param expressionLanguageString the expression language to evaluate
     * @param contextVariableDtos the current context with datas to use for evaluation
     * @return the result of the evaluation of the expression language in the context
     * @see MonoClassSearchService#addSecurity(UUID, MonoClassRequestDto, MonoClassContextRequest)
     */
    public Boolean evaluateAsBoolean(String expressionLanguageString, Collection<ContextVariableDto> contextVariableDtos) {
        StandardELContext context = buildElContext(elFactory, contextVariableDtos);
        return evaluate(expressionLanguageString, Boolean.class, context);
    }

    /**
     * Evaluate an expression language string which result should be a String (used to evaluate query and ABAC rule conditions)
     *
     * @param expressionLanguageString the expression language to evaluate
     * @param context the current context with datas to use for evaluation
     * @return the result of the evaluation of the expression language in the context
     * @see MonoClassSearchService#addSecurity(UUID, MonoClassRequestDto, MonoClassContextRequest)
     */
    private String evaluateAsString(String expressionLanguageString, StandardELContext context) {
        return evaluate(expressionLanguageString, String.class, context);
    }

    /**
     * Evaluate an expression language string which result should be a List of String (used to evaluate query and ABAC rule
     * conditions)
     *
     * @param expressionLanguageString the expression language to evaluate
     * @param context the current context with datas to use for evaluation
     * @return the result of the evaluation of the expression language in the context
     * @see MonoClassSearchService#addSecurity(UUID, MonoClassRequestDto, MonoClassContextRequest)
     */
    private List<String> evaluateAsList(String expressionLanguageString, StandardELContext context) {
        return evaluate(expressionLanguageString, List.class, context);
    }

    private <T> T evaluate(String value, Class<T> clazz, StandardELContext context) {
        if (value == null)
            return null;

        log.tracef("Evaluate value %s", value);
        var expression = elFactory.createValueExpression(context, value, clazz);
        var result = expression.getValue(context);
        if (result != null && !clazz.isInstance(result)) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Predicate evaluator expecting a result of type %s but result is of type %s"
                            .formatted(clazz, result.getClass()));
        }
        return clazz.cast(result);
    }

    public void evaluate(ConditionDto condition, Collection<ContextVariableDto> contextVariableDtos) {
        StandardELContext context = buildElContext(elFactory, contextVariableDtos);
        evaluate(condition, context);
    }

    private void evaluate(ConditionDto condition, StandardELContext context) {
        log.tracef("Evaluate condition %s", condition.type);
        switch (condition.type) {
            case OR, AND -> evaluate((ComposedConditionDto) condition, context);
            case ATTRIBUTE -> evaluate((AttributeConditionDto) condition, context);
            case METADATA -> evaluate((MetadataConditionDto) condition, context);
            case TRUE -> {
            }
            default -> throw new IllegalStateException("Unknown Condition type " + condition.type);
        }
    }

    private void evaluate(ComposedConditionDto condition, StandardELContext context) {
        log.tracef("Evaluate composed condition with size : %s", condition.composed.size());
        condition.composed.forEach(c -> evaluate(c, context));
    }

    private void evaluate(AttributeConditionDto condition, StandardELContext context) {
        log.trace("Evaluate attribute condition");
        String expressionLanguageString = condition.getValue();
        condition.setValue(evaluateAsString(expressionLanguageString, context));
        condition.setLocation(evaluateAsString(condition.getLocation(), context));
        try {
            condition.setValues(evaluateAsList(expressionLanguageString, context));
        } catch (ELException e) {
            log.tracef("value for expression language %s is not a List", expressionLanguageString);
        }
    }

    private void evaluate(MetadataConditionDto condition, StandardELContext context) {
        log.trace("Evaluate metadata condition");
        String expressionLanguageString = condition.getValue();
        condition.setValue(evaluateAsString(expressionLanguageString, context));
        try {
            condition.setValues(evaluateAsList(expressionLanguageString, context));
        } catch (ELException e) {
            log.tracef("value for expression language %s is not a List", expressionLanguageString);
        }
    }

    private StandardELContext buildElContext(ExpressionFactory factory, Collection<ContextVariableDto> contextVariableDtos) {
        StandardELContext context = new StandardELContext(factory);
        context.getVariableMapper().setVariable("user", factory.createValueExpression(abacContext.getUser(), User.class));
        context.getVariableMapper().setVariable("request",
                factory.createValueExpression(abacContext.getRequest(), ProvolyRequest.class));

        var contextVariables = contextVariableDtos
                .stream()
                .collect(Collectors.toMap(c -> c.name, c -> c.value));

        context.getVariableMapper().setVariable("var",
                factory.createValueExpression(contextVariables, Map.class));

        return context;
    }
}
