package com.company.specvalidator.service;

import com.company.specvalidator.service.ai.DatasetItemDto;
import com.company.specvalidator.service.ai.LangFuseClient;
import com.company.specvalidator.service.ai.LangFuseDatasetClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orquestra a execucao dos itens de um Dataset da Langfuse pelo pipeline real de validacao,
 * registrando cada resultado como um Dataset Run Item — permite comparar score/classificacao
 * entre execucoes (mesma EF varias vezes, ou versoes diferentes do prompt) direto na aba
 * Experiments da Langfuse.
 */
@Slf4j
@Service
public class DatasetRunService {

    private final LangFuseDatasetClient langFuseDatasetClient;
    private final ValidationAgentService validationAgentService;
    private final LangFuseClient langFuseClient;

    public DatasetRunService(LangFuseDatasetClient langFuseDatasetClient,
                             ValidationAgentService validationAgentService,
                             LangFuseClient langFuseClient) {
        this.langFuseDatasetClient = langFuseDatasetClient;
        this.validationAgentService = validationAgentService;
        this.langFuseClient = langFuseClient;
    }

    public DatasetRunResult runDataset(String datasetName, String runName) {
        List<DatasetItemDto> items = langFuseDatasetClient.fetchItems(datasetName);
        String sessionId = "dataset-run-" + runName;

        List<DatasetRunSummary> results = new ArrayList<>();
        for (DatasetItemDto item : items) {
            String traceId = UUID.randomUUID().toString();
            String label = itemLabel(item);
            try {
                DatasetRunItemResult result = validationAgentService.runForDatasetItem(
                        item.input(), traceId, sessionId, label);
                langFuseDatasetClient.linkRunItem(runName, item.id(), traceId);
                results.add(new DatasetRunSummary(
                        item.id(), label, result.score(),
                        result.classificacao(), result.qualidade(),
                        result.resumoExecutivo(), result.checklist(),
                        result.parecerFinal(), result.recomendacoes(),
                        result.pontosCriticos(), item.expectedOutput(), traceId));
            } catch (Exception e) {
                log.error("Falha ao rodar item de dataset '{}' ({})", item.id(), label, e);
            }
        }

        List<ConsistencyResult> consistency = computeAndSendConsistency(results);
        return new DatasetRunResult(results, consistency);
    }

    /**
     * Agrupa os resultados por itemLabel (dataset precisa ter as N linhas repetidas com o mesmo
     * metadata.nome pra cair no mesmo grupo). Grupos com menos de 2 execucoes nao geram nota —
     * consistencia so faz sentido comparando varias execucoes do mesmo item. Pra cada grupo
     * valido, manda a nota de volta pra Langfuse anexada em cada uma das N traces do grupo.
     */
    private List<ConsistencyResult> computeAndSendConsistency(List<DatasetRunSummary> results) {
        Map<String, List<DatasetRunSummary>> porLabel = results.stream()
                .collect(Collectors.groupingBy(DatasetRunSummary::itemLabel, LinkedHashMap::new, Collectors.toList()));

        List<ConsistencyResult> consistencyResults = new ArrayList<>();
        for (Map.Entry<String, List<DatasetRunSummary>> entry : porLabel.entrySet()) {
            List<DatasetRunSummary> grupo = entry.getValue();
            if (grupo.size() < 2) continue;

            List<Integer> scores = grupo.stream().map(DatasetRunSummary::score).toList();
            double media = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
            double variancia = scores.stream().mapToDouble(s -> Math.pow(s - media, 2)).average().orElse(0);
            double desvioPadrao = Math.sqrt(variancia);

            Map<String, Long> contagemClassificacao = grupo.stream()
                    .collect(Collectors.groupingBy(DatasetRunSummary::classificacao, Collectors.counting()));
            String classificacaoMaisComum = contagemClassificacao.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("N/A");
            long batendoComMaisComum = contagemClassificacao.getOrDefault(classificacaoMaisComum, 0L);
            double taxaEstabilidade = (batendoComMaisComum * 100.0) / grupo.size();

            ConsistencyResult consistency = new ConsistencyResult(
                    entry.getKey(), grupo.size(), scores, media, desvioPadrao, taxaEstabilidade, classificacaoMaisComum);
            consistencyResults.add(consistency);

            String comment = String.format(
                    "Media %.1f, desvio-padrao %.1f, %d execucoes, %.0f%% bateram com classificacao '%s'",
                    media, desvioPadrao, grupo.size(), taxaEstabilidade, classificacaoMaisComum);
            for (DatasetRunSummary item : grupo) {
                langFuseClient.recordScore(item.traceId(), "consistencia", taxaEstabilidade, comment);
            }
        }
        return consistencyResults;
    }

    private String itemLabel(DatasetItemDto item) {
        Object nome = item.metadata() != null ? item.metadata().get("nome") : null;
        return nome != null ? nome.toString() : item.id();
    }
}
