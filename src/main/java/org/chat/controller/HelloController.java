package org.chat.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.chat.service.Assistant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import dev.langchain4j.service.TokenStream;

@RestController
public class HelloController {
    @Autowired
    private Assistant assistant;

    @RequestMapping("hello")
    public String hello() {
        return "hello world";
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody chat(
            @RequestParam(required = false) String sessionId,
            @RequestParam String message) {

        final String sid = (sessionId == null || sessionId.isBlank())
                ? "default" : sessionId;

        return output -> {
            CountDownLatch finished = new CountDownLatch(1);
            AtomicBoolean disconnected = new AtomicBoolean(false);

            TokenStream stream = assistant.chat(sid, message)
                    .onPartialResponse(chunk -> {
                        if (disconnected.get()) {
                            return;
                        }

                        try {
                            synchronized (output) {
                                if (disconnected.get()) {
                                    return;
                                }

                                // SSE 格式：每条消息以空行结束
                                String data = chunk
                                        .replace("\r", "")
                                        .replace("\n", "\ndata: ");

                                output.write(("data: " + data + "\n\n")
                                        .getBytes(StandardCharsets.UTF_8));
                                output.flush();
                            }
                        } catch (IOException | IllegalStateException e) {
                            disconnected.set(true);
                        }
                    })
                    .onCompleteResponse(response -> finished.countDown())
                    .onError(error -> {
                        // 记录模型错误
                        System.err.println("LLM stream error: " + error);
                        finished.countDown();
                    });

            try {
                stream.start();

                // 等待模型结束，避免 Lambda 提前返回
                while (!finished.await(1, TimeUnit.SECONDS)) {
                    if (disconnected.get()) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    @GetMapping("/ingest")
    public void ingest() {
        // documentService.ingest();
    }
}
