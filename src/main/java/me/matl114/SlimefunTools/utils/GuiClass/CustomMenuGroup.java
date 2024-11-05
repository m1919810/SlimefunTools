package me.matl114.SlimefunTools.utils.GuiClass;

import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.Getter;
import me.matl114.SlimefunTools.utils.MenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class CustomMenuGroup {
    public interface CustomMenuClickHandler{
        static CustomMenuClickHandler of(ChestMenu.MenuClickHandler handler){
            return (cm)->handler;
        }
        public ChestMenu.MenuClickHandler getHandler(CustomMenu menu);
       // public ChestMenu.MenuClickHandler getHandler(ChestMenu menu);
    }
    @Getter
    String title;
    int sizePerPage;
    int pages;
    int[] contents=null;
    boolean placeItems=false;
    boolean placeOverrides=false;
    boolean placePresets=false;
    boolean placeBackHandlers=false;
    int prev;
    int next;
    boolean enablePageChangeSlot;
    List<ItemStack> items=new ArrayList<>();
    List<CustomMenuClickHandler> handlers=new ArrayList<>();
    HashMap<Integer,ItemStack> overrideItem=new HashMap<>();
    HashMap<Integer,CustomMenuClickHandler> overrideHandler=new HashMap<>();
    Function<Integer,ChestMenu> presetGenerator=null;
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
    public CustomMenuGroup enablePresets(Function<Integer,ChestMenu> presetGenerator){
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
    private <T extends Object> void addInternal(List<? super T>  list,int slot,T value){
        while(list.size()<=slot){
            list.add(null);
        }
        list.set(slot,value);
    }
    public CustomMenuGroup addItem(int slot,ItemStack item){
        assert this.placeItems;
        validSlot(slot);
        addInternal(items,slot,item);
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
        validSlot(slot);
        addInternal(handlers,slot,handler);
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
    public CustomMenuGroup openPage(Player p,int pages){
        CustomMenu menu=new CustomMenu(this,pages,presetGenerator);
        loadPage(menu);
        menu.openMenu(p);
        return this;
    }
    public CustomMenuGroup loadPage(CustomMenu menu){
        assert menu != null;
        assert menu.getPage()>=1&&menu.getPage()<=this.pages;
        menu.getMenu().replaceExistingItem(this.sizePerPage,null);
        menu.loadInternal();
        if(this.placeItems){
            int len=this.contents.length;
            int startIndex=Math.max(len*(menu.getPage()-1),0);
            int endIndex=Math.min(len*(menu.getPage()),this.items.size());
            for(int i=0;i<endIndex-startIndex;i++){
                menu.getMenu().replaceExistingItem(contents[i],this.items.get(startIndex+i));
                menu.getMenu().addMenuClickHandler(contents[i],getHandler(this.handlers.get(startIndex+i),menu));
            }
        }
        if(this.placeOverrides){
            Set<Integer> allOverrides=overrideItem.keySet();
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
