package me.matl114.SlimefunTools.utils.MenuClass;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

public class DummyItemGroup extends ItemGroup {
    final boolean hidden ;
    final boolean useOriginItem;
    @ParametersAreNonnullByDefault
    public DummyItemGroup(NamespacedKey key, ItemStack item,boolean isHiiden,boolean useOriginItem) {
        super(key, item);
        this.hidden = isHiiden;
        this.useOriginItem = useOriginItem;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean isHidden(Player p) {
        return hidden;
    }
    public ItemStack getItem(Player p){
        return useOriginItem? this.item: super.getItem(p);
    }
    public void setItem(ItemStack item){
        this.item.setType(item.getType());
        this.item.setItemMeta(item.getItemMeta());
        this.item.setData(item.getData());
    }
}