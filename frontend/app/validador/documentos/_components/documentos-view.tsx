'use client'

import { useState, useEffect, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import {
  Loader2,
  AlertTriangle,
  FileText,
  ChevronLeft,
  ChevronRight,
  PlayCircle,
  Inbox,
} from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import type {
  DocumentResponse,
  PageResponse,
  ValidationReportResponse,
} from '@/lib/types'

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatDate(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function statusBadgeClass(status: DocumentResponse['status']): string {
  switch (status) {
    case 'UPLOADED':
      return 'badge-info'
    case 'EXTRACTED':
      return 'badge-purple'
    case 'VALIDATED':
      return 'badge-success'
    case 'FAILED':
      return 'badge-danger'
  }
}

function statusLabel(status: DocumentResponse['status']): string {
  switch (status) {
    case 'UPLOADED':
      return 'Enviado'
    case 'EXTRACTED':
      return 'Extraido'
    case 'VALIDATED':
      return 'Validado'
    case 'FAILED':
      return 'Falhou'
  }
}

export function DocumentosView() {
  const router = useRouter()
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<DocumentResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [validatingId, setValidatingId] = useState<number | null>(null)

  const fetchDocuments = useCallback(async (p: number) => {
    setLoading(true)
    setError(null)
    try {
      const result = await api.get<PageResponse<DocumentResponse>>(
        `/api/v1/documents?page=${p}&size=20`
      )
      setData(result)
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Erro ao carregar documentos.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchDocuments(page)
  }, [page, fetchDocuments])

  const handleValidate = useCallback(
    async (id: number, e: React.MouseEvent) => {
      e.stopPropagation()
      setValidatingId(id)
      try {
        const result = await api.post<ValidationReportResponse>(
          `/api/v1/documents/${id}/validate`
        )
        toast.success('Validacao concluida!')
        router.push(`/validador/relatorio/${result.reportId}`)
      } catch (err) {
        const msg = err instanceof Error ? err.message : 'Erro ao validar documento.'
        toast.error(msg)
      } finally {
        setValidatingId(null)
      }
    },
    [router]
  )

  const handleRowClick = useCallback(
    (doc: DocumentResponse) => {
      if (doc.status === 'VALIDATED') {
        router.push(`/validador/relatorio/${doc.id}`)
      }
    },
    [router]
  )

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">Documentos</h1>
        <p className="page-subtitle">Lista de documentos enviados para validacao</p>
      </div>

      {/* Loading */}
      {loading && (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}>
          <Loader2 size={36} className="animate-spin" style={{ color: 'var(--clr-brand)' }} />
        </div>
      )}

      {/* Error */}
      {error && !loading && (
        <div className="alert alert-danger">
          <AlertTriangle size={20} />
          <span>{error}</span>
        </div>
      )}

      {/* Empty */}
      {!loading && !error && data && data.content.length === 0 && (
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            padding: '4rem 2rem',
            gap: '1rem',
          }}
        >
          <Inbox size={48} style={{ color: 'var(--text-muted)' }} />
          <p style={{ color: 'var(--text-muted)', margin: 0 }}>
            Nenhum documento encontrado
          </p>
        </div>
      )}

      {/* Table */}
      {!loading && !error && data && data.content.length > 0 && (
        <>
          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Nome do Arquivo</th>
                  <th>Tipo</th>
                  <th>Tamanho</th>
                  <th>Status</th>
                  <th>Data</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((doc) => (
                  <tr
                    key={doc.id}
                    onClick={() => handleRowClick(doc)}
                    style={{
                      cursor: doc.status === 'VALIDATED' ? 'pointer' : 'default',
                    }}
                  >
                    <td>{doc.id}</td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <FileText size={16} style={{ color: 'var(--text-muted)', flexShrink: 0 }} />
                        <span
                          style={{
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            maxWidth: 300,
                          }}
                        >
                          {doc.originalFileName}
                        </span>
                      </div>
                    </td>
                    <td>{doc.documentType}</td>
                    <td>{formatFileSize(doc.fileSize)}</td>
                    <td>
                      <span className={statusBadgeClass(doc.status)}>
                        {statusLabel(doc.status)}
                      </span>
                    </td>
                    <td style={{ whiteSpace: 'nowrap' }}>{formatDate(doc.createdAt)}</td>
                    <td>
                      {(doc.status === 'UPLOADED' || doc.status === 'EXTRACTED') && (
                        <button
                          className="btn btn-primary btn-sm"
                          onClick={(e) => handleValidate(doc.id, e)}
                          disabled={validatingId === doc.id}
                        >
                          {validatingId === doc.id ? (
                            <Loader2 size={14} className="animate-spin" />
                          ) : (
                            <>
                              <PlayCircle size={14} />
                              <span style={{ marginLeft: '0.25rem' }}>Validar</span>
                            </>
                          )}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginTop: '1rem',
              flexWrap: 'wrap',
              gap: '0.5rem',
            }}
          >
            <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
              Total: {data.totalElements} documentos
            </span>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                <ChevronLeft size={16} />
                Anterior
              </button>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', padding: '0 0.5rem' }}>
                Pagina {data.number + 1} de {data.totalPages || 1}
              </span>
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= data.totalPages - 1}
              >
                Proximo
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
