package org.chat.component;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

import java.util.List;

import org.chat.consts.CommonData;
import org.chat.utils.FormulaUtil;
import org.springframework.stereotype.Component;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;

@Component
public class DynamicContentRetriever implements ContentRetriever {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public DynamicContentRetriever(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) {

        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Override
    public List<Content> retrieve(Query query) {

        System.out.println("\n========================================");
        System.out.println("RAG Query: " + query);

        // 1. 判断用户问题里有没有明确方剂
        String formula = FormulaUtil.findFormula(query.text()).orElse(null);

        if (StrUtil.isNotBlank(formula)) {
            System.out.println("识别到方剂: " + formula);
        } else {
            System.out.println("未识别到方剂");
        }

        // 2. Query embedding
        var queryEmbedding = embeddingModel.embed(query.text()).content();

        // 3. 构建搜索请求
        EmbeddingSearchRequest request;

        // 4. 如果识别到方剂，则增加 metadata filter
        if (StrUtil.isNotBlank(formula)) {
            // metadata filter + vector search
            request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(CommonData.MAX_RESULT)
                    .minScore(CommonData.MIN_SCORE)
                    .filter(
                            metadataKey("formula")
                                    .isEqualTo(formula)
                    )
                    .build();
        } else {
            // 没有明确方剂：
            // 纯 vector search
            request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(CommonData.MAX_RESULT)
                    .minScore(CommonData.MIN_SCORE)
                    .build();
        }

        // 5. Qdrant 搜索
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();

        System.out.println("召回数量: " + matches.size());

        // 6. 打印调试信息
        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            System.out.println("\nTop " + (i + 1) + " Score=" + match.score());
            System.out.println(match.embedded().text());
            System.out.println(match.embedded().metadata());
        }

        System.out.println("========================================");

        // 7. 转成 LangChain4j Content
        return matches.stream()
                .map(match -> Content.from(match.embedded()))
                .toList();
    }
}

