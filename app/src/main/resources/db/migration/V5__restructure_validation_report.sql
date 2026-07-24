-- Reestrutura o relatorio de validacao para o novo formato de checklist
-- (baseado no modelo compartilhado pelo Marcio), substituindo o modelo
-- anterior de issues por severidade + perguntas por publico-alvo.
--
-- Relatorios existentes (formato antigo) ficam com as colunas antigas
-- preenchidas, mas essas colunas deixam de ser usadas por codigo novo.
-- Nao ha migracao de dado histórico para o novo formato.

ALTER TABLE validation_reports ALTER COLUMN status DROP NOT NULL;
ALTER TABLE validation_reports ALTER COLUMN summary DROP NOT NULL;
ALTER TABLE validation_reports ALTER COLUMN final_recommendation DROP NOT NULL;
ALTER TABLE validation_reports ALTER COLUMN positive_points_json DROP NOT NULL;
ALTER TABLE validation_reports ALTER COLUMN missing_sections_json DROP NOT NULL;
ALTER TABLE validation_reports ALTER COLUMN risk_analysis DROP NOT NULL;
ALTER TABLE validation_reports ALTER COLUMN critical_issues_count DROP NOT NULL;
ALTER TABLE validation_reports ALTER COLUMN moderate_issues_count DROP NOT NULL;
ALTER TABLE validation_reports ALTER COLUMN minor_issues_count DROP NOT NULL;

ALTER TABLE validation_reports ADD COLUMN IF NOT EXISTS classificacao VARCHAR(50);
ALTER TABLE validation_reports ADD COLUMN IF NOT EXISTS qualidade VARCHAR(50);
ALTER TABLE validation_reports ADD COLUMN IF NOT EXISTS resumo_executivo TEXT;
ALTER TABLE validation_reports ADD COLUMN IF NOT EXISTS principais_riscos_json TEXT;
ALTER TABLE validation_reports ADD COLUMN IF NOT EXISTS recomendacoes_json TEXT;
ALTER TABLE validation_reports ADD COLUMN IF NOT EXISTS parecer_final TEXT;

CREATE TABLE IF NOT EXISTS checklist_items (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL,
    chave VARCHAR(50) NOT NULL,
    item VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    comentario TEXT NOT NULL,
    pontos INTEGER NOT NULL,
    CONSTRAINT fk_checklist_item_report FOREIGN KEY (report_id) REFERENCES validation_reports(id)
);
CREATE INDEX IF NOT EXISTS idx_checklist_items_report_id ON checklist_items(report_id);

CREATE TABLE IF NOT EXISTS pontos_criticos (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL,
    gap TEXT NOT NULL,
    impacto TEXT NOT NULL,
    CONSTRAINT fk_ponto_critico_report FOREIGN KEY (report_id) REFERENCES validation_reports(id)
);
CREATE INDEX IF NOT EXISTS idx_pontos_criticos_report_id ON pontos_criticos(report_id);
