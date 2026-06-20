CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS extracted_documents (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL UNIQUE,
    raw_text TEXT NOT NULL,
    normalized_text TEXT NOT NULL,
    detected_sections TEXT NOT NULL,
    page_count INTEGER,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_extracted_document FOREIGN KEY (document_id) REFERENCES documents(id)
);

CREATE TABLE IF NOT EXISTS validation_reports (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    score INTEGER NOT NULL,
    summary TEXT NOT NULL,
    final_recommendation TEXT NOT NULL,
    positive_points_json TEXT NOT NULL,
    missing_sections_json TEXT NOT NULL,
    risk_analysis TEXT NOT NULL,
    critical_issues_count INTEGER NOT NULL,
    moderate_issues_count INTEGER NOT NULL,
    minor_issues_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_report_document FOREIGN KEY (document_id) REFERENCES documents(id)
);

CREATE TABLE IF NOT EXISTS validation_issues (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL,
    severity VARCHAR(50) NOT NULL,
    category VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    suggestion TEXT NOT NULL,
    CONSTRAINT fk_issue_report FOREIGN KEY (report_id) REFERENCES validation_reports(id)
);

CREATE TABLE IF NOT EXISTS validation_questions (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    reason TEXT NOT NULL,
    target_audience VARCHAR(50) NOT NULL,
    CONSTRAINT fk_question_report FOREIGN KEY (report_id) REFERENCES validation_reports(id)
);
