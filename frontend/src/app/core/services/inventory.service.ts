import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Supply {
  id?: number;
  name: string;
  description: string;
  currentStock: number;
  minStockLevel: number;
  unit: string;
  expirationDate: string;
}

@Injectable({
  providedIn: 'root'
})
export class InventoryService {

  private apiUrl = `${environment.apiUrl}/supplies`;

  constructor(private http: HttpClient) { }

  getAllSupplies(): Observable<Supply[]> {
    return this.http.get<Supply[]>(this.apiUrl);
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
}
