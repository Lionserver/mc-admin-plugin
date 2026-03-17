package com.zehelper.adminplugin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminPluginAdminTabCompleter implements TabCompleter {

    /** 관리자 탭 자동완성을 처리한다 */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 첫 번째 인자: 관리자 서브 명령어 목록
            completions.add("help");
            completions.add("reload");
            // 추가 관리자 서브 명령어는 기능 개발 시 등록
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "reload":
                    // reload는 추가 인자 불필요
                    break;
                default:
                    completions.addAll(getOnlinePlayerNames());
                    break;
            }
        }

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
