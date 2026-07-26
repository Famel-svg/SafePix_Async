package famel.com.safepix_async.controller;

import famel.com.safepix_async.domain.dto.PixEvent;
import famel.com.safepix_async.domain.dto.PixRequest;
import famel.com.safepix_async.observability.SensitiveDataMasker;
import famel.com.safepix_async.service.PixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pix")
@Tag(name = "Pix", description = "Operacoes para recebimento assincrono de Pix")
public class PixController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PixController.class);
    private static final String DEFAULT_TENANT_ID = "default";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    private final PixService pixService;

    public PixController(PixService pixService) {
        this.pixService = pixService;
    }

    @PostMapping
    @Operation(
            summary = "Recebe uma solicitacao de Pix",
            description = "Publica a solicitacao na fila RabbitMQ para processamento assincrono.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Pix aceito para processamento"),
                    @ApiResponse(responseCode = "400", description = "Payload invalido")
            }
    )
    public ResponseEntity<Void> receberPix(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId,
            @RequestHeader(value = TENANT_ID_HEADER, required = false, defaultValue = DEFAULT_TENANT_ID) String tenantId,
            @Valid @RequestBody PixRequest pixRequest
    ) {
        String resolvedCorrelationId = resolveCorrelationId(correlationId);
        PixEvent pixEvent = pixRequest.toEvent(resolvedCorrelationId, tenantId);

        LOGGER.info("Recebida solicitacao de Pix: id={}, chavePix={}, valor={}",
                pixEvent.id(), SensitiveDataMasker.maskPixKey(pixEvent.chavePix()), pixEvent.valor());
        pixService.enviarPix(pixEvent);
        LOGGER.info("Solicitacao de Pix aceita para processamento: id={}, correlationId={}",
                pixEvent.id(), pixEvent.correlationId());

        return ResponseEntity.accepted()
                .header(CORRELATION_ID_HEADER, pixEvent.correlationId())
                .location(URI.create("/api/v1/pix/%s/status".formatted(pixEvent.id())))
                .build();
    }

    private String resolveCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
