package sogeun.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "broadcast",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_broadcast_sender",
                        columnNames = {"sender_id"}
                )
        }
)
public class Broadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "broadcast_id")
    private Long broadcastId;

    @Column(name = "sender_id", nullable = false, unique = true)
    private Long senderId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // 송출 off 상태에서는 null 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_id")
    private Music music;   // 현재 송출 음악 (nullable)

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "radius_meter", nullable = false)
    private int radiusMeter;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    public void prePersist() {
        this.startedAt = LocalDateTime.now();
        this.updatedAt = this.startedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    public static Broadcast create(Long senderId) {
        Broadcast b = new Broadcast();
        b.senderId = senderId;
        b.likeCount = 0;
        b.radiusMeter = 200;
        b.isActive = false;  //기본 off
        return b;
    }

    // 송출 on
    public void activate(Music music, int radiusMeter) {
        this.isActive = true;
        this.music = music;
        this.radiusMeter = radiusMeter;
    }

    // 송출 off
    public void deactivate() {
        this.isActive = false;
        this.music = null;
    }

    public boolean isActive() {
        return this.isActive;
    }

    // 현재 음악만 변경 (좋아요/반경 유지하고)
    public void updateCurrentMusic(Music music) {
        this.music = music;
    }

    public void increaseLike(int newRadius) {
        this.likeCount++;
        this.radiusMeter = newRadius;
    }

    public void decreaseLike(int newRadius) {
        this.likeCount = Math.max(0, this.likeCount - 1);
        this.radiusMeter = newRadius;
    }

    public void updateRadiusMeter(int radiusMeter) {
        if (radiusMeter < 0) {
            throw new IllegalArgumentException("radiusMeter must be >= 0");
        }
        this.radiusMeter = radiusMeter;
    }

}

