package com.provoly.virt.search;

import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.ProvolyUserService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.metadata.MetadataSystem;
import com.provoly.common.search.*;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.entity.Relation;
import com.provoly.virt.search.mono.MonoClassSearchService;
import com.provoly.virt.search.mono.MonoMapper;
import com.provoly.virt.search.multi.MultiClassSearchService;
import com.provoly.virt.storage.StorageRelationAdapters;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiEmitter;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SearchService {

    private Logger logger;
    private MonoClassSearchService monoClassSearchService;
    private MultiClassSearchService multiClassSearchService;
    private ProvolyUserService provolyUserService;
    private DataVirtProperties dataVirtProperties;
    private StorageRelationAdapters storageRelationAdapters;

    private MonoMapper mapper;

    public SearchService(Logger logger,
            MonoClassSearchService monoClassSearchService,
            MultiClassSearchService multiClassSearchService,
            @RestClient ProvolyUserService provolyUserService,
            DataVirtProperties dataVirtProperties,
            StorageRelationAdapters storageRelationAdapters,
            MonoMapper mapper) {
        this.logger = logger;
        this.monoClassSearchService = monoClassSearchService;
        this.multiClassSearchService = multiClassSearchService;
        this.provolyUserService = provolyUserService;
        this.dataVirtProperties = dataVirtProperties;
        this.storageRelationAdapters = storageRelationAdapters;
        this.mapper = mapper;
    }

    public ItemsSearchResult search(SearchRequestDto request, SortDto sort) {
        if (request.getType() == SearchRequestType.MONO_CLASS) {
            updateRequestWithEffectiveSort((MonoClassRequestDto) request, sort);
        }
        return search(request);
    }

    private int checkOrGetDefault(int limit) {
        if (limit == 0) {
            return dataVirtProperties.searchLimit();
        }
        if (limit < 0 || limit > dataVirtProperties.maxSizeLimit()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Limit can't be negative or exceed %s.".formatted(dataVirtProperties.maxSizeLimit()));
        }
        return limit;
    }

    public ItemsSearchResult search(SearchRequestDto request) {

        var relationCondition = buildWithRelationCondition(request);
        if (relationCondition != null) {
            // if the relation condition is empty, we can return an empty result (non relation matches the requested one)
            if (relationCondition.composed.isEmpty()) {
                return new ItemsSearchResult();
            }
            // else we merge the relation condition with the existing condition
            MonoClassRequestDto monoRequest = (MonoClassRequestDto) request;
            AndConditionDto andCondition = new AndConditionDto();
            andCondition.composed.add(relationCondition);
            if (monoRequest.getCondition() != null) {
                logger.debug("Merging condition from SearchRequest with the relation condition");
                andCondition.composed.add(monoRequest.getCondition());
            }
            monoRequest.setCondition(andCondition);
        }

        request.setLimit(checkOrGetDefault(request.getLimit()));
        return switch (request) {
            case MonoClassRequestDto monoRequest -> {
                if (monoRequest.getSort() == null && monoRequest.getSearchAfter() != null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "Pagination isn't possible without sorting");
                }
                yield monoClassSearchService.search(monoRequest);
            }
            case MultiClassRequestDto multiRequest ->
                multiClassSearchService.search(multiRequest);
        };
    }

    public ItemsSearchResult searchByNamedQuery(UUID nameQueryId, SortDto sort, int limit) {
        var namedQuery = provolyUserService.getNamedQueryById(nameQueryId);
        if (namedQuery.getRequest().getType() == SearchRequestType.MONO_CLASS) {
            updateRequestWithEffectiveSort((MonoClassRequestDto) namedQuery.getRequest(), sort);
        }
        var searchResult = search(namedQuery.getRequest());
        provolyUserService.updateNamedQueryExecution(nameQueryId);
        return searchResult;
    }

    public Multi<ItemsSearchResult> searchAll(MonoClassRequestDto request, SortDto sort) {
        updateRequestWithEffectiveSort(request, sort);
        if (sort == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "It's not possible to search all items without sorting");
        }

        return Multi.createFrom().emitter(emitter -> searchAsync(emitter, request)
                .subscribe()
                .with(result -> completeOrContinuePaginating(result, emitter, request)));
    }

    public Uni<ItemsSearchResult> searchAsync(MultiEmitter emitter, MonoClassRequestDto request) {
        return Uni.createFrom()
                .voidItem()
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transform(ignored -> search(request))
                .onFailure().invoke(emitter::fail);
    }

    public void updateRequestWithEffectiveSort(MonoClassRequestDto request, SortDto sort) {
        request.setSort(sort != null ? sort : request.getSort());
    }

    private void completeOrContinuePaginating(ItemsSearchResult result,
            MultiEmitter emitter,
            MonoClassRequestDto request) {
        if (!result.getItems().isEmpty()) {
            emitter.emit(result);
            request.setSearchAfter(mapper.map(result.getSearchAfter()));
            searchAsync(emitter, request).subscribe()
                    .with(follow -> completeOrContinuePaginating(follow, emitter, request));
        } else {
            emitter.complete();
        }
    }

    /**
     * Builds an instance of {@see OrConditionDto} to filter on the relation requested
     *
     * @param request the current search request
     * @return an instance of {@see OrConditionDto} or null if the request doesn't contain any filter on relation
     * @throws BusinessException if the request is a multi class request
     */
    private OrConditionDto buildWithRelationCondition(SearchRequestDto request) {
        if (request.getWithRelation() == null) {
            return null;
        }

        if (!(request instanceof MonoClassRequestDto)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Filtering on multi class request is not available");
        }

        logger.debugf("Loading all relations for the relation %s", request.getWithRelation());
        Collection<Relation> relationsByItemAndRelation = storageRelationAdapters
                .getRelationsByItemAndRelation(request.getWithRelation());

        logger.debugf("Building orConditionDTO to filter on the %s itemsId", relationsByItemAndRelation.size());
        OrConditionDto itemIdCondition = new OrConditionDto();
        relationsByItemAndRelation.stream().map(relation -> {
            var filteredItem = request.getWithRelation().getSource() != null ? relation.getDestination().getAsString()
                    : relation.getSource().getAsString();
            return new MetadataConditionDto(MetadataSystem.ID, filteredItem, Operator.EQUALS);
        })
                .forEach(itemIdCondition.composed::add);

        return itemIdCondition;
    }
}