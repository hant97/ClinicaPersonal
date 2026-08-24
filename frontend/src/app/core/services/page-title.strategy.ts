import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';

import { ActivatedRouteSnapshot, RouterStateSnapshot, TitleStrategy } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class PageTitleStrategy extends TitleStrategy {
  private readonly brand = 'Clínica Personal';

  constructor(@Inject(DOCUMENT) private readonly document: Document) {
    super();
  }

  override buildTitle(snapshot: RouterStateSnapshot): string | undefined {
    let title: string | undefined;
    let route: ActivatedRouteSnapshot | null = snapshot.root;
    while (route) {
      if (route.title !== undefined) {
        title = route.title;
      }
      route = route.firstChild;
    }
    return title;
  }

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const routeTitle = this.buildTitle(snapshot);
    this.document.title = routeTitle ? `${routeTitle} | ${this.brand}` : this.brand;
  }
}
