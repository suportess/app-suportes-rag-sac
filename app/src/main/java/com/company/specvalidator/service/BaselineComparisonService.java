package com.company.specvalidator.service;

import com.company.specvalidator.entity.DatasetBaselineEntity;
import com.company.specvalidator.repository.DatasetBaselineRepository;
import com.company.specvalidator.service.ai.LangFuseClient;
import com.company.specvalidator.service.ai.LangFuseDatasetClient;
import com.company.specvalidator.service.ai.RunItemScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Marca um run existente como baseline de um dataset e compara runs futuros contra ele — baseline
 * fixo e trocado manualmente (nao "sempre o run anterior"), pra pegar regressao acumulada de forma
 * clara em vez de deixar passar quedas pequenas entre execucoes consecutivas.
 */
@Slf4j
@Service
public class BaselineComparisonService {

    private final DatasetBaselineRepository baselineRepository;
    private final LangFuseDatasetClient langFuseDatasetClient;
    private final LangFuseClient langFuseClient;

    public BaselineComparisonService(DatasetBaselineRepository baselineRepository,
                                     LangFuseDatasetClient langFuseDatasetClient,
                                     LangFuseClient langFuseClient) {
        this.baselineRepository = baselineRepository;
        this.langFuseDatasetClient = langFuseDatasetClient;
        this.langFuseClient = langFuseClient;
    }

    public DatasetBaselineEntity markBaseline(String datasetName, String runName) {
        DatasetBaselineEntity entity = baselineRepository.findByDatasetName(datasetName)
                .orElse(DatasetBaselineEntity.builder().datasetName(datasetName).build());
        entity.setRunName(runName);
        DatasetBaselineEntity saved = baselineRepository.save(entity);
        log.info("Baseline do dataset '{}' promovido para o run '{}'", datasetName, runName);
        return saved;
    }

    public Optional<RegressionReport> compare(String datasetName, String currentRunName,
                                              List<DatasetRunSummary> currentResults) {
        Optional<DatasetBaselineEntity> baselineOpt = baselineRepository.findByDatasetName(datasetName);
        if (baselineOpt.isEmpty()) {
            log.info("Nenhum baseline marcado pro dataset '{}' - pulando comparacao de regressao", datasetName);
            return Optional.empty();
        }
        String baselineRunName = baselineOpt.get().getRunName();
        if (baselineRunName.equals(currentRunName)) {
            log.info("Run atual '{}' e o proprio baseline - pulando comparacao", currentRunName);
            return Optional.empty();
        }

        Optional<String> datasetIdOpt = langFuseDatasetClient.fetchDatasetId(datasetName);
        if (datasetIdOpt.isEmpty()) {
            log.warn("Nao foi possivel resolver o id do dataset '{}' - pulando comparacao de regressao", datasetName);
            return Optional.empty();
        }

        List<RunItemScore> baselineScores = langFuseDatasetClient.fetchRunResults(datasetIdOpt.get(), baselineRunName);
        if (baselineScores.isEmpty()) {
            log.warn("Baseline '{}' do dataset '{}' nao retornou itens - pulando comparacao", baselineRunName, datasetName);
            return Optional.empty();
        }

        Integer baselinePromptVersion = baselineScores.stream()
                .findFirst()
                .flatMap(item -> langFuseDatasetClient.fetchPromptVersion(item.traceId()))
                .orElse(null);
        Integer currentPromptVersion = currentResults.stream()
                .findFirst()
                .flatMap(item -> langFuseDatasetClient.fetchPromptVersion(item.traceId()))
                .orElse(null);

        Map<String, RunItemScore> baselineByItemId = baselineScores.stream()
                .collect(Collectors.toMap(RunItemScore::datasetItemId, r -> r, (a, b) -> a));

        List<ItemDiff> mudancas = new ArrayList<>();
        int totalComparados = 0;
        int iguais = 0;
        for (DatasetRunSummary atual : currentResults) {
            RunItemScore baselineItem = baselineByItemId.get(atual.itemId());
            if (baselineItem == null || baselineItem.classificacao() == null) continue;
            totalComparados++;
            if (baselineItem.classificacao().equals(atual.classificacao())) {
                iguais++;
            } else {
                mudancas.add(new ItemDiff(atual.itemLabel(), baselineItem.classificacao(), baselineItem.score(),
                        atual.classificacao(), atual.score()));
            }
        }

        double percentual = totalComparados == 0 ? 0 : (iguais * 100.0) / totalComparados;

        RegressionReport report = new RegressionReport(datasetName, baselineRunName, currentRunName,
                baselinePromptVersion, currentPromptVersion, totalComparados, iguais, percentual, mudancas);

        String comment = String.format(
                "%d de %d itens mantiveram a mesma classificacao do baseline '%s' (promptVersion %s vs %s)",
                iguais, totalComparados, baselineRunName,
                baselinePromptVersion == null ? "?" : baselinePromptVersion,
                currentPromptVersion == null ? "?" : currentPromptVersion);
        for (DatasetRunSummary atual : currentResults) {
            langFuseClient.recordScore(atual.traceId(), "estabilidade_vs_baseline", percentual, comment);
        }

        return Optional.of(report);
    }
}
