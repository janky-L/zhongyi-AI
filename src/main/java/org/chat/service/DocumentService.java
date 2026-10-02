package org.chat.service;

import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.document.Metadata;

import dev.langchain4j.store.embedding.EmbeddingStore;

@Service
public class DocumentService {

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    public void ingest() {

        loadShl();
        loadJkyl();
    }

    private void loadShl() {
        try {
            String fileName = "伤寒论原文.xlsx";
            String path = "/Users/qqww/Downloads/traeProject/zhongyi-AI/docs/";
            InputStream inputStream = new FileInputStream(path + fileName);

            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            String titleTemp = "";
            int rowNumber = 0;
            for (Row row : sheet) {
                if (rowNumber++ < 2) continue;
                String title = row.getCell(0).getStringCellValue();
                if(StrUtil.isBlank(title)) {
                    title = titleTemp;
                } else {
                    titleTemp = title;
                }
                int num = (int) row.getCell(1).getNumericCellValue();
                String content = row.getCell(2).getStringCellValue();
                String keyWord = row.getCell(3).getStringCellValue();

                if(StrUtil.isBlank(content))    break;

                String text = """
                来源：《伤寒论》
                六经：%s
                条辩编号：第%s条
                原文：%s
                """.formatted(
                        title,
                        num,
                        content
                );

                System.out.println(text);

                Metadata metadata = new Metadata();
                metadata.put("source", "伤寒论");
                metadata.put("chapter", title);
                metadata.put("article_no", String.valueOf(num));
                if (StrUtil.isNotBlank(keyWord)) {
                    metadata.put("formula", keyWord);
                }

                TextSegment segment = TextSegment.from(text,metadata);
                Embedding embedding = embeddingModel.embed(text).content();
                embeddingStore.add(embedding, segment);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadJkyl() {
        try {
            String fileName = "金匮要略原文.xlsx";
            String path = "/Users/qqww/Downloads/traeProject/zhongyi-AI/docs/";
            InputStream inputStream = new FileInputStream(path + fileName);

            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            String titleTemp1 = "";
            String titleTemp2 = "";
            int rowNumber = 0;
            for (Row row : sheet) {
                if (rowNumber++ < 2) continue;
                String title1 = row.getCell(0).getStringCellValue();
                if(StrUtil.isBlank(title1)) {
                    title1 = titleTemp1;
                } else {
                    titleTemp1 = title1;
                }
                String title2 = row.getCell(1).getStringCellValue();
                if(StrUtil.isBlank(title2)) {
                    title2 = titleTemp2;
                } else {
                    titleTemp2 = title2;
                }
                String content = row.getCell(2).getStringCellValue();
                String keyWord = row.getCell(3).getStringCellValue();

                if(StrUtil.isBlank(content))    break;

                StringBuilder sb = new StringBuilder();
                sb.append("来源：《金匮要略》\n");
                sb.append(title1 + "章 " + title2 + "\n");
                sb.append("原文：" + content + "\n");
                if(StrUtil.isNotBlank(keyWord)) {
                    sb.append("药方：" + keyWord + "\n");
                }

                String text = sb.toString();

                System.out.println(text);

                Metadata metadata = new Metadata();
                metadata.put("source", "金匮要略");
                metadata.put("chapter", title1);
                metadata.put("sub_chapter", title2);
                if (StrUtil.isNotBlank(keyWord)) {
                    metadata.put("formula", keyWord);
                }

                TextSegment segment = TextSegment.from(text,metadata);
                Embedding embedding = embeddingModel.embed(text).content();
                embeddingStore.add(embedding, segment);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
