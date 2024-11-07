package me.matl114.SlimefunTools.utils.GuiClass;

import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.entity.Player;

import java.util.function.Function;
import java.util.function.IntFunction;

public class CustomMenu {
    @Getter
    CustomMenuGroup parent;
    @Getter
    int page;
    @Getter
    ChestMenu menu;
    public CustomMenu(CustomMenuGroup parent, int page) {
        this(parent, page, null);

    }
    public CustomMenu(CustomMenuGroup parent, int page, IntFunction<ChestMenu> generator) {
        this.parent = parent;
        this.page = page;
        this.menu=generator==null?new ChestMenu(parent.getTitle()): generator.apply(page);

    }
    public void loadInternal(){

    }
    public void openMenu(Player player){
        this.menu.open(player);
    }
    public void requestReload(){
        this.parent.loadPage(this);
    }
}
