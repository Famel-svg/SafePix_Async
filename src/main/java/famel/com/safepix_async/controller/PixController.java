package famel.com.safepix_async.controller;

import famel.com.safepix_async.domain.dto.PixDTO;
import famel.com.safepix_async.service.PixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pix")
@Tag(name = "Pix", description = "Operacoes para recebimento assincrono de Pix")
public class PixController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PixController.class);

    private final PixService pixService;

    public PixController(PixService pixService) {
        this.pixService = pixService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary = "Recebe uma solicitacao de Pix",
            description = "Publica a solicitacao na fila RabbitMQ para processamento assincrono.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Pix aceito para processamento"),
                    @ApiResponse(responseCode = "400", description = "Payload invalido")
            }
    )
    public void receberPix(@Valid @RequestBody PixDTO pixDTO) {
        LOGGER.info("Recebida solicitacao de Pix: id={}, chavePix={}, valor={}",
                pixDTO.id(), pixDTO.chavePix(), pixDTO.valor());
        pixService.enviarPix(pixDTO);
        LOGGER.info("Solicitacao de Pix aceita para processamento: id={}", pixDTO.id());
    }
}
