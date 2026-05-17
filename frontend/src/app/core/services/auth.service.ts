import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginResponse {
  token: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  
  // Strictly in-memory token storage (never saved to localStorage/sessionStorage)
  private token: string | null = null;
  private currentUserEmail: string | null = null;
  private currentTenantId: string | null = null;

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/auth/login', { email, password }).pipe(
      tap(response => {
        if (response && response.token) {
          this.token = response.token;
          const claims = this.parseJwt(response.token);
          if (claims) {
            this.currentUserEmail = claims.sub || email;
            this.currentTenantId = claims.tenantId || null;
          }
        }
      })
    );
  }

  logout(): void {
    this.token = null;
    this.currentUserEmail = null;
    this.currentTenantId = null;
  }

  getToken(): string | null {
    return this.token;
  }

  isAuthenticated(): boolean {
    return this.token !== null;
  }

  getEmail(): string | null {
    return this.currentUserEmail;
  }

  getTenantId(): string | null {
    return this.currentTenantId;
  }

  private parseJwt(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        window.atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch (e) {
      return null;
    }
  }
}
