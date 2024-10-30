package com.example.YT_aouth_test.controller;

import com.example.YT_aouth_test.service.YouTubeService;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveChatMessage;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    private final YouTubeService youTubeService;

    public HomeController(YouTubeService youTubeService) {
        this.youTubeService = youTubeService;
    }

    @GetMapping("/")
    public String index() {
        return "index"; // index.html 뷰로 이동
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // login.html 뷰로 이동
    }

    @GetMapping("/home")
    public String home(Model model, OAuth2AuthenticationToken authentication) {
        if (authentication != null) {
            model.addAttribute("name", authentication.getPrincipal().getAttributes().get("name"));

            try {
                Optional<LiveBroadcast> liveBroadcastOpt = youTubeService.getCurrentLiveBroadcast(authentication);
                if (liveBroadcastOpt.isPresent()) {
                    LiveBroadcast liveBroadcast = liveBroadcastOpt.get();
                    String liveChatId = youTubeService.getLiveChatId(liveBroadcast);
                    List<LiveChatMessage> messages = youTubeService.getLiveChatMessages(authentication, liveChatId);

                    // 비디오 ID 가져오기
                    String videoId = liveBroadcast.getId();

                    model.addAttribute("live", true);
                    model.addAttribute("messages", messages);
                    model.addAttribute("videoId", videoId); // 비디오 ID를 모델에 추가
                } else {
                    model.addAttribute("live", false);
                }
            } catch (IOException e) {
                e.printStackTrace();
                model.addAttribute("live", false);
            }
        }
        return "home"; // home.html 뷰로 이동
    }
}