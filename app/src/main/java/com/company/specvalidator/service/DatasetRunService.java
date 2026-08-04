package com.company.specvalidator.service;

import com.company.specvalidator.service.ai.DatasetItemDto;
import com.company.specvalidator.service.ai.LangFuseDatasetClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public DatasetRunService(LangFuseDatasetClient langFuseDatasetClient,
                             ValidationAgentService validationAgentService) {
        this.langFuseDatasetClient = langFuseDatasetClient;
        this.validationAgentService = validationAgentService;
    }

    public List<DatasetRunSummary> runDataset(String datasetName, String runName) {
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
                        item.id(), label, result.score(), result.classificacao(), result.qualidade(),
                        item.expectedOutput(), traceId));
            } catch (Exception e) {
                log.error("Falha ao rodar item de dataset '{}' ({})", item.id(), label, e);
            }
        }
        return results;
    }

    private String itemLabel(DatasetItemDto item) {
        Object nome = item.metadata() != null ? item.metadata().get("nome") : null;
        return nome != null ? nome.toString() : item.id();
    }
}
