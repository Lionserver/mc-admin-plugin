package com.zehelper.adminplugin.database;

import com.zehelper.adminplugin.AdminPlugin;
import com.zehelper.adminplugin.util.AsyncDBUtil;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MuteManager {

    private final DatabaseManager databaseManager;
    private final Map<UUID, MuteInfo> muteCache = new ConcurrentHashMap<>();

    public MuteManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        loadMutes();
    }

    /**
     * DB에서 활성 뮤트 목록을 로드하여 캐시에 저장한다.
     * 플러그인 시작 시 비동기로 호출된다.
     */
    public void loadMutes() {
        String sql = "SELECT player_uuid, player_name, reason, muted_by, expires_at FROM "
                + databaseManager.getTablePrefix() + "mutes WHERE active = 1";

        AsyncDBUtil.queryAsync(sql, ps -> {}, rs -> {
            Map<UUID, MuteInfo> loaded = new ConcurrentHashMap<>();
            try {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    String name = rs.getString("player_name");
                    String reason = rs.getString("reason");
                    String mutedBy = rs.getString("muted_by");
                    Timestamp expiresTs = rs.getTimestamp("expires_at");
                    Long expiresAt = (expiresTs != null) ? expiresTs.getTime() : null;

                    MuteInfo info = new MuteInfo(uuid, name, reason, mutedBy, expiresAt);
                    if (!info.isExpired()) {
                        loaded.put(uuid, info);
                    }
                }
            } catch (Exception e) {
                AdminPlugin.getInstance().getLogger().severe("뮤트 목록 로드 실패: " + e.getMessage());
            }
            return loaded;
        }).thenAccept(loaded -> {
            if (loaded != null) {
                muteCache.clear();
                muteCache.putAll(loaded);
                AdminPlugin.getInstance().getLogger().info("활성 뮤트 " + muteCache.size() + "건 로드 완료");
            }
        });
    }

    /**
     * 플레이어를 뮤트(채팅금지)한다.
     * DB에 INSERT하고 캐시에 추가한다.
     *
     * @param uuid     뮤트 대상 UUID
     * @param name     뮤트 대상 닉네임
     * @param reason   뮤트 사유
     * @param mutedBy  뮤트한 관리자 닉네임
     * @param expiresAt 만료 시각 (밀리초, 영구면 null)
     */
    public void mute(UUID uuid, String name, String reason, String mutedBy, Long expiresAt) {
        MuteInfo info = new MuteInfo(uuid, name, reason, mutedBy, expiresAt);
        muteCache.put(uuid, info);

        String sql = "INSERT INTO " + databaseManager.getTablePrefix()
                + "mutes (player_uuid, player_name, reason, muted_by, expires_at) VALUES (?, ?, ?, ?, ?)";

        AsyncDBUtil.executeAsync(sql, ps -> {
            try {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setString(3, reason);
                ps.setString(4, mutedBy);
                if (expiresAt != null) {
                    ps.setTimestamp(5, new Timestamp(expiresAt));
                } else {
                    ps.setNull(5, java.sql.Types.TIMESTAMP);
                }
            } catch (Exception e) {
                AdminPlugin.getInstance().getLogger().severe("뮤트 DB 기록 실패: " + e.getMessage());
            }
        });
    }

    /**
     * 플레이어가 현재 뮤트 상태인지 확인한다.
     * 만료된 뮤트는 자동으로 정리한다.
     *
     * @param uuid 확인할 플레이어 UUID
     * @return 뮤트 상태 여부
     */
    public boolean isMuted(UUID uuid) {
        MuteInfo info = muteCache.get(uuid);
        if (info == null) {
            return false;
        }
        if (info.isExpired()) {
            cleanupExpiredMute(uuid);
            return false;
        }
        return true;
    }

    /**
     * 플레이어의 뮤트 정보를 반환한다.
     *
     * @param uuid 조회할 플레이어 UUID
     * @return 뮤트 정보 (없으면 null)
     */
    public MuteInfo getMuteInfo(UUID uuid) {
        MuteInfo info = muteCache.get(uuid);
        if (info != null && info.isExpired()) {
            cleanupExpiredMute(uuid);
            return null;
        }
        return info;
    }

    /**
     * 만료된 뮤트를 캐시에서 제거하고 DB에서 비활성 처리한다.
     *
     * @param uuid 만료된 플레이어 UUID
     */
    private void cleanupExpiredMute(UUID uuid) {
        muteCache.remove(uuid);

        String sql = "UPDATE " + databaseManager.getTablePrefix()
                + "mutes SET active = 0 WHERE player_uuid = ? AND active = 1";

        AsyncDBUtil.executeAsync(sql, ps -> {
            try {
                ps.setString(1, uuid.toString());
            } catch (Exception e) {
                AdminPlugin.getInstance().getLogger().severe("만료 뮤트 정리 실패: " + e.getMessage());
            }
        });
    }

    /**
     * 뮤트 정보를 담는 레코드.
     *
     * @param uuid     뮤트된 플레이어 UUID
     * @param name     뮤트된 플레이어 닉네임
     * @param reason   뮤트 사유
     * @param mutedBy  뮤트한 관리자 닉네임
     * @param expiresAt 만료 시각 (밀리초, 영구면 null)
     */
    public record MuteInfo(UUID uuid, String name, String reason, String mutedBy, Long expiresAt) {

        /** 뮤트가 만료되었는지 확인한다 */
        public boolean isExpired() {
            return expiresAt != null && System.currentTimeMillis() > expiresAt;
        }
    }
}
