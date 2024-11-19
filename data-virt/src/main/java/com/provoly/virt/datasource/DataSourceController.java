package com.provoly.virt.datasource;

import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.provoly.clients.DataSourceService;
import com.provoly.common.datasource.DataSourceDetailsDto;
import com.provoly.common.datasource.Search;
import com.provoly.common.item.GeoFormat;
import com.provoly.common.item.ItemsSearchResultDto;
import com.provoly.common.search.*;
import com.provoly.common.user.Role;
import com.provoly.virt.item.DataSourceItemsService;
import com.provoly.virt.search.mono.MonoMapper;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

@Produces(MediaType.APPLICATION_JSON)
@Path("/data-sources")
@Consumes(MediaType.APPLICATION_JSON)
public class DataSourceController {

    private DataSourceService dataSourceService;
    private DataSourceItemsService dataSourceItemsService;
    private MonoMapper mapper;

    DataSourceController(@RestClient DataSourceService dataSourceService, DataSourceItemsService dataSourceItemsService,
            MonoMapper mapper) {
        this.dataSourceService = dataSourceService;
        this.dataSourceItemsService = dataSourceItemsService;
        this.mapper = mapper;
    }

    public static class AggregationParameters {
        @RestPath
        UUID dataSourceId;
        @RestQuery
        Double interval;

        @RestQuery
        String dateInterval;

        @RestQuery
        UUID aggregatedBy;

        @RestQuery
        String operation;

        @RestQuery
        UUID valueField;

        @RestQuery
        UUID groupBy;

        @RestQuery("order")
        SortAggregate sortAggregate;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    @Path("/id/{dataSourceId}")
    public DataSourceDetailsDto getDataSourceDetail(UUID dataSourceId) {
        return dataSourceService.getDataSourceDetails(dataSourceId);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    @Path("/id/{dataSourceId}/items")
    public ItemsSearchResultDto getItems(UUID dataSourceId,
            @RestQuery("order") SortDto sort,
            @RestQuery("filter") List<FilterDto> filters,
            @QueryParam("limit") int limit,
            @QueryParam("excludeGeo") boolean excludeGeo,
            @QueryParam("withSourceItems") boolean withSourceItems,
            @QueryParam("withDestinationItems") boolean withDestinationItems) {
        var searchRequest = new MonoClassRequestDto(null, null);
        searchRequest.setLimit(limit);
        searchRequest.setExcludeGeo(excludeGeo);
        searchRequest.setWithSourceItems(withSourceItems);
        searchRequest.setWithDestinationItems(withDestinationItems);
        var result = mapper.toDto(dataSourceItemsService.getItems(dataSourceId, sort, filters, searchRequest));
        return new ItemsSearchResultDto(result, GeoFormat.GEO_JSON);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    @Path("/id/{dataSourceId}/items")
    public ItemsSearchResultDto getItemsSearch(UUID dataSourceId,
            @RestQuery("order") SortDto sort,
            @RestQuery("filter") List<FilterDto> filters,
            SearchRequestDto request) {
        var result = mapper.toDto(dataSourceItemsService.getItems(dataSourceId, sort, filters, request));
        return new ItemsSearchResultDto(result, request.getGeoFormat());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.STR_SEARCH, Role.STR_DATASOURCE_READ })
    @Path("/id/{dataSourceId}/items/aggregate")
    public AggregationResultDto getItemsAggregate(@BeanParam AggregationParameters parameters,
            @RestQuery("filter") List<FilterDto> filters,
            @QueryParam("excludeGeo") boolean excludeGeo,
            @QueryParam("limit") int limit) {
        var aggregationParam = new AggregationParamDto(parameters.aggregatedBy,
                parameters.interval,
                DateInterval.from(parameters.dateInterval),
                AggregateOperation.from(parameters.operation),
                parameters.valueField,
                parameters.groupBy,
                parameters.sortAggregate);
        return dataSourceItemsService.getAggregationResult(parameters.dataSourceId, aggregationParam, filters, excludeGeo,
                limit);
    }

    @POST
    @RolesAllowed({ Role.STR_DATASOURCE_READ })
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/values")
    public List<String> searchForAttributeValues(Search search) {
        return dataSourceItemsService.searchForAttributeValues(search);
    }
}
