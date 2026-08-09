package app.vetra.ai.workflow.clinical.clinicalcase.operations.model;

import java.util.List;

/**
 * Reusable, deterministic pagination container.
 *
 * @param <T> item type
 * @param items paginated list of items
 * @param page 1-indexed current page number
 * @param pageSize requested page size
 * @param totalItems total available item count across all pages
 * @param totalPages total calculated pages count
 * @param hasNext true if subsequent page exists
 * @param hasPrevious true if previous page exists
 */
public record PageResult<T>(
    List<T> items,
    int page,
    int pageSize,
    long totalItems,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious) {

  /**
   * Constructs a deterministic {@link PageResult} from a full list of items.
   *
   * @param fullList complete sorted list of items
   * @param page requested 1-indexed page number
   * @param pageSize items per page
   * @param <T> item type
   * @return paginated result container
   */
  public static <T> PageResult<T> of(List<T> fullList, int page, int pageSize) {
    if (fullList == null || fullList.isEmpty()) {
      return new PageResult<>(List.of(), 1, Math.max(1, pageSize), 0, 0, false, false);
    }
    int validPageSize = Math.max(1, pageSize);
    int validPage = Math.max(1, page);
    long totalItems = fullList.size();
    int totalPages = (int) Math.ceil((double) totalItems / validPageSize);

    int fromIndex = (validPage - 1) * validPageSize;
    if (fromIndex >= fullList.size()) {
      return new PageResult<>(List.of(), validPage, validPageSize, totalItems, totalPages, false, validPage > 1);
    }
    int toIndex = Math.min(fromIndex + validPageSize, fullList.size());
    List<T> pageItems = List.copyOf(fullList.subList(fromIndex, toIndex));

    boolean hasNext = validPage < totalPages;
    boolean hasPrevious = validPage > 1;

    return new PageResult<>(pageItems, validPage, validPageSize, totalItems, totalPages, hasNext, hasPrevious);
  }
}
