import { EMPTY, Observable, expand, map, reduce } from 'rxjs';
import { PageResponse } from '../models/page.model';

export const MAX_PAGE_SIZE = 100;

export function fetchAllPages<T>(
  requestPage: (page: number, size: number) => Observable<PageResponse<T>>,
  pageSize: number = MAX_PAGE_SIZE
): Observable<T[]> {
  if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
    throw new Error(`El tamaño de página debe estar entre 1 y ${MAX_PAGE_SIZE}`);
  }

  return requestPage(0, pageSize).pipe(
    expand(page => {
      const nextPage = page.page.number + 1;
      return nextPage < page.page.totalPages ? requestPage(nextPage, pageSize) : EMPTY;
    }),
    map(page => page.content),
    reduce((allItems, pageItems) => [...allItems, ...pageItems], [] as T[])
  );
}
