package com.provoly.virt.search;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.provoly.clients.ProvolyUserService;
import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.search.*;
import com.provoly.virt.DataVirtProperties;
import com.provoly.virt.entity.ItemsSearchResult;
import com.provoly.virt.search.mono.MonoClassSearchService;
import com.provoly.virt.search.mono.MonoMapper;
import com.provoly.virt.search.multi.MultiClassSearchService;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiEmitter;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class SearchService {

    private MonoClassSearchService monoClassSearchService;
    private MultiClassSearchService multiClassSearchService;
    private ProvolyUserService provolyUserService;
    private DataVirtProperties dataVirtProperties;

    private MonoMapper mapper;

    public SearchService(MonoClassSearchService monoClassSearchService,
            MultiClassSearchService multiClassSearchService,
            @RestClient ProvolyUserService provolyUserService,
            DataVirtProperties dataVirtProperties,
            MonoMapper mapper) {
        this.monoClassSearchService = monoClassSearchService;
        this.multiClassSearchService = multiClassSearchService;
        this.provolyUserService = provolyUserService;
        this.dataVirtProperties = dataVirtProperties;
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
}