package me.matl114.SlimefunTools.utils;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class MenuUtils {


    public static final ItemStack PROCESSOR_NULL= new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " ");
    public static final ItemStack PROCESSOR_SPACE=new CustomItemStack(Material.RED_STAINED_GLASS_PANE,"&6进程完成","&c空间不足");
    public static final ItemStack PREV_BUTTON_ACTIVE = new SlimefunItemStack("_UI_PREVIOUS_ACTIVE", Material.LIME_STAINED_GLASS_PANE, "&r⇦ Previous Page", new String[0]);
    public static final ItemStack NEXT_BUTTON_ACTIVE = new SlimefunItemStack("_UI_NEXT_ACTIVE", Material.LIME_STAINED_GLASS_PANE, "&rNext Page ⇨", new String[0]);
    public static final ItemStack PREV_BUTTON_INACTIVE = new SlimefunItemStack("_UI_PREVIOUS_INACTIVE", Material.BLACK_STAINED_GLASS_PANE, "&8⇦ Previous Page", new String[0]);
    public static final ItemStack NEXT_BUTTON_INACTIVE = new SlimefunItemStack("_UI_NEXT_INACTIVE", Material.BLACK_STAINED_GLASS_PANE, "&8Next Page ⇨", new String[0]);
    public static final ItemStack BACK_BUTTON = new SlimefunItemStack("_UI_BACK", Material.ENCHANTED_BOOK, "&7⇦ 返回", (meta) -> {
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
    });
    public static final ItemStack SEARCH_BUTTON = new SlimefunItemStack("_UI_SEARCH", Material.NAME_TAG, "&b搜索...", "", ChatColor.GRAY + "⇨ " +"&b单击搜索物品");
    public static final ItemStack NO_ITEM = new SlimefunItemStack("_UI_NO_ITEM",Material.BARRIER,"&8 ");
    public static final ItemStack PRESET_INFO=new CustomItemStack(Material.CYAN_STAINED_GLASS_PANE,"&3配方类型信息");
    public static final ItemStack PRESET_MORE=new CustomItemStack(Material.LIME_STAINED_GLASS_PANE,"&a更多物品(已省略)");

    public static final ChestMenu.MenuClickHandler CLOSE_HANDLER=((player, i, itemStack, clickAction) -> {
        player.closeInventory();
        return false;

    });


    public static ItemStack getPreviousButton(int page, int pages) {
        return pages != 1 && page != 1 ? new CustomItemStack(PREV_BUTTON_ACTIVE, (meta) -> {
            ChatColor var10001 = ChatColor.WHITE;
            meta.setDisplayName("" + var10001 + "⇦ " + "上一页");
            meta.setLore(Arrays.asList("", ChatColor.GRAY + "(" + page + " / " + pages + ")"));
        }) : new CustomItemStack(PREV_BUTTON_INACTIVE, (meta) -> {
            ChatColor var10001 = ChatColor.DARK_GRAY;
            meta.setDisplayName("" + var10001 + "⇦ " +"上一页");
            meta.setLore(Arrays.asList("", ChatColor.GRAY + "(" + page + " / " + pages + ")"));
        });
    }
    public static ItemStack getNextButton( int page, int pages) {
        return pages != 1 && page != pages ? new CustomItemStack(NEXT_BUTTON_ACTIVE, (meta) -> {
            ChatColor var10001 = ChatColor.WHITE;
            meta.setDisplayName("" + var10001 + "下一页" + " ⇨");
            meta.setLore(Arrays.asList("", ChatColor.GRAY + "(" + page + " / " + pages + ")"));
        }) : new CustomItemStack(NEXT_BUTTON_INACTIVE, (meta) -> {
            ChatColor var10001 = ChatColor.DARK_GRAY;
            meta.setDisplayName("" + var10001 + "下一页" + " ⇨");
            meta.setLore(Arrays.asList("", ChatColor.GRAY + "(" + page + " / " + pages + ")"));
        });
    }
    public static ItemStack getBackButton( String... lore) {
        return new CustomItemStack(BACK_BUTTON, "&7⇦ " +"返回", lore);
    }
}
