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
  imageUrl?: string;
  specialty?: string;
}

export interface SupplyStats {
  totalSupplies: number;
  lowStockCount: number;
  outOfStockCount: number;
  expiringSoonCount: number;
  inventoryValue: number;
}

@Injectable({
  providedIn: 'root'
})
export class InventoryService {

  private apiUrl = `${environment.apiUrl}/v1/supplies`;
  private transactionApiUrl = `${environment.apiUrl}/v1/inventory-transactions`;

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

  getStats(): Observable<SupplyStats> {
    return this.http.get<SupplyStats>(`${this.apiUrl}/stats`);
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

  uploadSupplyImage(id: number, file: File): Observable<Supply> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Supply>(`${this.apiUrl}/${id}/image`, formData);
  }

  deleteSupply(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Transaction History
  getTransactionsBySupply(supplyId: number): Observable<InventoryTransaction[]> {
    return this.http.get<InventoryTransaction[]>(`${this.transactionApiUrl}/supply/${supplyId}`);
  }

  getRecentTransactions(size: number = 10): Observable<InventoryTransaction[]> {
    const params = new HttpParams().set('size', size.toString());
    return this.http.get<InventoryTransaction[]>(`${this.transactionApiUrl}/recent`, { params });
  }

  recordTransaction(transaction: InventoryTransaction): Observable<InventoryTransaction> {
    return this.http.post<InventoryTransaction>(this.transactionApiUrl, transaction);
  }
}
