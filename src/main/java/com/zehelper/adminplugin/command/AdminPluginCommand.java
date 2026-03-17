package com.zehelper.adminplugin.command;

import com.zehelper.adminplugin.AdminPlugin;
import com.zehelper.adminplugin.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminPluginCommand implements CommandExecutor {

    private final AdminPlugin plugin;
    private final List<MessageUtil.HelpEntry> helpEntries = new ArrayList<>();

    public AdminPluginCommand(AdminPlugin plugin) {
        this.plugin = plugin;
        // 도움말 항목 등록
        helpEntries.add(new MessageUtil.HelpEntry("help", "도움말을 표시합니다"));
        // 추가 서브 명령어는 기능 개발 시 등록
    }

    /** 유저 명령어를 처리한다 */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {}
            }
            MessageUtil.sendHelp(sender, "adminplugin", helpEntries, page);
            return true;
        }

        // 알 수 없는 명령어
        MessageUtil.send(sender, "general.unknown-command", Map.of("command", "adminplugin"));
        return true;
    }
}
