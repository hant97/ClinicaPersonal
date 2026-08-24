import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FocusTrapDirective } from './focus-trap.directive';

@Component({
  standalone: true,
  imports: [FocusTrapDirective],
  template: `
    <button id="trigger" type="button">Abrir</button>
    @if (isOpen) {
      <section appFocusTrap (trapClose)="isOpen = false">
        <button id="first" type="button">Primero</button>
        <button id="last" type="button">Último</button>
      </section>
    }
  `
})
class FocusTrapHostComponent {
  isOpen = false;
}

describe('FocusTrapDirective', () => {
  let fixture: ComponentFixture<FocusTrapHostComponent>;
  let component: FocusTrapHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FocusTrapHostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(FocusTrapHostComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('focuses the first control and restores the trigger after Escape', () => {
    const trigger = fixture.nativeElement.querySelector('#trigger') as HTMLButtonElement;
    trigger.focus();

    component.isOpen = true;
    fixture.detectChanges();

    const trap = fixture.nativeElement.querySelector('section') as HTMLElement;
    const first = fixture.nativeElement.querySelector('#first') as HTMLButtonElement;
    expect(document.activeElement).toBe(first);

    trap.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();

    expect(component.isOpen).toBeFalse();
    expect(document.activeElement).toBe(trigger);
  });

  it('cycles focus from the last control to the first with Tab', () => {
    component.isOpen = true;
    fixture.detectChanges();

    const trap = fixture.nativeElement.querySelector('section') as HTMLElement;
    const first = fixture.nativeElement.querySelector('#first') as HTMLButtonElement;
    const last = fixture.nativeElement.querySelector('#last') as HTMLButtonElement;
    last.focus();

    trap.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true }));

    expect(document.activeElement).toBe(first);
  });
});
