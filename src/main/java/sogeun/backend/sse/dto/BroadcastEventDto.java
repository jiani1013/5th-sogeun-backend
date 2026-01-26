package sogeun.backend.sse.dto;

import java.util.UUID;

public class BroadcastEventDto {

    private String type;      // BROADCAST_ON, BROADCAST_OFF, ...
    private Long senderId;
    private MusicDto music;   // OFF면 null 가능
    private long ts;
    private String eventId;

    private BroadcastEventDto(
            String type,
            Long senderId,
            MusicDto music
    ) {
        this.type = type;
        this.senderId = senderId;
        this.music = music;
        this.ts = System.currentTimeMillis();
        this.eventId = UUID.randomUUID().toString();
    }

    // ✅ 여기서 빨간 줄 해결됨
    public static BroadcastEventDto on(Long senderId, MusicDto music) {
        return new BroadcastEventDto("BROADCAST_ON", senderId, music);
    }

    public static BroadcastEventDto off(Long senderId) {
        return new BroadcastEventDto("BROADCAST_OFF", senderId, null);
    }

}
