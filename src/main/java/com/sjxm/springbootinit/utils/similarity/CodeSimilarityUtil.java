package com.sjxm.springbootinit.utils.similarity;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/2
 * @Description:
 */
@Slf4j
public class CodeSimilarityUtil {

    private static final Set<String> CODE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "java", "cpp", "c", "h", "hpp", "py", "js", "html", "css", "sql"
    ));

    public static double calculateCodeSimilarity(String sourceUrls1, String sourceUrls2) {
        if (StrUtil.isBlank(sourceUrls1) || StrUtil.isBlank(sourceUrls2)) {
            return 0.0;
        }

        try {
            // 解析所有源码文件内容
            Map<String, String> codeFiles1 = extractCodeFiles(sourceUrls1);
            Map<String, String> codeFiles2 = extractCodeFiles(sourceUrls2);

            if (codeFiles1.isEmpty() || codeFiles2.isEmpty()) {
                return 0.0;
            }

            // 按文件类型分组计算相似度
            Map<String, Double> typeSimilarities = new HashMap<>();
            Set<String> allExtensions = new HashSet<>();
            allExtensions.addAll(getFileExtensions(codeFiles1.keySet()));
            allExtensions.addAll(getFileExtensions(codeFiles2.keySet()));

            // 对每种类型的代码文件分别计算相似度
            for (String ext : allExtensions) {
                Map<String, String> typeFiles1 = filterByExtension(codeFiles1, ext);
                Map<String, String> typeFiles2 = filterByExtension(codeFiles2, ext);

                if (!typeFiles1.isEmpty() && !typeFiles2.isEmpty()) {
                    double typeSimilarity = calculateTypeCodeSimilarity(typeFiles1, typeFiles2);
                    typeSimilarities.put(ext, typeSimilarity);
                }
            }

            // 加权平均计算总相似度
            return calculateWeightedSimilarity(typeSimilarities);
        } catch (Exception e) {
            log.error("计算代码相似度失败", e);
            return 0.0;
        }
    }

    /**
     * 提取压缩包中的代码文件
     */
    private static Map<String, String> extractCodeFiles(String sourceUrls) throws IOException {
        Map<String, String> codeFiles = new HashMap<>();
        String[] urls = sourceUrls.split(",");

        for (String url : urls) {
            url = url.trim();
            if (!url.startsWith("http")) {
                continue;
            }

            try (InputStream inputStream = new BufferedInputStream(new URL(url).openStream());
                 ZipArchiveInputStream zipIn = new ZipArchiveInputStream(inputStream)) {

                ZipArchiveEntry entry;
                while ((entry = zipIn.getNextZipEntry()) != null) {
                    if (!entry.isDirectory()) {
                        String extension = FilenameUtils.getExtension(entry.getName()).toLowerCase();
                        if (CODE_EXTENSIONS.contains(extension)) {
                            // 读取代码文件内容
                            String content = readContent(zipIn);
                            codeFiles.put(entry.getName(), preprocessCode(content));
                        }
                    }
                }
            }
        }
        return codeFiles;
    }

    /**
     * 预处理代码内容
     */
    private static String preprocessCode(String code) {
        return code.replaceAll("\\s+", " ") // 统一空白字符
                .replaceAll("//.*?\\n", "\n") // 删除单行注释
                .replaceAll("/\\*.*?\\*/", "") // 删除多行注释
                .replaceAll("\".*?\"", "\"\"") // 统一字符串
                .replaceAll("'.*?'", "''") // 统一字符
                .trim();
    }

    /**
     * 按文件扩展名过滤代码文件
     */
    private static Map<String, String> filterByExtension(Map<String, String> files, String extension) {
        Map<String, String> filtered = new HashMap<>();
        for (Map.Entry<String, String> entry : files.entrySet()) {
            if (FilenameUtils.getExtension(entry.getKey()).equalsIgnoreCase(extension)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    /**
     * 获取所有文件扩展名
     */
    private static Set<String> getFileExtensions(Set<String> filenames) {
        Set<String> extensions = new HashSet<>();
        for (String filename : filenames) {
            extensions.add(FilenameUtils.getExtension(filename).toLowerCase());
        }
        return extensions;
    }

    /**
     * 计算特定类型代码文件的相似度
     */
    private static double calculateTypeCodeSimilarity(Map<String, String> files1, Map<String, String> files2) {
        List<Double> similarities = new ArrayList<>();

        for (Map.Entry<String, String> entry1 : files1.entrySet()) {
            for (Map.Entry<String, String> entry2 : files2.entrySet()) {
                // 使用多种算法计算代码相似度
                double tokenSimilarity = calculateTokenSimilarity(entry1.getValue(), entry2.getValue());
                double structureSimilarity = calculateStructureSimilarity(entry1.getValue(), entry2.getValue());

                // 综合不同算法的结果
                double similarity = tokenSimilarity * 0.7 + structureSimilarity * 0.3;
                similarities.add(similarity);
            }
        }

        // 返回最高相似度
        return similarities.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    /**
     * 计算代码token级别的相似度
     */
    private static double calculateTokenSimilarity(String code1, String code2) {
        // 将代码转换为token序列
        List<String> tokens1 = tokenizeCode(code1);
        List<String> tokens2 = tokenizeCode(code2);

        // 计算最长公共子序列
        int lcs = calculateLCS(tokens1, tokens2);

        // 计算相似度
        return (2.0 * lcs) / (tokens1.size() + tokens2.size());
    }

    /**
     * 计算代码结构相似度
     */
    private static double calculateStructureSimilarity(String code1, String code2) {
        // 提取代码结构特征
        Map<String, Integer> features1 = extractCodeFeatures(code1);
        Map<String, Integer> features2 = extractCodeFeatures(code2);

        // 计算特征向量的余弦相似度
        return calculateCosineSimilarity(features1, features2);
    }

    /**
     * 代码分词
     */
    private static List<String> tokenizeCode(String code) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();

        for (char c : code.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                token.append(c);
            } else {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token = new StringBuilder();
                }
                if (!Character.isWhitespace(c)) {
                    tokens.add(String.valueOf(c));
                }
            }
        }

        if (token.length() > 0) {
            tokens.add(token.toString());
        }

        return tokens;
    }

    /**
     * 提取代码结构特征
     */
    private static Map<String, Integer> extractCodeFeatures(String code) {
        Map<String, Integer> features = new HashMap<>();

        // 统计关键字
        countKeywords(code, features);
        // 统计操作符
        countOperators(code, features);
        // 统计控制结构
        countControlStructures(code, features);

        return features;
    }

    /**
     * 计算最长公共子序列长度
     */
    private static int calculateLCS(List<String> tokens1, List<String> tokens2) {
        int m = tokens1.size();
        int n = tokens2.size();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (tokens1.get(i - 1).equals(tokens2.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp[m][n];
    }

    /**
     * 计算加权平均相似度
     */
    private static double calculateWeightedSimilarity(Map<String, Double> typeSimilarities) {
        Map<String, Double> weights = new HashMap<>();
        weights.put("java", 1.0);
        weights.put("cpp", 1.0);
        weights.put("c", 1.0);
        weights.put("py", 1.0);
        weights.put("js", 0.8);
        weights.put("html", 0.6);
        weights.put("css", 0.6);
        weights.put("sql", 0.7);

        double totalWeight = 0.0;
        double weightedSum = 0.0;

        for (Map.Entry<String, Double> entry : typeSimilarities.entrySet()) {
            String type = entry.getKey();
            double similarity = entry.getValue();
            double weight = weights.getOrDefault(type, 0.5);

            weightedSum += similarity * weight;
            totalWeight += weight;
        }

        return totalWeight == 0 ? 0 : weightedSum / totalWeight;
    }

    /**
     * 读取压缩文件内容
     */
    private static String readContent(ZipArchiveInputStream zipIn) throws IOException {
        StringBuilder content = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = zipIn.read(buffer)) != -1) {
            content.append(new String(buffer, 0, bytesRead, "UTF-8"));
        }
        return content.toString();
    }

    /**
     * 计算余弦相似度
     */
    private static double calculateCosineSimilarity(Map<String, Integer> vector1, Map<String, Integer> vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        // 计算点积和向量模长
        for (String key : vector1.keySet()) {
            int value1 = vector1.get(key);
            Integer value2 = vector2.getOrDefault(key, 0);
            dotProduct += value1 * value2;
            norm1 += value1 * value1;
        }

        for (int value : vector2.values()) {
            norm2 += value * value;
        }

        // 避免除以零
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 统计代码中的关键字
     */
    private static void countKeywords(String code, Map<String, Integer> features) {
        // Java关键字列表
        Set<String> javaKeywords = new HashSet<>(Arrays.asList(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch",
                "char", "class", "const", "continue", "default", "do", "double",
                "else", "enum", "extends", "final", "finally", "float", "for",
                "goto", "if", "implements", "import", "instanceof", "int",
                "interface", "long", "native", "new", "package", "private",
                "protected", "public", "return", "short", "static", "strictfp",
                "super", "switch", "synchronized", "this", "throw", "throws",
                "transient", "try", "void", "volatile", "while"
        ));

        // C++关键字列表
        Set<String> cppKeywords = new HashSet<>(Arrays.asList(
                "auto", "break", "case", "char", "const", "continue", "default",
                "do", "double", "else", "enum", "extern", "float", "for", "goto",
                "if", "int", "long", "register", "return", "short", "signed",
                "sizeof", "static", "struct", "switch", "typedef", "union",
                "unsigned", "void", "volatile", "while", "class", "namespace",
                "try", "catch", "throw", "template", "virtual", "inline", "public",
                "private", "protected"
        ));

        // Python关键字列表
        Set<String> pythonKeywords = new HashSet<>(Arrays.asList(
                "False", "None", "True", "and", "as", "assert", "async", "await",
                "break", "class", "continue", "def", "del", "elif", "else", "except",
                "finally", "for", "from", "global", "if", "import", "in", "is",
                "lambda", "nonlocal", "not", "or", "pass", "raise", "return",
                "try", "while", "with", "yield"
        ));

        // 合并所有关键字
        Set<String> allKeywords = new HashSet<>();
        allKeywords.addAll(javaKeywords);
        allKeywords.addAll(cppKeywords);
        allKeywords.addAll(pythonKeywords);

        // 统计关键字出现次数
        for (String keyword : allKeywords) {
            String pattern = "\\b" + keyword + "\\b";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(code);
            int count = 0;
            while (m.find()) {
                count++;
            }
            if (count > 0) {
                features.put("keyword_" + keyword, count);
            }
        }
    }

    /**
     * 统计代码中的操作符
     */
    private static void countOperators(String code, Map<String, Integer> features) {
        // 算术运算符
        Map<String, Pattern> arithmeticOperators = new HashMap<>();
        arithmeticOperators.put("add", Pattern.compile("[^+]\\+[^+=]"));
        arithmeticOperators.put("subtract", Pattern.compile("[^-]\\-[^-=]"));
        arithmeticOperators.put("multiply", Pattern.compile("\\*[^=]"));
        arithmeticOperators.put("divide", Pattern.compile("/[^=]"));
        arithmeticOperators.put("modulo", Pattern.compile("%[^=]"));

        // 比较运算符
        Map<String, Pattern> comparisonOperators = new HashMap<>();
        comparisonOperators.put("equal", Pattern.compile("=="));
        comparisonOperators.put("not_equal", Pattern.compile("!="));
        comparisonOperators.put("greater", Pattern.compile("[^>]>[^>=]"));
        comparisonOperators.put("less", Pattern.compile("[^<]<[^<=]"));
        comparisonOperators.put("greater_equal", Pattern.compile(">="));
        comparisonOperators.put("less_equal", Pattern.compile("<="));

        // 逻辑运算符
        Map<String, Pattern> logicalOperators = new HashMap<>();
        logicalOperators.put("and", Pattern.compile("&&"));
        logicalOperators.put("or", Pattern.compile("\\|\\|"));
        logicalOperators.put("not", Pattern.compile("![^=]"));

        // 统计各类操作符
        countOperatorsByType(code, features, arithmeticOperators, "arithmetic_");
        countOperatorsByType(code, features, comparisonOperators, "comparison_");
        countOperatorsByType(code, features, logicalOperators, "logical_");
    }

    /**
     * 按类型统计操作符
     */
    private static void countOperatorsByType(String code, Map<String, Integer> features,
                                             Map<String, Pattern> operators, String prefix) {
        for (Map.Entry<String, Pattern> entry : operators.entrySet()) {
            Matcher matcher = entry.getValue().matcher(code);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            if (count > 0) {
                features.put(prefix + entry.getKey(), count);
            }
        }
    }

    /**
     * 统计控制结构
     */
    private static void countControlStructures(String code, Map<String, Integer> features) {
        // 定义控制结构模式
        Map<String, Pattern> controlPatterns = new HashMap<>();

        // 循环结构
        controlPatterns.put("for_loop", Pattern.compile("\\bfor\\s*\\("));
        controlPatterns.put("while_loop", Pattern.compile("\\bwhile\\s*\\("));
        controlPatterns.put("do_while", Pattern.compile("\\bdo\\s*\\{"));

        // 条件结构
        controlPatterns.put("if_statement", Pattern.compile("\\bif\\s*\\("));
        controlPatterns.put("else_statement", Pattern.compile("\\belse\\s*[{]"));
        controlPatterns.put("switch_statement", Pattern.compile("\\bswitch\\s*\\("));
        controlPatterns.put("case_statement", Pattern.compile("\\bcase\\s+.+:"));

        // 异常处理
        controlPatterns.put("try_block", Pattern.compile("\\btry\\s*\\{"));
        controlPatterns.put("catch_block", Pattern.compile("\\bcatch\\s*\\("));
        controlPatterns.put("finally_block", Pattern.compile("\\bfinally\\s*\\{"));

        // 函数定义
        controlPatterns.put("function_definition", Pattern.compile(
                "\\b(public|private|protected|static)?\\s*\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{"
        ));

        // 类定义
        controlPatterns.put("class_definition", Pattern.compile(
                "\\b(public|private|protected)?\\s*class\\s+\\w+\\s*(extends|implements)?\\s*\\w*\\s*\\{"
        ));

        // 统计各种控制结构的出现次数
        for (Map.Entry<String, Pattern> entry : controlPatterns.entrySet()) {
            Matcher matcher = entry.getValue().matcher(code);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            if (count > 0) {
                features.put("structure_" + entry.getKey(), count);
            }
        }

        // 统计代码块嵌套深度
        int maxNestingDepth = calculateNestingDepth(code);
        features.put("max_nesting_depth", maxNestingDepth);
    }

    /**
     * 计算代码嵌套深度
     */
    private static int calculateNestingDepth(String code) {
        int currentDepth = 0;
        int maxDepth = 0;

        for (char c : code.toCharArray()) {
            if (c == '{') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == '}') {
                currentDepth = Math.max(0, currentDepth - 1);
            }
        }

        return maxDepth;
    }
}
