import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ScheduleBlock } from '../models/schedule-block.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ScheduleBlockService {
  private apiUrl = `${environment.apiUrl}/v1/schedule-blocks`;

  constructor(private http: HttpClient) {}

  getBlocks(startDate?: string, endDate?: string, professionalId?: number): Observable<ScheduleBlock[]> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    if (professionalId) params = params.set('professionalId', professionalId.toString());

    return this.http.get<ScheduleBlock[]>(this.apiUrl, { params });
  }

  createBlock(block: ScheduleBlock): Observable<ScheduleBlock> {
    return this.http.post<ScheduleBlock>(this.apiUrl, block);
  }

  updateBlock(id: number, block: ScheduleBlock): Observable<ScheduleBlock> {
    return this.http.put<ScheduleBlock>(`${this.apiUrl}/${id}`, block);
  }

  deleteBlock(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
