import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should not add Authorization header if token is absent', () => {
    httpClient.get('/api/flags').subscribe();

    const req = httpMock.expectOne('/api/flags');
    expect(req.request.headers.has('Authorization')).toBeFalse();
  });

  it('should add Bearer token to Authorization header if present in AuthService', () => {
    // Spy on authService to return mock token
    spyOn(authService, 'getToken').and.returnValue('mock-jwt-token');

    httpClient.get('/api/flags').subscribe();

    const req = httpMock.expectOne('/api/flags');
    expect(req.request.headers.has('Authorization')).toBeTrue();
    expect(req.request.headers.get('Authorization')).toBe('Bearer mock-jwt-token');
  });
});
