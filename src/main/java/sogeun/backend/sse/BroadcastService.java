package sogeun.backend.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sogeun.backend.dto.request.MusicInfo;
import sogeun.backend.sse.dto.UserNearbyResponse;
import sogeun.backend.entity.Broadcast;
import sogeun.backend.entity.BroadcastLike;
import sogeun.backend.entity.Music;
import sogeun.backend.entity.User;
import sogeun.backend.repository.BroadcastLikeRepository;
import sogeun.backend.repository.BroadcastRepository;
import sogeun.backend.repository.UserRepository;
import sogeun.backend.service.MusicService;
import sogeun.backend.sse.dto.BroadcastEventDto;
import sogeun.backend.sse.dto.BroadcastLikeRequest;
import sogeun.backend.sse.dto.MusicDto;
import sogeun.backend.sse.dto.MusicResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BroadcastService {

    private final SseEmitterRegistry registry;
    private final LocationService locationService;

    private final BroadcastRepository broadcastRepository;
    private final BroadcastLikeRepository broadcastLikeRepository;

    private final UserRepository userRepository;
    private final MusicService musicService;


    // 켜져 있는 사용자 상태 저장
    private final Set<Long> activeSenders = ConcurrentHashMap.newKeySet();

    public BroadcastService(
            SseEmitterRegistry registry,
            LocationService locationService,
            BroadcastRepository broadcastRepository,
            BroadcastLikeRepository broadcastLikeRepository, UserRepository userRepository, MusicService musicService
    ) {
        this.registry = registry;
        this.locationService = locationService;
        this.broadcastRepository = broadcastRepository;
        this.broadcastLikeRepository = broadcastLikeRepository;
        this.userRepository = userRepository;
        this.musicService = musicService;
    }

    @Transactional
    public void turnOn(Long senderId, double lat, double lon, MusicInfo musicInfo) {

        log.info("[BROADCAST-ON] start senderId={}, lat={}, lon={}", senderId, lat, lon);

        if (musicInfo == null || musicInfo.getTrackId() == null) {
            throw new IllegalArgumentException("musicInfo.trackId is required");
        }

        // 1) 위치 최신화
        locationService.saveLocation(senderId, lat, lon);

        // 2) broadcast 없으면 생성, 있으면 가져오기
        Broadcast broadcast = broadcastRepository.findBySenderId(senderId)
                .orElseGet(() -> broadcastRepository.save(Broadcast.create(senderId)));

        // 3) Music 엔티티 find-or-create (trackId 기준)
        Music music = musicService.findOrCreate(musicInfo);

        // 4) 현재 음악 갱신
        broadcast.updateCurrentMusic(music);

        // 5) 좋아요 기반 반경 계산/반영
        int radius = calculateRadius(broadcast.getLikeCount());
        broadcast.updateRadiusMeter(radius);

        // 6) 주변 대상 조회
        List<Long> targetUserIds = locationService.findNearbyUsers(senderId, lat, lon, radius);

        // 7) 이벤트 생성
        MusicDto dto = new MusicDto(
                musicInfo.getTrackId(),
                musicInfo.getTrackName(),
                musicInfo.getArtistName(),
                musicInfo.getArtworkUrl(),
                musicInfo.getPreviewUrl()
        );

        BroadcastEventDto event = BroadcastEventDto.on(senderId, dto);

        activeSenders.add(senderId);
        sendToTargets(targetUserIds, "broadcast.on", senderId, event);

        log.info("[BROADCAST-ON] done senderId={}, targetCount={}", senderId,
                targetUserIds != null ? targetUserIds.size() : 0);
    }





    public void turnOff(Long senderId) {

        // 이미 off일떄 처리
        activeSenders.remove(senderId);

        Broadcast broadcast = broadcastRepository.findBySenderId(senderId)
                .orElseThrow(() -> new IllegalArgumentException("broadcast not found"));

        // db 상태도 off로 갱신
        broadcast.deactivate(); // 또는 turnOff()

        // 마지막 위치 가져오기
        Point p = locationService.getLocation(senderId);
        if (p == null) return;

        double lat = p.getY();
        double lon = p.getX();

        List<Long> targetUserIds =
                locationService.findNearbyUsers(senderId, lat, lon, broadcast.getRadiusMeter());


        BroadcastEventDto event = BroadcastEventDto.off(senderId);
        sendToTargets(targetUserIds, "broadcast.off", senderId, event);
    }


    //좋아요(토글임)
    @Transactional
    public void toggleLike(BroadcastLikeRequest request) {
        Long senderId = request.getSenderId();
        Long likerId = request.getLikerId();
        double lat = request.getLat();
        double lon = request.getLon();

        if (senderId.equals(likerId)) return;

        Broadcast broadcast = broadcastRepository.findBySenderId(senderId)
                .orElseThrow(() -> new IllegalArgumentException("broadcast not found"));

        // 1) 좋아요 토글 + 반경 갱신
        broadcastLikeRepository
                .findByBroadcast_BroadcastIdAndLikerUserId(broadcast.getBroadcastId(), likerId)
                .ifPresentOrElse(existing -> {
                    broadcastLikeRepository.delete(existing);

                    int nextLike = Math.max(0, broadcast.getLikeCount() - 1);
                    broadcast.decreaseLike(calculateRadius(nextLike));
                }, () -> {
                    broadcastLikeRepository.save(BroadcastLike.create(broadcast, likerId));

                    int nextLike = broadcast.getLikeCount() + 1;
                    broadcast.increaseLike(calculateRadius(nextLike));
                });

        // 2) 위치 최신으로 업뎃
        locationService.saveLocation(senderId, lat, lon);

        // 3) 변경된 반경으로 타겟 재계산
        List<Long> targetUserIds = locationService.findNearbyUsers(
                senderId,
                lat,
                lon,
                broadcast.getRadiusMeter()
        );

        // 4) SSE 전송
        BroadcastEventDto event = BroadcastEventDto.likeUpdated(
                senderId,
                broadcast.getLikeCount(),
                broadcast.getRadiusMeter()
        );

        sendToTargets(targetUserIds, "broadcast.like", senderId, event);
    }

    //반경 계산
    private int calculateRadius(int likeCount) {
        if (likeCount < 10) return 200; //이거 공식화 해야할듯ㅜㅜㅜㅜ
        if (likeCount < 30) return 400;
        if (likeCount < 60) return 600;
        return 800;
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

    @Transactional(readOnly = true)
    public List<UserNearbyResponse> findNearbyUsersWithBroadcast(List<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyList();

        // 1) 유저 정보 조회
        List<User> users = userRepository.findAllById(ids);

        // 2) on인 방송 조회
        List<Broadcast> broadcasts = broadcastRepository.findBySenderIdInAndIsActiveTrue(ids);

        Map<Long, Broadcast> broadcastMap = broadcasts.stream()
                .collect(Collectors.toMap(Broadcast::getSenderId, b -> b));

        return users.stream()
                .map(user -> {
                    Broadcast broadcast = broadcastMap.get(user.getUserId());

                    // 방송 중일 때만 음악 정보 생성...
                    MusicResponse musicDto = null;
                    if (broadcast != null && broadcast.getMusic() != null) {
                        Music m = broadcast.getMusic();
                        musicDto = new MusicResponse(
                                m.getTrackId(), m.getTitle(), m.getArtist(), m.getArtworkUrl(), m.getPreviewUrl()
                        );
                    }

                    return new UserNearbyResponse(
                            user.getUserId(),
                            user.getNickname(),
                            broadcast != null, // 방송 중 여부
                            musicDto,
                            broadcast != null ? broadcast.getRadiusMeter() : null,
                            broadcast != null ? broadcast.getLikeCount() : 0 // 테스트 시 0으로 보이면 좋음
                    );
                })
                .toList();
    }

}
