package com.example.YT_aouth_test.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtube.model.LiveChatMessage;
import com.google.api.services.youtube.model.LiveChatMessageListResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class YouTubeService {

    private final OAuth2AuthorizedClientService clientService;

    public YouTubeService(OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
    }

    // 1. YouTube 서비스 객체 생성 메서드
    public YouTube getYouTubeService(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName());

        GoogleCredential credential = new GoogleCredential().setAccessToken(client.getAccessToken().getTokenValue());

        return new YouTube.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("YOUR_APPLICATION_NAME")
                .build();
    }

    // 2. 현재 활성화된 라이브 방송 가져오기
    public Optional<LiveBroadcast> getCurrentLiveBroadcast(OAuth2AuthenticationToken authentication) throws IOException {
        YouTube youtubeService = getYouTubeService(authentication);

        YouTube.LiveBroadcasts.List request = youtubeService.liveBroadcasts()
                .list("snippet,contentDetails,status");

        request.setMine(true);
        // request.setBroadcastStatus("active"); // 이 줄을 제거합니다.

        LiveBroadcastListResponse response = request.execute();
        List<LiveBroadcast> broadcasts = response.getItems();

        if (broadcasts != null && !broadcasts.isEmpty()) {
            for (LiveBroadcast broadcast : broadcasts) {
                // 방송의 lifeCycleStatus가 'live'인지 확인합니다.
                if ("live".equals(broadcast.getStatus().getLifeCycleStatus())) {
                    return Optional.of(broadcast);
                }
            }
        }
        return Optional.empty();
    }

    // 3. 라이브 채팅 ID 가져오기
    public String getLiveChatId(LiveBroadcast liveBroadcast) {
        return liveBroadcast.getSnippet().getLiveChatId();
    }

    // 4. 라이브 채팅 메시지 가져오기
    public List<LiveChatMessage> getLiveChatMessages(OAuth2AuthenticationToken authentication, String liveChatId) throws IOException {
        YouTube youtubeService = getYouTubeService(authentication);

        YouTube.LiveChatMessages.List request = youtubeService.liveChatMessages()
                .list(liveChatId, "snippet,authorDetails"); // 수정된 부분
        request.setMaxResults(50L);

        LiveChatMessageListResponse response = request.execute();
        return response.getItems();
    }
}