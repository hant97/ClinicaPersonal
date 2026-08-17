import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, shareReplay, tap } from 'rxjs';
import { Catalog, CatalogItem } from '../models/catalog.model';
import { PageResponse } from '../models/page.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CatalogService {
  private apiUrl = `${environment.apiUrl}/v1/catalogs`;
  private activeItemsCache = new Map<string, Observable<CatalogItem[]>>();

  constructor(private http: HttpClient) { }

  getAllCatalogs(page: number = 0, size: number = 20, search?: string, specialty?: string): Observable<PageResponse<Catalog>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    if (specialty && specialty.trim()) {
      params = params.set('specialty', specialty.trim());
    }
    return this.http.get<PageResponse<Catalog>>(this.apiUrl, { params });
  }

  getAllAccessibleCatalogs(specialty?: string): Observable<Catalog[]> {
    let params = new HttpParams();
    if (specialty && specialty.trim()) {
      params = params.set('specialty', specialty.trim());
    }
    return this.http.get<Catalog[]>(`${this.apiUrl}/all`, { params });
  }

  getCatalogByCode(code: string): Observable<Catalog> {
    return this.http.get<Catalog>(`${this.apiUrl}/${code}`);
  }

  getActiveItemsByCatalogCode(code: string): Observable<CatalogItem[]> {
    if (!this.activeItemsCache.has(code)) {
      const request$ = this.http.get<CatalogItem[]>(`${this.apiUrl}/${code}/items/active`).pipe(
        shareReplay({ bufferSize: 1, refCount: false })
      );
      this.activeItemsCache.set(code, request$);
    }
    return this.activeItemsCache.get(code)!;
  }

  createCatalog(catalog: Catalog): Observable<Catalog> {
    return this.http.post<Catalog>(this.apiUrl, catalog).pipe(
      tap(() => this.clearCache())
    );
  }

  updateCatalog(id: number, catalog: Partial<Catalog>): Observable<Catalog> {
    return this.http.put<Catalog>(`${this.apiUrl}/${id}`, catalog).pipe(
      tap(() => this.clearCache())
    );
  }

  addCatalogItem(code: string, item: CatalogItem): Observable<CatalogItem> {
    return this.http.post<CatalogItem>(`${this.apiUrl}/${code}/items`, item).pipe(
      tap(() => this.clearCache(code))
    );
  }

  updateCatalogItem(itemId: number, item: CatalogItem, catalogCode?: string): Observable<CatalogItem> {
    return this.http.put<CatalogItem>(`${this.apiUrl}/items/${itemId}`, item).pipe(
      tap(() => this.clearCache(catalogCode))
    );
  }

  deleteCatalogItem(itemId: number, catalogCode?: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/items/${itemId}`).pipe(
      tap(() => this.clearCache(catalogCode))
    );
  }

  reorderCatalogItems(catalogCode: string, itemIds: number[]): Observable<CatalogItem[]> {
    return this.http.put<CatalogItem[]>(`${this.apiUrl}/${catalogCode}/items/reorder`, itemIds).pipe(
      tap(() => this.clearCache(catalogCode))
    );
  }

  clearCache(code?: string): void {
    if (code) {
      this.activeItemsCache.delete(code);
    } else {
      this.activeItemsCache.clear();
    }
  }
}
