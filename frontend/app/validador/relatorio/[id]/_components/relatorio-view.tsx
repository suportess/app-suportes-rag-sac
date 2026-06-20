'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Loader2, AlertTriangle, ArrowLeft } from 'lucide-react'
import { api } from '@/lib/api'
import type { ValidationReportResponse } from '@/lib/types'
import { ReportDisplay } from '@/app/validador/_components/report-display'

type RelatorioViewProps = {
  reportId: string
}

export function RelatorioView({ reportId }: RelatorioViewProps) {
  const router = useRouter()
  const [report, setReport] = useState<ValidationReportResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function fetchReport() {
      setLoading(true)
      setError(null)
      try {
        const result = await api.get<ValidationReportResponse>(
          `/api/v1/validations/${reportId}`
        )
        setReport(result)
      } catch (err) {
        const msg = err instanceof Error ? err.message : 'Erro ao carregar relatorio.'
        setError(msg)
      } finally {
        setLoading(false)
      }
    }
    fetchReport()
  }, [reportId])

  return (
    <div className="page">
      <div className="page-header">
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => router.push('/validador/documentos')}
          style={{ marginBottom: '0.5rem' }}
        >
          <ArrowLeft size={16} />
          <span style={{ marginLeft: '0.25rem' }}>Voltar para Documentos</span>
        </button>
        <h1 className="page-title">Relatorio de Validacao</h1>
        <p className="page-subtitle">Relatorio #{reportId}</p>
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

      {/* Report */}
      {report && !loading && <ReportDisplay report={report} />}
    </div>
  )
}
