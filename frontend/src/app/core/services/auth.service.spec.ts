import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should login and parse JWT in memory', () => {
    // Generate a valid mock JWT token structure with claims:
    // Header: {"alg":"HS256","typ":"JWT"}
    // Payload: {"sub":"admin@flowguard.com","tenantId":"a80b7212-005d-4f01-8b01-e2345000aa11"}
    // Signature: signature
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({ sub: 'admin@flowguard.com', tenantId: 'a80b7212-005d-4f01-8b01-e2345000aa11' }));
    const mockToken = `${header}.${payload}.signature`;

    service.login('admin@flowguard.com', 'password123').subscribe(res => {
      expect(res.token).toBe(mockToken);
      expect(service.getToken()).toBe(mockToken);
      expect(service.isAuthenticated()).toBeTrue();
      expect(service.getEmail()).toBe('admin@flowguard.com');
      expect(service.getTenantId()).toBe('a80b7212-005d-4f01-8b01-e2345000aa11');
    });

    const req = httpMock.expectOne('/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'admin@flowguard.com', password: 'password123' });
    req.flush({ token: mockToken });
  });

  it('should logout and clear states', () => {
    service.logout();
    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.getEmail()).toBeNull();
    expect(service.getTenantId()).toBeNull();
  });
});
