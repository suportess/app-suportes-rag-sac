export type DocumentUploadResponse = {
  id: number
  originalFileName: string
  contentType: string
  documentType: 'PDF' | 'DOCX' | 'TXT' | 'UNKNOWN'
  fileSize: number
  status: 'UPLOADED' | 'EXTRACTED' | 'VALIDATED' | 'FAILED'
  createdAt: string
}

export type DocumentResponse = {
  id: number
  originalFileName: string
  storedFileName: string
  contentType: string
  documentType: 'PDF' | 'DOCX' | 'TXT' | 'UNKNOWN'
  fileSize: number
  status: 'UPLOADED' | 'EXTRACTED' | 'VALIDATED' | 'FAILED'
  createdAt: string
  updatedAt: string
}

export type SectionStatus = {
  sectionName: string
  status: 'PRESENTE' | 'PARCIAL' | 'AUSENTE'
  detectedHeading?: string | null
}

export type ValidationReportResponse = {
  reportId: number
  documentId: number
  status: 'APPROVED' | 'APPROVED_WITH_WARNINGS' | 'REJECTED'
  score: number
  specificationSummary: string | null
  summary: string
  finalRecommendation: string
  issues: ValidationIssueResponse[]
  questions: ValidationQuestionResponse[]
  positivePoints: string[]
  missingSections: string[]
  riskAnalysis: string
  sectionAnalysis: SectionStatus[]
}

export type ValidationIssueResponse = {
  severity: 'CRITICAL' | 'MODERATE' | 'MINOR'
  category: string
  title: string
  description: string
  suggestion: string
}

export type ValidationQuestionResponse = {
  question: string
  reason: string
  targetAudience: string
}

export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export type ApiError = {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}
