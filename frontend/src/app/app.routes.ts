import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/shell').then((m) => m.Shell),
    children: [
      { path: 'customers', loadComponent: () => import('./features/customers/customers').then((m) => m.Customers) },
      { path: 'accounts', loadComponent: () => import('./features/accounts/accounts').then((m) => m.Accounts) },
      {
        path: 'transactions',
        loadComponent: () => import('./features/transactions/transactions').then((m) => m.Transactions),
      },
      {
        path: 'transactions/:id',
        loadComponent: () =>
          import('./features/transactions/transaction-detail/transaction-detail').then(
            (m) => m.TransactionDetail,
          ),
      },
      { path: '', redirectTo: 'customers', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '' },
];
