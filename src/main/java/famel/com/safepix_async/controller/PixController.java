package famel.com.safepix_async.controller;

import famel.com.safepix_async.domain.dto.PixDTO;
import famel.com.safepix_async.service.PixService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pix")
public class PixController {

    private final PixService pixService;

    public PixController(PixService pixService) {
        this.pixService = pixService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receberPix(@Valid @RequestBody PixDTO pixDTO) {
        pixService.enviarPix(pixDTO);
    }
}
