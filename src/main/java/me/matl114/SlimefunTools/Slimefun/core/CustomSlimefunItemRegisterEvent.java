package me.matl114.SlimefunTools.Slimefun.core;

import lombok.Getter;
import me.matl114.SlimefunTools.Slimefun.CustomSlimefunItem;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;

public class CustomSlimefunItemRegisterEvent extends SlimefunItemEvent{
    private static HandlerList handlers = new HandlerList();
    @Getter
    private CustomSlimefunItem item;
    public CustomSlimefunItemRegisterEvent(CustomSlimefunItem item) {
        super(false);
        this.item = item;
    }
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
