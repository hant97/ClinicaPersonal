import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Catalog, CatalogItem } from '../models/catalog.model';
import { PageResponse } from '../models/page.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CatalogService {
  private apiUrl = `${environment.apiUrl}/catalogs`;

  constructor(private http: HttpClient) { }

  getAllCatalogs(page: number = 0, size: number = 10): Observable<PageResponse<Catalog>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<Catalog>>(this.apiUrl, { params });
  }

  getCatalogByCode(code: string): Observable<Catalog> {
    return this.http.get<Catalog>(`${this.apiUrl}/${code}`);
  }

  getActiveItemsByCatalogCode(code: string): Observable<CatalogItem[]> {
    return this.http.get<CatalogItem[]>(`${this.apiUrl}/${code}/items/active`);
  }

  createCatalog(catalog: Catalog): Observable<Catalog> {
    return this.http.post<Catalog>(this.apiUrl, catalog);
  }

  addCatalogItem(code: string, item: CatalogItem): Observable<CatalogItem> {
    return this.http.post<CatalogItem>(`${this.apiUrl}/${code}/items`, item);
  }

  updateCatalogItem(itemId: number, item: CatalogItem): Observable<CatalogItem> {
    return this.http.put<CatalogItem>(`${this.apiUrl}/items/${itemId}`, item);
  }
}
