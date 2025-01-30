package com.sjxm.springbootinit.utils;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/21
 * @Description:
 */
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

public class RichTextXssFilterUtil {

    /**
     * 自定义白名单配置
     */
    private static final Safelist RICH_TEXT_SAFELIST = Safelist.relaxed()
            // 增加额外允许的标签
            .addTags("span", "div", "pre", "code", "hr", "font")
            // 允许的属性
            .addAttributes(":all", "style", "class", "id", "name")
            .addAttributes("font", "color", "face", "size")
            .addAttributes("img", "src", "alt", "title", "width", "height", "data-mce-src")
            // 允许的协议
            .addProtocols("img", "src", "http", "https", "data")
            // 允许的CSS属性
            .addAttributes(":all", "style")
            // 表情包可能使用的属性
            .addAttributes("img", "data-emoticon");

    /**
     * 配置允许的CSS属性
     */
    private static final String[] VALID_CSS_PROPERTIES = {
            "background", "background-color", "border", "border-radius",
            "color", "font-family", "font-size", "font-weight", "height",
            "line-height", "margin", "padding", "text-align", "text-decoration",
            "width", "display", "float", "vertical-align"
    };

    /**
     * 清理富文本内容
     */
    public static String clean(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        // 使用自定义配置清理HTML
        Document.OutputSettings outputSettings = new Document.OutputSettings()
                .prettyPrint(false)  // 保持原有格式
                .charset("UTF-8");   // 设置字符集

        String clean = Jsoup.clean(content, "", RICH_TEXT_SAFELIST, outputSettings);

        // 解析清理后的内容
        Document doc = Jsoup.parse(clean);

        // 处理所有元素的style属性
        doc.getAllElements().forEach(RichTextXssFilterUtil::cleanStyles);

        return doc.body().html();
    }

    /**
     * 清理元素的style属性
     */
    private static void cleanStyles(Element element) {
        String style = element.attr("style");
        if (!style.isEmpty()) {
            StringBuilder cleanStyle = new StringBuilder();

            // 分割并处理每个CSS属性
            String[] styles = style.split(";");
            for (String s : styles) {
                String[] propertyValue = s.split(":");
                if (propertyValue.length == 2) {
                    String property = propertyValue[0].trim().toLowerCase();
                    String value = propertyValue[1].trim();

                    // 检查CSS属性是否在白名单中
                    if (isValidCssProperty(property)) {
                        // 检查CSS值是否安全
                        value = cleanCssValue(value);
                        if (value != null) {
                            if (cleanStyle.length() > 0) {
                                cleanStyle.append("; ");
                            }
                            cleanStyle.append(property).append(": ").append(value);
                        }
                    }
                }
            }

            if (cleanStyle.length() > 0) {
                element.attr("style", cleanStyle.toString());
            } else {
                element.removeAttr("style");
            }
        }
    }

    /**
     * 检查CSS属性是否在白名单中
     */
    private static boolean isValidCssProperty(String property) {
        for (String validProperty : VALID_CSS_PROPERTIES) {
            if (validProperty.equals(property)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清理CSS值
     */
    private static String cleanCssValue(String value) {
        // 移除可能包含的JavaScript代码
        if (value.toLowerCase().contains("javascript:") ||
                value.toLowerCase().contains("expression") ||
                value.toLowerCase().contains("vbscript:")) {
            return null;
        }

        // 清理URL
        if (value.toLowerCase().contains("url(")) {
            // 只允许http和https协议的URL
            if (!value.matches("(?i)url\\s*\\(\\s*['\"]?\\s*https?://.*?['\"]?\\s*\\)")) {
                return null;
            }
        }

        return value;
    }

    /**
     * 额外的URL清理方法（用于href和src属性）
     */
    private static String cleanUrl(String url) {
        if (url == null) {
            return null;
        }

        url = url.trim().toLowerCase();

        // 只允许http、https和data协议（用于图片）
        if (url.startsWith("http://") ||
                url.startsWith("https://") ||
                url.startsWith("data:image/")) {
            return url;
        }

        return null;
    }
}
