import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuditLog, AuditLogFilter } from '../models/audit-log.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {
  private apiUrl = `${environment.apiUrl}/v1/audit-logs`;

  constructor(private http: HttpClient) {}

  getAuditLogs(filter: AuditLogFilter = {}): Observable<PageResponse<AuditLog>> {
    let params = new HttpParams()
      .set('page', (filter.page ?? 0).toString())
      .set('size', (filter.size ?? 20).toString());

    if (filter.startDate) {
      params = params.set('startDate', filter.startDate);
    }
    if (filter.endDate) {
      params = params.set('endDate', filter.endDate);
    }
    if (filter.username?.trim()) {
      params = params.set('username', filter.username.trim());
    }
    if (filter.specialty?.trim()) {
      params = params.set('specialty', filter.specialty.trim());
    }
    if (filter.action?.trim()) {
      params = params.set('action', filter.action.trim());
    }
    if (filter.entityType?.trim()) {
      params = params.set('entityType', filter.entityType.trim());
    }
    if (filter.query?.trim()) {
      params = params.set('query', filter.query.trim());
    }

    return this.http.get<PageResponse<AuditLog>>(this.apiUrl, { params });
  }

  getDistinctActions(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/actions`);
  }

  getDistinctEntityTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/entity-types`);
  }
}
