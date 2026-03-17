package com.zehelper.adminplugin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminPluginTabCompleter implements TabCompleter {

    /** 탭 자동완성을 처리한다 */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 첫 번째 인자: 서브 명령어 목록
            completions.add("help");
            // 추가 서브 명령어는 기능 개발 시 등록
        } else if (args.length == 2) {
            // 두 번째 인자: 각 서브 명령어에 맞는 자동완성
            switch (args[0].toLowerCase()) {
                case "help":
                    // help의 두 번째 인자: 페이지 번호 (자동완성 불필요)
                    break;
                default:
                    // 플레이어 이름이 필요한 서브 명령어인 경우 온라인 플레이어 목록
                    completions.addAll(getOnlinePlayerNames());
                    break;
            }
        }

        // 입력 중인 문자열로 필터링
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
