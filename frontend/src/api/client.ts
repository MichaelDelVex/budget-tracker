import type { ApiErrorResponse } from '../types/api';

export class ApiError extends Error {
  fields: Record<string, string>;

  constructor(message: string, fields: Record<string, string> = {}) {
    super(message);
    this.fields = fields;
  }
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      headers: options.body instanceof FormData ? options.headers : {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    });
  } catch {
    throw new ApiError('Unable to reach the API. Check that the backend is running.');
  }

  if (!response.ok) {
    let error: ApiErrorResponse = { message: `Request failed with status ${response.status}` };
    try {
      error = await response.json();
    } catch {
      // Keep the status-based fallback.
    }
    throw new ApiError(error.message, error.fields ?? {});
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}
