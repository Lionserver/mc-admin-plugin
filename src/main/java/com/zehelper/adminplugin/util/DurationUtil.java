package com.zehelper.adminplugin.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationUtil {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

    /** 시간 문자열을 밀리초로 변환한다 (예: 1h30m → 5400000) */
    public static long parse(String input) {
        if (input == null || input.isEmpty()) return -1;
        long total = 0;
        Matcher matcher = DURATION_PATTERN.matcher(input.toLowerCase());
        boolean found = false;
        while (matcher.find()) {
            found = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "s" -> total += value * 1000L;
                case "m" -> total += value * 60L * 1000L;
                case "h" -> total += value * 3600L * 1000L;
                case "d" -> total += value * 86400L * 1000L;
                case "w" -> total += value * 604800L * 1000L;
            }
        }
        return found ? total : -1;
    }

    /** 밀리초를 한국어 시간 문자열로 변환한다 */
    public static String format(long millis) {
        if (millis <= 0) return "0초";
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        days %= 7;
        hours %= 24;
        minutes %= 60;
        seconds %= 60;
        StringBuilder sb = new StringBuilder();
        if (weeks > 0) sb.append(weeks).append("주 ");
        if (days > 0) sb.append(days).append("일 ");
        if (hours > 0) sb.append(hours).append("시간 ");
        if (minutes > 0) sb.append(minutes).append("분 ");
        if (seconds > 0) sb.append(seconds).append("초");
        return sb.toString().trim();
    }

    /** 남은 시간을 한국어 문자열로 변환한다 */
    public static String formatRemaining(long expiresAtMs) {
        long remaining = expiresAtMs - System.currentTimeMillis();
        if (remaining <= 0) return "만료됨";
        return format(remaining);
    }

    /** 시간 형식이 유효한지 확인한다 */
    public static boolean isValid(String input) {
        return parse(input) > 0;
    }
}
