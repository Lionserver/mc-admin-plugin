package com.zehelper.adminplugin.database;

import com.zehelper.adminplugin.AdminPlugin;
import com.zehelper.adminplugin.util.AsyncDBUtil;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BanManager {

    private final DatabaseManager databaseManager;
    private final Map<UUID, BanInfo> banCache = new ConcurrentHashMap<>();

    public BanManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        loadBans();
    }

    /**
     * DB에서 활성 벤 목록을 로드하여 캐시에 저장한다.
     * 플러그인 시작 시 비동기로 호출된다.
     */
    public void loadBans() {
        String sql = "SELECT player_uuid, player_name, reason, banned_by, expires_at FROM "
                + databaseManager.getTablePrefix() + "bans WHERE active = 1";

        AsyncDBUtil.queryAsync(sql, ps -> {}, rs -> {
            Map<UUID, BanInfo> loaded = new ConcurrentHashMap<>();
            try {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    String name = rs.getString("player_name");
                    String reason = rs.getString("reason");
                    String bannedBy = rs.getString("banned_by");
                    Timestamp expiresTs = rs.getTimestamp("expires_at");
                    Long expiresAt = (expiresTs != null) ? expiresTs.getTime() : null;

                    BanInfo info = new BanInfo(uuid, name, reason, bannedBy, expiresAt);
                    if (!info.isExpired()) {
                        loaded.put(uuid, info);
                    }
                }
            } catch (Exception e) {
                AdminPlugin.getInstance().getLogger().severe("벤 목록 로드 실패: " + e.getMessage());
            }
            return loaded;
        }).thenAccept(loaded -> {
            if (loaded != null) {
                banCache.clear();
                banCache.putAll(loaded);
                AdminPlugin.getInstance().getLogger().info("활성 벤 " + banCache.size() + "건 로드 완료");
            }
        });
    }

    /**
     * 플레이어를 벤(차단)한다.
     * DB에 INSERT하고 캐시에 추가한다.
     *
     * @param uuid     차단 대상 UUID
     * @param name     차단 대상 닉네임
     * @param reason   차단 사유
     * @param bannedBy 차단한 관리자 닉네임
     * @param expiresAt 만료 시각 (밀리초, 영구면 null)
     */
    public void ban(UUID uuid, String name, String reason, String bannedBy, Long expiresAt) {
        BanInfo info = new BanInfo(uuid, name, reason, bannedBy, expiresAt);
        banCache.put(uuid, info);

        String sql = "INSERT INTO " + databaseManager.getTablePrefix()
                + "bans (player_uuid, player_name, reason, banned_by, expires_at) VALUES (?, ?, ?, ?, ?)";

        AsyncDBUtil.executeAsync(sql, ps -> {
            try {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setString(3, reason);
                ps.setString(4, bannedBy);
                if (expiresAt != null) {
                    ps.setTimestamp(5, new Timestamp(expiresAt));
                } else {
                    ps.setNull(5, java.sql.Types.TIMESTAMP);
                }
            } catch (Exception e) {
                AdminPlugin.getInstance().getLogger().severe("벤 DB 기록 실패: " + e.getMessage());
            }
        });
    }

    /**
     * 플레이어의 벤을 해제한다.
     * DB에서 active=0으로 업데이트하고 캐시에서 제거한다.
     *
     * @param playerName 차단 해제할 플레이어 닉네임
     * @param unbannedBy 차단 해제한 관리자 닉네임
     * @return 해제 성공 여부 (캐시에 존재했는지)
     */
    public boolean unban(String playerName, String unbannedBy) {
        UUID targetUuid = null;
        for (Map.Entry<UUID, BanInfo> entry : banCache.entrySet()) {
            if (entry.getValue().name().equalsIgnoreCase(playerName)) {
                targetUuid = entry.getKey();
                break;
            }
        }

        if (targetUuid == null) {
            return false;
        }

        banCache.remove(targetUuid);

        String sql = "UPDATE " + databaseManager.getTablePrefix()
                + "bans SET active = 0, unbanned_by = ?, unbanned_at = CURRENT_TIMESTAMP "
                + "WHERE player_name = ? AND active = 1";

        AsyncDBUtil.executeAsync(sql, ps -> {
            try {
                ps.setString(1, unbannedBy);
                ps.setString(2, playerName);
            } catch (Exception e) {
                AdminPlugin.getInstance().getLogger().severe("언벤 DB 업데이트 실패: " + e.getMessage());
            }
        });

        return true;
    }

    /**
     * 플레이어가 현재 벤 상태인지 확인한다.
     * 만료된 벤은 자동으로 정리한다.
     *
     * @param uuid 확인할 플레이어 UUID
     * @return 벤 상태 여부
     */
    public boolean isBanned(UUID uuid) {
        BanInfo info = banCache.get(uuid);
        if (info == null) {
            return false;
        }
        if (info.isExpired()) {
            cleanupExpiredBan(uuid, info);
            return false;
        }
        return true;
    }

    /**
     * 플레이어의 벤 정보를 반환한다.
     *
     * @param uuid 조회할 플레이어 UUID
     * @return 벤 정보 (없으면 null)
     */
    public BanInfo getBanInfo(UUID uuid) {
        BanInfo info = banCache.get(uuid);
        if (info != null && info.isExpired()) {
            cleanupExpiredBan(uuid, info);
            return null;
        }
        return info;
    }

    /**
     * 현재 활성 벤 중인 플레이어 닉네임 목록을 반환한다.
     * 탭 자동완성에 사용된다.
     *
     * @return 벤 중인 플레이어 닉네임 Set
     */
    public Set<String> getBannedNames() {
        return banCache.values().stream()
                .filter(info -> !info.isExpired())
                .map(BanInfo::name)
                .collect(Collectors.toSet());
    }

    /**
     * 만료된 벤을 캐시에서 제거하고 DB에서 비활성 처리한다.
     *
     * @param uuid 만료된 플레이어 UUID
     * @param info 만료된 벤 정보
     */
    private void cleanupExpiredBan(UUID uuid, BanInfo info) {
        banCache.remove(uuid);

        String sql = "UPDATE " + databaseManager.getTablePrefix()
                + "bans SET active = 0 WHERE player_uuid = ? AND active = 1";

        AsyncDBUtil.executeAsync(sql, ps -> {
            try {
                ps.setString(1, uuid.toString());
            } catch (Exception e) {
                AdminPlugin.getInstance().getLogger().severe("만료 벤 정리 실패: " + e.getMessage());
            }
        });
    }

    /**
     * 벤 정보를 담는 레코드.
     *
     * @param uuid      차단된 플레이어 UUID
     * @param name      차단된 플레이어 닉네임
     * @param reason    차단 사유
     * @param bannedBy  차단한 관리자 닉네임
     * @param expiresAt 만료 시각 (밀리초, 영구면 null)
     */
    public record BanInfo(UUID uuid, String name, String reason, String bannedBy, Long expiresAt) {

        /** 벤이 만료되었는지 확인한다 */
        public boolean isExpired() {
            return expiresAt != null && System.currentTimeMillis() > expiresAt;
        }

        /** 영구 벤인지 확인한다 */
        public boolean isPermanent() {
            return expiresAt == null;
        }
    }
}
