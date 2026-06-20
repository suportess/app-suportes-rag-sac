'use client'

import { useState, useRef, useCallback } from 'react'
import {
  UploadCloud,
  FileText,
  X,
  Loader2,
  AlertTriangle,
  CheckCircle,
} from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import type { ValidationReportResponse } from '@/lib/types'
import { ReportDisplay } from './report-display'

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

const ACCEPTED_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
]
const ACCEPTED_EXTENSIONS = ['.pdf', '.docx']

export function ValidadorView() {
  const [file, setFile] = useState<File | null>(null)
  const [dragging, setDragging] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [report, setReport] = useState<ValidationReportResponse | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  const isAccepted = (f: File) => {
    if (ACCEPTED_TYPES.includes(f.type)) return true
    const ext = f.name.substring(f.name.lastIndexOf('.')).toLowerCase()
    return ACCEPTED_EXTENSIONS.includes(ext)
  }

  const handleFile = useCallback((f: File) => {
    if (!isAccepted(f)) {
      toast.error('Formato invalido. Selecione um arquivo PDF ou DOCX.')
      return
    }
    setFile(f)
    setError(null)
    setReport(null)
  }, [])

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      setDragging(false)
      const dropped = e.dataTransfer.files[0]
      if (dropped) handleFile(dropped)
    },
    [handleFile]
  )

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setDragging(true)
  }, [])

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setDragging(false)
  }, [])

  const handleInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const selected = e.target.files?.[0]
      if (selected) handleFile(selected)
    },
    [handleFile]
  )

  const clearFile = useCallback(() => {
    setFile(null)
    setReport(null)
    setError(null)
    if (inputRef.current) inputRef.current.value = ''
  }, [])

  const handleValidate = useCallback(async () => {
    if (!file) return
    setLoading(true)
    setError(null)
    setReport(null)
    try {
      const result = await api.upload<ValidationReportResponse>(
        '/api/v1/documents/validate',
        file
      )
      setReport(result)
      toast.success('Validacao concluida com sucesso!')
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Erro ao validar o documento.'
      setError(msg)
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }, [file])

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">Validador de Especificacoes</h1>
        <p className="page-subtitle">
          Envie sua especificacao SAP ABAP para validacao automatica por IA
        </p>
      </div>

      {/* Upload Section */}
      <div className="card card-p" style={{ marginBottom: '1.5rem' }}>
        <input
          ref={inputRef}
          type="file"
          accept=".pdf,.docx"
          onChange={handleInputChange}
          style={{ display: 'none' }}
        />

        {!file ? (
          <div
            role="button"
            tabIndex={0}
            onClick={() => inputRef.current?.click()}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') inputRef.current?.click()
            }}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            style={{
              border: `2px dashed ${dragging ? 'var(--clr-brand)' : 'var(--d2b-border)'}`,
              borderRadius: '0.75rem',
              padding: '3rem 2rem',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '0.75rem',
              cursor: 'pointer',
              backgroundColor: dragging ? 'var(--bg-brand-muted)' : 'transparent',
              transition: 'all 0.2s ease',
            }}
          >
            <UploadCloud
              size={48}
              style={{ color: dragging ? 'var(--clr-brand)' : 'var(--text-muted)' }}
            />
            <p style={{ margin: 0, color: 'var(--text-primary)', fontWeight: 500 }}>
              Arraste seu arquivo PDF ou DOCX aqui
            </p>
            <p style={{ margin: 0, color: 'var(--text-muted)', fontSize: '0.875rem' }}>
              ou clique para selecionar
            </p>
          </div>
        ) : (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              padding: '1rem',
              border: '1px solid var(--d2b-border)',
              borderRadius: '0.5rem',
            }}
          >
            <FileText size={24} style={{ color: 'var(--clr-brand)', flexShrink: 0 }} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <p
                style={{
                  margin: 0,
                  fontWeight: 500,
                  color: 'var(--text-primary)',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}
              >
                {file.name}
              </p>
              <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                {formatFileSize(file.size)}
              </p>
            </div>
            <button
              className="btn btn-ghost btn-sm"
              onClick={clearFile}
              disabled={loading}
              aria-label="Remover arquivo"
            >
              <X size={18} />
            </button>
          </div>
        )}

        <div style={{ marginTop: '1rem', display: 'flex', justifyContent: 'flex-end' }}>
          <button
            className="btn btn-primary"
            onClick={handleValidate}
            disabled={!file || loading}
          >
            {loading ? (
              <>
                <Loader2 size={18} className="animate-spin" />
                <span style={{ marginLeft: '0.5rem' }}>Validando...</span>
              </>
            ) : (
              'Validar Especificacao'
            )}
          </button>
        </div>
      </div>

      {/* Loading state */}
      {loading && (
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            padding: '3rem',
            gap: '1rem',
          }}
        >
          <Loader2 size={40} className="animate-spin" style={{ color: 'var(--clr-brand)' }} />
          <p style={{ color: 'var(--text-secondary)', margin: 0 }}>
            Analisando especificacao... Isso pode levar ate 60 segundos.
          </p>
          <div className="progress-bar" style={{ width: '100%', maxWidth: 400 }}>
            <div
              className="progress-fill"
              style={{
                width: '60%',
                animation: 'indeterminate 2s ease-in-out infinite',
              }}
            />
          </div>
        </div>
      )}

      {/* Error state */}
      {error && !loading && (
        <div className="alert alert-danger" style={{ marginBottom: '1.5rem' }}>
          <AlertTriangle size={20} />
          <span>{error}</span>
        </div>
      )}

      {/* Report */}
      {report && !loading && (
        <div>
          <div className="divider" />
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.5rem' }}>
            <CheckCircle size={24} style={{ color: 'var(--clr-success)' }} />
            <h2 style={{ margin: 0, color: 'var(--text-primary)' }}>Relatorio de Validacao</h2>
          </div>
          <ReportDisplay report={report} />
        </div>
      )}
    </div>
  )
}
