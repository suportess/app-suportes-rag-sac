-- Indexes para otimizar consultas por foreign key e filtros comuns

CREATE INDEX IF NOT EXISTS idx_extracted_documents_document_id ON extracted_documents(document_id);

CREATE INDEX IF NOT EXISTS idx_validation_reports_document_id ON validation_reports(document_id);

CREATE INDEX IF NOT EXISTS idx_validation_issues_report_id ON validation_issues(report_id);

CREATE INDEX IF NOT EXISTS idx_validation_questions_report_id ON validation_questions(report_id);

CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);

CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_validation_reports_status ON validation_reports(status);

-- Evolucao futura: para RAG com pgvector, adicionar extensao e coluna de embedding:
-- CREATE EXTENSION IF NOT EXISTS vector;
-- ALTER TABLE extracted_documents ADD COLUMN embedding vector(1536);
-- CREATE INDEX idx_extracted_documents_embedding ON extracted_documents USING ivfflat (embedding vector_cosine_ops);
