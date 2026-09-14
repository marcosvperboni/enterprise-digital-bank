import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TransactionService } from '../../../core/services/transaction.service';
import { Transaction } from '../../../core/models/api.models';

@Component({
  selector: 'app-transaction-detail',
  imports: [DatePipe, DecimalPipe, MatButton, MatCard, MatCardContent, MatIcon, MatProgressSpinner, RouterLink],
  templateUrl: './transaction-detail.html',
  styleUrl: './transaction-detail.scss',
})
export class TransactionDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly transactionService = inject(TransactionService);

  protected readonly transaction = signal<Transaction | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loading.set(false);
      this.errorMessage.set('Transaction not found.');
      return;
    }

    this.transactionService.get(id).subscribe({
      next: (transaction) => {
        this.transaction.set(transaction);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Unable to load this transaction.');
        this.loading.set(false);
      },
    });
  }
}
