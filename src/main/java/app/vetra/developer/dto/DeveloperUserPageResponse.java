package app.vetra.developer.dto;

import java.util.List;

/** Paginated response wrapper for developer user directory. */
public record DeveloperUserPageResponse(
    List<DeveloperUserDto> users,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious) {}
