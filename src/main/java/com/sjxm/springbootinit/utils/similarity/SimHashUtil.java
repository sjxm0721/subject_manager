package com.sjxm.springbootinit.utils.similarity;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/2
 * @Description:
 */
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import cn.hutool.core.util.StrUtil;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

@Slf4j
public class SimHashUtil {
    private static final int HASH_BITS = 64;
    private static final int WINDOW_SIZE = 3;

    /**
     * 计算多文档组的相似度
     * @param ossUrls1 第一组文档的OSS URL列表（逗号分隔）
     * @param ossUrls2 第二组文档的OSS URL列表（逗号分隔）
     * @param fileType 文件类型（"pdf" 或 "docx"）
     * @return 两组文档的最大相似度
     */
    public static double calculateGroupSimilarity(String ossUrls1, String ossUrls2, String fileType) throws Exception {
        if (StrUtil.isBlank(ossUrls1) || StrUtil.isBlank(ossUrls2)) {
            return 0.0;
        }

        String[] urls1 = ossUrls1.split(",");
        String[] urls2 = ossUrls2.split(",");

        double maxSimilarity = 0.0;

        // 计算两组文档中任意两个文档的相似度，取最大值
        for (String url1 : urls1) {
            url1 = url1.trim();
            if (!url1.startsWith("http")) {
                continue;
            }

            for (String url2 : urls2) {
                url2 = url2.trim();
                if (!url2.startsWith("http")) {
                    continue;
                }

                try {
                    double similarity = calculateSimilarity(url1, url2, fileType);
                    maxSimilarity = Math.max(maxSimilarity, similarity);
                } catch (Exception e) {
                    log.error("计算文档相似度失败: {} - {}", url1, url2, e);
                }
            }
        }

        return maxSimilarity;
    }

    /**
     * 计算单个文档的相似度
     */
    private static double calculateSimilarity(String ossUrl1, String ossUrl2, String fileType) throws Exception {
        String content1 = extractContent(ossUrl1, fileType);
        String content2 = extractContent(ossUrl2, fileType);
        SimHashGenerator hash1 = new SimHashGenerator(content1, true);
        SimHashGenerator hash2 = new SimHashGenerator(content2, true);

        return hash1.getSimilarity(hash2);
    }

    /**
     * 从OSS URL提取文档内容
     */
    private static String extractContent(String ossUrl, String fileType) throws Exception {
        try (InputStream inputStream = new BufferedInputStream(new URL(ossUrl).openStream())) {
            switch (fileType.toLowerCase()) {
                case "pdf":
                    return extractPdfContent(inputStream);
                case "docx":
                    return extractWordContent(inputStream);
                default:
                    throw new IllegalArgumentException("Unsupported file type: " + fileType);
            }
        }
    }

    /**
     * 从输入流中提取PDF内容
     */
    private static String extractPdfContent(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 从输入流中提取Word内容
     */
    private static String extractWordContent(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }

    /**
     * SimHash生成器
     */
    static class SimHashGenerator {
        private BigInteger simHash;
        private int hashBits = HASH_BITS;

        public SimHashGenerator(String content, boolean useSemanticAnalysis) {
            this.simHash = computeSimHash(content, useSemanticAnalysis);
        }

        /**
         * 计算SimHash值
         */
        private BigInteger computeSimHash(String content, boolean useSemanticAnalysis) {
            int[] v = new int[hashBits];

            // 分词并计算词语权重
            Map<String, Double> weightOfTerms = tokenizeAndCalculateWeight(content, useSemanticAnalysis);

            // 对每个词语进行哈希计算
            for (Map.Entry<String, Double> entry : weightOfTerms.entrySet()) {
                String term = entry.getKey();
                Double weight = entry.getValue();

                // 计算词语的hash值
                BigInteger termHash = MurmurHash.hash64(term);
                for (int i = 0; i < hashBits; i++) {
                    BigInteger bitmask = BigInteger.ONE.shiftLeft(i);
                    if (termHash.and(bitmask).signum() != 0) {
                        v[i] += weight;
                    } else {
                        v[i] -= weight;
                    }
                }
            }

            // 生成最终的SimHash值
            BigInteger fingerprint = BigInteger.ZERO;
            for (int i = 0; i < hashBits; i++) {
                if (v[i] >= 0) {
                    fingerprint = fingerprint.add(BigInteger.ONE.shiftLeft(i));
                }
            }

            return fingerprint;
        }

        /**
         * 分词并计算权重
         */
        private Map<String, Double> tokenizeAndCalculateWeight(String content, boolean useSemanticAnalysis) {
            Map<String, Double> weightOfTerms = new HashMap<>();

            // 使用HanLP进行分词
            List<Term> terms = HanLP.segment(content);

            // 计算TF值
            Map<String, Integer> termFrequency = new HashMap<>();
            for (Term term : terms) {
                String word = term.word;
                termFrequency.merge(word, 1, Integer::sum);
            }

            // 计算词语权重
            double maxFreq = Collections.max(termFrequency.values());
            for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
                String term = entry.getKey();
                double tf = 0.5 + 0.5 * entry.getValue() / maxFreq;

                // 考虑词性权重
                double posWeight = calculatePosWeight(term);

                // 考虑语义相似度
                double semanticWeight = useSemanticAnalysis ? calculateSemanticWeight(term, terms) : 1.0;

                weightOfTerms.put(term, tf * posWeight * semanticWeight);
            }

            return weightOfTerms;
        }

        /**
         * 计算词性权重
         */
        private double calculatePosWeight(String term) {
            // 根据词性赋予不同权重，这里简化处理
            if (term.length() <= 1) return 0.1;
            return 1.0;
        }

        /**
         * 计算语义权重
         */
        private double calculateSemanticWeight(String term, List<Term> terms) {
            // 使用滑动窗口计算上下文相关性
            double semanticWeight = 1.0;
            int termIndex = -1;

            // 查找当前词语在分词结果中的位置
            for (int i = 0; i < terms.size(); i++) {
                if (terms.get(i).word.equals(term)) {
                    termIndex = i;
                    break;
                }
            }

            if (termIndex != -1) {
                // 计算与窗口内其他词的语义相关度
                int windowStart = Math.max(0, termIndex - WINDOW_SIZE);
                int windowEnd = Math.min(terms.size(), termIndex + WINDOW_SIZE + 1);

                for (int i = windowStart; i < windowEnd; i++) {
                    if (i != termIndex) {
                        // 这里可以使用Word2Vec或其他词向量计算语义相似度
                        // 简化处理，使用词语距离计算权重
                        double distance = Math.abs(i - termIndex);
                        semanticWeight += 1.0 / (distance + 1);
                    }
                }
            }

            return semanticWeight;
        }

        /**
         * 计算汉明距离
         */
        public int hammingDistance(SimHashGenerator other) {
            BigInteger x = this.simHash.xor(other.simHash);
            int distance = 0;
            while (x.signum() != 0) {
                distance += 1;
                x = x.and(x.subtract(BigInteger.ONE));
            }
            return distance;
        }

        /**
         * 计算相似度
         */
        public double getSimilarity(SimHashGenerator other) {
            int distance = this.hammingDistance(other);
            return 1 - distance * 1.0 / hashBits;
        }
    }

    /**
     * MurmurHash实现
     */
    static class MurmurHash {
        public static BigInteger hash64(String text) {
            byte[] bytes = text.getBytes();
            long h = 0xcafebabe;
            final long mul = 0xc6a4a7935bd1e995L;
            int length = bytes.length;
            int remaining = length;

            int offset = 0;
            while (remaining >= 8) {
                long k = bytes[offset];
                k |= ((long) bytes[offset + 1] << 8);
                k |= ((long) bytes[offset + 2] << 16);
                k |= ((long) bytes[offset + 3] << 24);
                k |= ((long) bytes[offset + 4] << 32);
                k |= ((long) bytes[offset + 5] << 40);
                k |= ((long) bytes[offset + 6] << 48);
                k |= ((long) bytes[offset + 7] << 56);

                k *= mul;
                k ^= k >>> 47;
                k *= mul;

                h ^= k;
                h *= mul;

                offset += 8;
                remaining -= 8;
            }

            if (remaining > 0) {
                long k = 0;
                for (int i = 0; i < remaining; i++) {
                    k |= ((long) bytes[offset + i] << (i * 8));
                }
                h ^= k;
                h *= mul;
            }

            h ^= h >>> 47;
            h *= mul;
            h ^= h >>> 47;

            return BigInteger.valueOf(h);
        }
    }
}
