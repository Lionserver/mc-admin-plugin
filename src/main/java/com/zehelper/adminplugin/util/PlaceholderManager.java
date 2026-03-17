package com.zehelper.adminplugin.util;

import java.util.Map;

public class PlaceholderManager {

    /** 문자열에 플레이스홀더를 적용한다 */
    public static String apply(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null) return text;
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return result;
    }
}
