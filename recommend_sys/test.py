# 安装必要库（如果还没安装的话）
# pip install sentence-transformers faiss-cpu torch numpy

from sentence_transformers import SentenceTransformer, CrossEncoder
import faiss
import numpy as np

# 1. 加载现成模型
retriever_model = SentenceTransformer('all-MiniLM-L6-v2')  # 轻量化Sentence-BERT
reranker_model = CrossEncoder('cross-encoder/ms-marco-MiniLM-L-6-v2')  # 轻量化Cross-Encoder

# 2. 新闻标题数据集（可以换成你的新闻标题）
news_titles = [
    "Global economy faces new challenges in 2025",
    "AI revolutionizes textile industry",
    "New material breakthroughs in smart fabrics",
    "Spring Fashion Week introduces innovative styles",
    "Eco-friendly trends in modern clothing",
    "Breakthroughs in renewable energy technologies",
    "Advancements in autonomous vehicle design",
    "Healthcare industry sees AI adoption rise",
    "Climate change initiatives gain momentum",
    "Smart city projects reshape urban living"
]

# 3. 离线阶段：对所有标题进行向量编码 + 建立Faiss索引
title_vectors = retriever_model.encode(news_titles, normalize_embeddings=True)

dimension = title_vectors.shape[1]
faiss_index = faiss.IndexFlatIP(dimension)  # 内积（越大越相似）
faiss_index.add(title_vectors)

# 4. 在线阶段：输入一个新的新闻标题，进行推荐
def recommend_titles(user_title, top_k_retrieve=5, top_k_final=3):
    # 4.1 Retriever阶段：用Retriever模型编码输入标题
    user_vector = retriever_model.encode([user_title], normalize_embeddings=True)

    # 检索Top N候选标题
    distances, indices = faiss_index.search(user_vector, top_k_retrieve)
    candidate_titles = [news_titles[idx] for idx in indices[0]]

    # 4.2 Re-ranker阶段：对候选标题进行精细打分
    pairs = [(user_title, candidate) for candidate in candidate_titles]
    rerank_scores = reranker_model.predict(pairs)

    # 根据Re-ranker得分排序
    sorted_indices = np.argsort(rerank_scores)[::-1]  # 分数降序
    final_recommendations = [candidate_titles[idx] for idx in sorted_indices[:top_k_final]]

    return final_recommendations

# 5. 测试：模拟用户正在看的一条新闻
user_current_title = "Smart fabrics are transforming fashion industry"

recommendations = recommend_titles(user_current_title)

# 6. 打印最终推荐结果
print(f"用户当前浏览新闻：{user_current_title}")
print("\n相关推荐：")
for i, title in enumerate(recommendations, 1):
    print(f"{i}. {title}")
