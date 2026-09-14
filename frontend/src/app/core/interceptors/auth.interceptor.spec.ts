import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpMock.verify());

  it('attaches the bearer token to outgoing requests', () => {
    jest.spyOn(authService, 'token').mockReturnValue('test-token');

    httpClient.get('/api/customers').subscribe();

    const req = httpMock.expectOne('/api/customers');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');
    req.flush({});
  });

  it('does not attach a header when there is no token', () => {
    jest.spyOn(authService, 'token').mockReturnValue(null);

    httpClient.get('/api/customers').subscribe();

    const req = httpMock.expectOne('/api/customers');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('logs out and redirects to /login on a 401 response', () => {
    jest.spyOn(authService, 'token').mockReturnValue('expired-token');
    const logoutSpy = jest.spyOn(authService, 'logout');
    const navigateSpy = jest.spyOn(router, 'navigate');

    httpClient.get('/api/customers').subscribe({ error: () => undefined });

    const req = httpMock.expectOne('/api/customers');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(logoutSpy).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });
});
