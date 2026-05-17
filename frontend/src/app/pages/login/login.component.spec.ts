import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../shared/services/toast.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: AuthService;
  let toastService: ToastService;
  let router: Router;

  beforeEach(async () => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        AuthService,
        ToastService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    toastService = TestBed.inject(ToastService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should validate form and show invalid fields on touch', () => {
    const form = component.loginForm;
    expect(form.valid).toBeFalse();

    const email = form.get('email');
    email?.setValue('invalid-email');
    email?.markAsTouched();
    fixture.detectChanges();

    expect(component.isFieldInvalid('email')).toBeTrue();
  });

  it('should call AuthService and navigate to dashboard on successful login', () => {
    spyOn(authService, 'login').and.returnValue(of({ token: 'mock-token' }));
    spyOn(toastService, 'success');

    component.loginForm.get('email')?.setValue('admin@flowguard.com');
    component.loginForm.get('password')?.setValue('password123');
    
    component.onSubmit();

    expect(authService.login).toHaveBeenCalledWith('admin@flowguard.com', 'password123');
    expect(toastService.success).toHaveBeenCalledWith('Logged in successfully!');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should show toast error on failed login', () => {
    spyOn(authService, 'login').and.returnValue(throwError(() => ({ error: { message: 'Bad credentials' } })));
    spyOn(toastService, 'error');

    component.loginForm.get('email')?.setValue('admin@flowguard.com');
    component.loginForm.get('password')?.setValue('wrong-password');
    
    component.onSubmit();

    expect(toastService.error).toHaveBeenCalledWith('Bad credentials');
  });
});
