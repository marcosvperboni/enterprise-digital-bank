import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { Customers } from './customers';
import { Customer } from '../../core/models/api.models';
import { environment } from '../../../environments/environment';

describe('Customers', () => {
  let fixture: ComponentFixture<Customers>;
  let component: Customers;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/api/customers`;

  const dialogMock = {
    open: jest.fn().mockReturnValue({ afterClosed: () => of(true) } as unknown as MatDialogRef<unknown>),
  };

  beforeEach(async () => {
    dialogMock.open.mockClear();

    await TestBed.configureTestingModule({
      imports: [Customers],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: MatDialog, useValue: dialogMock }],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Customers);
    component = fixture.componentInstance;

    httpMock.expectOne(baseUrl).flush([]);
  });

  afterEach(() => httpMock.verify());

  it('creates a customer and reloads the list', () => {
    component.openCreateForm();
    component['form'].setValue({ name: 'Ada Lovelace', email: 'ada@example.com', phone: '', address: '' });

    component.submit();

    const createReq = httpMock.expectOne(baseUrl);
    expect(createReq.request.method).toBe('POST');
    expect(createReq.request.body).toEqual({
      name: 'Ada Lovelace',
      email: 'ada@example.com',
      phone: '',
      address: '',
    });
    createReq.flush({ id: '1', name: 'Ada Lovelace', email: 'ada@example.com', phone: '', address: '' });

    httpMock
      .expectOne(baseUrl)
      .flush([{ id: '1', name: 'Ada Lovelace', email: 'ada@example.com', phone: '', address: '' }]);

    expect(component['showForm']()).toBe(false);
    expect(component['customers']()).toHaveLength(1);
  });

  it('deletes a customer after confirmation and reloads the list', () => {
    const customer: Customer = { id: '1', name: 'Ada Lovelace', email: 'ada@example.com', phone: '', address: '' };

    component.remove(customer);

    expect(dialogMock.open).toHaveBeenCalled();

    const deleteReq = httpMock.expectOne(`${baseUrl}/1`);
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush(null);

    httpMock.expectOne(baseUrl).flush([]);

    expect(component['customers']()).toHaveLength(0);
  });

  it('does not delete when the confirmation dialog is dismissed', () => {
    dialogMock.open.mockReturnValueOnce({ afterClosed: () => of(false) } as unknown as MatDialogRef<unknown>);
    const customer: Customer = { id: '1', name: 'Ada Lovelace', email: 'ada@example.com', phone: '', address: '' };

    component.remove(customer);

    httpMock.expectNone(`${baseUrl}/1`);
  });
});
