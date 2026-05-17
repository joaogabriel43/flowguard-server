import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

export interface AuditLogEntry {
  id: string;
  action: string;
  flagKey: string;
  performedBy: string;
  timestamp: string;
  details: string;
}

export interface PaginatedAuditLogs {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  // Session-persistent in-memory audit logs list
  private logs: AuditLogEntry[] = [
    {
      id: 'd3b07384-d113-4bf6-a511-d101d2345001',
      action: 'FLAG_CREATED',
      flagKey: 'payment-gateway-v2',
      performedBy: 'admin@flowguard.com',
      timestamp: new Date(Date.now() - 3600000 * 4).toISOString(),
      details: 'Created feature flag payment-gateway-v2 with rollout 0%.'
    },
    {
      id: 'd3b07384-d113-4bf6-a511-d101d2345002',
      action: 'FLAG_TOGGLED',
      flagKey: 'new-checkout-screen',
      performedBy: 'admin@flowguard.com',
      timestamp: new Date(Date.now() - 3600000 * 2).toISOString(),
      details: 'Toggled enabled status to: true.'
    },
    {
      id: 'd3b07384-d113-4bf6-a511-d101d2345003',
      action: 'FLAG_UPDATED',
      flagKey: 'payment-gateway-v2',
      performedBy: 'admin@flowguard.com',
      timestamp: new Date(Date.now() - 3600000).toISOString(),
      details: 'Updated progressive rollout percentage to: 50%.'
    }
  ];

  listLogs(page: number = 0, size: number = 5): Observable<PaginatedAuditLogs> {
    // Sort logs descending by timestamp
    const sorted = [...this.logs].sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    );

    const start = page * size;
    const end = start + size;
    const paginatedSlice = sorted.slice(start, end);
    const totalElements = sorted.length;
    const totalPages = Math.ceil(totalElements / size);

    return of({
      content: paginatedSlice,
      totalElements,
      totalPages,
      page,
      size
    });
  }

  addLog(action: 'FLAG_CREATED' | 'FLAG_UPDATED' | 'FLAG_DELETED' | 'FLAG_TOGGLED', flagKey: string, performedBy: string, details: string): void {
    const newLog: AuditLogEntry = {
      id: this.generateUUID(),
      action,
      flagKey,
      performedBy,
      timestamp: new Date().toISOString(),
      details
    };
    this.logs.unshift(newLog); // prepend to top
  }

  private generateUUID(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
}
