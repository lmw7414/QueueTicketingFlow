package org.example.flow.controller;

import lombok.RequiredArgsConstructor;
import org.example.flow.service.UserQueueService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class WaitingRoomController {
    private final UserQueueService userQueueService;

    @GetMapping("/waiting-room")
    public Mono<Rendering> waitingRoomPage(@RequestParam(name = "queue", defaultValue = "default") String queue,
                                           @RequestParam(name = "user_id") Long userId,
                                           @RequestParam(name = "redirect_url") String redirectUrl) {
        return userQueueService.isAllowed(queue, userId)// 1. 입장이 허용되어 page redirect가 가능한 상태인가
                .filter(allowed -> allowed)
                .flatMap(allowed -> Mono.just(Rendering.redirectTo(redirectUrl).build()))
                .switchIfEmpty( // 2. 대기 순위라면 어디로 이동시켜야 하는가?
                        userQueueService.registerWaitingQueue(queue, userId) // 대기 등록(페이지 접속 시 대기 큐에 등록 - 새로고침하면 뒷순위로)
                                .onErrorResume(ex -> userQueueService.getRank(queue, userId))// 이미 등록된 상태라면
                                .map(rank -> Rendering.view("waiting-room.html")
                                        .modelAttribute("number", rank)
                                        .modelAttribute("userId", userId)
                                        .modelAttribute("queue", queue)
                                        .build()
                                )
                );

    }

}
