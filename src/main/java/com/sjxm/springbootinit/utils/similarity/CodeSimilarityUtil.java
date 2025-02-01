package com.sjxm.springbootinit.utils.similarity;

import cn.hutool.core.util.StrUtil;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/2
 * @Description:
 */
public class CodeSimilarityUtil {
    private static final Logger logger = LoggerFactory.getLogger(CodeSimilarityUtil.class);

    // 支持的代码文件扩展名
    private static final Set<String> CODE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "java", "cpp", "c", "h", "hpp", "py", "js", "ts",
            "html", "css", "scss", "sass", "sql"
    ));

    /**
     * 计算两个代码压缩包的相似度
     */
    public static double calculateCodeSimilarity(String sourceUrls1, String sourceUrls2) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            logger.info("开始计算代码相似度: sourceUrls1长度={}, sourceUrls2长度={}",
                    sourceUrls1 != null ? sourceUrls1.length() : 0,
                    sourceUrls2 != null ? sourceUrls2.length() : 0);

            if (StrUtil.isBlank(sourceUrls1) || StrUtil.isBlank(sourceUrls2)) {
                logger.warn("代码URL为空, 返回0");
                return 0.0;
            }

            Map<String, String> files1 = extractCodeFiles(sourceUrls1);
            Map<String, String> files2 = extractCodeFiles(sourceUrls2);

            if (files1.isEmpty() || files2.isEmpty()) {
                logger.warn("代码文件为空, 返回0");
                return 0.0;
            }

            double totalSimilarity = 0.0;
            int totalFiles = 0;

            for (String ext : CODE_EXTENSIONS) {
                Map<String, String> typeFiles1 = filterByExtension(files1, ext);
                Map<String, String> typeFiles2 = filterByExtension(files2, ext);


                if (!typeFiles1.isEmpty() && !typeFiles2.isEmpty()) {
                    double similarity = compareFileGroups(typeFiles1, typeFiles2);
                    int weight = Math.max(typeFiles1.size(), typeFiles2.size());
                    totalSimilarity += similarity * weight;
                    totalFiles += weight;

                }
            }

            double finalSimilarity = totalFiles > 0 ? totalSimilarity / totalFiles : 0.0;
            stopWatch.stop();
            logger.info("代码相似度计算完成: similarity={}, 总文件数={}, 耗时={}ms",
                    finalSimilarity, totalFiles,stopWatch.getTotalTimeMillis());
            return finalSimilarity;
        } catch (Exception e) {
            logger.error("计算代码相似度失败", e);
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
            if (!url.trim().startsWith("http")) {
                continue;
            }

            try (InputStream in = new BufferedInputStream(new URL(url.trim()).openStream())) {
                // 读取前几个字节来判断文件类型
                in.mark(8);
                byte[] magicBytes = new byte[8];
                in.read(magicBytes);
                in.reset();

                // 根据文件格式选择处理方法
                String fileExtension = url.substring(url.lastIndexOf(".") + 1).toLowerCase();

                if (fileExtension.equals("zip") || isZipFile(magicBytes)) {
                    extractZipCode(in, codeFiles);
                } else if (fileExtension.equals("rar") || isRarFile(magicBytes)) {
                    extractRarCode(in, codeFiles);
                } else {
                    logger.warn("不支持的文件格式: {}", fileExtension);
                }
            } catch (Exception e) {
                logger.error("处理压缩文件失败: {}", url, e);
            }
        }
        return codeFiles;
    }

    private static void extractZipCode(InputStream in, Map<String, String> codeFiles) {
        try (ZipArchiveInputStream zipIn = new ZipArchiveInputStream(in, "UTF-8", true, true)) {
            ZipArchiveEntry entry;
            while ((entry = zipIn.getNextZipEntry()) != null) {
                if (!entry.isDirectory()) {
                    String ext = FilenameUtils.getExtension(entry.getName()).toLowerCase();
                    if (CODE_EXTENSIONS.contains(ext)) {
                        try {
                            String content = readContent(zipIn);
                            if (!content.isEmpty()) {
                                codeFiles.put(entry.getName(), preprocessCode(content));
                            }
                        } catch (IOException e) {
                            logger.warn("读取ZIP文件内容失败: {}", entry.getName(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解压ZIP文件失败", e);
        }
    }

    private static void extractRarCode(InputStream in, Map<String, String> codeFiles) {
        File tempFile = null;
        try {
            // 创建临时文件
            tempFile = File.createTempFile("temp", ".rar");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // 处理RAR文件
            try (Archive archive = new Archive(tempFile)) {
                FileHeader fileHeader;
                while ((fileHeader = archive.nextFileHeader()) != null) {
                    if (!fileHeader.isDirectory()) {
                        String name = fileHeader.getFileNameString();
                        String ext = FilenameUtils.getExtension(name).toLowerCase();

                        if (CODE_EXTENSIONS.contains(ext)) {
                            try {
                                String content = extractRarContent(archive, fileHeader);
                                if (!content.isEmpty()) {
                                    codeFiles.put(name, preprocessCode(content));
                                }
                            } catch (Exception e) {
                                logger.warn("读取RAR文件内容失败: {}", name, e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("处理RAR文件失败", e);
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private static String extractRarContent(Archive archive, FileHeader fileHeader) throws IOException, RarException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        archive.extractFile(fileHeader, baos);
        return new String(baos.toByteArray(), "UTF-8");
    }

    private static boolean isZipFile(byte[] bytes) {
        return bytes.length >= 4 &&
                bytes[0] == 0x50 && bytes[1] == 0x4B &&
                bytes[2] == 0x03 && bytes[3] == 0x04;
    }

    private static boolean isRarFile(byte[] bytes) {
        return bytes.length >= 7 &&
                bytes[0] == 0x52 && bytes[1] == 0x61 &&
                bytes[2] == 0x72 && bytes[3] == 0x21;
    }

    private static String readContent(ZipArchiveInputStream zipIn) throws IOException {
        StringBuilder content = new StringBuilder();
        byte[] buffer = new byte[4096];
        int bytesRead;
        int totalBytes = 0;
        final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB限制

        while ((bytesRead = zipIn.read(buffer)) != -1) {
            totalBytes += bytesRead;
            if (totalBytes > MAX_FILE_SIZE) {
                logger.warn("文件过大，已截断");
                break;
            }
            content.append(new String(buffer, 0, bytesRead, "UTF-8"));
        }

        return content.toString();
    }

    /**
     * 预处理代码内容
     */
    private static String preprocessCode(String code) {
        // 移除注释
        code = code.replaceAll("//.*|/\\*[\\s\\S]*?\\*/", "")
                .replaceAll("<!--[\\s\\S]*?-->", ""); // HTML注释

        // 移除字符串常量
        code = code.replaceAll("\"[^\"]*\"", "\"\"")
                .replaceAll("'[^']*'", "''");

        // 规范化空白字符
        code = code.replaceAll("\\s+", " ")
                .replaceAll("\\b\\s+\\b", " ")
                .trim();

        return code;
    }

    /**
     * 比较文件组的相似度
     */
    private static double compareFileGroups(Map<String, String> files1, Map<String, String> files2) {
        List<Double> similarities = new ArrayList<>();

        for (String content1 : files1.values()) {
            Set<String> features1 = extractFeatures(content1);

            for (String content2 : files2.values()) {
                Set<String> features2 = extractFeatures(content2);
                double similarity = calculateJaccardSimilarity(features1, features2);
                similarities.add(similarity);
            }
        }

        // 返回最高相似度
        return similarities.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
    }

    /**
     * 提取代码特征
     */
    private static Set<String> extractFeatures(String code) {
        Set<String> features = new HashSet<>();

        // 分割成token
        String[] tokens = code.split("[\\s\\{\\}\\(\\)\\[\\]\\;\\,\\.\\+\\-\\*\\/\\=\\<\\>\\!\\&\\|]+");

        // 添加单个token作为特征
        features.addAll(Arrays.asList(tokens));

        // 添加相邻token对作为特征
        for (int i = 0; i < tokens.length - 1; i++) {
            features.add(tokens[i] + " " + tokens[i + 1]);
        }

        // 添加简单的结构特征
        features.add("DEPTH:" + countBraceDepth(code));
        features.add("LENGTH:" + code.length() / 100); // 长度特征分段

        // 添加关键字特征
        addKeywordFeatures(code, features);

        return features;
    }

    /**
     * 计算代码的括号嵌套深度
     */
    private static int countBraceDepth(String code) {
        int depth = 0;
        int maxDepth = 0;

        for (char c : code.toCharArray()) {
            if (c == '{') {
                depth++;
                maxDepth = Math.max(maxDepth, depth);
            } else if (c == '}') {
                depth = Math.max(0, depth - 1);
            }
        }

        return maxDepth;
    }

    /**
     * 添加关键字特征
     */
    private static void addKeywordFeatures(String code, Set<String> features) {
        // Java关键字
        String[] javaKeywords = {
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
                "class", "const", "continue", "default", "do", "double", "else", "enum",
                "extends", "final", "finally", "float", "for", "goto", "if", "implements",
                "import", "instanceof", "int", "interface", "long", "native", "new", "package",
                "private", "protected", "public", "return", "short", "static", "strictfp",
                "super", "switch", "synchronized", "this", "throw", "throws", "transient",
                "try", "void", "volatile", "while"
        };

        // C/C++关键字
        String[] cppKeywords = {
                "auto", "break", "case", "char", "const", "continue", "default", "do",
                "double", "else", "enum", "extern", "float", "for", "goto", "if", "int",
                "long", "register", "return", "short", "signed", "sizeof", "static",
                "struct", "switch", "typedef", "union", "unsigned", "void", "volatile",
                "while", "asm", "bool", "catch", "class", "const_cast", "delete",
                "dynamic_cast", "explicit", "export", "false", "friend", "inline",
                "mutable", "namespace", "new", "operator", "private", "protected",
                "public", "reinterpret_cast", "static_cast", "template", "this",
                "throw", "true", "try", "typeid", "typename", "using", "virtual",
                "wchar_t", "nullptr"
        };

        // Python关键字
        String[] pythonKeywords = {
                "False", "None", "True", "and", "as", "assert", "async", "await",
                "break", "class", "continue", "def", "del", "elif", "else", "except",
                "finally", "for", "from", "global", "if", "import", "in", "is",
                "lambda", "nonlocal", "not", "or", "pass", "raise", "return",
                "try", "while", "with", "yield"
        };

        // JavaScript/TypeScript关键字
        String[] jsKeywords = {
                "break", "case", "catch", "class", "const", "continue", "debugger",
                "default", "delete", "do", "else", "enum", "export", "extends", "false",
                "finally", "for", "function", "if", "import", "in", "instanceof",
                "new", "null", "return", "super", "switch", "this", "throw", "true",
                "try", "typeof", "var", "void", "while", "with", "let", "static",
                "yield", "async", "await", "implements", "interface", "package",
                "private", "protected", "public", "as", "any", "boolean", "constructor",
                "declare", "get", "module", "require", "number", "set", "string",
                "symbol", "type", "undefined", "unique", "unknown", "from", "of"
        };

        // HTML标签和属性
        String[] htmlKeywords = {
                "html", "head", "body", "div", "span", "p", "a", "img", "ul", "li",
                "table", "tr", "td", "th", "form", "input", "button", "select",
                "option", "textarea", "script", "style", "link", "meta", "title",
                "class", "id", "href", "src", "alt", "type", "value", "name",
                "placeholder", "required", "disabled", "checked", "selected",
                "readonly", "multiple", "action", "method", "target", "rel"
        };

        // CSS关键字
        String[] cssKeywords = {
                "align", "background", "border", "bottom", "box", "clear", "color",
                "content", "cursor", "display", "flex", "float", "font", "grid",
                "height", "justify", "left", "line", "margin", "max", "min",
                "opacity", "order", "outline", "overflow", "padding", "position",
                "right", "text", "top", "transform", "transition", "visibility",
                "width", "z-index", "important", "hover", "active", "focus",
                "before", "after", "root", "nth-child", "first-child", "last-child"
        };

        // SQL关键字
        String[] sqlKeywords = {
                "select", "from", "where", "insert", "update", "delete", "create",
                "alter", "drop", "table", "index", "view", "into", "values", "set",
                "join", "left", "right", "inner", "outer", "on", "group", "by",
                "having", "order", "asc", "desc", "distinct", "between", "like",
                "in", "is", "null", "not", "and", "or", "primary", "key", "foreign",
                "references", "constraint", "default", "auto_increment", "unique",
                "database", "use", "grant", "revoke", "commit", "rollback", "union"
        };

        // 根据文件扩展名选择对应的关键字集合
        Map<String, String[]> languageKeywords = new HashMap<>();
        languageKeywords.put("java", javaKeywords);
        languageKeywords.put("cpp", cppKeywords);
        languageKeywords.put("c", cppKeywords);
        languageKeywords.put("hpp", cppKeywords);
        languageKeywords.put("h", cppKeywords);
        languageKeywords.put("py", pythonKeywords);
        languageKeywords.put("js", jsKeywords);
        languageKeywords.put("ts", jsKeywords);
        languageKeywords.put("html", htmlKeywords);
        languageKeywords.put("css", cssKeywords);
        languageKeywords.put("scss", cssKeywords);
        languageKeywords.put("sass", cssKeywords);
        languageKeywords.put("sql", sqlKeywords);

        // 通用的编程语言操作符和分隔符
        String[] commonOperators = {
                "+", "-", "*", "/", "%", "=", "==", "!=", "<", ">", "<=", ">=",
                "&&", "||", "!", "&", "|", "^", "~", "<<", ">>", "++", "--",
                "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>="
        };

        // 获取文件扩展名
        String fileExtension = getFileExtension(code).toLowerCase();

        // 添加对应语言的关键字特征
        if (languageKeywords.containsKey(fileExtension)) {
            String[] keywords = languageKeywords.get(fileExtension);
            for (String keyword : keywords) {
                Pattern pattern = Pattern.compile("\\b" + keyword + "\\b");
                Matcher matcher = pattern.matcher(code);
                if (matcher.find()) {
                    features.add("KW_" + fileExtension + ":" + keyword);
                }
            }
        }

        // 添加通用操作符特征
        for (String operator : commonOperators) {
            if (code.contains(operator)) {
                features.add("OP:" + operator);
            }
        }
    }

    /**
     * 从代码内容推测文件扩展名
     */
    private static String getFileExtension(String code) {
        // 基于代码特征判断语言类型
        if (code.contains("public class") || code.contains("private class")) {
            return "java";
        } else if (code.contains("<!DOCTYPE html") || code.contains("<html")) {
            return "html";
        } else if (code.contains("#include") || code.contains("std::")) {
            return "cpp";
        } else if (code.contains("def ") || code.contains("import ") && code.contains(":")) {
            return "py";
        } else if (code.contains("function") || code.contains("const ") || code.contains("let ")) {
            return "js";
        } else if (code.contains("interface ") || code.contains("type ") || code.contains("namespace ")) {
            return "ts";
        } else if (code.contains("{") && code.contains("}") && (code.contains(";") || code.contains("px"))) {
            return "css";
        } else if (code.contains("SELECT ") || code.contains("CREATE TABLE")) {
            return "sql";
        }
        return "unknown";
    }

    /**
     * 计算Jaccard相似度
     */
    private static double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() && set2.isEmpty()) {
            return 1.0;
        }

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        return (double) intersection.size() / union.size();
    }

    /**
     * 按文件扩展名过滤代码文件
     */
    private static Map<String, String> filterByExtension(Map<String, String> files, String extension) {
        return files.entrySet().stream()
                .filter(e -> FilenameUtils.getExtension(e.getKey()).equalsIgnoreCase(extension))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

//    /**
//     * 读取压缩文件内容
//     */
//    private static String readContent(ZipArchiveInputStream zipIn) throws IOException {
//        StringBuilder content = new StringBuilder();
//        byte[] buffer = new byte[4096];
//        int bytesRead;
//
//        while ((bytesRead = zipIn.read(buffer)) != -1) {
//            content.append(new String(buffer, 0, bytesRead, "UTF-8"));
//        }
//
//        return content.toString();
//    }
}
