package famel.com.safepix_async.controller;

import famel.com.safepix_async.service.DlqAdminService;
import famel.com.safepix_async.service.DlqMessageView;
import famel.com.safepix_async.service.DlqReprocessResult;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dlq/pix")
public class DlqAdminController {

    private final DlqAdminService dlqAdminService;

    public DlqAdminController(DlqAdminService dlqAdminService) {
        this.dlqAdminService = dlqAdminService;
    }

    @GetMapping
    public List<DlqMessageView> list(@RequestParam(defaultValue = "10") int limit) {
        return dlqAdminService.listMessages(limit);
    }

    @PostMapping("/reprocess")
    public DlqReprocessResult reprocess(@RequestParam(defaultValue = "10") int limit) {
        return dlqAdminService.reprocessMessages(limit);
    }
}
