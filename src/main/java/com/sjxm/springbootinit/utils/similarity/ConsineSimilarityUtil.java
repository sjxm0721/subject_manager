package com.sjxm.springbootinit.utils.similarity;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/2
 * @Description: 余弦相似度算法，适用于短文本
 */
public class ConsineSimilarityUtil {
    public static double calculate(String text1, String text2) {
        //将文本转换为词频向量
        Map<String, Integer> vector1 = getTermFrequencyMap(text1);
        Map<String, Integer> vector2 = getTermFrequencyMap(text2);

        //计算向量点积
        double dotProduct = 0.0;
        for (String term : vector1.keySet()) {
            if (vector2.containsKey(term)) {
                dotProduct += vector1.get(term) * vector2.get(term);
            }
        }

        //计算向量模长
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (double freq : vector1.values()) {
            norm1 += freq * freq;
        }
        for (double freq : vector2.values()) {
            norm2 += freq * freq;
        }

        //计算余弦相似度
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private static Map<String, Integer> getTermFrequencyMap(String text) {
        Map<String, Integer> termFreqMap = new HashMap<>();
        String[] terms = text.split("\\s+");
        for (String term : terms) {
            termFreqMap.merge(term, 1, Integer::sum);
        }
        return termFreqMap;
    }
}
