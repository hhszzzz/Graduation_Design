import torch
from transformers import BertTokenizer, BertModel
import numpy as np
import pandas as pd
from sklearn.metrics.pairwise import cosine_similarity


def get_bert_embeddings(text_list, model_name='bert-base-chinese', device='cpu'):
    """
    输入：一组文本（新闻标题），输出：每条文本的向量表示
    """
    tokenizer = BertTokenizer.from_pretrained(model_name)
    model = BertModel.from_pretrained(model_name)
    model.to(device)
    model.eval()

    embeddings = []

    for text in text_list:
        inputs = tokenizer(text, return_tensors='pt', truncation=True, max_length=64)
        input_ids = inputs['input_ids'].to(device)
        attention_mask = inputs['attention_mask'].to(device)

        with torch.no_grad():
            outputs = model(input_ids, attention_mask=attention_mask)
            # 一般取[CLS]向量或整句平均，这里演示[CLS]向量
            cls_vec = outputs.last_hidden_state[:, 0, :]

        cls_vec = cls_vec.cpu().numpy().flatten()
        embeddings.append(cls_vec)

    return np.array(embeddings)


if __name__ == '__main__':
    # 1) 读取csv，提取新闻标题
    df = pd.read_csv('../graduation_design/backend/data/news_export/daily_news_titles_2025-03-21.csv')
    news_titles = df['title'].tolist()

    # 2) 获取每个标题的向量
    device = "cuda" if torch.cuda.is_available() else "cpu"
    embeddings = get_bert_embeddings(news_titles, device=device)

    # 3) 计算相似度矩阵
    similarity_matrix = cosine_similarity(embeddings)
    print("相似度矩阵：\n", similarity_matrix)

    # 4) 给定某条新闻，找最相似的其他新闻
    target_idx = 0  # 假设以第0条新闻为基准
    sim_scores = similarity_matrix[target_idx]
    sorted_indices = np.argsort(sim_scores)[::-1]

    print(f"\n当前标题: {news_titles[target_idx]}")
    print("相似排序(去掉自身):")
    for idx in sorted_indices:
        if idx == target_idx:
            continue
        print(f"    相似度={sim_scores[idx]:.4f}, 标题={news_titles[idx]}")
