import { describe, expect, it, vi } from 'vitest';
import { z } from 'zod';
import { ApiError } from './apiError';
import { createHttpClient, type HttpClientOptions } from './httpClient';

const ItemSchema = z.object({ name: z.string() });

function client(fetchFn: typeof fetch, overrides: Partial<HttpClientOptions> = {}) {
  return createHttpClient({
    basePath: '/api',
    csrfCookieName: 'XSRF-TOKEN',
    csrfHeaderName: 'X-XSRF-TOKEN',
    csrfPrimePath: '/auth/me',
    readCookie: () => 'token-1',
    origin: () => 'http://app.test',
    fetchFn,
    ...overrides,
  });
}

const json = (body: unknown, status = 200, contentType = 'application/json') =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': contentType } });

describe('httpClient', () => {
  it('sends the CSRF header and same-origin credentials on POST', async () => {
    const fetchFn = vi.fn<typeof fetch>().mockResolvedValue(json({ name: 'ok' }));
    await client(fetchFn).post('/things', { a: 1 }, ItemSchema);

    const [url, init] = fetchFn.mock.calls[0];
    expect(url).toBe('http://app.test/api/things');
    expect(init?.credentials).toBe('same-origin');
    expect((init?.headers as Record<string, string>)['X-XSRF-TOKEN']).toBe('token-1');
    expect(init?.body).toBe('{"a":1}');
  });

  it('does not send the CSRF header on GET', async () => {
    const fetchFn = vi.fn<typeof fetch>().mockResolvedValue(json({ name: 'ok' }));
    await client(fetchFn).get('/things/1', ItemSchema);
    expect((fetchFn.mock.calls[0][1]?.headers as Record<string, string>)['X-XSRF-TOKEN']).toBeUndefined();
  });

  it('primes the CSRF cookie with one GET when it is missing, then uses it', async () => {
    let cookie: string | null = null;
    const fetchFn = vi.fn<typeof fetch>().mockImplementation(async (input) => {
      if (String(input).endsWith('/auth/me')) {
        cookie = 'fresh-token';
        return json({ status: 401 }, 401);
      }
      return json({ name: 'ok' });
    });
    await client(fetchFn, { readCookie: () => cookie }).post('/things', {}, ItemSchema);

    expect(fetchFn).toHaveBeenCalledTimes(2);
    expect(String(fetchFn.mock.calls[0][0])).toBe('http://app.test/api/auth/me');
    expect((fetchFn.mock.calls[1][1]?.headers as Record<string, string>)['X-XSRF-TOKEN']).toBe('fresh-token');
  });

  it('maps a problem+json error to ApiError with the server detail', async () => {
    const fetchFn = vi.fn<typeof fetch>().mockResolvedValue(
      json({ title: 'Unauthorized', status: 401, detail: 'Invalid email or password' }, 401, 'application/problem+json'),
    );
    const error = await client(fetchFn).post('/auth/login', {}, ItemSchema).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ kind: 'http', status: 401, detail: 'Invalid email or password' });
  });

  it('maps a non-JSON error body to ApiError without a detail', async () => {
    const fetchFn = vi.fn<typeof fetch>().mockResolvedValue(new Response('<html>Bad gateway</html>', { status: 502 }));
    await expect(client(fetchFn).get('/x', ItemSchema)).rejects.toMatchObject({ kind: 'http', status: 502, detail: null });
  });

  it('reports a network failure as kind "network"', async () => {
    const fetchFn = vi.fn<typeof fetch>().mockRejectedValue(new TypeError('Failed to fetch'));
    await expect(client(fetchFn).get('/x', ItemSchema)).rejects.toMatchObject({ kind: 'network', status: 0 });
  });

  it('rejects a response that does not match the expected schema', async () => {
    const fetchFn = vi.fn<typeof fetch>().mockResolvedValue(json({ unexpected: true }));
    await expect(client(fetchFn).get('/x', ItemSchema)).rejects.toMatchObject({ kind: 'invalid-response' });
  });
});
