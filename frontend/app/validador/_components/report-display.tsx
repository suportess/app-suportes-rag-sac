'use client'

import {
  CheckCircle,
  XCircle,
  AlertTriangle,
  Info,
  BookOpen,
  FileText,
  MessageSquare,
  HelpCircle,
  ThumbsUp,
  AlertOctagon,
  ShieldAlert,
  ListChecks,
} from 'lucide-react'
import type { ValidationReportResponse, ValidationIssueResponse, SectionStatus } from '@/lib/types'

function scoreColor(score: number): string {
  if (score >= 85) return 'var(--clr-success)'
  if (score >= 60) return 'var(--clr-warning)'
  return 'var(--clr-danger)'
}

function statusLabel(status: ValidationReportResponse['status']): string {
  switch (status) {
    case 'APPROVED':
      return 'Aprovado'
    case 'APPROVED_WITH_WARNINGS':
      return 'Aprovado com Ressalvas'
    case 'REJECTED':
      return 'Rejeitado'
  }
}

function statusBadgeClass(status: ValidationReportResponse['status']): string {
  switch (status) {
    case 'APPROVED':
      return 'badge-success'
    case 'APPROVED_WITH_WARNINGS':
      return 'badge-warning'
    case 'REJECTED':
      return 'badge-danger'
  }
}

function StatusIcon({ status }: { status: ValidationReportResponse['status'] }) {
  switch (status) {
    case 'APPROVED':
      return <CheckCircle size={20} />
    case 'APPROVED_WITH_WARNINGS':
      return <AlertTriangle size={20} />
    case 'REJECTED':
      return <XCircle size={20} />
  }
}

function severityBadgeClass(severity: ValidationIssueResponse['severity']): string {
  switch (severity) {
    case 'CRITICAL':
      return 'badge-danger'
    case 'MODERATE':
      return 'badge-warning'
    case 'MINOR':
      return 'badge-info'
  }
}

function severityLabel(severity: ValidationIssueResponse['severity']): string {
  switch (severity) {
    case 'CRITICAL':
      return 'Critico'
    case 'MODERATE':
      return 'Moderado'
    case 'MINOR':
      return 'Menor'
  }
}

function severityOrder(severity: ValidationIssueResponse['severity']): number {
  switch (severity) {
    case 'CRITICAL':
      return 0
    case 'MODERATE':
      return 1
    case 'MINOR':
      return 2
  }
}

function sectionStatusBadgeClass(status: SectionStatus['status']): string {
  switch (status) {
    case 'PRESENTE':
      return 'badge-success'
    case 'PARCIAL':
      return 'badge-warning'
    case 'AUSENTE':
      return 'badge-danger'
  }
}

function SectionStatusIcon({ status }: { status: SectionStatus['status'] }) {
  switch (status) {
    case 'PRESENTE':
      return <CheckCircle size={16} style={{ color: 'var(--clr-success)', flexShrink: 0 }} />
    case 'PARCIAL':
      return <AlertTriangle size={16} style={{ color: 'var(--clr-warning)', flexShrink: 0 }} />
    case 'AUSENTE':
      return <XCircle size={16} style={{ color: 'var(--clr-danger)', flexShrink: 0 }} />
  }
}

type ReportDisplayProps = {
  report: ValidationReportResponse
}

export function ReportDisplay({ report }: ReportDisplayProps) {
  const sortedIssues = [...report.issues].sort(
    (a, b) => severityOrder(a.severity) - severityOrder(b.severity)
  )

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* KPI Row */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '1rem',
        }}
      >
        <div className="kpi-card">
          <div className="kpi-icon" style={{ color: scoreColor(report.score) }}>
            <ListChecks size={24} />
          </div>
          <div className="kpi-value" style={{ color: scoreColor(report.score) }}>
            {report.score}
          </div>
          <div className="kpi-label">Score</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-icon">
            <StatusIcon status={report.status} />
          </div>
          <div className="kpi-value">
            <span className={statusBadgeClass(report.status)}>
              {statusLabel(report.status)}
            </span>
          </div>
          <div className="kpi-label">Status</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-icon" style={{ color: 'var(--clr-danger)' }}>
            <AlertOctagon size={24} />
          </div>
          <div className="kpi-value">{report.issues.length}</div>
          <div className="kpi-label">Problemas</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-icon" style={{ color: 'var(--clr-info)' }}>
            <HelpCircle size={24} />
          </div>
          <div className="kpi-value">{report.questions.length}</div>
          <div className="kpi-label">Perguntas</div>
        </div>
      </div>

      {/* Section Coverage */}
      {report.sectionAnalysis && report.sectionAnalysis.length > 0 && (
        <div className="card card-p">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
            <ListChecks size={20} style={{ color: 'var(--clr-brand)' }} />
            <h3 style={{ margin: 0, fontSize: '1.1rem', color: 'var(--text-primary)' }}>
              Cobertura de Secoes EF
            </h3>
            <span style={{ marginLeft: 'auto', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
              {report.sectionAnalysis.filter(s => s.status === 'PRESENTE').length}/
              {report.sectionAnalysis.length} presentes
            </span>
          </div>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
              gap: '0.5rem',
            }}
          >
            {report.sectionAnalysis.map((section, i) => (
              <div
                key={i}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.6rem',
                  padding: '0.5rem 0.75rem',
                  borderRadius: '0.375rem',
                  backgroundColor: 'var(--bg-muted)',
                }}
              >
                <SectionStatusIcon status={section.status} />
                <span style={{ flex: 1, fontSize: '0.875rem', color: 'var(--text-primary)' }}>
                  {section.sectionName}
                </span>
                <span className={sectionStatusBadgeClass(section.status)} style={{ fontSize: '0.7rem' }}>
                  {section.status}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Specification Summary */}
      {report.specificationSummary && (
        <div className="card card-p">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
            <BookOpen size={20} style={{ color: 'var(--clr-brand)' }} />
            <h3 style={{ margin: 0, fontSize: '1.1rem', color: 'var(--text-primary)' }}>
              Resumo da Especificacao
            </h3>
          </div>
          <p style={{ margin: 0, color: 'var(--text-secondary)', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
            {report.specificationSummary}
          </p>
        </div>
      )}

      {/* Summary */}
      <div className="card card-p">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
          <FileText size={20} style={{ color: 'var(--clr-brand)' }} />
          <h3 style={{ margin: 0, fontSize: '1.1rem', color: 'var(--text-primary)' }}>
            Resumo da Analise
          </h3>
        </div>
        <p style={{ margin: 0, color: 'var(--text-secondary)', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
          {report.summary}
        </p>
      </div>

      {/* Final Recommendation */}
      <div className="card card-p">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
          <MessageSquare size={20} style={{ color: 'var(--clr-brand)' }} />
          <h3 style={{ margin: 0, fontSize: '1.1rem', color: 'var(--text-primary)' }}>
            Recomendacao Final
          </h3>
        </div>
        <p style={{ margin: 0, color: 'var(--text-secondary)', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
          {report.finalRecommendation}
        </p>
      </div>

      {/* Issues */}
      {sortedIssues.length > 0 && (
        <div>
          <h3 style={{ color: 'var(--text-primary)', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <AlertTriangle size={20} style={{ color: 'var(--clr-danger)' }} />
            Problemas Encontrados ({sortedIssues.length})
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {sortedIssues.map((issue, i) => (
              <div key={i} className="card card-p">
                <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem', flexWrap: 'wrap' }}>
                  <span className={severityBadgeClass(issue.severity)}>
                    {severityLabel(issue.severity)}
                  </span>
                  <span className="badge-secondary">{issue.category}</span>
                </div>
                <h4 style={{ margin: '0 0 0.5rem 0', color: 'var(--text-primary)' }}>
                  {issue.title}
                </h4>
                <p style={{ margin: '0 0 0.75rem 0', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                  {issue.description}
                </p>
                {issue.suggestion && (
                  <div
                    style={{
                      padding: '0.75rem 1rem',
                      borderRadius: '0.5rem',
                      backgroundColor: 'var(--bg-muted)',
                      borderLeft: '3px solid var(--clr-brand)',
                    }}
                  >
                    <p style={{ margin: 0, fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                      <strong style={{ color: 'var(--text-primary)' }}>Sugestao:</strong>{' '}
                      {issue.suggestion}
                    </p>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Questions */}
      {report.questions.length > 0 && (
        <div>
          <h3 style={{ color: 'var(--text-primary)', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <HelpCircle size={20} style={{ color: 'var(--clr-info)' }} />
            Perguntas para Esclarecimento ({report.questions.length})
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {report.questions.map((q, i) => (
              <div key={i} className="card card-p">
                <h4 style={{ margin: '0 0 0.5rem 0', color: 'var(--text-primary)' }}>
                  {q.question}
                </h4>
                <p style={{ margin: '0 0 0.5rem 0', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                  {q.reason}
                </p>
                <span className="badge-secondary">{q.targetAudience}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Positive Points */}
      {report.positivePoints.length > 0 && (
        <div className="card card-p">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
            <ThumbsUp size={20} style={{ color: 'var(--clr-success)' }} />
            <h3 style={{ margin: 0, fontSize: '1.1rem', color: 'var(--text-primary)' }}>
              Pontos Positivos
            </h3>
          </div>
          <ul style={{ margin: 0, paddingLeft: '1.5rem', color: 'var(--clr-success)' }}>
            {report.positivePoints.map((point, i) => (
              <li key={i} style={{ marginBottom: '0.35rem' }}>
                <span style={{ color: 'var(--text-secondary)' }}>{point}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Missing Sections */}
      {report.missingSections.length > 0 && (
        <div className="card card-p">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
            <AlertTriangle size={20} style={{ color: 'var(--clr-warning)' }} />
            <h3 style={{ margin: 0, fontSize: '1.1rem', color: 'var(--text-primary)' }}>
              Secoes Ausentes
            </h3>
          </div>
          <ul style={{ margin: 0, paddingLeft: '1.5rem', color: 'var(--clr-warning)' }}>
            {report.missingSections.map((section, i) => (
              <li key={i} style={{ marginBottom: '0.35rem' }}>
                <span style={{ color: 'var(--text-secondary)' }}>{section}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Risk Analysis */}
      {report.riskAnalysis && (
        <div className="card card-p">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
            <ShieldAlert size={20} style={{ color: 'var(--clr-danger)' }} />
            <h3 style={{ margin: 0, fontSize: '1.1rem', color: 'var(--text-primary)' }}>
              Analise de Riscos
            </h3>
          </div>
          <p style={{ margin: 0, color: 'var(--text-secondary)', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
            {report.riskAnalysis}
          </p>
        </div>
      )}
    </div>
  )
}
