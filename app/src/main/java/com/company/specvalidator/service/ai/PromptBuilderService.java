package com.company.specvalidator.service.ai;

import org.springframework.stereotype.Service;

@Service
public class PromptBuilderService {

    public String buildValidationPrompt(String documentText) {
        return """
                Voce e um especialista SAP ABAP, arquiteto de solucoes e revisor de especificacoes funcionais.

                Sua tarefa e analisar uma especificacao funcional SAP ABAP e verificar se ela possui informacoes suficientes para que um desenvolvedor ABAP consiga implementar a solucao com seguranca.

                Avalie obrigatoriamente:

                1. Clareza do objetivo
                2. Escopo e fora de escopo
                3. Regras de negocio
                4. Campos SAP envolvidos
                5. Tabelas SAP envolvidas
                6. Transacoes SAP
                7. BAPIs, BADIs, User Exits, Enhancements ou Reports
                8. Integracoes com outros sistemas
                9. Validacoes e mensagens de erro
                10. Perfis e autorizacoes
                11. Cenarios de teste
                12. Criterios de aceite
                13. Ambiguidades
                14. Riscos para desenvolvimento ABAP
                15. Dependencias tecnicas ou funcionais

                Classifique os problemas como:

                - CRITICAL: impede o desenvolvimento ou pode causar implementacao errada
                - MODERATE: nao impede totalmente, mas gera risco ou retrabalho
                - MINOR: melhoria de clareza ou documentacao

                Retorne obrigatoriamente um JSON valido, sem markdown, neste formato:

                {
                  "status": "APPROVED | APPROVED_WITH_WARNINGS | REJECTED",
                  "score": 0,
                  "specificationSummary": "Resumo do que a especificacao solicita: qual o objetivo do desenvolvimento, o que deve ser construido, qual processo de negocio esta envolvido e quais sistemas sao impactados. Descreva em 3 a 5 frases o que o documento pede.",
                  "summary": "Resumo geral da qualidade do documento",
                  "finalRecommendation": "Recomendacao final",
                  "issues": [
                    {
                      "severity": "CRITICAL | MODERATE | MINOR",
                      "category": "ESTRUTURA | SAP_ABAP | REGRA_NEGOCIO | INTEGRACAO | TESTES | AUTORIZACAO | DADOS | OUTROS",
                      "title": "Titulo do problema",
                      "description": "Descricao detalhada",
                      "suggestion": "Como corrigir"
                    }
                  ],
                  "questions": [
                    {
                      "question": "Pergunta objetiva para esclarecer o documento",
                      "reason": "Por que essa pergunta e necessaria",
                      "targetAudience": "FUNCIONAL | ABAP | ARQUITETURA | NEGOCIO"
                    }
                  ],
                  "positivePoints": [
                    "Ponto positivo identificado"
                  ],
                  "missingSections": [
                    "Secao ausente"
                  ],
                  "riskAnalysis": "Analise dos riscos de seguir com o desenvolvimento usando esta especificacao"
                }

                Documento para analise:

                %s
                """.formatted(documentText);
    }
}
