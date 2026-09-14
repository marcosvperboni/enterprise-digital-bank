import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable,
} from '@angular/material/table';
import { ConfirmDialog } from '../../shared/confirm-dialog/confirm-dialog';
import { AccountService } from '../../core/services/account.service';
import { Account, AccountStatus, AccountType } from '../../core/models/api.models';

const ACCOUNT_TYPES: AccountType[] = ['CHECKING', 'SAVINGS'];
const ACCOUNT_STATUSES: AccountStatus[] = ['ACTIVE', 'INACTIVE', 'CLOSED'];

@Component({
  selector: 'app-accounts',
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    MatButton,
    MatIconButton,
    MatCard,
    MatCardContent,
    MatFormField,
    MatLabel,
    MatError,
    MatInput,
    MatSelect,
    MatOption,
    MatIcon,
    MatProgressSpinner,
    MatTable,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderCellDef,
    MatCell,
    MatCellDef,
    MatHeaderRow,
    MatHeaderRowDef,
    MatRow,
    MatRowDef,
  ],
  templateUrl: './accounts.html',
  styleUrl: './accounts.scss',
})
export class Accounts {
  private readonly accountService = inject(AccountService);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);

  protected readonly accountTypes = ACCOUNT_TYPES;
  protected readonly accountStatuses = ACCOUNT_STATUSES;

  protected readonly accounts = signal<Account[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly showForm = signal(false);
  protected readonly saving = signal(false);
  protected readonly editingAccount = signal<Account | null>(null);
  protected readonly displayedColumns = [
    'customerId',
    'accountNumber',
    'accountType',
    'balance',
    'status',
    'actions',
  ];

  protected readonly form = this.fb.nonNullable.group({
    customerId: ['', Validators.required],
    accountNumber: ['', Validators.required],
    accountType: this.fb.nonNullable.control<AccountType>('CHECKING', Validators.required),
    balance: [0, [Validators.required, Validators.min(0)]],
    status: this.fb.nonNullable.control<AccountStatus>('ACTIVE', Validators.required),
  });

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.accountService.list().subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Unable to load accounts.');
        this.loading.set(false);
      },
    });
  }

  openCreateForm(): void {
    this.editingAccount.set(null);
    this.form.reset({ customerId: '', accountNumber: '', accountType: 'CHECKING', balance: 0, status: 'ACTIVE' });
    this.showForm.set(true);
  }

  openEditForm(account: Account): void {
    this.editingAccount.set(account);
    this.form.reset(account);
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingAccount.set(null);
  }

  submit(): void {
    if (this.form.invalid || this.saving()) {
      return;
    }
    this.saving.set(true);
    const value = this.form.getRawValue();
    const editing = this.editingAccount();
    const request$ = editing ? this.accountService.update(editing.id, value) : this.accountService.create(value);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.editingAccount.set(null);
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.errorMessage.set(editing ? 'Unable to update account.' : 'Unable to create account.');
      },
    });
  }

  remove(account: Account): void {
    const ref = this.dialog.open(ConfirmDialog, {
      data: {
        title: 'Remove account',
        message: `Are you sure you want to remove account ${account.accountNumber}? This action cannot be undone.`,
        confirmLabel: 'Remove',
      },
    });

    ref.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.accountService.delete(account.id).subscribe(() => this.load());
    });
  }
}
