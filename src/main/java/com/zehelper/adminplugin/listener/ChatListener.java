package com.zehelper.adminplugin.listener;

import com.zehelper.adminplugin.AdminPlugin;
import com.zehelper.adminplugin.database.MuteManager;
import com.zehelper.adminplugin.util.DurationUtil;
import com.zehelper.adminplugin.util.MessageUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;

/**
 * 채팅 잠금 및 뮤트 상태를 확인하여 채팅을 차단하는 리스너.
 * 관리자 권한을 가진 플레이어는 채팅 잠금과 뮤트 모두 면제된다.
 */
public class ChatListener implements Listener {

    private final AdminPlugin plugin;

    /** ChatListener를 생성한다 */
    public ChatListener(AdminPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 채팅 이벤트에서 채팅 잠금 및 뮤트 상태를 확인한다.
     * AsyncChatEvent는 비동기 스레드에서 실행되므로 메시지 전송은 메인 스레드에서 수행한다.
     *
     * @param event 비동기 채팅 이벤트
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // 관리자 권한이 있는 플레이어는 면제
        if (player.hasPermission("adminplugin.admin")) {
            return;
        }

        // 채팅 잠금 확인
        if (plugin.isChatLocked()) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () ->
                    MessageUtil.send(player, "admin.chatlock.blocked", Map.of())
            );
            return;
        }

        // 뮤트 확인
        MuteManager muteManager = plugin.getMuteManager();
        if (muteManager.isMuted(player.getUniqueId())) {
            event.setCancelled(true);
            MuteManager.MuteInfo info = muteManager.getMuteInfo(player.getUniqueId());
            if (info != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (info.expiresAt() != null) {
                        // 기간제 뮤트
                        MessageUtil.send(player, "admin.tempmute.blocked", Map.of(
                                "reason", info.reason(),
                                "remaining", DurationUtil.formatRemaining(info.expiresAt())
                        ));
                    } else {
                        // 영구 뮤트
                        MessageUtil.send(player, "admin.mute.blocked", Map.of(
                                "reason", info.reason()
                        ));
                    }
                });
            }
        }
    }
}
