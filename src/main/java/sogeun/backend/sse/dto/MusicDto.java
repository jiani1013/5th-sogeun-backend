package sogeun.backend.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MusicDto {

    //추후 수정 예정
    private String title;
    private String artist;
    private String albumArtUrl;
    private String trackKey;
}
