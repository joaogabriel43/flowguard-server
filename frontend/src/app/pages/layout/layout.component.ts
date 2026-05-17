import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SseService } from '../../core/services/sse.service';
import { ToastComponent } from '../../shared/components/toast/toast.component';
import { ToastService } from '../../shared/services/toast.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ToastComponent],
  template: `
    <div class="flex h-screen bg-slate-950 text-slate-100 overflow-hidden">
      <!-- Sidebar -->
      <aside class="w-64 bg-slate-900 border-r border-slate-800 flex flex-col justify-between">
        <div>
          <!-- Sidebar Header -->
          <div class="h-16 flex items-center px-6 border-b border-slate-800 gap-3">
            <div class="h-8 w-8 flex items-center justify-center rounded-lg bg-indigo-600/10 border border-indigo-500/20 text-indigo-400">
              <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <span class="text-lg font-bold bg-gradient-to-r from-white to-slate-400 bg-clip-text text-transparent">FlowGuard</span>
          </div>

          <!-- Navigation Links -->
          <nav class="mt-6 px-4 space-y-1">
            <a
              routerLink="/dashboard"
              routerLinkActive="bg-indigo-600/10 border-indigo-500/30 text-white"
              [routerLinkActiveOptions]="{ exact: true }"
              class="flex items-center px-4 py-3 rounded-xl border border-transparent text-slate-400 hover:text-white hover:bg-slate-800/50 transition-all gap-3 text-sm font-medium"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v4a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v4a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v4a2 2 0 01-2 2H6a2 2 0 01-2-2v-4zM14 16a2 2 0 012-2h2a2 2 0 012 2v4a2 2 0 01-2 2h-2a2 2 0 01-2-2v-4z" />
              </svg>
              Dashboard
            </a>

            <a
              routerLink="/audit"
              routerLinkActive="bg-indigo-600/10 border-indigo-500/30 text-white"
              class="flex items-center px-4 py-3 rounded-xl border border-transparent text-slate-400 hover:text-white hover:bg-slate-800/50 transition-all gap-3 text-sm font-medium"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Audit Log
            </a>
          </nav>
        </div>

        <!-- Sidebar Footer / User Profile -->
        <div class="p-4 border-t border-slate-800 bg-slate-900/50 flex flex-col gap-2">
          <div class="flex items-center gap-3">
            <div class="h-9 w-9 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center font-bold text-indigo-400 text-sm">
              U
            </div>
            <div class="overflow-hidden">
              <p class="text-xs font-semibold text-white truncate">{{ userEmail }}</p>
              <p class="text-[10px] text-slate-500 uppercase tracking-wider font-bold">Administrator</p>
            </div>
          </div>
          <button
            (click)="onLogout()"
            class="w-full flex items-center justify-center gap-2 mt-2 px-4 py-2 border border-slate-800 hover:border-rose-500/20 rounded-xl hover:bg-rose-500/10 text-slate-400 hover:text-rose-400 text-xs font-medium transition-all"
          >
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            Logout
          </button>
        </div>
      </aside>

      <!-- Main Layout Body -->
      <div class="flex-1 flex flex-col overflow-hidden">
        <!-- Main Header -->
        <header class="h-16 bg-slate-900 border-b border-slate-800 flex items-center justify-between px-8 flex-shrink-0">
          <div>
            <h1 class="text-base font-bold text-white uppercase tracking-wider text-xs">
              FlowGuard Control Panel
            </h1>
          </div>
          <!-- SSE Pulsing Badge Portal -->
          <div class="flex items-center gap-2">
            <div 
              *ngIf="sseConnected$ | async; else disconnected"
              class="flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-950 border border-emerald-500/30 text-emerald-400 text-xs font-semibold"
            >
              <span class="relative flex h-2 w-2">
                <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span class="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
              </span>
              Conexão Real-time Ativa
            </div>
            <ng-template #disconnected>
              <div class="flex items-center gap-2 px-3 py-1.5 rounded-full bg-rose-950 border border-rose-500/30 text-rose-400 text-xs font-semibold">
                <span class="h-2 w-2 rounded-full bg-rose-500"></span>
                Desconectado
              </div>
            </ng-template>
          </div>
        </header>

        <!-- Dynamic Content Router Outlet -->
        <main class="flex-1 overflow-y-auto bg-slate-950 p-8">
          <router-outlet></router-outlet>
        </main>
      </div>

      <!-- Toast portal rendering in background -->
      <app-toast></app-toast>
    </div>
  `
})
export class LayoutComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private sseService = inject(SseService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  userEmail = '';
  sseConnected$ = this.sseService.isConnected$;

  ngOnInit(): void {
    this.userEmail = this.authService.getEmail() || 'admin@flowguard.com';
    // Automatically trigger authorized SSE connection on layout hydration
    this.sseService.connect();
  }

  ngOnDestroy(): void {
    // Gracefully terminate SSE connections to avoid connection leakages on logout/nav resets
    this.sseService.disconnect();
  }

  onLogout(): void {
    this.sseService.disconnect();
    this.authService.logout();
    this.toastService.success('Logged out successfully.');
    this.router.navigate(['/login']);
  }
}
