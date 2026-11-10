package za.ac.skiet.vyou2.service;

import java.util.List;
import java.util.function.Function;

/** A page of results, independent of Spring Data, so paging types don't leak into the API layer. */
public record PageResult<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public PageResult {
        items = List.copyOf(items);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalItems, totalPages);
    }
}
