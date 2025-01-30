package com.sjxm.springbootinit.utils.similarity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public class ConsineSimilarityUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConsineSimilarityUtil.class);

    public static double calculate(String text1, String text2) {
        try {
            logger.info("开始计算余弦相似度: text1长度={}, text2长度={}",
                    text1 != null ? text1.length() : 0,
                    text2 != null ? text2.length() : 0);

            if (text1 == null || text2 == null) {
                logger.warn("输入文本存在null值, 返回0");
                return 0.0;
            }

            Map<String, Integer> vector1 = getTermFrequencyMap(text1);
            Map<String, Integer> vector2 = getTermFrequencyMap(text2);

            logger.info("向量1大小={}, 向量2大小={}", vector1.size(), vector2.size());

            double dotProduct = 0.0;
            for (String term : vector1.keySet()) {
                if (vector2.containsKey(term)) {
                    dotProduct += vector1.get(term) * vector2.get(term);
                }
            }

            double norm1 = 0.0;
            double norm2 = 0.0;
            for (double freq : vector1.values()) {
                norm1 += freq * freq;
            }
            for (double freq : vector2.values()) {
                norm2 += freq * freq;
            }

            if (norm1 == 0 || norm2 == 0) {
                logger.warn("向量模长为0, 返回0: norm1={}, norm2={}", norm1, norm2);
                return 0.0;
            }

            double similarity = dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
            logger.info("余弦相似度计算完成: similarity={}", similarity);
            return similarity;
        } catch (Exception e) {
            logger.error("余弦相似度计算出错", e);
            return 0.0;
        }
    }

    private static Map<String, Integer> getTermFrequencyMap(String text) {
        try {
            Map<String, Integer> termFreqMap = new HashMap<>();
            String[] terms = text.split("\\s+");
            logger.info("分词结果: terms数量={}", terms.length);

            for (String term : terms) {
                termFreqMap.merge(term, 1, Integer::sum);
            }
            return termFreqMap;
        } catch (Exception e) {
            logger.error("生成词频图出错", e);
            return new HashMap<>();
        }
    }
}