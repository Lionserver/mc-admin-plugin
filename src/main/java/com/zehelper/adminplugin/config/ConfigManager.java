package com.zehelper.adminplugin.config;

import org.bukkit.configuration.file.FileConfiguration;
import com.zehelper.adminplugin.AdminPlugin;

public class ConfigManager {

    private final AdminPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(AdminPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** 설정 파일을 다시 불러온다 */
    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    /** MySQL 호스트를 반환한다 */
    public String getMysqlHost() {
        return config.getString("database.mysql.host", "127.0.0.1");
    }

    /** MySQL 포트를 반환한다 */
    public int getMysqlPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    /** MySQL 데이터베이스명을 반환한다 */
    public String getMysqlDatabase() {
        return config.getString("database.mysql.database", "minecraft");
    }

    /** MySQL 사용자명을 반환한다 */
    public String getMysqlUsername() {
        return config.getString("database.mysql.username", "root");
    }

    /** MySQL 비밀번호를 반환한다 */
    public String getMysqlPassword() {
        return config.getString("database.mysql.password", "");
    }

    /** 테이블 접두사를 반환한다 */
    public String getTablePrefix() {
        return config.getString("database.mysql.table_prefix", "oops_adminplugin_");
    }

    /** Redis 활성화 여부를 반환한다 */
    public boolean isRedisEnabled() {
        return config.getBoolean("redis.enabled", false);
    }

    /** Redis 호스트를 반환한다 */
    public String getRedisHost() {
        return config.getString("redis.host", "127.0.0.1");
    }

    /** Redis 포트를 반환한다 */
    public int getRedisPort() {
        return config.getInt("redis.port", 6379);
    }

    /** Redis 비밀번호를 반환한다 */
    public String getRedisPassword() {
        return config.getString("redis.password", "");
    }

    /** 디버그 모드 활성화 여부를 반환한다 */
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }

    /** 벤/언벤 시 서버 전체 알림 여부를 반환한다 */
    public boolean isBanBroadcast() {
        return config.getBoolean("ban.broadcast", true);
    }

    /** 벤 DB 로그 기록 여부를 반환한다 */
    public boolean isBanLogToDb() {
        return config.getBoolean("ban.log-to-db", true);
    }

    /** 킥 시 서버 전체 알림 여부를 반환한다 */
    public boolean isKickBroadcast() {
        return config.getBoolean("kick.broadcast", true);
    }

    /** 뮤트 시 서버 전체 알림 여부를 반환한다 */
    public boolean isMuteBroadcast() {
        return config.getBoolean("chat.mute-broadcast", true);
    }
}
