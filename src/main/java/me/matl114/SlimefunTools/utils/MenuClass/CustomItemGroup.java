package me.matl114.SlimefunTools.utils.MenuClass;

import city.norain.slimefun4.VaultIntegration;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.groups.FlexItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.chat.ChatInput;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.Setter;
import me.matl114.SlimefunTools.implement.Debug;
import me.matl114.SlimefunTools.utils.AddUtils;
import me.matl114.SlimefunTools.utils.GuiClass.CustomMenu;
import me.matl114.SlimefunTools.utils.GuiClass.CustomMenuGroup;
import me.matl114.SlimefunTools.utils.MenuUtils;
import me.matl114.SlimefunTools.utils.StructureClass.Manager;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class CustomItemGroup extends FlexItemGroup  {
    protected static final ItemStack INVOKE_ERROR=new CustomItemStack(Material.BARRIER,"&c","","&c获取物品组物品展示失败");
    protected int pages;
    protected int size;
    protected int contentPerPage;
    protected int[] contents;
    protected boolean isVisible;
    protected CustomMenuGroup group;
    protected HashMap<Integer,ItemGroup> subGroups;
    protected HashMap<Integer,SlimefunItem> items;
    boolean loaded=false;
    @Override
    public ItemStack getItem(Player p){
        return this.item;
    }

    public void setItem(ItemStack item){
        this.item.setType(item.getType());
        this.item.setItemMeta(item.getItemMeta());
        this.item.setData(item.getData());
    }
    public CustomItemGroup(NamespacedKey key,ItemStack item,boolean hide){
        super(key,item);
        this.isVisible=!hide    ;
    }
    public CustomItemGroup setLoader(CustomMenuGroup group,
                                     HashMap<Integer,ItemGroup> subGroup,
                                     HashMap<Integer, SlimefunItem> researches){
        this.group=group;
        assert group.isPlaceItems();
        this.pages=group.getPages();
        this.size=group.getSizePerPage();
        this.contentPerPage=group.getContents().length;
        this.contents=group.getContents();
        this.subGroups=subGroup;
        this.items=researches;
        this.loaded=true;
        return this;
    }

    public boolean isVisible(Player var1, PlayerProfile var2, SlimefunGuideMode var3){
        return isVisible;
    }
    public boolean isHidden(Player p) {
        return !isVisible;
    }
    public void open(Player var1, PlayerProfile var2, SlimefunGuideMode var3){
        assert loaded;
        int page=getLastPage(var1,var2,var3);
        if(page<=0||page>this.pages){
            page=1;
        }
        openPage(var1,var2,var3,page);
    }
    @Setter
    private HashSet<Integer> searchButton=new HashSet<>();
    @Setter
    private HashSet<Integer> backButton=new HashSet<>();
    public static ItemStack getItemGroupIcon(ItemGroup group){
        try{
            Class clazz= Class.forName("io.github.thebusybiscuit.slimefun4.api.items.ItemGroup");
            Field _hasType=clazz.getDeclaredField("item");
            _hasType.setAccessible(true);
            return (ItemStack)_hasType.get((ItemGroup)group);
        }catch (Throwable e){
            return AddUtils.renameItem(INVOKE_ERROR,group.getUnlocalizedName());
        }
    }
    public static boolean setItemGroupIcon(ItemGroup group,ItemStack stack){
        try{
            Class clazz= Class.forName("io.github.thebusybiscuit.slimefun4.api.items.ItemGroup");
            Field _hasType=clazz.getDeclaredField("item");
            _hasType.setAccessible(true);
            _hasType.set(group,stack);
            return true;
        }catch (Throwable e){
            return false;
        }
    }
    public void openPage(Player var1, PlayerProfile var2, SlimefunGuideMode var3,int page){
        assert page>=1&&page<=pages;
        var2.getGuideHistory().add(this,page);
        ChestMenu menu=this.group.buildPage(page).getMenu();
        //prev键
        menu.addMenuClickHandler(this.group.getPrev(),((player, i, itemStack, clickAction) -> {
            if(page>1){
                this.openPage(var1,var2,var3,page-1);
            }return false;
        }));
        //next键
        menu.addMenuClickHandler(this.group.getNext(),((player, i, itemStack, clickAction) -> {
            if(page<this.pages){
                this.openPage(var1,var2,var3,page+1);
            }return false;
        }));
        //搜索键
        for(Integer i:this.searchButton){
            menu.replaceExistingItem(i,ChestMenuUtils.getSearchButton(var1));
            menu.addMenuClickHandler(i, (pl, slot, item, action) -> {
                pl.closeInventory();
                Slimefun.getLocalization().sendMessage(pl, "guide.search.message");
                ChatInput.waitForPlayer(
                        Slimefun.instance(),
                        pl,
                        msg -> SlimefunGuide.openSearch(var2, msg, SlimefunGuideMode.SURVIVAL_MODE, true));
                return false;
            });
        }
        for(Integer i:this.backButton){
            menu.replaceExistingItem(i, MenuUtils.getBackButton());
            menu.addMenuClickHandler(i,((player, j, itemStack, clickAction) -> {
                var2.getGuideHistory().goBack(Slimefun.getRegistry().getSlimefunGuide(var3));
                return false;
            }));
        }
        int startIndex=Math.max(0,this.contentPerPage*(page-1));
        int endIndex=Math.min(this.contentPerPage*page,this.contentPerPage*this.pages);
        for (Map.Entry<Integer,ItemGroup> entry:this.subGroups.entrySet()){
            int index=entry.getKey();
            if(index>=startIndex&&index<endIndex){
                int realIndex=contents[ (index-startIndex)%this.contentPerPage];
                final ItemGroup group=entry.getValue();
                menu.replaceExistingItem(realIndex,this.getItemGroupIcon(group));
                menu.addMenuClickHandler(realIndex,((player, i, itemStack, clickAction) -> {
                    SlimefunGuide.openItemGroup(var2, group, var3, 1);
                    return false;
                }));
            }
        }
        for (Map.Entry<Integer,SlimefunItem> entry:this.items.entrySet()){
            int index=entry.getKey();
            if(index>=startIndex&&index<endIndex){
                int realIndex=contents[ (index-startIndex)%this.contentPerPage];
                displaySlimefunItem(menu,this,var1,var2,entry.getValue(),var3,page,realIndex);
            }
        }
        menu.open(var1);
    }
    private void displaySlimefunItem(ChestMenu menu, ItemGroup itemGroup, Player p, PlayerProfile profile, SlimefunItem sfitem,SlimefunGuideMode mode, int page, int index) {
        Research research = sfitem.getResearch();
        if (SlimefunGuideMode.CHEAT_MODE!=mode && !Slimefun.getPermissionsService().hasPermission(p, sfitem)) {
            List<String> message = Slimefun.getPermissionsService().getLore(sfitem);
            menu.addItem(index, new CustomItemStack(ChestMenuUtils.getNoPermissionItem(), sfitem.getItemName(), (String[])message.toArray(new String[0])));
            menu.addMenuClickHandler(index, ChestMenuUtils.getEmptyClickHandler());
        } else if (SlimefunGuideMode.CHEAT_MODE!=mode&& research != null && !profile.hasUnlocked(research)) {
            String lore;
            if (VaultIntegration.isEnabled()) {
                Object[] var10001 = new Object[]{research.getCurrencyCost()};
                lore = String.format("%.2f", var10001) + " 游戏币";
            } else {
                lore = research.getLevelCost() + " 级经验";
            }

            menu.addItem(index, new CustomItemStack(new CustomItemStack(ChestMenuUtils.getNoPermissionItem(), "&f" + ItemUtils.getItemName(sfitem.getItem()), new String[]{"&7" + sfitem.getId(), "&4&l" + Slimefun.getLocalization().getMessage(p, "guide.locked"), "", "&a> 单击解锁", "", "&7需要 &b", lore})));
            menu.addMenuClickHandler(index, (pl, slot, item, action) -> {
                research.unlockFromGuide(Slimefun.getRegistry().getSlimefunGuide(mode), p, profile, sfitem, itemGroup, page);
                return false;
            });
        } else {
            menu.addItem(index, sfitem.getItem());
            menu.addMenuClickHandler(index, (pl, slot, item, action) -> {
                try {
                    if (SlimefunGuideMode.CHEAT_MODE!=mode) {
                        Slimefun.getRegistry().getSlimefunGuide(mode).displayItem(profile, sfitem, true);
                    } else if (pl.hasPermission("slimefun.cheat.items")) {
                        if (sfitem instanceof MultiBlockMachine) {
                            Slimefun.getLocalization().sendMessage(pl, "guide.cheat.no-multiblocks");
                        } else {
                            ItemStack clonedItem = sfitem.getItem().clone();
                            if (action.isShiftClicked()) {
                                clonedItem.setAmount(clonedItem.getMaxStackSize());
                            }

                            pl.getInventory().addItem(new ItemStack[]{clonedItem});
                        }
                    } else {
                        Slimefun.getLocalization().sendMessage(pl, "messages.no-permission", true);
                    }
                } catch (LinkageError | Exception var8) {
                    Throwable x = var8;
                    p.sendMessage(ChatColor.DARK_RED + "An internal server error has occurred. Please inform an admin, check the console for further info.");
                    sfitem.error("This item has caused an error message to be thrown while viewing it in the Slimefun guide.", x);
                }
                return false;
            });
        }

    }

    //modified from guizhan Infinity Expansion 2
    private int getLastPage(Player var1, PlayerProfile var2, SlimefunGuideMode var3){
        try{
            Class clazz= GuideHistory.class;
            Method getEntryMethod=clazz.getDeclaredMethod("getLastEntry",boolean.class);
            getEntryMethod.setAccessible(true);
            Object entry=getEntryMethod.invoke(var2.getGuideHistory(),false);
            Class entryClass= Class.forName("io.github.thebusybiscuit.slimefun4.core.guide.GuideEntry");
            Method entryGetObjMethod=entryClass.getDeclaredMethod("getIndexedObject");
            entryGetObjMethod.setAccessible(true);
            Object obj=entryGetObjMethod.invoke(entry);
            if(obj instanceof CustomItemGroup){
                Method entryGetPageMethod=entryClass.getDeclaredMethod("getPage");
                entryGetPageMethod.setAccessible(true);
                return (Integer)entryGetPageMethod.invoke(entry);
            }else{
                return 1;
            }

        }catch (Throwable e){
            return 1;
        }

    }
}
