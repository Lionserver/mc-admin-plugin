package com.zehelper.adminplugin.util;

import com.zehelper.adminplugin.AdminPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class MessageUtil {

    private static final int HELP_PER_PAGE = 5;

    /** 대상에게 접두사 포함 메시지를 전송한다 */
    public static void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(AdminPlugin.getInstance().getMessageManager().getMessage(path, placeholders));
    }

    /** 대상에게 접두사 없이 메시지를 전송한다 */
    public static void sendNoPrefix(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(AdminPlugin.getInstance().getMessageManager().getMessageNoPrefix(path, placeholders));
    }

    /**
     * 페이지형 도움말을 전송한다
     * - 명령어 클릭 시 자동 채팅 기입
     * - 하단에 [이전] [페이지] [다음] 네비게이션
     * - 구분선/테두리 사용하지 않음
     */
    public static void sendHelp(CommandSender sender, String command, List<HelpEntry> entries, int page) {
        var mm = AdminPlugin.getInstance().getMessageManager();
        int maxPage = (int) Math.ceil((double) entries.size() / HELP_PER_PAGE);
        if (maxPage == 0) maxPage = 1;
        if (page < 1) page = 1;
        if (page > maxPage) page = maxPage;

        // 헤더
        Map<String, String> pageMap = Map.of("page", String.valueOf(page), "maxpage", String.valueOf(maxPage), "command", command);
        sender.sendMessage(mm.getMessageNoPrefix("help.header", pageMap));

        // 명령어 목록
        int start = (page - 1) * HELP_PER_PAGE;
        int end = Math.min(start + HELP_PER_PAGE, entries.size());
        for (int i = start; i < end; i++) {
            HelpEntry entry = entries.get(i);
            Map<String, String> map = Map.of("command", command, "sub", entry.sub(), "description", entry.description());
            Component line = mm.getMessageNoPrefix("help.format", map);
            // 클릭 시 명령어 자동 기입
            line = line.clickEvent(ClickEvent.suggestCommand("/" + command + " " + entry.sub()));
            sender.sendMessage(line);
        }

        // 네비게이션
        Component prev = mm.getMessageNoPrefix("help.footer-prev", pageMap)
                .clickEvent(ClickEvent.runCommand("/" + command + " help " + (page - 1)));
        Component pageComp = mm.getMessageNoPrefix("help.footer-page", pageMap);
        Component next = mm.getMessageNoPrefix("help.footer-next", pageMap)
                .clickEvent(ClickEvent.runCommand("/" + command + " help " + (page + 1)));

        sender.sendMessage(Component.text("              ").append(prev).append(Component.text("     ")).append(pageComp).append(Component.text("      ")).append(next));
    }

    /** 도움말 항목 레코드 */
    public record HelpEntry(String sub, String description) {}
}
