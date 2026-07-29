-- A V6 criou pontos_conquistados como NUMERIC(4,1), mas o campo Java (Double) o Hibernate
-- espera mapeado como FLOAT/DOUBLE PRECISION. Com spring.jpa.hibernate.ddl-auto=validate,
-- isso falha a inicializacao com "wrong column type encountered ... found [numeric],
-- but expecting [float(53)]". Corrige o tipo da coluna sem precisar editar a V6 ja aplicada
-- (editar uma migration ja executada quebraria o checksum do Flyway).

ALTER TABLE checklist_items ALTER COLUMN pontos_conquistados TYPE DOUBLE PRECISION;
