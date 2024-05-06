package com.provoly.virt.search.multi;

import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.provoly.clients.ModelService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.CountDto;
import com.provoly.common.model.AttributeDefDetailsDto;
import com.provoly.common.model.OClassDetailsDto;
import com.provoly.common.search.*;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.search.mono.MonoClassSearchService;
import com.provoly.virt.storage.StorageRelationAdapters;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MultiClassSearchService {

    @Inject
    Logger log;

    @Inject
    @RestClient
    ModelService modelService;

    @Inject
    MonoClassSearchService monoClassSearchService;

    @Inject
    StorageRelationAdapters relationService;

    public ItemsSearchResult search(MultiClassRequestDto request) {
        log.infof("Starting a multiclass searchRequest");
        ItemsSearchResult resultAllClass = new ItemsSearchResult();

        Map<UUID, CountDto> totalRemainingItem = new HashMap<>();

        for (OClassDetailsDto oClass : loadOClasses(request.getoClasses())) {
            log.debugf("Build mono class search for class %s/%s", oClass.getName(), oClass.getId());

            ComposedConditionDto searchMonoCondition;
            switch (request.getMultiType()) {
                case AND -> {
                    if (!isAllFieldsInClass(oClass, request.getFields())) {
                        // We're ignoring the current class as no item can satisfy condition
                        continue;
                    }
                    searchMonoCondition = new AndConditionDto();
                }
                case OR -> searchMonoCondition = new OrConditionDto();
                default -> throw new BusinessException(ErrorCode.TECHNICAL, "invalid condition type : AND or OR");
            }

            for (FieldConditionDto field : request.getFields()) {
                List<AttributeDefDetailsDto> allMatchedAttr = oClass.getAttributes()
                        .stream()
                        .filter(attributeDefDto -> attributeDefDto.field.id.equals(field.getField()))
                        .toList();

                log.debugf("fields %s - match attributes %s", field.getField(), allMatchedAttr);

                ComposedConditionDto subCondition = null;
                if (allMatchedAttr.size() == 1) {
                    log.debugf("Exactly one attribute match in this class %s - %s", oClass, allMatchedAttr.get(0).name);
                    subCondition = searchMonoCondition;
                } else if (allMatchedAttr.size() > 1) {
                    log.debugf("More than one field match in this class %s", oClass.getName());
                    subCondition = new OrConditionDto();
                    searchMonoCondition.composed.add(subCondition);
                }
                for (var matched : allMatchedAttr) {
                    AttributeConditionDto attributeCondition = new AttributeConditionDto(matched.id, field.getValue(),
                            field.getUpperValue(), field.getLocation(), field.getOperator());
                    subCondition.composed.add(attributeCondition);
                }
            }

            if (searchMonoCondition.composed.isEmpty()) {
                if (request.getFullSearch() == null) {
                    // No field and no full search => Skip class
                    continue;
                } else {
                    searchMonoCondition = null;
                }
            }

            MonoClassRequestDto monoRequest = new MonoClassRequestDto(oClass.getId(), searchMonoCondition,
                    request.getFullSearch(), request.getLimit());
            ItemsSearchResult resultByClass = monoClassSearchService.search(monoRequest);

            log.debugf("Add %s items to global results", resultByClass.size());
            resultByClass.getItems().forEach(resultAllClass::add);

            request.setLimit(request.getLimit() - resultByClass.size());
            log.debugf("update limit number for next search : %s", request.getLimit());

            if (resultByClass.getCount() != null) {
                totalRemainingItem.putAll(resultByClass.getCount());
            }
        }
        log.infof("Global result size : %s", resultAllClass.size());
        relationService.loadRelations(resultAllClass);

        resultAllClass.setCount(totalRemainingItem);
        return resultAllClass;
    }

    private Collection<OClassDetailsDto> loadOClasses(Collection<UUID> oClasses) {
        if (oClasses.isEmpty()) {
            return modelService.getAllClasses()
                    .stream()
                    .map(oClassReadDto -> modelService.getDetails(oClassReadDto.getId()))
                    .toList();
        } else {
            return oClasses.stream()
                    .map(modelService::getDetails)
                    .toList();
        }

    }

    private boolean isAllFieldsInClass(OClassDetailsDto oClass, Collection<FieldConditionDto> fields) {
        List<UUID> fieldsFromClass = oClass.getAttributes().stream()
                .map(attributeDefDto -> attributeDefDto.field.id)
                .toList();

        return fieldsFromClass.containsAll(fields.stream().map(FieldConditionDto::getField).toList());
    }
}
