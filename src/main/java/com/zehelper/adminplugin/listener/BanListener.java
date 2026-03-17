package com.zehelper.adminplugin.listener;

import com.zehelper.adminplugin.AdminPlugin;
import com.zehelper.adminplugin.database.BanManager;
import com.zehelper.adminplugin.util.DurationUtil;
import com.zehelper.adminplugin.util.PlaceholderManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 플레이어 접속 시 벤 여부를 확인하여 접속을 차단하는 리스너.
 */
public class BanListener implements Listener {

    private final AdminPlugin plugin;

    /** BanListener를 생성한다 */
    public BanListener(AdminPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 플레이어 로그인 시 벤 상태를 확인하여 접속을 차단한다.
     * 영구 벤과 기간제 벤에 따라 다른 킥 화면 메시지를 표시한다.
     *
     * @param event 플레이어 로그인 이벤트
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        BanManager banManager = plugin.getBanManager();

        if (banManager.isBanned(player.getUniqueId())) {
            BanManager.BanInfo info = banManager.getBanInfo(player.getUniqueId());
            if (info != null) {
                String screenPath = info.isPermanent() ? "admin.ban.screen" : "admin.tempban.screen";
                String raw = plugin.getMessageManager().getRaw(screenPath);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("reason", info.reason());
                placeholders.put("admin", info.bannedBy());
                if (!info.isPermanent()) {
                    placeholders.put("remaining", DurationUtil.formatRemaining(info.expiresAt()));
                }

                raw = PlaceholderManager.apply(raw, placeholders);
                raw = raw.replace("\\n", "\n");
                raw = raw.replaceAll("<#([A-Fa-f0-9]{6})>", "<color:#$1>");

                Component kickMsg = MiniMessage.miniMessage().deserialize(raw);
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, kickMsg);
            }
        }
    }
}
