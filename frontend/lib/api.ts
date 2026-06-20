const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

async function request<T>(path: string, opts?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...opts,
    headers: { ...opts?.headers },
  })

  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw new Error(body?.message || `Erro ${res.status}`)
  }

  return res.json()
}

export const api = {
  get: <T>(path: string) => request<T>(path),

  post: <T>(path: string, body?: unknown) =>
    request<T>(path, {
      method: 'POST',
      headers:
        body instanceof FormData ? {} : { 'Content-Type': 'application/json' },
      body:
        body instanceof FormData
          ? body
          : body
            ? JSON.stringify(body)
            : undefined,
    }),

  upload: <T>(path: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<T>(path, { method: 'POST', body: form })
  },
}
