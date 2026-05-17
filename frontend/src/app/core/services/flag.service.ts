import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FlagRule {
  id?: string;
  flagId?: string;
  attributeKey: string;
  operator: 'EQUALS' | 'NOT_EQUALS' | 'CONTAINS' | 'STARTS_WITH' | 'IN';
  attributeValue: string;
}

export interface FeatureFlag {
  key: string;
  name: string;
  description: string;
  enabled: boolean;
  rolloutPercentage: number;
  rules: FlagRule[];
}

@Injectable({
  providedIn: 'root'
})
export class FlagService {
  private http = inject(HttpClient);

  listFlags(): Observable<FeatureFlag[]> {
    return this.http.get<FeatureFlag[]>('/api/flags');
  }

  createFlag(flag: FeatureFlag): Observable<FeatureFlag> {
    return this.http.post<FeatureFlag>('/api/flags', flag);
  }

  updateFlag(key: string, flag: FeatureFlag): Observable<FeatureFlag> {
    return this.http.put<FeatureFlag>(`/api/flags/${key}`, flag);
  }

  deleteFlag(key: string): Observable<void> {
    return this.http.delete<void>(`/api/flags/${key}`);
  }

  toggleFlag(key: string): Observable<FeatureFlag> {
    return this.http.patch<FeatureFlag>(`/api/flags/${key}/toggle`, {});
  }
}
