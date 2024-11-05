package me.matl114.SlimefunTools.implement;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import lombok.Getter;
import me.matl114.SlimefunTools.functions.CustomItemBase;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.nio.file.Paths;

public class SlimefunTools extends JavaPlugin implements   SlimefunAddon {
    @Getter
    private static SlimefunTools instance;
    public void onEnable() {
        instance = this;
        ConfigLoader.load(this);
        new CustomItemBase()
                .init(Paths.get(getDataFolder().toString(),"item-base").toString())
                .registerCommand(this);
    }

    public JavaPlugin getJavaPlugin() {
        return this;
    }
    public String getBugTrackerURL() {
        // 你可以在这里返回你的问题追踪器的网址，而不是 null
        return null;
    }
    public static void runSync(Runnable runnable) {
        new BukkitRunnable() {
            public void run() {
                runnable.run();
            }
        }.runTask(getInstance());
    }
    public static void runSync(BukkitRunnable runnable) {
        runnable.runTask(getInstance());
    }
}