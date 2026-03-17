package com.zehelper.adminplugin;

import com.zehelper.adminplugin.command.AdminPluginCommand;
import com.zehelper.adminplugin.command.AdminPluginAdminCommand;
import com.zehelper.adminplugin.command.AdminPluginTabCompleter;
import com.zehelper.adminplugin.command.AdminPluginAdminTabCompleter;
import com.zehelper.adminplugin.config.ConfigManager;
import com.zehelper.adminplugin.config.MessageManager;
import com.zehelper.adminplugin.database.BanManager;
import com.zehelper.adminplugin.database.DatabaseManager;
import com.zehelper.adminplugin.database.LogManager;
import com.zehelper.adminplugin.database.MuteManager;
import com.zehelper.adminplugin.listener.BanListener;
import com.zehelper.adminplugin.listener.ChatListener;
import com.zehelper.adminplugin.listener.FreezeListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdminPlugin extends JavaPlugin {

    private static AdminPlugin instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private LogManager logManager;
    private BanManager banManager;
    private MuteManager muteManager;
    private boolean freezeAll = false;
    private boolean chatLocked = false;

    @Override
    public void onEnable() {
        instance = this;

        // 설정 파일 초기화
        initConfigs();

        // 데이터베이스 연결
        initDatabase();

        // 명령어 등록
        registerCommands();

        // 리스너 등록
        registerListeners();

        getLogger().info(messageManager.getRaw("general.plugin-enabled"));
    }

    @Override
    public void onDisable() {
        // 데이터베이스 연결 종료
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("AdminPlugin 플러그인이 비활성화되었습니다.");
    }

    /** 설정 파일을 초기화한다 */
    private void initConfigs() {
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
    }

    /** 데이터베이스를 초기화하고 연결한다 */
    private void initDatabase() {
        databaseManager = new DatabaseManager(this, configManager);
        logManager = new LogManager(databaseManager);
        banManager = new BanManager(databaseManager);
        muteManager = new MuteManager(databaseManager);
    }

    /** 명령어와 탭 자동완성을 등록한다 */
    private void registerCommands() {
        getCommand("adminplugin").setExecutor(new AdminPluginCommand(this));
        getCommand("adminplugin").setTabCompleter(new AdminPluginTabCompleter());
        getCommand("관리자").setExecutor(new AdminPluginAdminCommand(this));
        getCommand("관리자").setTabCompleter(new AdminPluginAdminTabCompleter());
    }

    /** 이벤트 리스너를 등록한다 */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BanListener(this), this);
        getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    /** 플러그인 인스턴스를 반환한다 */
    public static AdminPlugin getInstance() {
        return instance;
    }

    /** ConfigManager를 반환한다 */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /** MessageManager를 반환한다 */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /** DatabaseManager를 반환한다 */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /** LogManager를 반환한다 */
    public LogManager getLogManager() {
        return logManager;
    }

    /** BanManager를 반환한다 */
    public BanManager getBanManager() {
        return banManager;
    }

    /** MuteManager를 반환한다 */
    public MuteManager getMuteManager() {
        return muteManager;
    }

    /** 전체 정지 상태를 토글하고 결과를 반환한다 */
    public boolean toggleFreezeAll() {
        freezeAll = !freezeAll;
        return freezeAll;
    }

    /** 전체 정지 상태를 반환한다 */
    public boolean isFreezeAll() {
        return freezeAll;
    }

    /** 채팅 잠금 상태를 토글하고 결과를 반환한다 */
    public boolean toggleChatLock() {
        chatLocked = !chatLocked;
        return chatLocked;
    }

    /** 채팅 잠금 상태를 반환한다 */
    public boolean isChatLocked() {
        return chatLocked;
    }
}
