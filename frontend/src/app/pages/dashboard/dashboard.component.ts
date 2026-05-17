import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { FlagService, FeatureFlag } from '../../core/services/flag.service';
import { SseService } from '../../core/services/sse.service';
import { ToastService } from '../../shared/services/toast.service';
import { AuthService } from '../../core/services/auth.service';
import { AuditService } from '../../core/services/audit.service';
import { FlagModalComponent } from './components/flag-modal/flag-modal.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FlagModalComponent],
  template: `
    <div class="space-y-6">
      <!-- Top Actions Bar -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-bold text-white tracking-tight">Feature Flags</h2>
          <p class="text-sm text-slate-400">Manage rules, segmentation, and progressive rollouts in real-time.</p>
        </div>
        <button
          (click)="openCreateModal()"
          class="flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 rounded-xl font-semibold text-sm text-white transition-all shadow-lg shadow-indigo-600/30"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          New Flag
        </button>
      </div>

      <!-- Loading State Skeleton -->
      <div *ngIf="isLoading" class="space-y-3">
        <div class="h-12 bg-slate-900 border border-slate-800 rounded-xl animate-pulse"></div>
        <div class="h-16 bg-slate-900 border border-slate-800 rounded-xl animate-pulse" *ngFor="let item of [1, 2, 3]"></div>
      </div>

      <!-- Empty State -->
      <div 
        *ngIf="!isLoading && flags.length === 0" 
        class="flex flex-col items-center justify-center p-12 border border-dashed border-slate-800 rounded-2xl bg-slate-900/30"
      >
        <div class="h-12 w-12 flex items-center justify-center rounded-xl bg-indigo-600/10 border border-indigo-500/20 text-indigo-400 mb-4">
          <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
          </svg>
        </div>
        <h3 class="text-sm font-semibold text-white">No feature flags</h3>
        <p class="text-xs text-slate-500 mt-1">Get started by creating your first targeting flag.</p>
      </div>

      <!-- Feature Flags Table -->
      <div *ngIf="!isLoading && flags.length > 0" class="overflow-x-auto bg-slate-900 border border-slate-800 rounded-2xl shadow-xl">
        <table class="w-full border-collapse text-left text-sm text-slate-300">
          <thead class="bg-slate-950 border-b border-slate-800 text-[10px] font-bold uppercase tracking-wider text-slate-400">
            <tr>
              <th scope="col" class="px-6 py-4">Status</th>
              <th scope="col" class="px-6 py-4">Flag Key & Name</th>
              <th scope="col" class="px-6 py-4">Description</th>
              <th scope="col" class="px-6 py-4">Rollout</th>
              <th scope="col" class="px-6 py-4 text-center">Rules</th>
              <th scope="col" class="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-800/60">
            <tr *ngFor="let flag of flags" class="hover:bg-slate-800/30 transition-colors">
              <!-- Inline Active Toggle Status -->
              <td class="px-6 py-4 whitespace-nowrap">
                <button
                  (click)="onToggle(flag)"
                  [disabled]="isActionLoading"
                  class="relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-slate-900"
                  [ngClass]="{ 'bg-indigo-600': flag.enabled, 'bg-slate-700': !flag.enabled }"
                >
                  <span
                    class="pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out"
                    [ngClass]="{ 'translate-x-5': flag.enabled, 'translate-x-0': !flag.enabled }"
                  ></span>
                </button>
              </td>

              <!-- Flag Details -->
              <td class="px-6 py-4">
                <div class="font-bold text-white text-sm tracking-tight">{{ flag.key }}</div>
                <div class="text-xs text-slate-500 mt-0.5">{{ flag.name }}</div>
              </td>

              <td class="px-6 py-4 max-w-xs truncate text-xs text-slate-400">
                {{ flag.description || 'No description provided.' }}
              </td>

              <!-- Rollout Slider Display Badge -->
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex items-center gap-2">
                  <div class="w-16 bg-slate-850 h-1.5 rounded-full overflow-hidden border border-slate-800">
                    <div class="bg-indigo-500 h-full rounded-full" [style.width.%]="flag.rolloutPercentage"></div>
                  </div>
                  <span class="text-xs font-semibold text-slate-300">{{ flag.rolloutPercentage }}%</span>
                </div>
              </td>

              <!-- Rules Count -->
              <td class="px-6 py-4 whitespace-nowrap text-center">
                <span 
                  class="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium border"
                  [ngClass]="{
                    'bg-indigo-950 border-indigo-500/20 text-indigo-400': flag.rules.length > 0,
                    'bg-slate-850 border-slate-700 text-slate-500': flag.rules.length === 0
                  }"
                >
                  {{ flag.rules.length }} rules
                </span>
              </td>

              <!-- Actions Menu Buttons -->
              <td class="px-6 py-4 whitespace-nowrap text-right text-xs font-medium">
                <div class="flex items-center justify-end gap-2">
                  <button
                    (click)="openEditModal(flag)"
                    class="p-2 border border-slate-800 hover:border-slate-750 hover:bg-slate-800 rounded-lg text-slate-400 hover:text-white transition-colors"
                    title="Edit Feature Flag"
                  >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </button>

                  <button
                    (click)="triggerDeleteConfirm(flag.key)"
                    class="p-2 border border-slate-800 hover:border-rose-500/20 hover:bg-rose-500/10 rounded-lg text-slate-400 hover:text-rose-400 transition-colors"
                    title="Delete Feature Flag"
                  >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Delete Confirmation Modal (Pure Tailwind) -->
      <div 
        *ngIf="deleteConfirmKey" 
        class="fixed inset-0 z-40 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm"
      >
        <div class="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden p-6 space-y-6">
          <div class="flex items-center gap-3 text-rose-400">
            <svg class="w-6 h-6 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <h3 class="text-lg font-bold text-white">Delete Feature Flag?</h3>
          </div>
          <p class="text-sm text-slate-400">
            Are you absolutely sure you want to delete flag <span class="font-bold text-white font-mono bg-slate-950 px-1.5 py-0.5 rounded border border-slate-800">{{ deleteConfirmKey }}</span>?
            This will permanently evict this feature flag from the database and dispatch real-time deletion events to all active SDK consumers.
          </p>
          <div class="flex items-center justify-end gap-3">
            <button
              (click)="cancelDelete()"
              class="px-4 py-2 border border-slate-800 hover:bg-slate-800 hover:text-white rounded-xl text-xs font-semibold text-slate-400 transition-colors"
            >
              Cancel
            </button>
            <button
              (click)="confirmDelete()"
              class="px-4 py-2 bg-rose-600 hover:bg-rose-500 rounded-xl text-xs font-semibold text-white transition-all shadow-lg shadow-rose-600/30"
            >
              Delete Permanently
            </button>
          </div>
        </div>
      </div>

      <!-- Create/Edit Modal Component (Pure CSS backdrop) -->
      <app-flag-modal
        *ngIf="isModalOpen"
        [flagToEdit]="activeFlag"
        (save)="onSave($event)"
        (close)="closeModal()"
      ></app-flag-modal>
    </div>
  `
})
export class DashboardComponent implements OnInit, OnDestroy {
  private flagService = inject(FlagService);
  private sseService = inject(SseService);
  private toastService = inject(ToastService);
  private authService = inject(AuthService);
  private auditService = inject(AuditService);

  flags: FeatureFlag[] = [];
  isLoading = true;
  isActionLoading = false;

  // Modal control states
  isModalOpen = false;
  activeFlag: FeatureFlag | null = null;
  deleteConfirmKey: string | null = null;

  private sseSub: Subscription | null = null;

  ngOnInit(): void {
    this.loadInitialFlags();
    this.subscribeToSseEvents();
  }

  ngOnDestroy(): void {
    if (this.sseSub) {
      this.sseSub.unsubscribe();
    }
  }

  private loadInitialFlags(): void {
    this.isLoading = true;
    this.flagService.listFlags().subscribe({
      next: (data) => {
        this.flags = data || [];
        this.isLoading = false;
      },
      error: () => {
        this.toastService.error('Failed to load initial feature flags from server.');
        this.isLoading = false;
      }
    });
  }

  private subscribeToSseEvents(): void {
    this.sseSub = this.sseService.events$.subscribe({
      next: (event) => {
        const data = event.data;
        console.debug('Dashboard SSE event payload:', event.type, data);

        if (event.type === 'flag-snapshot') {
          if (Array.isArray(data)) {
            this.flags = data;
          }
        } else if (event.type === 'flag-updated') {
          const updatedFlag = data.featureFlag || data;
          if (updatedFlag && updatedFlag.key) {
            const idx = this.flags.findIndex(f => f.key === updatedFlag.key);
            if (idx !== -1) {
              this.flags[idx] = updatedFlag;
              this.toastService.success(`Flag '${updatedFlag.key}' updated via SSE.`);
            } else {
              this.flags.push(updatedFlag);
              this.toastService.success(`Flag '${updatedFlag.key}' created via SSE.`);
            }
          }
        } else if (event.type === 'flag-toggled') {
          const key = data.key || data.flagKey || (data.featureFlag && data.featureFlag.key);
          if (key) {
            const idx = this.flags.findIndex(f => f.key === key);
            if (idx !== -1) {
              if (data.featureFlag) {
                this.flags[idx] = data.featureFlag;
              } else if (data.enabled !== undefined) {
                this.flags[idx].enabled = data.enabled;
              } else {
                this.flags[idx].enabled = !this.flags[idx].enabled;
              }
              this.toastService.success(`Flag '${key}' status toggled via SSE.`);
            }
          }
        } else if (event.type === 'flag-deleted') {
          const key = data.key || data.flagKey || (data.featureFlag && data.featureFlag.key);
          if (key) {
            this.flags = this.flags.filter(f => f.key !== key);
            this.toastService.error(`Flag '${key}' deleted via SSE.`);
          }
        }
      }
    });
  }

  // Interactive local PATCH toggle
  onToggle(flag: FeatureFlag): void {
    this.isActionLoading = true;
    this.flagService.toggleFlag(flag.key).subscribe({
      next: (updated) => {
        this.isActionLoading = false;
        // In local state, update toggle immediately if SSE delay occurs:
        const idx = this.flags.findIndex(f => f.key === flag.key);
        if (idx !== -1) {
          this.flags[idx] = updated;
        }
        
        const details = `Toggled enabled status to: ${updated.enabled}`;
        this.auditService.addLog(
          'FLAG_TOGGLED', 
          flag.key, 
          this.authService.getEmail() || 'admin@flowguard.com', 
          details
        );
      },
      error: () => {
        this.toastService.error(`Failed to toggle flag '${flag.key}'.`);
        this.isActionLoading = false;
      }
    });
  }

  // Modal actions
  openCreateModal(): void {
    this.activeFlag = null;
    this.isModalOpen = true;
  }

  openEditModal(flag: FeatureFlag): void {
    this.activeFlag = flag;
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.activeFlag = null;
  }

  onSave(flag: FeatureFlag): void {
    this.closeModal();
    this.isLoading = true;

    if (this.activeFlag) {
      // Edit Mode
      this.flagService.updateFlag(this.activeFlag.key, flag).subscribe({
        next: (updated) => {
          const idx = this.flags.findIndex(f => f.key === updated.key);
          if (idx !== -1) {
            this.flags[idx] = updated;
          }
          this.isLoading = false;
          this.toastService.success(`Feature Flag '${updated.key}' updated successfully.`);

          const details = `Updated progressive rollout percentage to: ${updated.rolloutPercentage}% with ${updated.rules.length} segmentation rules.`;
          this.auditService.addLog(
            'FLAG_UPDATED', 
            updated.key, 
            this.authService.getEmail() || 'admin@flowguard.com', 
            details
          );
        },
        error: () => {
          this.toastService.error(`Failed to update feature flag '${flag.key}'.`);
          this.isLoading = false;
        }
      });
    } else {
      // Create Mode
      this.flagService.createFlag(flag).subscribe({
        next: (created) => {
          this.flags.push(created);
          this.isLoading = false;
          this.toastService.success(`Feature Flag '${created.key}' created successfully.`);

          const details = `Created feature flag ${created.key} with rollout ${created.rolloutPercentage}% and ${created.rules.length} segmentation rules.`;
          this.auditService.addLog(
            'FLAG_CREATED', 
            created.key, 
            this.authService.getEmail() || 'admin@flowguard.com', 
            details
          );
        },
        error: () => {
          this.toastService.error(`Failed to create feature flag '${flag.key}'.`);
          this.isLoading = false;
        }
      });
    }
  }

  // Delete Action Controls
  triggerDeleteConfirm(key: string): void {
    this.deleteConfirmKey = key;
  }

  cancelDelete(): void {
    this.deleteConfirmKey = null;
  }

  confirmDelete(): void {
    if (!this.deleteConfirmKey) {
      return;
    }

    const key = this.deleteConfirmKey;
    this.deleteConfirmKey = null;
    this.isLoading = true;

    this.flagService.deleteFlag(key).subscribe({
      next: () => {
        this.flags = this.flags.filter(f => f.key !== key);
        this.isLoading = false;
        this.toastService.success(`Feature Flag '${key}' deleted successfully.`);

        const details = `Permanently deleted feature flag ${key}.`;
        this.auditService.addLog(
          'FLAG_DELETED', 
          key, 
          this.authService.getEmail() || 'admin@flowguard.com', 
          details
        );
      },
      error: () => {
        this.toastService.error(`Failed to delete feature flag '${key}'.`);
        this.isLoading = false;
      }
    });
  }
}
