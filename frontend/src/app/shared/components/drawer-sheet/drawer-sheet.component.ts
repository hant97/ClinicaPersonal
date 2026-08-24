import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, X } from 'lucide-angular';
import { FocusTrapDirective } from '../../directives/focus-trap.directive';

@Component({
  selector: 'app-drawer-sheet',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, FocusTrapDirective],
  template: `
    @if (isOpen) {
      <div
        class="fixed inset-0 z-50 overflow-hidden"
        role="dialog"
        [attr.aria-modal]="isOpen"
        [attr.aria-label]="title || 'Panel lateral'"
        appFocusTrap
        (trapClose)="close()"
      >
        <!-- Backdrop -->
        <div
          class="fixed inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity"
          aria-hidden="true"
          (click)="close()"
        ></div>

        <div class="fixed inset-y-0 right-0 flex max-w-full pl-0 sm:pl-10">
          <div
            class="w-screen bg-surface shadow-floating border-l border-line flex flex-col transform transition-transform ease-in-out duration-300"
            [ngClass]="widthClass"
          >
            <!-- Header -->
            <div class="flex items-center justify-between px-4 sm:px-6 py-3.5 sm:py-4 border-b border-line bg-slate-50/50">
              <div class="min-w-0 flex-1 pr-3 sm:pr-4">
                <h3 class="text-base font-semibold text-ink truncate">{{ title }}</h3>
                @if (subtitle) {
                  <p class="text-xs text-muted truncate mt-0.5">{{ subtitle }}</p>
                }
              </div>
              <div class="flex items-center gap-2">
                <ng-content select="[drawer-header-actions]"></ng-content>
                <button
                  type="button"
                  class="btn-text touch-icon-target !p-1.5"
                  aria-label="Cerrar panel"
                  (click)="close()"
                >
                  <lucide-icon [img]="X" [size]="18"></lucide-icon>
                </button>
              </div>
            </div>

            <!-- Body -->
            <div class="flex-1 overflow-y-auto p-4 sm:p-6 scroll-touch">
              <ng-content></ng-content>
            </div>

            <!-- Footer -->
            <div class="border-t border-line px-4 sm:px-6 py-3 bg-slate-50/80 flex items-center justify-end gap-2.5 sm:gap-3 safe-bottom">
              <ng-content select="[drawer-footer]"></ng-content>
            </div>
          </div>
        </div>
      </div>
    }
  `,
})
export class DrawerSheetComponent {
  readonly X = X;

  @Input() isOpen: boolean = false;
  @Input() title: string = '';
  @Input() subtitle: string = '';
  @Input() widthClass: string = 'max-w-xl';
  @Output() closed = new EventEmitter<void>();

  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKey(event: Event): void {
    if (this.isOpen) {
      event.preventDefault();
      this.close();
    }
  }

  close(): void {
    this.closed.emit();
  }
}
