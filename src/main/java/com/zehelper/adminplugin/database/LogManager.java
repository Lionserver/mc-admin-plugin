package com.zehelper.adminplugin.database;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class LogManager {

    private final DatabaseManager databaseManager;

    public LogManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        createLogTable();
    }

    /** 로그 테이블을 생성한다 */
    private void createLogTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + databaseManager.getTablePrefix() + "logs ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "player_uuid VARCHAR(36), "
                + "player_name VARCHAR(16), "
                + "action VARCHAR(64), "
                + "detail TEXT"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 로그를 기록한다 */
    public void log(String playerUuid, String playerName, String action, String detail) {
        String sql = "INSERT INTO " + databaseManager.getTablePrefix() + "logs (player_uuid, player_name, action, detail) VALUES (?, ?, ?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, playerName);
            ps.setString(3, action);
            ps.setString(4, detail);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
