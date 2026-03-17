package com.zehelper.adminplugin.command;

import com.zehelper.adminplugin.AdminPlugin;
import com.zehelper.adminplugin.database.BanManager;
import com.zehelper.adminplugin.database.MuteManager;
import com.zehelper.adminplugin.util.DurationUtil;
import com.zehelper.adminplugin.util.MessageUtil;
import com.zehelper.adminplugin.util.PlaceholderManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminPluginAdminCommand implements CommandExecutor {

    private final AdminPlugin plugin;
    private final List<MessageUtil.HelpEntry> helpEntries = new ArrayList<>();

    public AdminPluginAdminCommand(AdminPlugin plugin) {
        this.plugin = plugin;
        // 도움말 항목 등록
        helpEntries.add(new MessageUtil.HelpEntry("벤 <유저> <사유>", "플레이어를 영구 차단합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("기간벤 <유저> <시간> <사유>", "플레이어를 기간제 차단합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("영구벤 <유저> <사유>", "플레이어를 영구 차단합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("언벤 <유저>", "플레이어의 차단을 해제합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("킥 <유저> <사유>", "플레이어를 추방합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("모두정지", "전체 정지를 토글합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("채팅잠금", "채팅 잠금을 토글합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("모두주기 <수량>", "손에 든 아이템을 전체 지급합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("랜덤주기 <인원> <수량>", "손에 든 아이템을 랜덤 지급합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("랜덤추첨 <인원>", "랜덤 추첨을 합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("채팅금지 <유저> <사유>", "플레이어의 채팅을 금지합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("시간채팅금지 <유저> <시간> <사유>", "플레이어의 채팅을 기간제 금지합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("help", "관리자 도움말을 표시합니다"));
        helpEntries.add(new MessageUtil.HelpEntry("reload", "설정 파일을 다시 불러옵니다"));
    }

    /** 관리자 명령어를 처리한다 */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 권한 확인
        if (!sender.hasPermission("adminplugin.admin")) {
            MessageUtil.send(sender, "general.no-permission", Map.of());
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {}
            }
            MessageUtil.sendHelp(sender, "관리자", helpEntries, page);
            return true;
        }

        switch (args[0]) {
            case "벤" -> handleBan(sender, args);
            case "기간벤" -> handleTempBan(sender, args);
            case "영구벤" -> handlePermaBan(sender, args);
            case "언벤" -> handleUnban(sender, args);
            case "킥" -> handleKick(sender, args);
            case "모두정지" -> handleFreezeAll(sender);
            case "채팅잠금" -> handleChatLock(sender);
            case "모두주기" -> handleGiveAll(sender, args);
            case "랜덤주기" -> handleGiveRandom(sender, args);
            case "랜덤추첨" -> handleRandomDraw(sender, args);
            case "채팅금지" -> handleMute(sender, args);
            case "시간채팅금지" -> handleTempMute(sender, args);
            case "reload" -> handleReload(sender);
            default -> MessageUtil.send(sender, "general.unknown-command", Map.of("command", "관리자"));
        }
        return true;
    }

    /** 영구 벤을 처리한다 */
    private void handleBan(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "admin.usage.ban", Map.of());
            return;
        }

        String targetName = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String adminName = getAdminName(sender);

        PlayerLookupResult result = lookupPlayer(targetName);
        if (result == null) {
            MessageUtil.send(sender, "admin.ban.player-not-found", Map.of("target", targetName));
            return;
        }

        BanManager banManager = plugin.getBanManager();
        if (banManager.isBanned(result.uuid)) {
            MessageUtil.send(sender, "admin.ban.already-banned", Map.of("target", result.name));
            return;
        }

        banManager.ban(result.uuid, result.name, reason, adminName, null);

        // 온라인이면 킥
        if (result.onlinePlayer != null) {
            Component kickMsg = createKickMessage("admin.ban.screen", Map.of(
                    "reason", reason, "admin", adminName
            ));
            result.onlinePlayer.kick(kickMsg);
        }

        MessageUtil.send(sender, "admin.ban.success", Map.of("target", result.name, "reason", reason));

        if (plugin.getConfigManager().isBanBroadcast()) {
            Component broadcastMsg = plugin.getMessageManager().getMessage("admin.ban.broadcast",
                    Map.of("target", result.name, "reason", reason));
            Bukkit.broadcast(broadcastMsg);
        }
    }

    /** 기간제 벤을 처리한다 */
    private void handleTempBan(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtil.send(sender, "admin.usage.tempban", Map.of());
            return;
        }

        String targetName = args[1];
        String durationStr = args[2];
        String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        String adminName = getAdminName(sender);

        long durationMs = DurationUtil.parse(durationStr);
        if (durationMs <= 0) {
            MessageUtil.send(sender, "admin.tempban.invalid-duration", Map.of());
            return;
        }

        PlayerLookupResult result = lookupPlayer(targetName);
        if (result == null) {
            MessageUtil.send(sender, "admin.ban.player-not-found", Map.of("target", targetName));
            return;
        }

        BanManager banManager = plugin.getBanManager();
        if (banManager.isBanned(result.uuid)) {
            MessageUtil.send(sender, "admin.ban.already-banned", Map.of("target", result.name));
            return;
        }

        long expiresAt = System.currentTimeMillis() + durationMs;
        banManager.ban(result.uuid, result.name, reason, adminName, expiresAt);

        String durationFormatted = DurationUtil.format(durationMs);
        String remaining = DurationUtil.formatRemaining(expiresAt);

        // 온라인이면 킥
        if (result.onlinePlayer != null) {
            Component kickMsg = createKickMessage("admin.tempban.screen", Map.of(
                    "reason", reason, "admin", adminName, "remaining", remaining
            ));
            result.onlinePlayer.kick(kickMsg);
        }

        MessageUtil.send(sender, "admin.tempban.success", Map.of(
                "target", result.name, "duration", durationFormatted, "reason", reason
        ));

        if (plugin.getConfigManager().isBanBroadcast()) {
            Component broadcastMsg = plugin.getMessageManager().getMessage("admin.tempban.broadcast",
                    Map.of("target", result.name, "duration", durationFormatted));
            Bukkit.broadcast(broadcastMsg);
        }
    }

    /** 영구 벤을 처리한다 (벤과 동일, 메시지만 다름) */
    private void handlePermaBan(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "admin.usage.permaban", Map.of());
            return;
        }

        String targetName = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String adminName = getAdminName(sender);

        PlayerLookupResult result = lookupPlayer(targetName);
        if (result == null) {
            MessageUtil.send(sender, "admin.ban.player-not-found", Map.of("target", targetName));
            return;
        }

        BanManager banManager = plugin.getBanManager();
        if (banManager.isBanned(result.uuid)) {
            MessageUtil.send(sender, "admin.ban.already-banned", Map.of("target", result.name));
            return;
        }

        banManager.ban(result.uuid, result.name, reason, adminName, null);

        // 온라인이면 킥
        if (result.onlinePlayer != null) {
            Component kickMsg = createKickMessage("admin.permaban.screen", Map.of(
                    "reason", reason, "admin", adminName
            ));
            result.onlinePlayer.kick(kickMsg);
        }

        MessageUtil.send(sender, "admin.permaban.success", Map.of("target", result.name, "reason", reason));

        if (plugin.getConfigManager().isBanBroadcast()) {
            Component broadcastMsg = plugin.getMessageManager().getMessage("admin.permaban.broadcast",
                    Map.of("target", result.name));
            Bukkit.broadcast(broadcastMsg);
        }
    }

    /** 벤 해제를 처리한다 */
    private void handleUnban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(sender, "admin.usage.unban", Map.of());
            return;
        }

        String targetName = args[1];
        String adminName = getAdminName(sender);

        BanManager banManager = plugin.getBanManager();
        boolean success = banManager.unban(targetName, adminName);

        if (!success) {
            MessageUtil.send(sender, "admin.unban.not-found", Map.of("target", targetName));
            return;
        }

        MessageUtil.send(sender, "admin.unban.success", Map.of("target", targetName));

        if (plugin.getConfigManager().isBanBroadcast()) {
            Component broadcastMsg = plugin.getMessageManager().getMessage("admin.unban.broadcast",
                    Map.of("target", targetName));
            Bukkit.broadcast(broadcastMsg);
        }
    }

    /** 킥을 처리한다 */
    private void handleKick(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "admin.usage.kick", Map.of());
            return;
        }

        String targetName = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String adminName = getAdminName(sender);

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            MessageUtil.send(sender, "admin.kick.player-not-found", Map.of("target", targetName));
            return;
        }

        Component kickMsg = createKickMessage("admin.kick.screen", Map.of(
                "reason", reason, "admin", adminName
        ));
        target.kick(kickMsg);

        MessageUtil.send(sender, "admin.kick.success", Map.of("target", target.getName(), "reason", reason));

        if (plugin.getConfigManager().isKickBroadcast()) {
            Component broadcastMsg = plugin.getMessageManager().getMessage("admin.kick.broadcast",
                    Map.of("target", target.getName()));
            Bukkit.broadcast(broadcastMsg);
        }
    }

    /** 모두정지 토글을 처리한다 */
    private void handleFreezeAll(CommandSender sender) {
        boolean frozen = plugin.toggleFreezeAll();

        if (frozen) {
            MessageUtil.send(sender, "admin.freeze.enabled", Map.of());
        } else {
            MessageUtil.send(sender, "admin.freeze.disabled", Map.of());
        }
    }

    /** 채팅잠금 토글을 처리한다 */
    private void handleChatLock(CommandSender sender) {
        boolean locked = plugin.toggleChatLock();

        if (locked) {
            MessageUtil.send(sender, "admin.chatlock.enabled", Map.of());
        } else {
            MessageUtil.send(sender, "admin.chatlock.disabled", Map.of());
        }
    }

    /** 모두주기를 처리한다 */
    private void handleGiveAll(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "general.player-only", Map.of());
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(sender, "admin.usage.giveall", Map.of());
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "admin.giveall.invalid-amount", Map.of());
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            MessageUtil.send(sender, "admin.giveall.must-hold-item", Map.of());
            return;
        }

        String itemName = getItemName(heldItem);

        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack gift = heldItem.clone();
            gift.setAmount(amount);
            p.getInventory().addItem(gift);
        }

        MessageUtil.send(sender, "admin.giveall.success", Map.of(
                "itemname", itemName, "amount", String.valueOf(amount)
        ));
    }

    /** 랜덤주기를 처리한다 */
    private void handleGiveRandom(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "general.player-only", Map.of());
            return;
        }

        if (args.length < 3) {
            MessageUtil.send(sender, "admin.usage.giverandom", Map.of());
            return;
        }

        int count;
        int amount;
        try {
            count = Integer.parseInt(args[1]);
            amount = Integer.parseInt(args[2]);
            if (count <= 0 || amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "admin.usage.giverandom", Map.of());
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            MessageUtil.send(sender, "admin.giveall.must-hold-item", Map.of());
            return;
        }

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.size() < count) {
            MessageUtil.send(sender, "admin.giverandom.not-enough-players",
                    Map.of("online", String.valueOf(onlinePlayers.size())));
            return;
        }

        Collections.shuffle(onlinePlayers);
        List<Player> winners = onlinePlayers.subList(0, count);

        String itemName = getItemName(heldItem);

        for (Player p : winners) {
            ItemStack gift = heldItem.clone();
            gift.setAmount(amount);
            p.getInventory().addItem(gift);
        }

        String winnerNames = winners.stream()
                .map(Player::getName)
                .collect(Collectors.joining(", "));

        MessageUtil.send(sender, "admin.giverandom.success", Map.of(
                "itemname", itemName, "amount", String.valueOf(amount), "count", String.valueOf(count)
        ));
        MessageUtil.send(sender, "admin.giverandom.winners", Map.of("players", winnerNames));
    }

    /** 랜덤추첨을 처리한다 */
    private void handleRandomDraw(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(sender, "admin.usage.draw", Map.of());
            return;
        }

        int count;
        try {
            count = Integer.parseInt(args[1]);
            if (count <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "admin.usage.draw", Map.of());
            return;
        }

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.size() < count) {
            MessageUtil.send(sender, "admin.draw.not-enough-players",
                    Map.of("online", String.valueOf(onlinePlayers.size())));
            return;
        }

        Collections.shuffle(onlinePlayers);
        List<Player> winners = onlinePlayers.subList(0, count);

        MessageUtil.sendNoPrefix(sender, "admin.draw.result", Map.of());
        for (int i = 0; i < winners.size(); i++) {
            MessageUtil.sendNoPrefix(sender, "admin.draw.entry", Map.of(
                    "index", String.valueOf(i + 1), "playername", winners.get(i).getName()
            ));
        }
    }

    /** 영구 뮤트를 처리한다 */
    private void handleMute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "admin.usage.mute", Map.of());
            return;
        }

        String targetName = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String adminName = getAdminName(sender);

        PlayerLookupResult result = lookupPlayer(targetName);
        if (result == null) {
            MessageUtil.send(sender, "admin.ban.player-not-found", Map.of("target", targetName));
            return;
        }

        MuteManager muteManager = plugin.getMuteManager();
        if (muteManager.isMuted(result.uuid)) {
            MessageUtil.send(sender, "admin.mute.already-muted", Map.of("target", result.name));
            return;
        }

        muteManager.mute(result.uuid, result.name, reason, adminName, null);

        MessageUtil.send(sender, "admin.mute.success", Map.of("target", result.name, "reason", reason));

        if (plugin.getConfigManager().isMuteBroadcast()) {
            Component broadcastMsg = plugin.getMessageManager().getMessage("admin.mute.broadcast",
                    Map.of("target", result.name));
            Bukkit.broadcast(broadcastMsg);
        }
    }

    /** 기간제 뮤트를 처리한다 */
    private void handleTempMute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtil.send(sender, "admin.usage.tempmute", Map.of());
            return;
        }

        String targetName = args[1];
        String durationStr = args[2];
        String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        String adminName = getAdminName(sender);

        long durationMs = DurationUtil.parse(durationStr);
        if (durationMs <= 0) {
            MessageUtil.send(sender, "admin.tempmute.invalid-duration", Map.of());
            return;
        }

        PlayerLookupResult result = lookupPlayer(targetName);
        if (result == null) {
            MessageUtil.send(sender, "admin.ban.player-not-found", Map.of("target", targetName));
            return;
        }

        MuteManager muteManager = plugin.getMuteManager();
        if (muteManager.isMuted(result.uuid)) {
            MessageUtil.send(sender, "admin.mute.already-muted", Map.of("target", result.name));
            return;
        }

        long expiresAt = System.currentTimeMillis() + durationMs;
        muteManager.mute(result.uuid, result.name, reason, adminName, expiresAt);

        String durationFormatted = DurationUtil.format(durationMs);

        MessageUtil.send(sender, "admin.tempmute.success", Map.of(
                "target", result.name, "duration", durationFormatted, "reason", reason
        ));

        if (plugin.getConfigManager().isMuteBroadcast()) {
            Component broadcastMsg = plugin.getMessageManager().getMessage("admin.tempmute.broadcast",
                    Map.of("target", result.name, "duration", durationFormatted));
            Bukkit.broadcast(broadcastMsg);
        }
    }

    /** 설정 리로드를 처리한다 */
    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        plugin.getMessageManager().reload();
        MessageUtil.send(sender, "general.reload-success", Map.of());
    }

    // ─── 유틸리티 메서드 ───

    /** 관리자 이름을 반환한다 */
    private String getAdminName(CommandSender sender) {
        return (sender instanceof Player p) ? p.getName() : "Console";
    }

    /** 플레이어를 조회한다 (온라인 우선, 오프라인 캐시 fallback) */
    private PlayerLookupResult lookupPlayer(String name) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) {
            return new PlayerLookupResult(online.getUniqueId(), online.getName(), online);
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
        if (offline != null) {
            return new PlayerLookupResult(offline.getUniqueId(), offline.getName(), null);
        }

        return null;
    }

    /** 킥 메시지 Component를 생성한다 */
    private Component createKickMessage(String messagePath, Map<String, String> placeholders) {
        String raw = plugin.getMessageManager().getRaw(messagePath);
        raw = PlaceholderManager.apply(raw, placeholders);
        raw = raw.replace("\\n", "\n");
        raw = raw.replaceAll("<#([A-Fa-f0-9]{6})>", "<color:#$1>");
        return MiniMessage.miniMessage().deserialize(raw);
    }

    /** 아이템 이름을 반환한다 (displayName 우선, 없으면 Material 이름) */
    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return MiniMessage.miniMessage().serialize(item.getItemMeta().displayName());
        }
        return item.getType().name();
    }

    /** 플레이어 조회 결과를 담는 레코드 */
    private record PlayerLookupResult(UUID uuid, String name, Player onlinePlayer) {}
}
