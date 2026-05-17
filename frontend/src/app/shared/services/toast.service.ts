import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ToastMessage {
  id: number;
  type: 'success' | 'error';
  text: string;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastsSubject = new BehaviorSubject<ToastMessage[]>([]);
  toasts$ = this.toastsSubject.asObservable();
  private nextId = 0;

  success(text: string): void {
    this.addToast('success', text);
  }

  error(text: string): void {
    this.addToast('error', text);
  }

  private addToast(type: 'success' | 'error', text: string): void {
    const id = this.nextId++;
    const current = this.toastsSubject.value;
    this.toastsSubject.next([...current, { id, type, text }]);

    // Auto-remove after 4 seconds
    setTimeout(() => this.remove(id), 4000);
  }

  remove(id: number): void {
    const current = this.toastsSubject.value.filter(t => t.id !== id);
    this.toastsSubject.next(current);
  }
}
