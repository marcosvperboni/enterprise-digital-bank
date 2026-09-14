import { Injectable, effect, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const THEME_KEY = 'theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>(this.resolveInitialTheme());

  constructor() {
    effect(() => {
      const theme = this.theme();
      document.documentElement.setAttribute('data-theme', theme);
      localStorage.setItem(THEME_KEY, theme);
    });
  }

  toggle(): void {
    this.theme.set(this.theme() === 'light' ? 'dark' : 'light');
  }

  private resolveInitialTheme(): Theme {
    const stored = localStorage.getItem(THEME_KEY);
    if (stored === 'light' || stored === 'dark') {
      return stored;
    }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
}
