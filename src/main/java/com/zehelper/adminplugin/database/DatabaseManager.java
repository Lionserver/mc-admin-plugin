package com.zehelper.adminplugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zehelper.adminplugin.AdminPlugin;
import com.zehelper.adminplugin.config.ConfigManager;

import java.sql.Connection;
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
        // 프로젝트별 테이블 생성은 기능 개발 시 추가
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
