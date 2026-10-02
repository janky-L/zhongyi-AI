package org.chat.config;

import org.chat.component.DynamicContentRetriever;
import org.chat.service.Assistant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;

@Configuration
public class AiConfig {

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.model-name}")
    private String modelName;

    @Value("${ollama.temperature}")
    private double temperature;

    @Value("${ollama.embedding-model}")
    private String embeddingModelName;

    @Bean
    public ChatModel chatModel() {
        return OllamaChatModel.builder().baseUrl(baseUrl).modelName(modelName).temperature(temperature).build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder().baseUrl(baseUrl).modelName(embeddingModelName).build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            @Value("${qdrant.host}") String host,
            @Value("${qdrant.port}") int port,
            @Value("${qdrant.collection-name}") String collectionName) {

        return QdrantEmbeddingStore.builder()
                .host(host)
                .port(port)
                .collectionName(collectionName)
                .build();
    }

    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) {

        return EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    @Bean
    public Assistant assistant(
            ChatModel chatModel,
            DynamicContentRetriever contentRetriever,
            ChatMemoryProvider chatMemoryProvider) {

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {

        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(50)    // 最多保留50条上下文消息
                .build();
    }
}
