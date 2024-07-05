package com.provoly.ref.datasetversion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import com.provoly.common.dataset.DatasetState;
import com.provoly.common.search.Direction;
import com.provoly.ref.dataset.Dataset;
import com.provoly.ref.dataset.Dataset_;

public record DatasetVersionGetAllParams(Integer limit, Integer offset, Instant dateMax, Instant dateMin,
        UUID datasetId, DatasetState state, DatasetVersionOrderBy orderBy,
        Direction sortBy) {

    public static final DatasetVersionGetAllParams NONE_FILTERS = new DatasetVersionGetAllParams(null, null, null, null,
            null, null, null, null);

    public Order getOrderBy(CriteriaBuilder cb, Root<DatasetVersion> rootQuery, From<DatasetVersion, Dataset> fromDataset) {

        Path<?> criteriaPath = orderBy == null ? DatasetVersionOrderBy.DEFAULT.getCriteriaPath(rootQuery, fromDataset)
                : orderBy.getCriteriaPath(rootQuery, fromDataset);

        Function<Path<?>, Order> orderFunction;
        if (sortBy == null || sortBy == Direction.asc) {
            orderFunction = cb::asc;
        } else {
            orderFunction = cb::desc;
        }

        return orderFunction.apply(criteriaPath);
    }

    public void addPaginationOptions(TypedQuery<DatasetVersion> query) {
        if (limit != null) {
            query.setMaxResults(limit);
        }
        if (offset != null) {
            query.setFirstResult(offset);
        }
    }

    public Predicate getFilters(CriteriaBuilder cb, Root<DatasetVersion> rootQuery) {
        List<Predicate> filters = new ArrayList<>();
        if (dateMax != null) {
            filters.add(cb.lessThanOrEqualTo(rootQuery.get(DatasetVersion_.lastModified), dateMax));
        }
        if (dateMin != null) {
            filters.add(cb.greaterThanOrEqualTo(rootQuery.get(DatasetVersion_.lastModified), dateMin));
        }
        if (datasetId != null) {
            filters.add(cb.equal(rootQuery.get(DatasetVersion_.dataset).get(Dataset_.id), datasetId));
        }
        if (state != null) {
            filters.add(cb.equal(rootQuery.get(DatasetVersion_.state), state));
        }
        return cb.and(filters.toArray(new Predicate[0]));
    }
}
