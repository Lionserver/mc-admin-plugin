package com.zehelper.adminplugin.command;

import com.zehelper.adminplugin.AdminPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminPluginAdminTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = List.of(
            "벤", "기간벤", "영구벤", "언벤", "킥",
            "모두정지", "채팅잠금",
            "모두주기", "랜덤주기", "랜덤추첨",
            "채팅금지", "시간채팅금지",
            "help", "reload"
    );

    private static final List<String> AMOUNT_HINTS = List.of("1", "16", "32", "64");
    private static final List<String> COUNT_HINTS = List.of("1", "2", "3", "5");
    private static final List<String> DURATION_HINTS = List.of("1h", "30m", "1d", "1w");

    /** 서브커맨드 중 플레이어 이름이 필요한 것 */
    private static final Set<String> PLAYER_ARG_COMMANDS = Set.of(
            "벤", "기간벤", "영구벤", "킥", "채팅금지", "시간채팅금지"
    );

    /** 서브커맨드 중 추가 인자가 없는 것 */
    private static final Set<String> NO_EXTRA_ARG_COMMANDS = Set.of(
            "모두정지", "채팅잠금", "help", "reload"
    );

    /** 관리자 탭 자동완성을 처리한다 */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("adminplugin.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 첫 번째 인자: 서브커맨드 목록
            completions.addAll(SUB_COMMANDS);
        } else if (args.length == 2) {
            String sub = args[0];
            if (PLAYER_ARG_COMMANDS.contains(sub)) {
                // 플레이어 이름
                completions.addAll(getOnlinePlayerNames());
            } else if ("언벤".equals(sub)) {
                // 벤된 유저 목록
                completions.addAll(AdminPlugin.getInstance().getBanManager().getBannedNames());
            } else if ("모두주기".equals(sub)) {
                completions.addAll(AMOUNT_HINTS);
            } else if ("랜덤주기".equals(sub) || "랜덤추첨".equals(sub)) {
                completions.addAll(COUNT_HINTS);
            }
            // 나머지 (모두정지, 채팅잠금, help, reload): 자동완성 없음
        } else if (args.length == 3) {
            String sub = args[0];
            if ("기간벤".equals(sub) || "시간채팅금지".equals(sub)) {
                completions.addAll(DURATION_HINTS);
            } else if ("랜덤주기".equals(sub)) {
                completions.addAll(AMOUNT_HINTS);
            }
            // 벤, 영구벤, 킥, 채팅금지: 사유 입력 (자동완성 없음)
        }
        // 나머지 args: 자동완성 없음 (사유 입력)

        return filterCompletions(completions, args[args.length - 1]);
    }

    /** 온라인 플레이어 이름 목록을 반환한다 */
    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    /** 입력 중인 문자열로 자동완성 목록을 필터링한다 */
    private List<String> filterCompletions(List<String> completions, String input) {
        if (input.isEmpty()) return completions;
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
