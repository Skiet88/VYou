import type { ZodType } from 'zod';
import { ProblemDetailSchema } from '../schemas/problem';
import { ApiError } from './apiError';

export interface HttpClient {
  get<T>(path: string, schema: ZodType<T>): Promise<T>;
  post<T>(path: string, body: unknown, schema: ZodType<T>): Promise<T>;
  /** For endpoints that return no body (e.g. logout). */
  postWithoutResponse(path: string, body?: unknown): Promise<void>;
}

export interface HttpClientOptions {
  basePath: string;
  csrfCookieName: string;
  csrfHeaderName: string;
  /** A GET that makes the backend issue the CSRF cookie, used once if it is missing. */
  csrfPrimePath: string;
  fetchFn?: typeof fetch;
  readCookie: (name: string) => string | null;
  origin?: () => string;
}

type Method = 'GET' | 'POST';

/**
 * Thin fetch wrapper: same-origin credentials (the httpOnly session cookie), the double-submit
 * CSRF header on every state-changing request, problem+json errors mapped to ApiError, and every
 * response body validated against a schema before it reaches the rest of the app.
 */
export function createHttpClient(options: HttpClientOptions): HttpClient {
  const fetchFn = options.fetchFn ?? ((...args: Parameters<typeof fetch>) => fetch(...args));
  const origin = options.origin ?? (() => window.location.origin);
  const url = (path: string) => new URL(options.basePath + path, origin()).toString();

  async function csrfToken(): Promise<string | null> {
    let token = options.readCookie(options.csrfCookieName);
    if (token === null) {
      // First state-changing request from a fresh visitor: any request makes the backend set the
      // cookie, so make one, then read it. If it is still missing the server will reject with 403.
      try {
        await fetchFn(url(options.csrfPrimePath), { method: 'GET', credentials: 'same-origin' });
      } catch {
        // Surface the real failure on the actual request below instead.
      }
      token = options.readCookie(options.csrfCookieName);
    }
    return token;
  }

  async function send(method: Method, path: string, body?: unknown): Promise<Response> {
    const headers: Record<string, string> = { Accept: 'application/json, application/problem+json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (method !== 'GET') {
      const token = await csrfToken();
      if (token !== null) headers[options.csrfHeaderName] = token;
    }

    let response: Response;
    try {
      response = await fetchFn(url(path), {
        method,
        headers,
        credentials: 'same-origin',
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch {
      throw new ApiError('network', 0, null);
    }

    if (!response.ok) throw await toApiError(response);
    return response;
  }

  async function parse<T>(response: Response, schema: ZodType<T>): Promise<T> {
    let json: unknown;
    try {
      json = await response.json();
    } catch {
      throw new ApiError('invalid-response', response.status, null);
    }
    const result = schema.safeParse(json);
    if (!result.success) throw new ApiError('invalid-response', response.status, null);
    return result.data;
  }

  return {
    async get(path, schema) {
      return parse(await send('GET', path), schema);
    },
    async post(path, body, schema) {
      return parse(await send('POST', path, body), schema);
    },
    async postWithoutResponse(path, body) {
      await send('POST', path, body);
    },
  };
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const result = ProblemDetailSchema.safeParse(await response.json());
    if (result.success) {
      return new ApiError('http', response.status, result.data.detail ?? null, result.data.reasons ?? []);
    }
  } catch {
    // Not JSON (e.g. a bare 403 or a proxy error page): fall through to a detail-less error.
  }
  return new ApiError('http', response.status, null);
}
