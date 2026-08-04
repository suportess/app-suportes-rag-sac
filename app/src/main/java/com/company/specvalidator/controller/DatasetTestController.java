package com.company.specvalidator.controller;

import com.company.specvalidator.service.DatasetRunService;
import com.company.specvalidator.service.DatasetRunSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/testing/datasets")
@Tag(name = "Dataset Testing", description = "Roda datasets de referencia da Langfuse pelo pipeline real de validacao")
public class DatasetTestController {

    private final DatasetRunService datasetRunService;

    public DatasetTestController(DatasetRunService datasetRunService) {
        this.datasetRunService = datasetRunService;
    }

    @Operation(summary = "Roda todos os itens de um dataset da Langfuse pelo pipeline real e registra o resultado como Dataset Run")
    @PostMapping("/{datasetName}/run")
    public List<DatasetRunSummary> run(@PathVariable String datasetName,
                                       @RequestParam(required = false) String runName) {
        String actualRunName = (runName == null || runName.isBlank()) ? "run-" + Instant.now() : runName;
        log.info("Rodando dataset '{}' como run '{}'", datasetName, actualRunName);
        return datasetRunService.runDataset(datasetName, actualRunName);
    }
}
