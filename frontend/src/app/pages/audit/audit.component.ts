import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuditService, PaginatedAuditLogs, AuditLogEntry } from '../../core/services/audit.service';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-6">
      <!-- Top Actions Bar -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-bold text-white tracking-tight">Audit Logs</h2>
          <p class="text-sm text-slate-400">Track all administration activities, targeting changes, and state mutations.</p>
        </div>
      </div>

      <!-- Loading State Skeleton -->
      <div *ngIf="isLoading" class="space-y-3">
        <div class="h-12 bg-slate-900 border border-slate-800 rounded-xl animate-pulse"></div>
        <div class="h-16 bg-slate-900 border border-slate-800 rounded-xl animate-pulse" *ngFor="let item of [1, 2, 3]"></div>
      </div>

      <!-- Empty State -->
      <div 
        *ngIf="!isLoading && paginatedData?.content?.length === 0" 
        class="flex flex-col items-center justify-center p-12 border border-dashed border-slate-800 rounded-2xl bg-slate-900/30"
      >
        <div class="h-12 w-12 flex items-center justify-center rounded-xl bg-slate-850 border border-slate-700 text-slate-500 mb-4">
          <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <h3 class="text-sm font-semibold text-white">No audit logs</h3>
        <p class="text-xs text-slate-500 mt-1">Activities will appear here once feature flags are mutated.</p>
      </div>

      <!-- Audit Logs Table -->
      <div *ngIf="!isLoading && paginatedData && paginatedData.content.length > 0" class="space-y-4">
        <div class="overflow-x-auto bg-slate-900 border border-slate-800 rounded-2xl shadow-xl">
          <table class="w-full border-collapse text-left text-sm text-slate-300">
            <thead class="bg-slate-950 border-b border-slate-800 text-[10px] font-bold uppercase tracking-wider text-slate-400">
              <tr>
                <th scope="col" class="px-6 py-4">Timestamp</th>
                <th scope="col" class="px-6 py-4">Action</th>
                <th scope="col" class="px-6 py-4">Flag Key</th>
                <th scope="col" class="px-6 py-4">Performed By</th>
                <th scope="col" class="px-6 py-4">Mutation Details</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800/60">
              <tr *ngFor="let log of paginatedData.content" class="hover:bg-slate-800/30 transition-colors">
                <!-- Timestamp -->
                <td class="px-6 py-4 whitespace-nowrap text-xs text-slate-400">
                  {{ log.timestamp | date:'yyyy-MM-dd HH:mm:ss' }}
                </td>

                <!-- Action Type Badge -->
                <td class="px-6 py-4 whitespace-nowrap">
                  <span 
                    class="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-bold tracking-wider uppercase border"
                    [ngClass]="{
                      'bg-emerald-950 border-emerald-500/20 text-emerald-400': log.action === 'FLAG_CREATED',
                      'bg-indigo-950 border-indigo-500/20 text-indigo-400': log.action === 'FLAG_UPDATED',
                      'bg-rose-950 border-rose-500/20 text-rose-400': log.action === 'FLAG_DELETED',
                      'bg-amber-950 border-amber-500/20 text-amber-400': log.action === 'FLAG_TOGGLED'
                    }"
                  >
                    {{ log.action.replace('_', ' ') }}
                  </span>
                </td>

                <!-- Flag Key -->
                <td class="px-6 py-4 whitespace-nowrap font-mono text-xs text-white">
                  {{ log.flagKey }}
                </td>

                <!-- Performed By -->
                <td class="px-6 py-4 whitespace-nowrap text-xs text-slate-300">
                  {{ log.performedBy }}
                </td>

                <!-- Mutation Details -->
                <td class="px-6 py-4 text-xs text-slate-450">
                  {{ log.details }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination Footer Navigation -->
        <div class="flex items-center justify-between px-4 py-3 bg-slate-900 border border-slate-800 rounded-xl">
          <div class="flex-1 flex justify-between sm:hidden">
            <button
              (click)="prevPage()"
              [disabled]="pageIndex === 0"
              class="relative inline-flex items-center px-4 py-2 border border-slate-800 text-xs font-semibold rounded-lg text-slate-300 hover:text-white bg-slate-950 hover:bg-slate-850 disabled:opacity-40 transition-colors"
            >
              Previous
            </button>
            <button
              (click)="nextPage()"
              [disabled]="pageIndex >= paginatedData.totalPages - 1"
              class="ml-3 relative inline-flex items-center px-4 py-2 border border-slate-800 text-xs font-semibold rounded-lg text-slate-300 hover:text-white bg-slate-950 hover:bg-slate-850 disabled:opacity-40 transition-colors"
            >
              Next
            </button>
          </div>
          
          <div class="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
            <div>
              <p class="text-xs text-slate-400">
                Showing
                <span class="font-bold text-white">{{ pageIndex * pageSize + 1 }}</span>
                to
                <span class="font-bold text-white">{{ Math.min((pageIndex + 1) * pageSize, paginatedData.totalElements) }}</span>
                of
                <span class="font-bold text-white">{{ paginatedData.totalElements }}</span>
                results
              </p>
            </div>
            
            <div class="flex items-center gap-2">
              <span class="text-xs text-slate-500 mr-2">
                Page {{ pageIndex + 1 }} of {{ paginatedData.totalPages }}
              </span>
              
              <nav class="relative z-0 inline-flex rounded-lg shadow-sm gap-1">
                <button
                  (click)="prevPage()"
                  [disabled]="pageIndex === 0"
                  class="relative inline-flex items-center p-2 rounded-lg border border-slate-800 text-slate-400 hover:text-white bg-slate-950 hover:bg-slate-850 disabled:opacity-40 transition-colors"
                >
                  <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                  </svg>
                </button>
                
                <button
                  (click)="nextPage()"
                  [disabled]="pageIndex >= paginatedData.totalPages - 1"
                  class="relative inline-flex items-center p-2 rounded-lg border border-slate-800 text-slate-400 hover:text-white bg-slate-950 hover:bg-slate-850 disabled:opacity-40 transition-colors"
                >
                  <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
              </nav>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class AuditComponent implements OnInit {
  private auditService = inject(AuditService);

  paginatedData?: PaginatedAuditLogs;
  isLoading = true;
  
  pageIndex = 0;
  pageSize = 5;

  Math = Math;

  ngOnInit(): void {
    this.fetchLogs();
  }

  private fetchLogs(): void {
    this.isLoading = true;
    this.auditService.listLogs(this.pageIndex, this.pageSize).subscribe({
      next: (data) => {
        this.paginatedData = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  prevPage(): void {
    if (this.pageIndex > 0) {
      this.pageIndex--;
      this.fetchLogs();
    }
  }

  nextPage(): void {
    if (this.paginatedData && this.pageIndex < this.paginatedData.totalPages - 1) {
      this.pageIndex++;
      this.fetchLogs();
    }
  }
}
