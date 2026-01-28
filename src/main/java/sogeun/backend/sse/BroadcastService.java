package sogeun.backend.sse;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sogeun.backend.sse.dto.BroadcastEventDto;
import sogeun.backend.sse.dto.MusicDto;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//@Component
@Service
public class BroadcastService {

    private final SseEmitterRegistry registry;

    // ✅ "켜져 있는 사용자" 상태 저장 (최소 토글)
    private final Set<Long> activeSenders = ConcurrentHashMap.newKeySet();

    public BroadcastService(SseEmitterRegistry registry) {
        this.registry = registry;
    }

    public void turnOn(Long senderId) {
        if (!activeSenders.add(senderId)) return;

        // 1. 반경 내 사용자 조회 (지금은 임시, 나중에 Redis GEO)
        MusicDto music = new MusicDto("title", "artist", "albumArtUrl", "trackKey"); // 현재는

        List<Long> targetUserIds = findTargets(senderId); // TODO: 나중에 반경 조회로 교체

        // 2. 실시간 SSE 전송
        BroadcastEventDto event = BroadcastEventDto.on(senderId, music);//타 유저에게 보내는 데이터 -> 나중에 이게 설정한 음악이 되어야함
        sendToTargets(targetUserIds, "broadcast.on", senderId, event);

    }


    public void turnOff(Long senderId) {
        // 이미 OFF면 무시
        if (!activeSenders.remove(senderId)) return;

        List<Long> targetUserIds = findTargets(senderId); // TODO: 나중에 반경 조회로 교체

        BroadcastEventDto event = BroadcastEventDto.off(senderId);
        sendToTargets(targetUserIds, "broadcast.off", senderId, senderId);

    }

    private void sendToTargets(List<Long> targetUserIds, String eventName, Long senderId, Object data) {
        for (Long targetId : targetUserIds) {
            if (targetId.equals(senderId)) continue;

            SseEmitter emitter = registry.get(targetId);
            if (emitter == null) continue;

            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                registry.remove(targetId);
                try { emitter.completeWithError(e); } catch (Exception ignore) {}
            }
        }
    }



    // 테스트 단계에서는 "반경 내 유저"를 임시로 고정함
    private List<Long> findTargets(Long senderId) {
        return List.of(2L, 3L); // TODO: Redis GEO 결과로 교체
    }
}
