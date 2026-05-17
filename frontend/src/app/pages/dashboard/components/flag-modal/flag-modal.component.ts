import { Component, Input, Output, EventEmitter, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { FeatureFlag, FlagRule } from '../../../../core/services/flag.service';

@Component({
  selector: 'app-flag-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="fixed inset-0 z-40 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm overflow-y-auto">
      <div class="w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden transform transition-all my-8">
        <!-- Modal Header -->
        <div class="px-6 py-4 border-b border-slate-800 flex items-center justify-between">
          <h3 class="text-lg font-bold text-white">
            {{ isEditMode ? 'Edit Feature Flag' : 'Create Feature Flag' }}
          </h3>
          <button (click)="onClose()" class="text-slate-400 hover:text-slate-200 transition-colors">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- Modal Body Form -->
        <form [formGroup]="flagForm" (ngSubmit)="onSubmit()" class="p-6 space-y-6 max-h-[70vh] overflow-y-auto">
          <!-- Basic Details -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label for="flag-key" class="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1">
                Flag Key *
              </label>
              <input
                id="flag-key"
                type="text"
                formControlName="key"
                [readOnly]="isEditMode"
                class="appearance-none block w-full px-4 py-2.5 border border-slate-800 rounded-xl bg-slate-950 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                placeholder="my-awesome-flag"
                [ngClass]="{ 'border-rose-500 focus:ring-rose-500': isFieldInvalid('key') }"
              />
              <p *ngIf="isFieldInvalid('key')" class="mt-1 text-xs text-rose-400">
                Key is required (alphanumeric, dashes, periods only).
              </p>
            </div>

            <div>
              <label for="flag-name" class="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1">
                Display Name *
              </label>
              <input
                id="flag-name"
                type="text"
                formControlName="name"
                class="appearance-none block w-full px-4 py-2.5 border border-slate-800 rounded-xl bg-slate-950 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all text-sm"
                placeholder="My Awesome Flag"
                [ngClass]="{ 'border-rose-500 focus:ring-rose-500': isFieldInvalid('name') }"
              />
              <p *ngIf="isFieldInvalid('name')" class="mt-1 text-xs text-rose-400">
                Display Name is required.
              </p>
            </div>
          </div>

          <div>
            <label for="flag-desc" class="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1">
              Description
            </label>
            <textarea
              id="flag-desc"
              formControlName="description"
              rows="2"
              class="appearance-none block w-full px-4 py-2.5 border border-slate-800 rounded-xl bg-slate-950 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all text-sm resize-none"
              placeholder="Controls access to the new checkout screen."
            ></textarea>
          </div>

          <!-- Rollout Percentage Slider -->
          <div class="p-4 bg-slate-950 border border-slate-800 rounded-xl space-y-3">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Progressive Rollout Percentage
              </span>
              <span class="px-2.5 py-1 text-xs font-bold rounded-lg bg-indigo-600/20 border border-indigo-500/30 text-indigo-400">
                {{ flagForm.get('rolloutPercentage')?.value }}%
              </span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              step="1"
              formControlName="rolloutPercentage"
              class="w-full h-2 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500 focus:outline-none"
            />
            <p class="text-[10px] text-slate-500">
              Users fall consistently into the rollout bucketing based on the MurmurHash3 calculation over their ID.
            </p>
          </div>

          <!-- Segmentation Rules Section -->
          <div class="space-y-4">
            <div class="flex items-center justify-between border-b border-slate-800 pb-2">
              <h4 class="text-sm font-bold text-white flex items-center gap-2">
                <svg class="w-4 h-4 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
                </svg>
                Segmentation Rules (AND composite)
              </h4>
              <button
                type="button"
                (click)="addRule()"
                class="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 border border-slate-700 hover:border-indigo-500/30 hover:bg-slate-800/80 rounded-lg text-xs font-semibold text-slate-300 hover:text-white transition-all"
              >
                <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
                Add Rule
              </button>
            </div>

            <!-- Rules Form List -->
            <div formArrayName="rules" class="space-y-3">
              <div *ngIf="rules.length === 0" class="text-center py-6 border border-dashed border-slate-800 rounded-xl">
                <p class="text-xs text-slate-500">No targeting rules configured. All users will pass to rollout evaluation.</p>
              </div>

              <div 
                *ngFor="let rule of rules.controls; let idx = index" 
                [formGroupName]="idx"
                class="flex flex-col md:flex-row gap-3 p-4 bg-slate-950 border border-slate-800 rounded-xl relative group"
              >
                <div class="flex-1 grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <label class="block text-[10px] font-bold uppercase tracking-wider text-slate-500 mb-1">Attribute Key *</label>
                    <input
                      type="text"
                      formControlName="attributeKey"
                      class="appearance-none block w-full px-3 py-2 border border-slate-800 rounded-lg bg-slate-900 text-white focus:outline-none focus:ring-1 focus:ring-indigo-500 text-xs"
                      placeholder="e.g. country, email"
                      [ngClass]="{ 'border-rose-500': isRuleFieldInvalid(idx, 'attributeKey') }"
                    />
                  </div>

                  <div>
                    <label class="block text-[10px] font-bold uppercase tracking-wider text-slate-500 mb-1">Operator *</label>
                    <select
                      formControlName="operator"
                      class="appearance-none block w-full px-3 py-2 border border-slate-800 rounded-lg bg-slate-900 text-white focus:outline-none focus:ring-1 focus:ring-indigo-500 text-xs"
                      [ngClass]="{ 'border-rose-500': isRuleFieldInvalid(idx, 'operator') }"
                    >
                      <option value="EQUALS">EQUALS</option>
                      <option value="NOT_EQUALS">NOT EQUALS</option>
                      <option value="CONTAINS">CONTAINS</option>
                      <option value="STARTS_WITH">STARTS WITH</option>
                      <option value="IN">IN (comma-separated list)</option>
                    </select>
                  </div>

                  <div>
                    <label class="block text-[10px] font-bold uppercase tracking-wider text-slate-500 mb-1">Target Value *</label>
                    <input
                      type="text"
                      formControlName="attributeValue"
                      class="appearance-none block w-full px-3 py-2 border border-slate-800 rounded-lg bg-slate-900 text-white focus:outline-none focus:ring-1 focus:ring-indigo-500 text-xs"
                      placeholder="e.g. BR, US"
                      [ngClass]="{ 'border-rose-500': isRuleFieldInvalid(idx, 'attributeValue') }"
                    />
                  </div>
                </div>

                <div class="flex items-end justify-end md:justify-center md:items-center">
                  <button
                    type="button"
                    (click)="removeRule(idx)"
                    class="p-2 border border-slate-800 hover:border-rose-500/20 hover:bg-rose-500/10 text-slate-400 hover:text-rose-400 rounded-lg transition-colors"
                    title="Remove target rule"
                  >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </div>
            </div>
            <p *ngIf="flagForm.get('rules')?.invalid && flagForm.get('rules')?.touched" class="text-xs text-rose-400">
              Please complete or remove any empty segmentation rules. All added rules must be filled in.
            </p>
          </div>
        </form>

        <!-- Modal Footer -->
        <div class="px-6 py-4 border-t border-slate-800 bg-slate-900/50 flex items-center justify-end gap-3">
          <button
            type="button"
            (click)="onClose()"
            class="px-4 py-2 border border-slate-800 hover:bg-slate-800 hover:text-white rounded-xl text-sm font-semibold text-slate-400 transition-colors"
          >
            Cancel
          </button>
          <button
            type="button"
            (click)="onSubmit()"
            [disabled]="flagForm.invalid"
            class="px-5 py-2 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl text-sm font-semibold text-white transition-all shadow-lg shadow-indigo-600/30"
          >
            {{ isEditMode ? 'Save Changes' : 'Create Flag' }}
          </button>
        </div>
      </div>
    </div>
  `
})
export class FlagModalComponent implements OnInit {
  private fb = inject(FormBuilder);

  @Input() flagToEdit: FeatureFlag | null = null;
  @Output() save = new EventEmitter<FeatureFlag>();
  @Output() close = new EventEmitter<void>();

  isEditMode = false;
  flagForm!: FormGroup;

  ngOnInit(): void {
    this.isEditMode = this.flagToEdit !== null;
    this.initForm();
  }

  private initForm(): void {
    this.flagForm = this.fb.group({
      key: [
        this.flagToEdit?.key || '', 
        [Validators.required, Validators.pattern(/^[a-zA-Z0-9\.\-_]+$/)]
      ],
      name: [this.flagToEdit?.name || '', [Validators.required]],
      description: [this.flagToEdit?.description || ''],
      enabled: [this.flagToEdit?.enabled ?? false],
      rolloutPercentage: [this.flagToEdit?.rolloutPercentage ?? 0, [Validators.required]],
      rules: this.fb.array([])
    });

    if (this.flagToEdit && this.flagToEdit.rules) {
      this.flagToEdit.rules.forEach(rule => {
        this.rules.push(this.createRuleFormGroup(rule));
      });
    }
  }

  get rules(): FormArray {
    return this.flagForm.get('rules') as FormArray;
  }

  private createRuleFormGroup(rule?: FlagRule): FormGroup {
    return this.fb.group({
      id: [rule?.id || null],
      flagId: [rule?.flagId || null],
      attributeKey: [rule?.attributeKey || '', [Validators.required]],
      operator: [rule?.operator || 'EQUALS', [Validators.required]],
      attributeValue: [rule?.attributeValue || '', [Validators.required]]
    });
  }

  addRule(): void {
    this.rules.push(this.createRuleFormGroup());
    this.flagForm.get('rules')?.markAsTouched();
  }

  removeRule(index: number): void {
    this.rules.removeAt(index);
  }

  isFieldInvalid(field: string): boolean {
    const control = this.flagForm.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  isRuleFieldInvalid(ruleIdx: number, field: string): boolean {
    const ruleGroup = this.rules.at(ruleIdx) as FormGroup;
    const control = ruleGroup.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  onClose(): void {
    this.close.emit();
  }

  onSubmit(): void {
    if (this.flagForm.invalid) {
      return;
    }

    const formVal = this.flagForm.getRawValue();
    
    // Trim rules text parameters
    const processedRules = (formVal.rules || []).map((r: FlagRule) => ({
      ...r,
      attributeKey: r.attributeKey.trim(),
      attributeValue: r.attributeValue.trim()
    }));

    const result: FeatureFlag = {
      ...formVal,
      key: formVal.key.trim(),
      name: formVal.name.trim(),
      description: formVal.description?.trim() || '',
      rules: processedRules
    };

    this.save.emit(result);
  }
}
