package me.matl114.SlimefunTools.Slimefun.core;

import org.bukkit.event.Event;

public abstract class SlimefunItemEvent extends Event {
    public SlimefunItemEvent(boolean async) {
        super(async);
    }
}
