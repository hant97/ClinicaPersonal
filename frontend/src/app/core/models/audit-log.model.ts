export interface AuditLog {
  id: number;
  userId?: number;
  username?: string;
  specialty?: string;
  action: string;
  entityType: string;
  entityId?: string;
  detail?: string;
  ip?: string;
  createdAt: string;
}

export interface AuditLogFilter {
  startDate?: string;
  endDate?: string;
  username?: string;
  specialty?: string;
  action?: string;
  entityType?: string;
  query?: string;
  page?: number;
  size?: number;
}
