import { of } from 'rxjs';
import { PageResponse } from '../models/page.model';
import { fetchAllPages } from './pagination.util';

describe('fetchAllPages', () => {
  it('recorre páginas sucesivas sin superar el límite global', () => {
    const pages: PageResponse<number>[] = [
      { content: [1, 2], page: { number: 0, size: 2, totalElements: 3, totalPages: 2 } },
      { content: [3], page: { number: 1, size: 2, totalElements: 3, totalPages: 2 } }
    ];
    const requestPage = jasmine.createSpy('requestPage').and.callFake((page: number) => of(pages[page]));

    fetchAllPages(requestPage, 2).subscribe(items => expect(items).toEqual([1, 2, 3]));

    expect(requestPage.calls.allArgs()).toEqual([[0, 2], [1, 2]]);
  });

  it('rechaza tamaños fuera del contrato', () => {
    expect(() => fetchAllPages<number>(
      () => of({} as PageResponse<number>),
      101
    )).toThrowError(/entre 1 y 100/);
  });
});
