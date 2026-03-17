package com.zehelper.adminplugin.listener;

import com.zehelper.adminplugin.AdminPlugin;
import com.zehelper.adminplugin.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;

/**
 * 전체 정지 활성화 시 모든 플레이어의 이동을 차단하는 리스너.
 * 관리자 권한을 가진 플레이어는 면제된다.
 */
public class FreezeListener implements Listener {

    private final AdminPlugin plugin;

    /** FreezeListener를 생성한다 */
    public FreezeListener(AdminPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 전체 정지 상태에서 플레이어의 이동을 차단한다.
     * 블록 좌표가 변경될 때만 차단하며, 고개 돌리기는 허용한다.
     * 관리자 권한이 있는 플레이어는 면제된다.
     *
     * @param event 플레이어 이동 이벤트
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.isFreezeAll()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("adminplugin.admin")) {
            return;
        }

        // 블록 좌표가 변경될 때만 차단 (고개 돌리기는 허용)
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setCancelled(true);
            MessageUtil.send(player, "admin.freeze.notice", Map.of());
        }
    }

    /**
     * 전체 정지 상태에서 새로 접속한 플레이어에게 정지 알림을 보낸다.
     *
     * @param event 플레이어 접속 이벤트
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.isFreezeAll()) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("adminplugin.admin")) {
            MessageUtil.send(player, "admin.freeze.notice", Map.of());
        }
    }
}
