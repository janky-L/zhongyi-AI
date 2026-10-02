本地中医经典知识库实验项目

Java + LangChain4j + Ollama + qwen2.5:7b + Qdrent


java 17 

Ollama + qwen2.5:7b / nomic-embed-text:latest


在项目里的docker目录下执行
docker compose up -d


创建向量数据库
curl -X PUT 'http://localhost:6333/collections/knowledge_base' \
-H 'Content-Type: application/json' \
-d '{
  		"vectors": {
     		"size": 768,
        	"distance": "Cosine"
        }
    }'


本地访问Qdrant数据库
http://localhost:6333/dashboard
