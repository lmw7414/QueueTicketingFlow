package org.example.flow.service;

import org.example.flow.EmbeddedRedis;
import org.example.flow.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(EmbeddedRedis.class)
@ActiveProfiles("test")
class UserQueueServiceTest {
    @Autowired
    private UserQueueService userQueueService;
    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @BeforeEach
    public void beforeEach() {
        ReactiveRedisConnection redisConnection = reactiveRedisTemplate.getConnectionFactory().getReactiveConnection();
        redisConnection.serverCommands().flushAll().subscribe();
    }

    @Test
    void registerWaitingQueue() {
        StepVerifier.create(userQueueService.registerWaitingQueue("default", 100L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitingQueue("default", 101L))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitingQueue("default", 102L))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void alreadyRegisteredUserinWaitingQueue() {
        StepVerifier.create(userQueueService.registerWaitingQueue("default", 100L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitingQueue("default", 100L))
                .expectError(ApplicationException.class)
                .verify();
    }

    @Test
    void isNotAllowed() {
        StepVerifier.create(
                        userQueueService.isAllowed("default", 100L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void isNotAllowed2() {
        StepVerifier.create(
                        userQueueService.registerWaitingQueue("default", 100L)
                                .then(userQueueService.allowUser("default", 3L))
                                .then(userQueueService.isAllowed("default", 101L)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void isAllowed() {
        StepVerifier.create(
                        userQueueService.registerWaitingQueue("default", 100L)
                                .then(userQueueService.allowUser("default", 3L))
                                .then(userQueueService.isAllowed("default", 100L)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void EmptyAllowUser() {
        StepVerifier.create(userQueueService.allowUser("default", 3L))
                .expectNext(0L)
                .verifyComplete();

    }

    @Test
    void allowUser() {
        StepVerifier.create(
                        userQueueService.registerWaitingQueue("default", 100L)
                                .then(userQueueService.registerWaitingQueue("default", 101L))
                                .then(userQueueService.registerWaitingQueue("default", 102L))
                                .then(userQueueService.allowUser("default", 2L)))
                .expectNext(2L)
                .verifyComplete();

    }

    @Test
    void allowUser2() {
        StepVerifier.create(
                        userQueueService.registerWaitingQueue("default", 100L)
                                .then(userQueueService.registerWaitingQueue("default", 101L))
                                .then(userQueueService.registerWaitingQueue("default", 102L))
                                .then(userQueueService.allowUser("default", 5L)))
                .expectNext(3L)
                .verifyComplete();

    }

    @Test
    void allowUserAfterRegisterWaitQueue() {
        StepVerifier.create(
                        userQueueService.registerWaitingQueue("default", 100L)
                                .then(userQueueService.registerWaitingQueue("default", 101L))
                                .then(userQueueService.registerWaitingQueue("default", 102L))
                                .then(userQueueService.allowUser("default", 3L))
                                .then(userQueueService.registerWaitingQueue("default", 200L)))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void getRank() {
        StepVerifier.create(
                        userQueueService.registerWaitingQueue("default", 100L)
                                .then(userQueueService.registerWaitingQueue("default", 101L))
                                .then(userQueueService.getRank("default", 101L)))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void emptyRank() {
        StepVerifier.create(
                        userQueueService.registerWaitingQueue("default", 100L)
                                .then(userQueueService.getRank("default", 101L)))
                .expectNext(-1L)
                .verifyComplete();
    }

}