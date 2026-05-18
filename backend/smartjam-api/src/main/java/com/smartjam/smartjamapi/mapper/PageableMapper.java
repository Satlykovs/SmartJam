package com.smartjam.smartjamapi.mapper;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PageableMapper {
    public Pageable toPageable(Integer page, Integer size, String sort) {
        String sortParam = (sort == null || sort.isBlank()) ? "createdAt,desc" : sort;
        String[] argsSort = sortParam.split(",\\s*", 2);
        String field = argsSort[0].isBlank() ? "createdAt" : argsSort[0];

        Sort.Direction direction =
                (argsSort.length > 1 && argsSort[1].equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, field));
    }
}
