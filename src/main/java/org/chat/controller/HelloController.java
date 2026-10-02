package org.chat.controller;

import org.chat.service.AiService;
import org.chat.service.Assistant;
import org.chat.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;


@RestController
public class HelloController {
    @Autowired
    private AiService aiService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private Assistant assistant;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @RequestMapping("hello")
    public String hello() {
        return "hello world";
    }

    @GetMapping("/chat")
    public String chat(
            @RequestParam String sessionId,
            @RequestParam String message) {

        if (StrUtil.isBlank(sessionId)) {
            sessionId = "default";
        }

        return assistant.chat(sessionId, message);
    }

    @GetMapping("/ingest")
    public void ingest() {
        // documentService.ingest();
    }
}
