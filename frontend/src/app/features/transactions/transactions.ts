import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
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
import { Router } from '@angular/router';
import { TransactionService } from '../../core/services/transaction.service';
import { Transaction, TransactionType } from '../../core/models/api.models';

const TRANSACTION_TYPES: TransactionType[] = ['DEPOSIT', 'WITHDRAWAL', 'TRANSFER'];

@Component({
  selector: 'app-transactions',
  imports: [
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    MatButton,
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
    MatPaginator,
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
  templateUrl: './transactions.html',
  styleUrl: './transactions.scss',
})
export class Transactions {
  private readonly transactionService = inject(TransactionService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  protected readonly transactionTypes = TRANSACTION_TYPES;

  protected readonly transactions = signal<Transaction[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly showForm = signal(false);
  protected readonly saving = signal(false);
  protected readonly displayedColumns = ['accountId', 'type', 'amount', 'status', 'createdAt'];

  private readonly pageIndex = signal(0);
  private readonly pageSize = signal(5);

  protected readonly pagedTransactions = computed(() => {
    const start = this.pageIndex() * this.pageSize();
    return this.transactions().slice(start, start + this.pageSize());
  });

  protected readonly form = this.fb.nonNullable.group({
    accountId: ['', Validators.required],
    type: this.fb.nonNullable.control<TransactionType>('DEPOSIT', Validators.required),
    amount: [0, [Validators.required, Validators.min(0.01)]],
    destinationAccountId: [''],
  });

  protected readonly isTransfer = signal(false);

  constructor() {
    this.load();

    this.form.controls.type.valueChanges.pipe(takeUntilDestroyed()).subscribe((type) => {
      this.isTransfer.set(type === 'TRANSFER');
      const destinationControl = this.form.controls.destinationAccountId;
      if (type === 'TRANSFER') {
        destinationControl.addValidators(Validators.required);
      } else {
        destinationControl.clearValidators();
        destinationControl.setValue('');
      }
      destinationControl.updateValueAndValidity();
    });
  }

  private load(): void {
    this.loading.set(true);
    this.transactionService.list().subscribe({
      next: (transactions) => {
        this.transactions.set(transactions);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Unable to load transactions.');
        this.loading.set(false);
      },
    });
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  toggleForm(): void {
    this.showForm.update((value) => !value);
  }

  submit(): void {
    if (this.form.invalid || this.saving()) {
      return;
    }
    this.saving.set(true);
    const value = this.form.getRawValue();
    this.transactionService
      .create({
        accountId: value.accountId,
        type: value.type,
        amount: value.amount,
        destinationAccountId: value.type === 'TRANSFER' ? value.destinationAccountId : undefined,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.showForm.set(false);
          this.form.reset({ accountId: '', type: 'DEPOSIT', amount: 0, destinationAccountId: '' });
          this.load();
        },
        error: () => {
          this.saving.set(false);
          this.errorMessage.set('Unable to create transaction.');
        },
      });
  }

  viewDetail(transaction: Transaction): void {
    this.router.navigate(['/transactions', transaction.id]);
  }
}
