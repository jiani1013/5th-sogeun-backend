package sogeun.backend.sse;

public class LocationController {

    //위치 변화시 해당위치를 프론트한테 받아서 이후 반경 계산 어쩌구...할 컨트롤러입니다
    //-> 이 위치를 redis에 담아야 함    userId → (lon, lat)
    //방송 ON/OFF 시 senderId 좌표를 기준으로 GEORADIUS(또는 동등 연산)로 주변 유저 ID 목록을 조회해야
    //반경 내 유저 조회: sender 위치 기준으로 radius 검색
    //redis 역할은 위치 저장 + 주변사람 찾기 인것(유저   리스트뽑아서 넘겨주기)
}
