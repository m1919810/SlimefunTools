package me.matl114.SlimefunTools.utils.GuiClass;

import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.Getter;
import me.matl114.SlimefunTools.utils.MenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;

public class CustomMenuGroup {
    public interface CustomMenuClickHandler{
        static CustomMenuClickHandler of(ChestMenu.MenuClickHandler handler){
            return (cm)->handler;
        }
        static CustomMenuClickHandler ofEmpty(){
            return (cm)->ChestMenuUtils.getEmptyClickHandler();
        }
        public ChestMenu.MenuClickHandler getHandler(CustomMenu menu);
       // public ChestMenu.MenuClickHandler getHandler(ChestMenu menu);
    }
    @Getter
    String title;
    @Getter
    int sizePerPage;
    @Getter
    int pages;
    @Getter
    int[] contents=null;
    @Getter
    boolean placeItems=false;
    boolean placeOverrides=false;
    boolean placePresets=false;
    boolean placeBackHandlers=false;
    @Getter
    int prev;
    @Getter
    int next;
    boolean enablePageChangeSlot;
    //todo changed to suppilers
    List<ItemStack> items=new ArrayList<>();
    List<CustomMenuClickHandler> handlers=new ArrayList<>();
    HashMap<Integer,ItemStack> overrideItem=new HashMap<>();
    HashMap<Integer,CustomMenuClickHandler> overrideHandler=new HashMap<>();
    IntFunction<ChestMenu> presetGenerator=null;
    public CustomMenuGroup(String title,int PageSize,int pages){
        assert pages > 0;
        this.title=title;
        this.sizePerPage=PageSize;
        this.pages=pages;
    }
    public void validSlot(int slot){
        assert slot >= 0&&slot<this.sizePerPage;
    }
    public CustomMenuGroup enableContentPlace(int[] contents){
        assert contents != null;
        this.placeItems=true;
        for(int i=0;i<contents.length;i++){
            validSlot(contents[i]);
        }
        this.contents=contents;
        return this;
    }
    public CustomMenuGroup enableOverrides(){
        this.placeOverrides=true;
        return this;
    }
    public CustomMenuGroup enablePresets(IntFunction<ChestMenu> presetGenerator){
        assert presetGenerator != null;
        this.placePresets=true;
        this.presetGenerator=presetGenerator;
        return this;
    }
    public CustomMenuGroup setPageChangeSlots(int prev,int next){
        validSlot(prev);
        validSlot(next);
        this.enablePageChangeSlot = true;
        this.prev=prev;
        this.next=next;
        return this;
    }
    public CustomMenuGroup setOverrideItem(int slot,ItemStack stack){
        assert this.placeOverrides;
        validSlot(slot);
        this.overrideItem.put(slot,stack);
        return this;
    }
    public CustomMenuGroup setOverrideHandler(int slot,CustomMenuClickHandler handler){
        assert this.placeOverrides;
        validSlot(slot);
        this.overrideHandler.put(slot,handler);
        return this;
    }
    public CustomMenuGroup setOverrideItem(int slot,ItemStack stack,CustomMenuClickHandler handler){
        assert this.placeOverrides;
        validSlot(slot);
        setOverrideItem(slot,stack);
        setOverrideHandler(slot,handler);
        return this;
    }
    //return if expand
    private <T extends Object> boolean addInternal(List<? super T>  list,int slot,T value){
        if(list.size()>slot){
            list.set(slot,value);
            return false;
        }
        while(list.size()<=slot){
            list.add(null);
        }
        list.set(slot,value);
        return true;
    }
    public CustomMenuGroup addItem(int slot,ItemStack item){
        assert this.placeItems;
        if(addInternal(items,slot,item)){
            resetPageSize();
        }
        return this;
    }
    public CustomMenuGroup resetPageSize(){
        assert this.placeItems;
        int should=(this.items.size()-1)/this.contents.length+1;
        if(this.pages<should){
            this.pages=should;
        }
        return this;
    }
    public CustomMenuGroup resetItems(List<ItemStack> items){
        assert  this.placeItems;
        this.items=items;

        resetPageSize();
        return this;
    }
    public CustomMenuGroup resetHandlers(List<CustomMenuClickHandler> handlers){
        assert this.placeItems;
        this.handlers=handlers;
        resetPageSize();
        return this;
    }
    public CustomMenuGroup addHandler(int slot,CustomMenuClickHandler handler){
        assert this.placeItems;
        if(addInternal(handlers,slot,handler)){
            resetPageSize();
        }
        return this;
    }
    public CustomMenuGroup addItem(int slot,ItemStack item,CustomMenuClickHandler handler){
        addItem(slot,item);
        addHandler(slot,handler);
        return this;
    }
    public ChestMenu.MenuClickHandler getHandler(CustomMenuClickHandler handler,CustomMenu menu){
        if(handler==null){
            return ChestMenuUtils.getEmptyClickHandler();
        }else {
            return handler.getHandler(menu);
        }
    }
    public IntFunction<ChestMenu> getDefaultGenerator(){
        return (integer)->{
            ChestMenu cmenu=new ChestMenu(this.title);
            cmenu.addItem(this.sizePerPage-1,null);
            for(int i=0;i<this.sizePerPage;++i){
                cmenu.addMenuClickHandler(i,ChestMenuUtils.getEmptyClickHandler());
            }
            return cmenu;
        };
    }
    public CustomMenu buildPage(int page){
        CustomMenu menu=new CustomMenu(this,page,presetGenerator!=null?presetGenerator:getDefaultGenerator());
        loadPage(menu);
        return menu;
    }
    public CustomMenuGroup openPage(Player p,int pages){
        CustomMenu menu=buildPage(pages);
        menu.openMenu(p);
        return this;
    }
    public CustomMenuGroup loadPage(CustomMenu menu){
        assert menu != null;
        assert menu.getPage()>=1&&menu.getPage()<=this.pages;

        menu.loadInternal();
        if(this.placeItems){
            int len=this.contents.length;
            int startIndex=Math.max(len*(menu.getPage()-1),0);
            int endIndex=Math.min(len*(menu.getPage()),this.items.size());
            int i=0;
            for(;i<endIndex-startIndex;i++){
                menu.getMenu().replaceExistingItem(contents[i],this.items.get(startIndex+i));
                menu.getMenu().addMenuClickHandler(contents[i],getHandler(this.handlers.get(startIndex+i),menu));
            }
            for(;i<len;i++){
                menu.getMenu().addMenuClickHandler(contents[i],ChestMenuUtils.getEmptyClickHandler());
                menu.getMenu().replaceExistingItem(contents[i],null);
            }
        }
        if(this.placeOverrides){
            HashSet<Integer> allOverrides=new HashSet<>(overrideItem.keySet());
            allOverrides.addAll(overrideHandler.keySet());
            for(Integer slot:allOverrides){
                if(overrideItem.containsKey(slot)){
                    menu.getMenu().replaceExistingItem(slot,overrideItem.get(slot));
                }
                menu.getMenu().addMenuClickHandler(slot,getHandler( overrideHandler.get(slot),menu));
            }
        }
        if(this.enablePageChangeSlot){
            menu.getMenu().replaceExistingItem(this.prev, MenuUtils.getPreviousButton(menu.getPage(),this.pages));
            menu.getMenu().replaceExistingItem(this.next, MenuUtils.getNextButton(menu.getPage(),this.pages));
            menu.getMenu().addMenuClickHandler(this.prev,((player, i, itemStack, clickAction) -> {
                if(menu.getPage()>1){
                    this.openPage(player,menu.getPage()-1);
                }
                return false;
            }));
            menu.getMenu().addMenuClickHandler(this.next,((player, i, itemStack, clickAction) -> {
                if(menu.getPage()<pages){
                    this.openPage(player,menu.getPage()+1);
                }
                return false;
            }));
        }
        if(this.placeBackHandlers){
            //remain not completed
        }
        //
        return  null;
    }




}
