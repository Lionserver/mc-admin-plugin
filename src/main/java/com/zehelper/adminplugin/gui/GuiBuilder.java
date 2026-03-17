package com.zehelper.adminplugin.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.zehelper.adminplugin.AdminPlugin;

import java.util.*;
import java.util.function.Consumer;

public class GuiBuilder implements Listener {

    /** 열린 GUI 인스턴스를 관리하는 맵 */
    private static final Map<UUID, GuiBuilder> openGuis = new HashMap<>();
    private static boolean listenerRegistered = false;

    private final Inventory inventory;
    private final Map<Integer, Consumer<Player>> clickActions = new HashMap<>();

    /** GUI를 생성한다 */
    public GuiBuilder(String title, int rows) {
        if (!listenerRegistered) {
            Bukkit.getPluginManager().registerEvents(this, AdminPlugin.getInstance());
            listenerRegistered = true;
        }
        Component titleComponent = MiniMessage.miniMessage().deserialize(
                title.replaceAll("<#([A-Fa-f0-9]{6})>", "<color:#$1>"));
        this.inventory = Bukkit.createInventory(null, rows * 9, titleComponent);
    }

    /** 슬롯에 아이템을 배치한다 */
    public GuiBuilder setItem(int slot, ItemStack item, Consumer<Player> onClick) {
        inventory.setItem(slot, item);
        if (onClick != null) {
            clickActions.put(slot, onClick);
        }
        return this;
    }

    /** 슬롯에 아이템을 배치한다 (클릭 액션 없음) */
    public GuiBuilder setItem(int slot, ItemStack item) {
        return setItem(slot, item, null);
    }

    /** 빈 슬롯을 유리판으로 채운다 */
    public GuiBuilder fillEmpty(Material material) {
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.empty());
        filler.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
        return this;
    }

    /** 플레이어에게 GUI를 연다 */
    public void open(Player player) {
        openGuis.put(player.getUniqueId(), this);
        player.openInventory(inventory);
    }

    /** 아이템을 간편하게 생성한다 */
    public static ItemStack createItem(Material material, String displayName, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(
                displayName.replaceAll("<#([A-Fa-f0-9]{6})>", "<color:#$1>")));
        if (lore.length > 0) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(MiniMessage.miniMessage().deserialize(
                        line.replaceAll("<#([A-Fa-f0-9]{6})>", "<color:#$1>")));
            }
            meta.lore(loreComponents);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** 인벤토리 클릭 이벤트를 처리한다 */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        GuiBuilder gui = openGuis.get(player.getUniqueId());
        if (gui == null || !event.getInventory().equals(gui.inventory)) return;
        event.setCancelled(true);
        Consumer<Player> action = gui.clickActions.get(event.getRawSlot());
        if (action != null) {
            action.accept(player);
        }
    }

    /** 인벤토리 닫기 이벤트를 처리한다 */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openGuis.remove(player.getUniqueId());
        }
    }
}
