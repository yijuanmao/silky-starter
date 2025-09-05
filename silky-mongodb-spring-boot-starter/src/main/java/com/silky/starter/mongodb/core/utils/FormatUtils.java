package com.silky.starter.mongodb.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 转换工具类
 *
 * @author zy
 * @date 2022-11-22 11:34
 **/
public class FormatUtils {

    private static final Pattern REGEX = Pattern.compile("\\$\\{([^}]*)\\}");

    /**
     * 转换json字符串
     *
     * @param json json内容
     * @return
     */
    public static String bson(String json) {
        json = transString(json);
        String blank = "    ";
        // 缩进
        String indent = "";
        StringBuilder sb = new StringBuilder();

        for (char c : json.toCharArray()) {
            switch (c) {
                case '{':
                    indent += blank;
                    sb.append("{\n").append(indent);
                    break;
                case '}':
                    indent = indent.substring(0, indent.length() - blank.length());
                    sb.append("\n").append(indent).append("}");
                    break;
                case '[':
                    indent += blank;
                    sb.append("[\n").append(indent);
                    break;
                case ']':
                    indent = indent.substring(0, indent.length() - blank.length());
                    sb.append("\n").append(indent).append("]");
                    break;
                case ',':
                    sb.append(",\n").append(indent);
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 转换$oid为ObjectId()
     *
     * @param str 转String
     * @return String
     */
    private static String transString(String str) {
        str = str.replace(", ", ",").replace("{\"$oid\":", "${");
        List<String> temp = getContentInfo(str);
        for (String tp : temp) {
            str = str.replace("${" + tp + "}", "ObjectId(" + tp.trim() + ")");
        }
        return str;
    }

    /**
     * 获取表达式中${}中的值
     *
     * @param content 内容
     * @return List<String>
     */
    private static List<String> getContentInfo(String content) {
        Matcher matcher = REGEX.matcher(content);
        List<String> list = new ArrayList<>(4);
        while (matcher.find()) {
            list.add(matcher.group(1));
        }
        return list;
    }
}
