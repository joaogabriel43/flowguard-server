import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { FlagService, FeatureFlag } from '../../core/services/flag.service';
import { SseService, SseEvent } from '../../core/services/sse.service';
import { ToastService } from '../../shared/services/toast.service';
import { Subject } from 'rxjs';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let flagService: FlagService;
  let sseService: SseService;
  let toastService: ToastService;
  let sseEventSubject: Subject<SseEvent>;

  const mockFlags: FeatureFlag[] = [
    {
      key: 'my-flag',
      name: 'My Flag',
      description: 'Desc',
      enabled: false,
      rolloutPercentage: 0,
      rules: []
    }
  ];

  beforeEach(async () => {
    sseEventSubject = new Subject<SseEvent>();

    // Mock SseService
    const mockSse = {
      events$: sseEventSubject.asObservable(),
      isConnected$: of(true),
      connect: () => {},
      disconnect: () => {}
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        FlagService,
        ToastService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SseService, useValue: mockSse }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    flagService = TestBed.inject(FlagService);
    toastService = TestBed.inject(ToastService);
    sseService = TestBed.inject(SseService);

    spyOn(flagService, 'listFlags').and.returnValue(of(mockFlags));
    fixture.detectChanges();
  });

  it('should create and load initial flags', () => {
    expect(component).toBeTruthy();
    expect(flagService.listFlags).toHaveBeenCalled();
    expect(component.flags.length).toBe(1);
    expect(component.flags[0].key).toBe('my-flag');
  });

  it('should update local flag enabled state when toggle flag is successful', () => {
    const updatedFlag: FeatureFlag = { ...mockFlags[0], enabled: true };
    spyOn(flagService, 'toggleFlag').and.returnValue(of(updatedFlag));

    component.onToggle(mockFlags[0]);

    expect(flagService.toggleFlag).toHaveBeenCalledWith('my-flag');
    expect(component.flags[0].enabled).toBeTrue();
  });

  it('should respond to SSE flag-toggled events dynamically without page refresh', () => {
    // Emit toggled event payload over SSE subject
    const toggledFlag: FeatureFlag = { ...mockFlags[0], enabled: true };
    sseEventSubject.next({
      type: 'flag-toggled',
      data: {
        featureFlag: toggledFlag
      }
    });

    expect(component.flags[0].enabled).toBeTrue();
  });
});
