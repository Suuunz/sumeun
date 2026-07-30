package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.ai.ChatService;
import com.sunz.hidden_travel.controller.dto.ChatRequest;
import com.sunz.hidden_travel.controller.dto.ChatResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * AI 여행 상담 챗봇.
 * 화면은 /chat, 대화는 POST /api/chat 로 주고받는다.
 */
@Controller
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    public String chatPage(Model model) {
        // 키가 없으면 화면 상단에 설정 안내를 띄운다
        model.addAttribute("aiConfigured", chatService.isConfigured());
        return "chat";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }
}
