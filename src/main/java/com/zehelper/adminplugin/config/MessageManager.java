package com.zehelper.adminplugin.config;

import com.zehelper.adminplugin.AdminPlugin;
import com.zehelper.adminplugin.util.PlaceholderManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

public class MessageManager {

    private final AdminPlugin plugin;
    private FileConfiguration messageConfig;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageManager(AdminPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** 메시지 설정 파일을 다시 불러온다 */
    public void reload() {
        File file = new File(plugin.getDataFolder(), "message.yml");
        if (!file.exists()) {
            plugin.saveResource("message.yml", false);
        }
        messageConfig = YamlConfiguration.loadConfiguration(file);
    }

    /** 원본 메시지 문자열을 반환한다 (플레이스홀더 미적용) */
    public String getRaw(String path) {
        return messageConfig.getString(path, "<#FFB4B4>메시지를 찾을 수 없음: " + path);
    }

    /** 접두사가 포함된 메시지를 Component로 반환한다 */
    public Component getMessage(String path, Map<String, String> placeholders) {
        String prefix = getRaw("prefix");
        String raw = getRaw(path);
        String replaced = PlaceholderManager.apply(prefix + raw, placeholders);
        // HEX 파스텔톤 <#RRGGBB> 형식을 MiniMessage <color:#RRGGBB> 형식으로 변환
        String converted = convertHexToMiniMessage(replaced);
        return miniMessage.deserialize(converted);
    }

    /** 접두사 없이 메시지를 Component로 반환한다 */
    public Component getMessageNoPrefix(String path, Map<String, String> placeholders) {
        String raw = getRaw(path);
        String replaced = PlaceholderManager.apply(raw, placeholders);
        String converted = convertHexToMiniMessage(replaced);
        return miniMessage.deserialize(converted);
    }

    /** <#RRGGBB> 형식을 MiniMessage <color:#RRGGBB> 형식으로 변환한다 */
    private String convertHexToMiniMessage(String input) {
        return input.replaceAll("<#([A-Fa-f0-9]{6})>", "<color:#$1>");
    }
}
