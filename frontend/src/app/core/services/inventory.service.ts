import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.model';
import { InventoryTransaction } from '../models/inventory-transaction.model';

export interface Supply {
  id?: number;
  name: string;
  description: string;
  currentStock: number;
  minStockLevel: number;
  unit: string;
  price?: number;
  expirationDate: string;
}

@Injectable({
  providedIn: 'root'
})
export class InventoryService {

  private apiUrl = `${environment.apiUrl}/supplies`;
  private transactionApiUrl = `${environment.apiUrl}/inventory-transactions`;

  constructor(private http: HttpClient) { }

  getAllSupplies(searchTerm: string = '', page: number = 0, size: number = 10): Observable<PageResponse<Supply>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (searchTerm) {
      params = params.set('name', searchTerm);
    }
    return this.http.get<PageResponse<Supply>>(this.apiUrl, { params });
  }

  getLowStockSupplies(): Observable<Supply[]> {
    return this.http.get<Supply[]>(`${this.apiUrl}/low-stock`);
  }

  getSupplyById(id: number): Observable<Supply> {
    return this.http.get<Supply>(`${this.apiUrl}/${id}`);
  }

  createSupply(supply: Supply): Observable<Supply> {
    return this.http.post<Supply>(this.apiUrl, supply);
  }

  updateSupply(id: number, supply: Supply): Observable<Supply> {
    return this.http.put<Supply>(`${this.apiUrl}/${id}`, supply);
  }

  deleteSupply(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Transaction History
  getTransactionsBySupply(supplyId: number): Observable<InventoryTransaction[]> {
    return this.http.get<InventoryTransaction[]>(`${this.transactionApiUrl}/supply/${supplyId}`);
  }

  recordTransaction(transaction: InventoryTransaction): Observable<InventoryTransaction> {
    return this.http.post<InventoryTransaction>(this.transactionApiUrl, transaction);
  }
}
