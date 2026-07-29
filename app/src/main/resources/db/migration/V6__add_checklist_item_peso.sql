-- Adiciona peso (1-5, definido pelo negocio) e pontos_conquistados (fracao, ex: 1.5) por item
-- do checklist, pro demonstrativo de calculo do score exibir "quanto valia" e "quanto foi
-- conquistado" de forma transparente, em vez de so o valor de perda ja arredondado.
-- Relatorios historicos ficam com essas duas colunas em branco (nao ha migracao de dado antigo,
-- mesmo padrao adotado na V5).

ALTER TABLE checklist_items ADD COLUMN IF NOT EXISTS peso INTEGER;
ALTER TABLE checklist_items ADD COLUMN IF NOT EXISTS pontos_conquistados NUMERIC(4,1);
