package com.company.specvalidator.service.ai;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class PromptBuilderService {

    enum DevType {
        REPORT("Report/Relatorio ALV"),
        ENHANCEMENT("Enhancement/Exit/BADI"),
        INTERFACE("Interface/Integracao"),
        WORKFLOW("Workflow/Fluxo de Aprovacao"),
        FORMS("Formulario (SmartForm/SapScript/Adobe Forms)"),
        BATCH("Batch Input/BDC/Carga de Dados"),
        TABLE("Tabela ou Estrutura ABAP Customizada"),
        UNKNOWN("Tipo nao identificado - aplicando criterios gerais");

        private final String displayName;

        DevType(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public String buildValidationPrompt(String documentText) {
        DevType devType = detectDevType(documentText);
        String typeSpecificCriteria = buildTypeSpecificCriteria(devType);

        return """
                Voce e um especialista SAP ABAP, arquiteto de solucoes e revisor de especificacoes funcionais.

                Sua tarefa e analisar uma especificacao funcional SAP ABAP e verificar se ela possui informacoes suficientes para que um desenvolvedor ABAP consiga implementar a solucao sem descobrir lacunas durante o desenvolvimento.

                TIPO DE DESENVOLVIMENTO IDENTIFICADO: %s

                === CRITERIOS GERAIS (obrigatorios para qualquer tipo de desenvolvimento) ===

                1. Clareza do objetivo e contexto de negocio
                2. Escopo e fora de escopo definidos
                3. Regras de negocio com condicoes documentadas (SE/ENTAO/SENAO)
                4. Fluxos alternativos e tratamento de excecoes documentados
                5. TRIGGER: o que aciona este desenvolvimento? (evento SAP, transacao, agendamento, acao do usuario, interface de entrada?)
                6. Volume e frequencia: processamento online ou background? Quantos registros? Com que frequencia?
                7. Tratamento de erros: como o desenvolvimento se comporta em falha? Ha log? Mensagens ao usuario definidas?
                8. Campos SAP com NOMES TECNICOS (ex: VBAK-VBELN, nao apenas "numero do pedido")
                9. Tabelas SAP envolvidas com nomes tecnicos (ex: MARA, ZRET001)
                10. Transacoes SAP nomeadas (ex: VA01, QM01) — nao apenas descricoes genericas
                11. BAPIs, BADIs, User Exits, Enhancements ou Function Modules com NOME TECNICO EXATO
                12. Integracoes com outros sistemas ou modulos SAP — com tecnologia especificada
                13. Cenarios de teste: positivos, negativos e de excecao
                14. Responsavel funcional identificado com contato
                15. Perfis e autorizacoes — se N/A, deve incluir justificativa aprovada pela equipe de arquitetura

                %s=== REGRAS CRITICAS DE CLASSIFICACAO ===

                CRITICAL — somente quando impossibilita o inicio da implementacao:
                - BAPI, BADI, Enhancement Point, User Exit ou Function Module mencionado SEM nome tecnico exato (ex: "sera usada uma BADI" sem citar qual) = CRITICAL, categoria SAP_ABAP
                - Tabela SAP citada APENAS por descricao generica sem nenhum nome tecnico (ex: "tabela de materiais" sem MARA, MARC) = CRITICAL
                - Objetivo do desenvolvimento completamente ausente ou incompreensivel = CRITICAL

                MODERATE — gera risco ou retrabalho mas nao impede iniciar:
                - Trigger: se o documento nomeia transacao SAP (ex: QM01, VA01, MM60) ou aplicativo Fiori = trigger PRESENTE, NAO classificar como problema. Cobrar MODERATE apenas quando NENHUM evento, transacao, interface de entrada ou acao de usuario estiver identificado
                - Integracao com sistema externo ou outro modulo mencionada sem tecnologia de comunicacao (RFC, IDoc, REST, SOAP, CPI) = MODERATE
                - Campos SAP sem nome tecnico em alguns casos (mas o documento ja apresenta outros com nomes tecnicos) = MODERATE
                - Tratamento de erros e mensagens ao usuario nao documentados = MODERATE
                - Cenarios de teste ausentes = MODERATE
                - Seguranca/autorizacao N/A sem justificativa = MODERATE + gerar pergunta obrigatoria com targetAudience ARQUITETURA

                MINOR — melhora a documentacao mas nao afeta implementacao:
                - Responsavel tecnico ausente
                - Log de processamento nao detalhado
                - Lacunas menores de documentacao

                Criterios de aceite: NAO cobrar — este criterio nao e avaliado nesta versao

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
                """.formatted(devType.displayName(), typeSpecificCriteria, documentText);
    }

    DevType detectDevType(String documentText) {
        String text = documentText == null ? "" : documentText.toLowerCase(Locale.ROOT);

        int reportScore = 0, enhancementScore = 0, interfaceScore = 0,
                workflowScore = 0, formsScore = 0, batchScore = 0, tableScore = 0;

        if (containsAny(text, "alv", " report ", "relatorio", "relatório", "listagem", "extrator de dados", "extrato")) reportScore += 2;
        if (containsAny(text, "variante de selecao", "variante de seleção", "tela de selecao", "layout alv")) reportScore += 2;
        if (containsAny(text, "se38", "se80", "output list", "download excel", "download xls")) reportScore++;

        if (containsAny(text, "badi", "user exit", "enhancement point", "enhancement spot")) enhancementScore += 3;
        if (containsAny(text, "classe de implementacao", "filtro de badi", "exit de usuario", "saida de usuario")) enhancementScore += 2;
        if (containsAny(text, "se19", "se18", "spro", "exit_")) enhancementScore++;

        if (containsAny(text, "idoc", "rfc", "cpi", "pi/po", "sap pi", "sap po", "sap cpi")) interfaceScore += 3;
        if (containsAny(text, "webservice", "web service", "soap", "rest", "odata", "middleware")) interfaceScore += 2;
        if (containsAny(text, "inbound", "outbound", "sistema externo", "comunicacao entre sistemas")) interfaceScore++;

        if (containsAny(text, "workflow", "workitem", "wi_id", "fluxo de aprovacao", "sap workflow")) workflowScore += 3;
        if (containsAny(text, "aprovação", "aprovacao", "aprovador", "nivel de aprovacao")) workflowScore += 2;
        if (containsAny(text, "swi1", "swel", "notificacao por email", "tarefa de workflow")) workflowScore++;

        if (containsAny(text, "smartform", "sapscript", "sap script", "adobe form", "adobe forms")) formsScore += 3;
        if (containsAny(text, "formulario", "etiqueta", "nota fiscal", "layout de impressao")) formsScore += 2;
        if (containsAny(text, "impressao", "impressão", "saida de impressao", "logo", "sp01")) formsScore++;

        if (containsAny(text, "batch input", "bdc", "call transaction", "migracao de dados")) batchScore += 3;
        if (containsAny(text, "sessao bdc", "session bdc", "modo a ", "modo n ", "modo e ")) batchScore += 2;
        if (containsAny(text, "planilha de carga", "arquivo de entrada", "carga de dados")) batchScore++;

        if (containsAny(text, "tabela customizada", "tabela z", "criar tabela", "nova tabela", "tabela de configuracao")) tableScore += 3;
        if (containsAny(text, "se11", "dicionario abap", "dominio", "elemento de dados", "classe de entrega")) tableScore += 2;
        if (containsAny(text, "chave primaria", "indice secundario", "se16", "transparente")) tableScore++;

        int max = Math.max(reportScore, Math.max(enhancementScore, Math.max(interfaceScore,
                Math.max(workflowScore, Math.max(formsScore, Math.max(batchScore, tableScore))))));

        if (max < 2) return DevType.UNKNOWN;
        if (max == reportScore) return DevType.REPORT;
        if (max == enhancementScore) return DevType.ENHANCEMENT;
        if (max == interfaceScore) return DevType.INTERFACE;
        if (max == workflowScore) return DevType.WORKFLOW;
        if (max == formsScore) return DevType.FORMS;
        if (max == batchScore) return DevType.BATCH;
        return DevType.TABLE;
    }

    private String buildTypeSpecificCriteria(DevType devType) {
        return switch (devType) {
            case REPORT -> """
                    === CRITERIOS ESPECIFICOS: REPORT/RELATORIO ALV ===
                    - Variantes de selecao documentadas (campos, ranges, obrigatoriedade)?
                    - Layout do ALV definido (colunas, titulos, ordenacao padrao)?
                    - Saida do relatorio: tela ALV, download Excel/PDF, email?
                    - Pode ser executado em background (job agendado)?
                    - Parametros obrigatorios vs. opcionais claramente separados?

                    """;
            case ENHANCEMENT -> """
                    === CRITERIOS ESPECIFICOS: ENHANCEMENT/EXIT/BADI ===
                    - Nome tecnico EXATO do ponto de enhancement (BADI, Enhancement Spot, User Exit com nome da implementacao)?
                    - Condicoes de disparo: quando exatamente o exit e chamado?
                    - Impacto no processo standard SAP documentado (o que muda no comportamento padrao)?
                    - Function Modules ou BAPIs chamadas dentro do enhancement com nome tecnico?
                    - Em caso de erro no enhancement: o processo base SAP e comprometido ou continua?

                    """;
            case INTERFACE -> """
                    === CRITERIOS ESPECIFICOS: INTERFACE/INTEGRACAO ===
                    - Landscape completo: sistema de origem, sistema de destino, versoes?
                    - Tecnologia de comunicacao: RFC, IDoc (com tipo), REST, SOAP, SAP CPI, PI/PO?
                    - Procedimento de recuperacao em caso de falha (retry, fila de erro, reprocessamento)?
                    - Log de processamento com rastreabilidade de mensagens?
                    - Frequencia de execucao e janela de tempo (tempo real, batch, agendado)?
                    - Mapeamento campo-a-campo entre sistemas (de-para com nomes tecnicos)?

                    """;
            case WORKFLOW -> """
                    === CRITERIOS ESPECIFICOS: WORKFLOW/FLUXO DE APROVACAO ===
                    - Eventos acionadores com nomes tecnicos SAP (business object, evento)?
                    - Aprovadores com perfis e hierarquia de niveis definidos?
                    - Diagrama ou sequencia textual das etapas do fluxo?
                    - Templates de email e notificacoes (destinatarios, conteudo)?
                    - Prazos (SLA) por etapa e acao em caso de estouro (escalonamento)?
                    - Comportamento em rejeicao, cancelamento e reenvio?

                    """;
            case FORMS -> """
                    === CRITERIOS ESPECIFICOS: FORMULARIO (SMARTFORM/SAPSCRIPT/ADOBE) ===
                    - Tipo de tecnologia: SmartForm, SapScript ou Adobe Forms?
                    - Layout do formulario descrito (cabecalho, corpo, rodape, logo)?
                    - Tipo de saida: impressao direta, PDF, email, spool?
                    - Campos SAP com nomes tecnicos (tabela-campo, ex: VBAK-VBELN)?
                    - Condicoes de impressao (quando imprimir, condicoes de supressao de blocos)?
                    - Driver program ou transacao que aciona o formulario?

                    """;
            case BATCH -> """
                    === CRITERIOS ESPECIFICOS: BATCH INPUT/BDC/CARGA DE DADOS ===
                    - Transacao alvo com nome tecnico (ex: MIGO, VA01, MB51)?
                    - Mapeamento de campos: colunas do arquivo -> campos de tela SAP com nomes tecnicos?
                    - Modo de execucao: A (display todos) / N (sem display) / E (somente erros)?
                    - Tratamento de erros de sessao: log, reprocessamento, separacao de registros com erro?
                    - Arquivo de entrada: formato (xlsx, csv, txt), delimitador, encoding?

                    """;
            case TABLE -> """
                    === CRITERIOS ESPECIFICOS: TABELA/ESTRUTURA ABAP CUSTOMIZADA ===
                    - Nome Z da tabela com padrao do projeto (ex: ZPROD_001)?
                    - Campos com tipos de dados SAP e dominios (ex: MATNR, MENGE, DATS)?
                    - Chave primaria claramente definida?
                    - Indices secundarios necessarios para performance?
                    - Classe de entrega (A=customizing, C=client-dep, G=master, L=temporaria)?
                    - Manutencao via SM30 necessaria? Grupo de autorizacao?

                    """;
            case UNKNOWN -> "";
        };
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }
}
