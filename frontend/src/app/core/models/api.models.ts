export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export interface Customer {
  id: string;
  name: string;
  email: string;
  phone: string;
  address: string;
}

export type CustomerInput = Omit<Customer, 'id'>;

export type AccountType = 'CHECKING' | 'SAVINGS';
export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'CLOSED';

export interface Account {
  id: string;
  customerId: string;
  accountNumber: string;
  accountType: AccountType;
  balance: number;
  status: AccountStatus;
}

export type AccountInput = Omit<Account, 'id'>;

export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER';

export interface Transaction {
  id: string;
  accountId: string;
  type: TransactionType;
  amount: number;
  destinationAccountId?: string;
  status: string;
  createdAt: string;
}

export interface TransactionInput {
  accountId: string;
  type: TransactionType;
  amount: number;
  destinationAccountId?: string;
}
