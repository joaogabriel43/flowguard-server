import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { AuthService } from './auth.service';
import { fetchEventSource } from '@microsoft/fetch-event-source';

export interface SseEvent {
  type: string;
  data: any;
}

@Injectable({
  providedIn: 'root'
})
export class SseService {
  private authService = inject(AuthService);

  private isConnectedSubject = new BehaviorSubject<boolean>(false);
  isConnected$ = this.isConnectedSubject.asObservable();

  private eventSubject = new Subject<SseEvent>();
  events$ = this.eventSubject.asObservable();

  private abortController: AbortController | null = null;

  connect(): void {
    if (this.isConnectedSubject.value) {
      return; // Already connected
    }

    const token = this.authService.getToken();
    if (!token) {
      console.warn('SSE Connection skipped: No JWT token in memory.');
      return;
    }

    this.abortController = new AbortController();

    fetchEventSource('/api/sse/flags', {
      headers: {
        'Authorization': `Bearer ${token}`
      },
      signal: this.abortController.signal,
      openWhenHidden: true,
      onopen: async (response) => {
        if (response.ok) {
          this.isConnectedSubject.next(true);
          console.info('Successfully established authorized FlowGuard SSE connection.');
        } else {
          this.isConnectedSubject.next(false);
          console.warn(`SSE connection failed with HTTP status: ${response.status}`);
        }
      },
      onmessage: (msg) => {
        if (msg.event && msg.data) {
          try {
            const data = JSON.parse(msg.data);
            this.eventSubject.next({ type: msg.event, data });
          } catch (e) {
            console.error('Failed to parse SSE event message data:', e, msg.data);
          }
        }
      },
      onclose: () => {
        this.isConnectedSubject.next(false);
        console.info('FlowGuard SSE connection closed by server.');
      },
      onerror: (err) => {
        this.isConnectedSubject.next(false);
        console.error('FlowGuard SSE stream encountered communication failure:', err);
        // Let it retry automatically unless aborted
      }
    });
  }

  disconnect(): void {
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = null;
    }
    this.isConnectedSubject.next(false);
    console.info('FlowGuard SSE connection manually disconnected.');
  }
}
