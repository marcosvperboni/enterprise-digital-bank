import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
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
import { ConfirmDialog } from '../../shared/confirm-dialog/confirm-dialog';
import { CustomerService } from '../../core/services/customer.service';
import { Customer } from '../../core/models/api.models';

@Component({
  selector: 'app-customers',
  imports: [
    ReactiveFormsModule,
    MatButton,
    MatIconButton,
    MatCard,
    MatCardContent,
    MatFormField,
    MatLabel,
    MatError,
    MatInput,
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
  templateUrl: './customers.html',
  styleUrl: './customers.scss',
})
export class Customers {
  private readonly customerService = inject(CustomerService);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);

  protected readonly customers = signal<Customer[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly showForm = signal(false);
  protected readonly saving = signal(false);
  protected readonly editingCustomer = signal<Customer | null>(null);
  protected readonly displayedColumns = ['name', 'email', 'phone', 'address', 'actions'];

  private readonly pageIndex = signal(0);
  private readonly pageSize = signal(5);

  protected readonly pagedCustomers = computed(() => {
    const start = this.pageIndex() * this.pageSize();
    return this.customers().slice(start, start + this.pageSize());
  });

  protected readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    address: [''],
  });

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.customerService.list().subscribe({
      next: (customers) => {
        this.customers.set(customers);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Unable to load customers.');
        this.loading.set(false);
      },
    });
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  openCreateForm(): void {
    this.editingCustomer.set(null);
    this.form.reset({ name: '', email: '', phone: '', address: '' });
    this.showForm.set(true);
  }

  openEditForm(customer: Customer): void {
    this.editingCustomer.set(customer);
    this.form.reset(customer);
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingCustomer.set(null);
  }

  submit(): void {
    if (this.form.invalid || this.saving()) {
      return;
    }
    this.saving.set(true);
    const value = this.form.getRawValue();
    const editing = this.editingCustomer();
    const request$ = editing ? this.customerService.update(editing.id, value) : this.customerService.create(value);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.editingCustomer.set(null);
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.errorMessage.set(editing ? 'Unable to update customer.' : 'Unable to create customer.');
      },
    });
  }

  remove(customer: Customer): void {
    const ref = this.dialog.open(ConfirmDialog, {
      data: {
        title: 'Remove customer',
        message: `Are you sure you want to remove ${customer.name}? This action cannot be undone.`,
        confirmLabel: 'Remove',
      },
    });

    ref.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.customerService.delete(customer.id).subscribe(() => this.load());
    });
  }
}
