package sogeun.backend.sse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BroadcastController {

    private final BroadcastService broadcastService;

    public BroadcastController(BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    //송신 시작
    @PostMapping("/broadcast/on")
    public ResponseEntity<String> on(@RequestParam Long senderId) {
        broadcastService.turnOn(senderId);
        return ResponseEntity.ok("on sent");
    }

    //송신 종료
    @PostMapping("/broadcast/off")
    public ResponseEntity<String> off(@RequestParam Long senderId) {
        broadcastService.turnOff(senderId);
        return ResponseEntity.ok("off sent");
    }
}
