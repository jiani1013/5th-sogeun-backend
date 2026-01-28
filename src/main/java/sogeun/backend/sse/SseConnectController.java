package sogeun.backend.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class SseConnectController {

    private static final Logger log =
            LoggerFactory.getLogger(SseConnectController.class);

    private final SseEmitterRegistry registry;

    public SseConnectController(SseEmitterRegistry registry) {
        this.registry = registry;
    }


    //sse 연결 포인트 (무조건 이거부터 시작해야함)
    @GetMapping("/sse/stream")
    public SseEmitter connect(@RequestParam Long userId) {
        log.info("[SSE-CONNECT] userId={} connect request", userId);

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30분

        // 중복 연결 처리 (기존 emitter 종료 후 교체)
        registry.addOrReplace(userId, emitter);

        emitter.onCompletion(() -> {
            registry.remove(userId);
            log.info("[SSE-DISCONNECT] userId={} completed", userId);
        });

        emitter.onTimeout(() -> {
            registry.remove(userId);
            log.info("[SSE-DISCONNECT] userId={} timeout", userId);
        });

        emitter.onError(e -> {
            registry.remove(userId);
            log.warn("[SSE-DISCONNECT] userId={} error={}", userId, e.toString());
        });

        return emitter;
    }
}
