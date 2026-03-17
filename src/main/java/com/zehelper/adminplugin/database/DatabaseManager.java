package com.zehelper.adminplugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zehelper.adminplugin.AdminPlugin;
import com.zehelper.adminplugin.config.ConfigManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private final AdminPlugin plugin;
    private HikariDataSource dataSource;
    private final String tablePrefix;

    public DatabaseManager(AdminPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.tablePrefix = config.getTablePrefix();
        connect(config);
    }

    /** MySQL 데이터베이스에 연결한다 */
    private void connect(ConfigManager config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getMysqlHost() + ":" + config.getMysqlPort() + "/" + config.getMysqlDatabase() + "?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");
        hikariConfig.setUsername(config.getMysqlUsername());
        hikariConfig.setPassword(config.getMysqlPassword());
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setIdleTimeout(300000);
        hikariConfig.setConnectionTimeout(10000);

        try {
            dataSource = new HikariDataSource(hikariConfig);
            plugin.getLogger().info("MySQL 데이터베이스 연결 성공");
            createTables();
        } catch (Exception e) {
            plugin.getLogger().severe("MySQL 데이터베이스 연결 실패: " + e.getMessage());
        }
    }

    /** 필요한 테이블을 생성한다 */
    private void createTables() {
        createBansTable();
        createMutesTable();
    }

    /** 벤(차단) 테이블을 생성한다 */
    private void createBansTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "bans ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "player_name VARCHAR(16) NOT NULL, "
                + "reason TEXT NOT NULL, "
                + "banned_by VARCHAR(16) NOT NULL, "
                + "expires_at TIMESTAMP NULL, "
                + "active TINYINT(1) DEFAULT 1, "
                + "unbanned_by VARCHAR(16) NULL, "
                + "unbanned_at TIMESTAMP NULL, "
                + "INDEX idx_player_uuid (player_uuid), "
                + "INDEX idx_active (active)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("bans 테이블 생성 실패: " + e.getMessage());
        }
    }

    /** 뮤트(채팅금지) 테이블을 생성한다 */
    private void createMutesTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "mutes ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "player_name VARCHAR(16) NOT NULL, "
                + "reason TEXT NOT NULL, "
                + "muted_by VARCHAR(16) NOT NULL, "
                + "expires_at TIMESTAMP NULL, "
                + "active TINYINT(1) DEFAULT 1, "
                + "INDEX idx_player_uuid (player_uuid), "
                + "INDEX idx_active (active)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("mutes 테이블 생성 실패: " + e.getMessage());
        }
    }

    /** DB 커넥션을 반환한다 */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** 테이블 접두사를 반환한다 */
    public String getTablePrefix() {
        return tablePrefix;
    }

    /** 데이터베이스 연결을 종료한다 */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("MySQL 데이터베이스 연결 종료");
        }
    }
}
