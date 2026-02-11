package sogeun.backend.dto.request;

import lombok.Getter;

@Getter
public class MusicLikeRequest {

    private MusicInfo music;
    private Long userId;
    private Long musicId;

    // Standard Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getMusicId() { return musicId; }
    public void setMusicId(Long musicId) { this.musicId = musicId; }
}