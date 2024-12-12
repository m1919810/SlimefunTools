package me.matl114.SlimefunTools.core;

import com.google.common.base.Preconditions;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.groups.FlexItemGroup;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.Getter;
import me.matl114.SlimefunTools.Slimefun.CustomRecipeType;
import me.matl114.SlimefunTools.Slimefun.CustomSlimefunItem;
import me.matl114.SlimefunTools.Slimefun.core.CustomSlimefunItemRegisterEvent;
import me.matl114.SlimefunTools.functional.*;
import me.matl114.SlimefunTools.implement.Configs;
import me.matl114.SlimefunTools.implement.Debug;
import me.matl114.SlimefunTools.implement.SlimefunTools;
import me.matl114.matlib.Utils.*;
import me.matl114.matlib.Utils.Command.CommandGroup.ComplexCommandExecutor;
import me.matl114.matlib.Utils.Command.CommandGroup.SubCommand;
import me.matl114.matlib.Utils.Command.CommandUtils;
import me.matl114.matlib.Utils.Command.Params.SimpleCommandArgs;
import me.matl114.matlib.Utils.Menu.GuideMenu.CustomItemGroup;
import me.matl114.matlib.Utils.Menu.GuideMenu.DummyItemGroup;
import me.matl114.matlib.Utils.Menu.MenuClickHandler.DataContainer;
import me.matl114.matlib.Utils.Menu.MenuGroup.CustomMenu;
import me.matl114.matlib.Utils.Menu.MenuGroup.CustomMenuGroup;
import me.matl114.matlib.Utils.Menu.MenuUtils;
import me.matl114.matlib.Utils.Reflect.ReflectUtils;
import me.matl114.matlib.core.Manager;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class ItemRegister implements Manager, ComplexCommandExecutor, TabCompleter {
    HashMap<String, SlimefunItem> registeredItems=new LinkedHashMap<>();
    HashMap<String, ItemGroup> registeredGroups =new LinkedHashMap<>();
    private boolean registered=false;
    private String itemPath;
    private Config itemConfig;
    private String groupPath;
    private Config groupConfig;
    private Plugin plugin;
    private final HashMap<String, RecipeType> recipes=new LinkedHashMap<>();
    private final HashMap<String, ItemGroup> groups=new LinkedHashMap<>();
    private final HashMap<String,SlimefunItem> items=new LinkedHashMap<>();
    public ItemGroup getGroup(String val){
        return groups.get(val);
    }
    public RecipeType getRtype(String val){
        return recipes.get(val);
    }
    @Getter
    static ItemRegister manager;
    public ItemRegister(){
        Preconditions.checkState(CustomItemBase.getManager()!=null, "ItemRegister requires CustomItemBase to be loaded!.");
        manager=this;
    }
    private ItemStack FUNCTIONAL=new CustomItemStack(Material.BARRIER,"&cSlimefunTools功能性分类");
    private static final ItemStack PARSE_FAILED=new CustomItemStack(Material.STONE,"&c物品解析失败!","&e请检查配置文件");
    public CustomRecipeType VANILLA_CRAFT_TABLE=new CustomRecipeType(new NamespacedKey("minecraft","crafting_table_sftool"),new CustomItemStack(Material.CRAFTING_TABLE,"&a工作台","","from sfTools"));
    public CustomRecipeType VANILLA_FURNACE=new CustomRecipeType(new NamespacedKey("minecraft","furnace_sftool"),new CustomItemStack(Material.FURNACE,"&a熔炉","","from sfTools"));

    HashSet<String> rtype6x6=new HashSet<>(){{
        add("omc_obsidian_forge");
        add("infinity_forge");
        add("bug_crafter");
    }};
    private boolean is6x6(RecipeType type){
        String name=type.getKey().toString();
        for(String rtype:rtype6x6){
            if(name.endsWith(rtype))return true;
        }
        return false;
    }
    private ItemStack convertIfSf(ItemStack item){
        SlimefunItem sfitem=SlimefunItem.getByItem(item);
        if(sfitem==null){
            return item;
        }else {
            ItemStack stack=sfitem.getItem();
            if(stack instanceof SlimefunItemStack sfstack&&SlimefunUtils.isItemSimilar(stack,sfstack,true,false,true,false)){
                return new SlimefunItemStack(sfstack.getItemId(),item);
            }
            return item;
        }
    }
    private Identifier<SlimefunItem> slimefunItemIdentifier=new Identifier<SlimefunItem>(ItemRegister.class)
            .setIdentifier(SlimefunItem.class)
            .setSetter("item",(item,obj)->{
                ItemStack stack=(ItemStack)obj;
                SlimefunItemStack stack1=new SlimefunItemStack(item.getId(),stack);
                ItemStack stack2= item.getItem();
                Boolean has=(Boolean) ReflectUtils.invokeGetRecursively(stack2, Settings.FIELD,"locked");
                ReflectUtils.setFieldRecursively(stack2,"locked",false);
                AddUtils.copyItem(stack1,stack2);
                if(has!=null){
                    ReflectUtils.setFieldRecursively(stack2,"locked",has);
                }
//                if(!ReflectUtils.invokeSetRecursively(item,"itemStackTemplate",stack1)){
//                    Debug.logger("Failed to override slimefun item Item",item.getId());
//                }
            })
            .setGetter("item",(item)->new ItemStack(item.getItem()))
            .setComparator("item",(r,item1,item2)->SlimefunUtils.isItemSimilar((ItemStack) item1,(ItemStack)item2,true,true,true,false))
            .setType("item",ItemStack.class)
            .setSetter("group",((slimefunItem, o) -> {
                //slimefunItem.getItemGroup().remove(slimefunItem); setItemGroup中已有该操作
                slimefunItem.setItemGroup((ItemGroup)o );
            }))
            .setGetter("group",(SlimefunItem::getItemGroup))
            .setType("group",ItemGroup.class)
            .setSetter("rtype",((slimefunItem, o) -> {
                RecipeType type=(RecipeType)o;
                try{
                    slimefunItem.getRecipeType().unregister(slimefunItem.getRecipe(),slimefunItem.getRecipeOutput());
                }catch (Throwable e){
                    Debug.logger("An error occured while unregistering recipeType,item:",slimefunItem.getId(),"recipeType:",slimefunItem.getRecipeType().getKey());
                }
                slimefunItem.setRecipeType(type);
                if(is6x6(type)){
                    ItemStack[] recipes=slimefunItem.getRecipe();
                    recipes=Arrays.copyOf(recipes,36);
                    if(!ReflectUtils.setFieldRecursively(slimefunItem,SlimefunItem.class,"recipe",recipes)){
                        Debug.logger("Failed to override slimefun item Recipe while setting 6x6 recipe",slimefunItem.getId());
                    }
                }
                try{
                    type.register(slimefunItem.getRecipe(),slimefunItem.getRecipeOutput());
                }catch (Throwable e){
                    Debug.logger("An error occured while registering recipeType,item:",slimefunItem.getId(),"recipeType:",type.getKey());
                }
            }))
            .setGetter("rtype",SlimefunItem::getRecipeType)
            .setType("rtype",RecipeType.class)
            .setSetter("output",((slimefunItem, o) -> {
                try{
                    slimefunItem.getRecipeType().unregister(slimefunItem.getRecipe(),slimefunItem.getRecipeOutput());
                }catch (Throwable e){
                    Debug.logger("An error occured while unregistering output from recipeType,item:",slimefunItem.getId(),"recipeType:",slimefunItem.getRecipeType().getKey());
                }
                slimefunItem.setRecipeOutput(convertIfSf((ItemStack)o) );
                try{
                    slimefunItem.getRecipeType().register(slimefunItem.getRecipe(),slimefunItem.getRecipeOutput());
                }catch (Throwable e){
                    Debug.logger("An error occured while registering output from recipeType,item:",slimefunItem.getId(),"recipeType:",slimefunItem.getRecipeType().getKey());
                }
            }))
            .setGetter("output",(SlimefunItem::getRecipeOutput))
            //比较output和recipe需要比较数量
            .setComparator("output",(r,item1,item2)->
                SlimefunUtils.isItemSimilar((ItemStack) item1,(ItemStack)item2,true,true,true,false)
            )
            .setType("output",ItemStack.class)
            .setSetter("recipe",((slimefunItem, o) -> {
                RecipeType type=slimefunItem.getRecipeType();
                try{
                    type.unregister(slimefunItem.getRecipe(),slimefunItem.getRecipeOutput());
                }catch (Throwable e){
                    Debug.logger("An error occured while unregistering recipe from recipeType,item: ",slimefunItem.getId(),"recipeType:",type.getKey());
                }
                ItemStack[] recipe=(ItemStack[]) o;
                ItemStack[] originRecipe=slimefunItem.getRecipe();
                boolean needResize=false;
                if(is6x6(type)){
                    if(recipe.length<36||originRecipe.length<36){
                        needResize=true;
                        recipe=Arrays.copyOf(recipe,36);
                    }
                }else{
                    if(recipe.length<originRecipe.length){
                        recipe=Arrays.copyOf(recipe,originRecipe.length);
                    }
                }
                if(!needResize){
                    for(int i=originRecipe.length;i<recipe.length;++i){
                        //多出来的其实是null
                        if(recipe[i]!=null){
                            needResize=true;
                        }
                    }
                }
                if(needResize){
                    if(!ReflectUtils.setFieldRecursively(slimefunItem,SlimefunItem.class,"recipe",recipe)){
                        Debug.logger("Failed to override slimefun item Recipe",slimefunItem.getId());
                    }
                }else{
                    //modify recipe without ReflectUtil like simply copy array
                    System.arraycopy(recipe,0,originRecipe,0,originRecipe.length);
                }
                try{
                    type.register(slimefunItem.getRecipe(),slimefunItem.getRecipeOutput());
                }catch (Throwable e){
                    Debug.logger("An error occured while unregistering recipe from recipeType,item:",slimefunItem.getId(),"recipeType:",type.getKey());
                }
            }))
            .setGetter("recipe",(s)->{
                ItemStack[] recipe=s.getRecipe();
                return Arrays.copyOf(recipe,recipe.length);
            } )
            .setComparator("recipe",(r,item1,item2)->{

                ItemStack[] recipe1=(ItemStack[]) item1;
                ItemStack[] recipe2=(ItemStack[]) item2;
                int maxIndex=Math.max(recipe1.length,recipe2.length);
                for(int i=0;i<maxIndex;++i){
                    if(i<recipe1.length&&i<recipe2.length){
                        //比较output和recipe需要比较数量
                        if(!SlimefunUtils.isItemSimilar(recipe1[i],recipe2[i],true,true,true,false)){
                            //不相同 return false
                            return false;
                        }
                    }else if(i<recipe1.length){
                        if(recipe1[i]!=null)return false;
                    }else if(i<recipe2.length){
                        if(recipe2[i]!=null)return false;
                    }
                }
                return true;
            })
            .setType("recipe",ItemStack[].class);
    private Identifier<ItemGroup> itemGroupIdentifier=new Identifier<ItemGroup>(ItemRegister.class)
            .setIdentifier(ItemGroup.class)
            .setGetter("item",CustomItemGroup::getItemGroupIcon)
            .setSetter("item",(group,stack)->{
                if(!CustomItemGroup.setItemGroupIcon(group,(ItemStack) stack)){
                    Debug.logger("Failed to override itemGroup icon item ",group.getKey());
                }
            })
            .setComparator("item",(r,o1,o2)->SlimefunUtils.isItemSimilar((ItemStack) o1,(ItemStack) o2,true,false,true,false))
            .setType("item",ItemStack.class)
            .setGetter("tier",ItemGroup::getTier)
            .setSetter("tier",(v,i)->v.setTier((Integer)i))
            .setType("tier",Integer.class);

    //records of modification
    public HashMap<String, InstanceModifyRecord<SlimefunItem>> itemModifyRecords=new HashMap<>();
    public HashMap<String,InstanceModifyRecord<ItemGroup>> itemGroupModifyRecords=new HashMap<>();
    private NamespacedKey ofNSKey(String namespacedKey){
        String[] re= namespacedKey.split(":");
        if(re.length==2){
            return new NamespacedKey(re[0],re[1]);
        }else return null;
    }
    private static String c(String... str){
        return String.join(".", str);
    }
    private static ItemStack gsf(String id){
        var sf= SlimefunItem.getById(id.toUpperCase());
        if(sf!=null){
            return new ItemStack(sf.getItem());
        }else return null;
    }
    private static ItemStack gmc(String str){
        var mc=Material.getMaterial(str.toUpperCase());
        if(mc!=null){
            return new ItemStack(mc);
        }else return null;
    }
    private static ItemStack gcs(String id){
        return CustomItemBase.getManager().getItem(id);
    }
    private static ItemStack gi(String id){
        if(id.startsWith("slimefun:")){
            return gsf(id.substring("slimefun:".length()));
        }else if(id.startsWith("sf:")){
            return gsf(id.substring("sf:".length()));
        }else if(id.startsWith("minecraft:")){
            return gmc(id.substring("minecraft:".length()));
        }else if(id.startsWith("mc:")){
            return gmc(id.substring("mc:".length()));
        }else if(id.startsWith("custom:")){
            return gcs(id.substring("custom:".length()));
        }else if(id.startsWith("cs:")){
            return gcs(id.substring("cs:".length()));
        }else{
            return Utils.computeTilPresent(id,ItemRegister::gsf,ItemRegister::gmc,ItemRegister::gcs);
        }
    }
    static Pattern re=Pattern.compile("^([0-9]*)(.*)$");
    public static ItemStack git(String id){
        if(id==null||"null".equals(id)){
            return null;
        }
        Matcher info= re.matcher(id);
        int cnt;
        String _id;
        if(info.find()){
            String amount=info.group(1);
            _id=info.group(2);
            try{
                cnt=Integer.parseInt(amount);
            }catch(NumberFormatException e){
                cnt=1;
            }
        }else{
            _id=id;
            cnt=1;
        }
        ItemStack item=gi(_id);
        if(item!=null){
            item.setAmount(cnt);
            return item;

        }else {
            Debug.logger("parse id failed: ",id);
            return PARSE_FAILED.clone();
        }
    }
    public static String dmt(Material material){
        return "minecraft:"+material.toString().toLowerCase();
    }
    private static String dit(ItemStack item){
        if(item==null){
            return "null";
        }
        String id=di(item);
        if(id==null)return null;
        return (item.getAmount()==1?"":String.valueOf(item.getAmount()))+id;
    }
    @Nullable
    private static String di(ItemStack item){
        if(item==null){
            return "null";
        }
        if(!item.hasItemMeta()){
            return dmt(item.getType());
        }else{
            SlimefunItem sfitem=SlimefunItem.getByItem(item);
            if(sfitem!=null&& SlimefunUtils.isItemSimilar(item,sfitem.getItem(),true,false,true,false)){
                return "slimefun:"+sfitem.getId().toLowerCase();
            }else {
                String str=CustomItemBase.getManager().getItemId(item);
                if(str!=null){
                    return "custom:"+str;
                }else {
                    return null;
                }
            }
        }
    }
    @Nonnull
    public static String ditOa(ItemStack item, Supplier<String> defaultName){
        return Utils.orCompute(dit(item),()->{
            String def=defaultName.get();
            CustomItemBase.getManager().addItem(item,def);
            return (item.getAmount()==1?"":String.valueOf(item.getAmount()))+def;
        });
    }
//    private String ditOa(ItemStack item,String defaultName){
//        return Utils.orCompute(dit(item),()->{
//            CustomItemBase.getManager().addItem(item,defaultName);
//            return defaultName;
//        });
//    }
    @Getter
    private DummyItemGroup functionalGroup;
    public void loadConfigs(){
        functionalGroup=new DummyItemGroup(new NamespacedKey("slimefuntools","functional"),FUNCTIONAL.clone(),true,true);
        //these part of
        registeredGroups.put("slimefuntools:functional",functionalGroup);
        this.groups.put("slimefuntools:functional",functionalGroup);
        Set<String> groups=groupConfig.getKeys();

        HashSet<String> postRegisterIcon=new HashSet<>();
        //check if override
        HashSet<String> customGroups = new HashSet<>();
        for(String group:groups){
            if(this.registeredGroups.containsKey(group)){

                Debug.logger("物品组 ",group,": ItemGroup already Exists: ",group);
                continue;
            }
            if(this.groups.containsKey(group)){
                //add to Overrides
                //override groups icon
                postRegisterIcon.add(group);
            }else{
                //create new group
                //and override groups
                NamespacedKey key=ofNSKey(group);
                if(key!=null){
                    try{
                        String type=groupConfig.getString(c(group,"type"));
                        boolean hide=groupConfig.getBoolean(c(group,"hide"));
                        String item=groupConfig.getString(c(group,"item"));
                        switch (type){
                            case "custom":
                                customGroups.add(group);
                                postRegisterIcon.add(group);
                                CustomItemGroup cgroup=new CustomItemGroup(key,FUNCTIONAL.clone(),hide);
                                this.registeredGroups.put(group,cgroup);
                                this.groups.put(group,cgroup);
                                break;
                            case "normal":
                                postRegisterIcon.add(group);
                                registeredGroups.put(group,new DummyItemGroup(key,FUNCTIONAL.clone(),hide   ,true));
                                this.groups.put(group,registeredGroups.get(group));
                                break;
                            default:
                                Debug.logger("物品组 ",group,": Unknown group type: ",group);
                        }
                    }catch (Throwable e){
                        Debug.logger(e);
                    }
                }else {
                    Debug.logger("物品组 ",group,": Unknown namespacedKey format: ",group);
                }
            }
        }
        Set<String> items=itemConfig.getKeys();
        //override Recipes
        HashMap<String,List<String>> recipeOverrides=new HashMap<>();
        for(String item:items){
            SlimefunItem sfitem=SlimefunItem.getById(item);
            boolean override=sfitem!=null;
            if(!override&&registeredItems.containsKey(item)){
                Debug.logger("物品 ",item,": item ID already registered before: ",item);
                continue;
            }
            boolean hasItemGroup=itemConfig.contains(c(item,"group"));
            String itemGroup=itemConfig.getString(c(item,"group"));
            boolean hasItemId=itemConfig.contains(c(item,"item"));
            String itemId=itemConfig.getString(c(item,"item"));
            boolean hasRecipe=itemConfig.contains(c(item,"recipe"));
            List<String> recipe= Utils.orDefault(itemConfig.getStringList(c(item,"recipe")),new ArrayList<>());
            boolean hasRecipeType=itemConfig.contains(c(item,"rtype"));

            String recipeType=itemConfig.getString(c(item,"rtype"));

            String output=itemConfig.contains(c(item,"output"))? itemConfig.getString(c(item,"output")):"__default__";
            ItemStack stack=git(itemId);
            if(stack==null&&hasItemId){
                Debug.logger("物品 ",item,": item id shouldn't be null: ",itemId);
                continue;
            }
            ItemGroup group=this.groups.get(itemGroup);
            if(group==null&&hasItemGroup){
                Debug.logger("物品 ",item,": invalid item group: ",itemGroup);
                continue;
            }
            RecipeType recipeType1=this.recipes.get(recipeType);
            if(recipeType1==null&&hasRecipeType){
                Debug.logger("物品 ",item,": invalid recipe type: ",recipeType);
                continue;
            }
            ItemStack outputItem=null;
            if(!"__default__".equals(output)){
                outputItem=git(output);
                if(outputItem==null){
                    Debug.logger("物品 ",item,": invalid outputItem: ",output);
                }
            }
            if(override){
                try{
                    overrideSlimefunItem(sfitem,stack,group,recipeType1,outputItem,SaveMethod.NONE);
                }catch (Throwable e){
                    Debug.logger("物品 ",item,": error in item override: ");
                    Debug.logger(e);
                }
            }else{
                if(hasItemGroup&&hasRecipeType&&hasItemId){
                    CustomSlimefunItem newSfItem=registerSlimefunItem(item,stack,group,recipeType1,outputItem,
                            //TODO add item Type
                            "default");
                    if(newSfItem!=null){
                        registeredItems.put(item,newSfItem );
                        this.items.put(item,newSfItem);
                    }

                }else {
                    Debug.logger("物品 ",item,": lack arguments ");
                }
            }
            if(!override||hasRecipe){
                recipeOverrides.put(item,recipe);
            }
        }
        //itemGroup注册
        var iter=postRegisterIcon.iterator();
        while(iter.hasNext()){
            String entry=iter.next();
            String item=groupConfig.getString(c(entry,"item"));
            ItemStack icon=item==null?null:git(groupConfig.getString(c(entry,"item")));
            Integer tier= CommandUtils.parseIntegerOrDefault( groupConfig.getString(c(entry,"tier")),null);
            if(icon==null&&item!=null){
                Debug.logger("物品组 ",entry,": Unknown icon id ",item);
                continue;
            }
            ItemGroup group= this.groups.get(entry);
            try{
                overrideItemGroups(group,icon,tier,SaveMethod.NONE);
                //some auto registry
                if(!group.isRegistered()){
                    group.register((SlimefunAddon) plugin);
                }
            }catch (Throwable e){
                Debug.logger("物品组 ",entry,": error in registering group");
                Debug.logger(e);
            }
        }
        //recipes覆写
        for(Map.Entry<String,List<String>> override:recipeOverrides.entrySet()){
            SlimefunItem sfitem=SlimefunItem.getById(override.getKey());
            if(sfitem==null){
                Debug.logger("配方覆写 ",override.getKey(),": Unknown item id ",override.getKey());
                continue;
            }
            ItemStack[] recipe=override.getValue().stream().map(ItemRegister::git).toArray(ItemStack[]::new);
            try{
                overrideSlimefunItemRecipe(sfitem,recipe,SaveMethod.NONE);
               }catch (Throwable e){
                Debug.logger("配方覆写 ",override.getKey(),": error in override recipe ");
                Debug.logger(e);
            }
        }
        //创建custom物品组
        for(String cgroupId:customGroups){
            NamespacedKey key=ofNSKey(cgroupId);
            if(key==null){
                Debug.logger("物品组 ",cgroupId,": Unknown namespacedKey format: ",cgroupId);
                continue;
            }
            try{
                //registered
                CustomItemGroup group=(CustomItemGroup) this.registeredGroups.get(cgroupId);
                //load menu loader
                List<String> pattern=groupConfig.getStringList(c(cgroupId,"preset","pattern"));
                HashSet<Integer> back=new HashSet<>();
                HashSet<Integer> search=new HashSet<>();
                int prev=-1;
                int next=-1;
                HashMap<Character,ItemStack> patternMap=new HashMap<>();
                for(String patternId:groupConfig.getKeys(c(cgroupId,"preset"))){
                    if(patternId.length()==1){
                        String item=groupConfig.getString(c(cgroupId,"preset",patternId));
                        patternMap.put(patternId.charAt(0),git(item));
                    }
                }
                assert pattern.size()<=6;
                List<Integer> contents=new ArrayList<>();
                int line=0;
                CustomMenuGroup customMenu= new CustomMenuGroup(AddUtils.resolveColor(this.groupConfig.getString(c(cgroupId,"title"))),9*pattern.size(),1).enableOverrides();
                for(String patternLine:pattern){
                    assert patternLine.length()==9;
                    for(int i=0;i<9;++i){
                        char c=patternLine.charAt(i);
                        switch(c){
                            case 'B': back.add(9*line+i); break;
                            case 'S': search.add(9*line+i); break;
                            case 'P': prev=9*line+i; break;
                            case 'N': next=9*line+i; break;
                            default:
                                ItemStack stack=patternMap.get(c);
                                if(stack==null){
                                    contents.add(9*line+i);
                                }else {
                                    customMenu.setOverrideItem(9*line+i,stack);
                                    customMenu.setOverrideHandler(9*line+i, CustomMenuGroup.CustomMenuClickHandler.ofEmpty());
                                }
                                break;
                        }
                    }
                    line++;
                }
                if(prev>=0&&next>=0){
                    customMenu.setPageChangeSlots(prev,next);
                }
                customMenu.enableContentPlace(contents.stream().mapToInt(Integer::intValue).toArray());

                HashMap<Integer,ItemGroup> addItemGroups=new HashMap<>();
                HashMap<Integer,SlimefunItem> addItemResearches=new HashMap<>();
                for(String index:this.groupConfig.getKeys(c(cgroupId,"contents"))){
                    int i;
                    try{
                        i=Integer.parseInt(index);
                    }catch (NumberFormatException e){
                        continue;
                    }
                    assert i>=0;
                    String id=this.groupConfig.getString(c(cgroupId,"contents",index));
                    if(this.groups.containsKey(id)){
                        addItemGroups.put(i,this.groups.get(id));
                        customMenu.addItem(i,null);
                    }else if(SlimefunItem.getById(id)!=null){
                        addItemResearches.put(i,SlimefunItem.getById(id));
                        customMenu.addItem(i,null);
                    }
                    else {
                        customMenu.addItem(i,git(id),CustomMenuGroup.CustomMenuClickHandler.ofEmpty());
                    }
                }
                group.setBackButton(back);
                group.setSearchButton(search);
                group.setLoader(customMenu,addItemGroups,addItemResearches);
            }catch (Throwable e) {
                Debug.logger("自定义物品组: ",cgroupId,": error in config file ");
                Debug.logger(e);
            }
        }
        Debug.logger("物品和物品组注册成功!");
    }

    private void overrideItemGroups(ItemGroup group,ItemStack icon,Integer tier,SaveMethod save){
        InstanceModifyRecord<ItemGroup> record=itemGroupModifyRecords.computeIfAbsent(group.getKey().toString(),(s)->
            new InstanceModifyRecord<ItemGroup>(group,this.groupConfig,(g)->g.getKey().toString(),ItemGroup.class,false));
        if(icon!=null&&record.checkDifference("item",icon)){
            record.executeModification("item",icon);
            record.executeDataSave("item",save,()->"_gp_"+group.getKey().getNamespace()+"_"+group.getKey().getKey()+"_item_");
        }
        if(tier!=null&&record.checkDifference("tier",tier)){
            record.executeModification("tier",tier);
            record.executeDataSave("tier",save);
        }

    }
    private void createSlimefunItemInstance(String id,ItemStack stack,ItemGroup group,RecipeType rtype,ItemStack[] recipes,ItemStack output,SaveMethod save){
        //todo add itemType
        SlimefunItem instance=registerSlimefunItem(id,stack,group,rtype,output,"default");
        this.registeredItems.put(id,instance);
        this.registerItem(instance);
        InstanceModifyRecord<SlimefunItem> record= new InstanceModifyRecord<>(instance,this.itemConfig,SlimefunItem::getId,SlimefunItem.class,false);
        itemModifyRecords.put(id,record);
        record.executeDataSave("item",save!=SaveMethod.NONE?SaveMethod.USE_CS:SaveMethod.NONE,()->"_sf_" +
                ""+instance.getId()+"_item_");
        record.executeDataSave("group",save);
        record.executeDataSave("rtype",save);
        //配方需要统一设置 防止出问题
        record.executeModification("recipe",recipes);
        record.executeDataSave("recipe",save,()->"_sf_"+instance.getId()+"_recipe_");
        ((CustomSlimefunItem)instance).registerItem((SlimefunAddon) plugin);
        if(output!=null){
            record.executeDataSave("output",save,()->"_sf_"+instance.getId()+"_output_");
        }
    }
    private void overrideSlimefunItem(SlimefunItem item,ItemStack stack,ItemGroup group,RecipeType recipeType,ItemStack output,SaveMethod save){
        InstanceModifyRecord<SlimefunItem> record= itemModifyRecords.computeIfAbsent(item.getId(),(s)->new InstanceModifyRecord<>(item,this.itemConfig,SlimefunItem::getId,SlimefunItem.class,false));
        if(stack!=null&&record.checkDifference("item",stack)){
            record.executeModification("item",stack);
            //这里需要使用cs模式存储,因为修改了sf实例之后 100%保存会解析到sf ID
            record.executeDataSave("item",save!=SaveMethod.NONE?SaveMethod.USE_CS:SaveMethod.NONE,()->"_sf_"+item.getId()+"_item_");
        }
        if(group!=null&&record.checkDifference("group",group)){
            record.executeModification("group",group);
            record.executeDataSave("group",save);
        }
        if(recipeType!=null&&record.checkDifference("rtype",recipeType)){
            record.executeModification("rtype",recipeType);
            record.executeDataSave("rtype",save);
        }
        //output is nullable
        //you can't set null output ,but we will let the comparator tell you
        if(output!=null&&record.checkDifference("output",output)){
            record.executeModification("output",output);
            record.executeDataSave("output",save,()->"_sf_"+item.getId()+"_output_");
        }
    }
    private void overrideSlimefunItemRecipe(SlimefunItem item,ItemStack[] recipe,SaveMethod save){
        InstanceModifyRecord<SlimefunItem> record= itemModifyRecords.computeIfAbsent(item.getId(),(s)->new InstanceModifyRecord<>(item,this.itemConfig,SlimefunItem::getId,SlimefunItem.class,false));
        if(recipe!=null&&record.checkDifference("recipe",recipe)){
            record.executeModification("recipe",recipe);
            record.executeDataSave("recipe",save,()->"_sf_"+item.getId()+"_recipe_");
        }
    }

    private void initStats(){
        for(ItemGroup group:Slimefun.getRegistry().getAllItemGroups()){
            registerItemGroup(group);
        }
        try{
            Field[] recipes=RecipeType.class.getDeclaredFields();
            for(Field f:recipes){
                try{
                    f.setAccessible(true);
                    if(f.getType() .isAssignableFrom(RecipeType.class)){
                        RecipeType recipe=(RecipeType)f.get(null);
                        this.recipes.put(recipe.getKey().toString(),recipe);
                    }}catch (Throwable e){}
            }
        }catch (Throwable e){
            e.printStackTrace();
        }
        for(SlimefunItem item: Slimefun.getRegistry().getAllSlimefunItems()){
            items.put(item.getId(),item);
            RecipeType recipeType=item.getRecipeType();
            registerRecipeType(recipeType);
        }
    }
    private void registerRecipeType(RecipeType type){
        if(!recipes.containsValue(type)){
            recipes.put(type.getKey().toString(), type );
        }
    }
    private void registerItemGroup(ItemGroup group){
        if(!groups.containsValue(group)){
            groups.put(group.getKey().toString(), group);
        }
    }
    private void registerItem(SlimefunItem sfitem){
        if(!items.containsKey(sfitem.getId())){
            items.put(sfitem.getId(),sfitem);
        }
    }
    public ItemRegister init(Plugin plugin, String... savePath){

        Debug.logger("Enabling Item Register");
        this.addToRegistry();
        this.plugin=plugin;
        this.itemPath=savePath[0];
        this.itemConfig= ConfigLoader.loadExternalConfig(itemPath);
        this.groupPath=savePath[1];
        this.groupConfig= ConfigLoader.loadExternalConfig(groupPath);
        //init fuunctional parts which needs configs file
        this.initStats();
        //init functional parts
        registerRecipeType(VANILLA_CRAFT_TABLE);
        registerRecipeType(VANILLA_FURNACE);
        //load configs
        loadConfigs();
        //load commands
        registerFunctional();
        return this;
    }
    public ItemRegister reload(){
        deconstruct();
        return init(plugin,itemPath,groupPath);
    }
    public void deconstruct(){
        this.removeFromRegistry();
        unregisterFunctional();
        for(InstanceModifyRecord<SlimefunItem> record:this.itemModifyRecords.values()){
            record.executeUndoAllModifications();
        }
        for(InstanceModifyRecord<ItemGroup> record:this.itemGroupModifyRecords.values()){
            record.executeUndoAllModifications();
        }
        for(Map.Entry<String,SlimefunItem> entry:registeredItems.entrySet()){
            unregisterSlimefunItem(entry.getValue());
        }
        for(Map.Entry<String,ItemGroup> entry:registeredGroups.entrySet()){
           disableItemGroup(entry.getValue());
        }
        //clear stats
        this.groups.clear();
        this.items.clear();
        this.recipes.clear();
        //saveConfigs();
        //unload
        DynamicFunctional.onModuleManagerDisable(ItemRegister.class);
        //how about sending Events?
        Debug.logger("Disabling Item Register");
    }
    public void saveConfigs(){
        this.itemConfig.save();
        this.groupConfig.save();
    }

    /**
     only execute the origin registry,do not adding to ItemRegister map
     * @return
     */
    public CustomSlimefunItem registerSlimefunItem(String item,ItemStack stack,ItemGroup group,RecipeType rtype,ItemStack output,String itemType){
        try{
            SlimefunItemStack newItem=new SlimefunItemStack(item,stack);
            //setOutput: if not null, set Output
            CustomSlimefunItem newSfItem=new CustomSlimefunItem(group,newItem,rtype,new ItemStack[]{}).setOutput(output).registerItem((SlimefunAddon) plugin);
            plugin.getServer().getPluginManager().callEvent(new CustomSlimefunItemRegisterEvent(newSfItem));
            return newSfItem;
        }catch (Throwable e){
            Debug.logger("物品 ",item,": error in item register: ");
            Debug.logger(e);
            return null;
        }
    }
    //todo add unregister Event
    /**
     only execute the origin unregistry,do not removing from ItemRegister map
     * @return
     */
    public void unregisterSlimefunItem(SlimefunItem item){
        Iterator<ItemHandler> handlers= item.getHandlers().iterator();
        while(handlers.hasNext()){
            ItemHandler handler=handlers.next();
            try{
                var re=Slimefun.getRegistry().getGlobalItemHandlers().get(handler.getIdentifier());
                if(re!=null){
                    re.remove(handler);
                }
            }catch (Throwable e){
                Debug.logger(e);
            }
        }
        item.disable();
        Slimefun.getRegistry().getAllSlimefunItems().remove(item);
        Slimefun.getRegistry().getSlimefunItemIds().remove(item.getId());
        Slimefun.getRegistry().getDisabledSlimefunItems().remove(item);
        if(item instanceof GEOResource geo){
            Slimefun.getRegistry().getGEOResources().remove(geo.getKey());
        }
        item.getItemGroup().remove(item);
    }
    public void disableItemGroup(ItemGroup group){
        Slimefun.getRegistry().getAllItemGroups().remove(group);
        List<ItemGroup> categories = Slimefun.getRegistry().getAllItemGroups();
        Collections.sort(categories, Comparator.comparingInt(ItemGroup::getTier));
    }
    public void deleteSlimefunItemModification(String id){
        if(this.itemModifyRecords.containsKey(id)){
            InstanceModifyRecord<SlimefunItem> record=this.itemModifyRecords.remove(id);
            record.executeAllDataDelete();
        }
        if(registeredItems.containsKey(id)){
            unregisterSlimefunItem(registeredItems.remove(id));
        }
    }
    public void deleteSlimefunItemConfig(String id){
        deleteSlimefunItemModification(id);
        this.itemConfig.setValue(id,null);
        this.itemConfig.save();
    }
    private ItemRegister registerFunctional(){
        Validate.isTrue(!registered, "ItemRegister functional have already been registered!");
        plugin.getServer().getPluginCommand("itemreg").setExecutor(this);
        plugin.getServer().getPluginCommand("itemreg").setTabCompleter(this);
        this.registered=true;
        return this;
    }
    private ItemRegister unregisterFunctional(){
        Validate.isTrue(registered, "ItemRegister functional havem't been unregistered!");
        plugin.getServer().getPluginCommand("itemreg").setExecutor(null);
        plugin.getServer().getPluginCommand("itemreg").setTabCompleter(null);
        this.registered=false;
        return this;
    }
    private List<String> statsType=new ArrayList<>(){{
        add("rtype");
        add("group");
        add("sfitem");
    }};
    private void openStatsGui(Player player,String type,String filter,int page){
        if(page<=0){
            AddUtils.sendMessage(player,"&c无效的页数!");
            return;
        }
        CustomMenuGroup menuGroup= new CustomMenuGroup(AddUtils.resolveColor("&a粘液信息统计"),54,1 )
                .enableContentPlace(IntStream.range(0,45).toArray())
                .setPageChangeSlots(46,52)
                .enableOverrides()
                .setOverrideItem(47,ChestMenuUtils.getBackground(), CustomMenuGroup.CustomMenuClickHandler.ofEmpty())
                .setOverrideItem(51,ChestMenuUtils.getBackground(), CustomMenuGroup.CustomMenuClickHandler.ofEmpty());
        if(filter==null){
            menuGroup.setOverrideItem(45, MenuUtils.getSearchButton(),(cm)->((player1, i, itemStack, clickAction) -> {
                player1.closeInventory();
                ChatUtils.awaitInput(player1,(string -> {
                    openStatsGui(player1,type,string,1);
                }));
                return false;
            }))
            .setOverrideItem(53, MenuUtils.getSearchButton(), (cm)->((player1, i, itemStack, clickAction) -> {
                player1.closeInventory();
                ChatUtils.awaitInput(player1,(string -> {
                    openStatsGui(player1,type,string,1);
                }));
                return false;
            }));
        }else{
            menuGroup.setOverrideItem(45,MenuUtils.getSearchOffButton(),(cm)->((player1, i, itemStack, clickAction) -> {openStatsGui(player1,type,null,1);return false;}));
            menuGroup.setOverrideItem(53,MenuUtils.getSearchOffButton(),(cm)->((player1, i, itemStack, clickAction) -> {openStatsGui(player1,type,null,1);return false;}));
        }
        int index=48;
        for (String statsType:statsType){
            ItemStack stack=new CustomItemStack(Material.RED_STAINED_GLASS_PANE,"&a统计项目: &f"+statsType,"&7显示所有可用值","&7当你找不到可用值时,请考虑在plugin.yml中加入soft depends");
            if(type.equals(statsType)){
                stack.setType(Material.GREEN_STAINED_GLASS_PANE);
                menuGroup.setOverrideItem(index,stack, CustomMenuGroup.CustomMenuClickHandler.ofEmpty());
            }else {
                menuGroup.setOverrideItem(index,stack, (cm)->((player1, i, itemStack, clickAction) -> {
                    openStatsGui(player1,statsType,null,1);
                    return false;
                }));
            }
            index+=1;
        }
        var re=getDisplayed(type,filter);
        menuGroup.resetItems(re.getFirstValue());
        menuGroup.resetHandlers(re.getSecondValue());
        final int openpage=Math.min(menuGroup.getPages(),page);
        menuGroup.openPage(player,openpage);
    }
    private HashMap<UUID,CustomMenu> PLAYER_EDITORGUI3x3=new HashMap<>();
    private HashMap<UUID,CustomMenu> PLAYER_EDITORGUI6X6=new HashMap<>();
    private interface SfItemArgumentConsumer{
        public void consume(@Nonnull String id, @Nonnull ItemStack stack, @Nonnull  ItemGroup group, @Nonnull  RecipeType rtype, @Nonnull  ItemStack[] recipe, @Nullable ItemStack output);
    }
    private EditorGuiPreset PRESET3X3=new EditorGuiPreset(45,new int[]{10,11,12,19,20,21,28,29,30},0,13,31,14,32,16,34,17,35,25,23,26);
    private EditorGuiPreset PRESET6x6=new EditorGuiPreset(54,new int[]{
            0,1,2,3,4,5,9,10,11,12,13,14,18,19,20,21,22,23,27,28,29,30,31,32,36,37,38,39,40,41,45,46,47,48,49,50
    },8,15,17,16,34,24,33,26,35,52,7,43);
    private class EditorGuiPreset implements IntFunction<ChestMenu> {
        public EditorGuiPreset(int sizePage,int[] recipeSlots,int dataSlot,int recipeTypeSlot,int itemGroupSlot,int sfItemSlot,int outputSlot,int saveSF,int saveSC,int createSF,int createSC,int clear,int loadSlot,int displayAllSlot){
            this.sizePage=sizePage;
            this.dataSlot=dataSlot;
            this.recipeSlots=recipeSlots;
            this.recipeTypeSlot=recipeTypeSlot;
            this.itemGroupSlot=itemGroupSlot;
            this.sfItemSlot=sfItemSlot;
            this.outputSlot=outputSlot;
            this.saveSF=saveSF;
            this.saveSC=saveSC;
            this.createSF=createSF;
            this.createSC=createSC;
            this.clear=clear;
            this.loadSlot=loadSlot;
            this.displayAllSlot=displayAllSlot;
        }
        @Getter
        int dataSlot;
        int displayAllSlot;
         int sizePage=45;
         int[] recipeSlots;
         int recipeTypeSlot=13;
         int itemGroupSlot=31;
         int sfItemSlot=14;
         int outputSlot=32;
         int saveSF=16;
         int saveSC=34;
         int createSF=17;
         int createSC=35;
         int clear=25;
         int loadSlot=23;
        public DataContainer getDataHolder(ChestMenu menu){
            ChestMenu.MenuClickHandler handler=menu.getMenuClickHandler(dataSlot);
            if(handler instanceof DataContainer dh){
                return dh;
            }else {
                DataContainer dh=new DataContainer() {
                    Object recipeType;
                    Object itemGroup;
                    String id;
                    public Object getObject(int val) {return val==0? recipeType:itemGroup;}
                    public void setObject(int val, Object val2) {
                        if(val==0) recipeType=val2;else itemGroup=val2;
                    }
                    public void setString(int val,String val2){this.id=val2;}
                    public String getString(int val){return this.id;}
                    public boolean onClick(Player player, int i, ItemStack itemStack, ClickAction clickAction) {return false;}
                };
                menu.addMenuClickHandler(dataSlot,dh );
                return dh;
            }
        }
        public void provideSfInstanceInfoOrFail(ChestMenu menu,SfItemArgumentConsumer consumer,Consumer<String> outputError){
            DataContainer dh=getDataHolder(menu);
            String id=dh.getString(0);
            if(id==null){
                outputError.accept("&c请先点击加载键加载SF实例");
                return ;
            }
            ItemStack[] recipe= Arrays.stream(recipeSlots).mapToObj(menu::getItemInSlot).map(item__->Utils.computeIfPresent(item__,ItemStack::new)).toArray(ItemStack[]::new);
            ItemStack output=menu.getItemInSlot(outputSlot);
            ItemStack sfItem=menu.getItemInSlot(sfItemSlot);
            if(sfItem==null){
                outputError.accept("&c粘液物品槽位不能为空!");
                return;
            }
            RecipeType type=(RecipeType) dh.getObject(0);
            if(type==null){
                outputError.accept("&c配方类型为空,请先选择配方类型");
                return;
            }
            ItemGroup group=(ItemGroup) dh.getObject(1);
            if(group==null){
                outputError.accept("&c物品组为空,请先选择配方类型");
                return;
            }else if(group instanceof FlexItemGroup ){
                outputError.accept("&c这是一个FlexItemGroup,你不能选择它来设置物品组!");
                return ;
            }
            consumer.consume(id,sfItem,group,type,recipe,output);
        }
        public void setRecipeType(ChestMenu menu,RecipeType type){
            getDataHolder(menu).setObject(0,type);
            menu.replaceExistingItem(recipeTypeSlot,AddUtils.addLore(rtypeIconGen.apply(type),"&a点击切换配方类型"));
        }
        public void setItemGroup(ChestMenu  menu,ItemGroup group){
            getDataHolder(menu).setObject(1,group);
            menu.replaceExistingItem(itemGroupSlot,AddUtils.addLore(itemGroupIconGen.apply(group),"&a点击切换物品组"));
        }
        public void setId(ChestMenu menu,String id){
            getDataHolder(menu).setString(0,id);
            menu.replaceExistingItem(loadSlot,new CustomItemStack(Material.BEACON,"&a点击重新加载当前SF物品",
                    "&a当前加载物品: &7"+id,"&7将物品放在本按钮上方的槽位,作为加载和修改对象","","&e下方槽位为配方输出物显示槽"));
        }
        public void loadSlimefunItem(ChestMenu menu,SlimefunItem item){
            setId(menu,item.getId());
            setRecipeType(menu,item.getRecipeType());
            setItemGroup(menu,item.getItemGroup());
            menu.replaceExistingItem(sfItemSlot,item.getItem());
            menu.replaceExistingItem(outputSlot,item.getRecipeOutput());
            int len=item.getRecipe().length;
            for(int j=0;j<Math.min(len,this.recipeSlots.length);++j){
                menu.replaceExistingItem(recipeSlots[j],item.getRecipe()[j]);
            }
        }
        public ChestMenu apply(int __i){
            ChestMenu menu= new ChestMenu(AddUtils.resolveColor("&a懒鬼型配方编辑器"));
            menu.setPlayerInventoryClickable(true);
            menu.setEmptySlotsClickable(true);
            IntStream.range(0,this.sizePage).forEach((j)->menu.addItem(j,ChestMenuUtils.getBackground(),ChestMenuUtils.getEmptyClickHandler()));

            for(int i:recipeSlots){
                menu.replaceExistingItem(i,null);
                menu.addMenuClickHandler(i,null);
            }
            menu.replaceExistingItem(sfItemSlot,null);
            menu.addMenuClickHandler(sfItemSlot,null);
            menu.replaceExistingItem(outputSlot,null);
            menu.addMenuClickHandler(outputSlot,null);
            getDataHolder(menu);
            menu.replaceExistingItem(recipeTypeSlot,new CustomItemStack(Material.KNOWLEDGE_BOOK,"&a点击选择配方类型"));
            menu.addMenuClickHandler(recipeTypeSlot,((player, i, itemStack, clickAction) -> {
                MenuUtils.openSelectMenu(player,1,null,recipes,(c,player1)->{
                    getDataHolder(menu).setObject(0,c);
                    menu.replaceExistingItem(recipeTypeSlot,AddUtils.addLore(rtypeIconGen.apply(c),"&a点击切换配方类型"));
                    menu.open(player1);
                },(c,player1)->AddUtils.sendMessage(player1,"&c请不要使用shift点击"),rtypeIconGen,(cm,p)->menu.open(p));
                return false;
            }));
            menu.replaceExistingItem(itemGroupSlot,new CustomItemStack(Material.STRUCTURE_VOID,"&a点击选择物品组"));
            menu.addMenuClickHandler(itemGroupSlot,((player, i, itemStack, clickAction) -> {
                MenuUtils.openSelectMenu(player,1,null,groups,(c,player1)->{
                    getDataHolder(menu).setObject(1,c);
                    menu.replaceExistingItem(itemGroupSlot,AddUtils.addLore(itemGroupIconGen.apply(c),"&a点击切换物品组"));
                    menu.open(player1);
                },(c,player1)->AddUtils.sendMessage(player1,"&c请不要使用shift点击"),itemGroupIconGen,(cm,p)->menu.open(p));
                return false;
            }));
            menu.replaceExistingItem(loadSlot,new CustomItemStack(Material.BEACON,"&a点击重新加载当前SF物品",
                    "","&7将物品放在本按钮上方的槽位,作为加载和修改对象","","&e下方槽位为配方输出物显示槽"));
            menu.addMenuClickHandler(loadSlot,((player, i, itemStack, clickAction) -> {
                itemStack =menu.getItemInSlot(sfItemSlot);
                if(itemStack==null||itemStack.getType().isAir()){
                    AddUtils.sendMessage(player,"&c槽位为空!");
                    return false;
                }
                SlimefunItem item=null;
                ItemStack stackToBeCompared=new ItemStack(itemStack);
                item=SlimefunItem.getByItem(stackToBeCompared);
                if(item==null){
                    for(SlimefunItem instances:Slimefun.getRegistry().getAllSlimefunItems()){
                        ItemStack insItem=new ItemStack(instances.getItem());
                        if(stackToBeCompared.isSimilar(insItem)){
                            item=instances;
                            break   ;
                        }
                    }
                }
                if(item==null){
                    AddUtils.sendMessage(player,"&c不是有效的粘液物品");
                    return false;
                }
                AddUtils.sendMessage(player,"&a成功加载粘液实例!");
                loadSlimefunItem(menu,item);
                return false;
            }));
            menu.replaceExistingItem(saveSF,new CustomItemStack(Material.BOOK,"&a保存数据到配置文件","会尽可能的解析物品为id","&7会将粘液物品存储为粘液id","&7会尝试解析为物品库的id","&7推荐的模式"));
            menu.addMenuClickHandler(saveSF,((player, i, itemStack, clickAction) -> {
                provideSfInstanceInfoOrFail(menu,((id, stack, group, rtype, recipe, output) -> {
                    SlimefunItem item=SlimefunItem.getById(id);
                    if(item!=null){
                        overrideSlimefunItem(item,stack,group,rtype,output,SaveMethod.USE_ID);
                        overrideSlimefunItemRecipe(item,recipe,SaveMethod.USE_ID);
                        AddUtils.sendMessage(player,"&a覆写SF物品数据成功");
                    }else{
                        AddUtils.sendMessage(player,"&c这不是有效的SF ID,请重新载入粘液物品数据");
                    }
                }),(s->AddUtils.sendMessage(player,s)));
                return false;
            }));
            menu.replaceExistingItem(saveSC,new CustomItemStack(Material.BOOK,"&c强制保存模式","将所有带nbt的物品创建物品库项目保存","&7会生成大量配置文件","&7不推荐的模式"));
            menu.addMenuClickHandler(saveSC,((player, i, itemStack, clickAction) -> {
                provideSfInstanceInfoOrFail(menu,((id, stack, group, rtype, recipe, output) -> {
                    SlimefunItem item=SlimefunItem.getById(id);
                    if(item!=null){
                        overrideSlimefunItem(item,stack,group,rtype,output,SaveMethod.USE_CS);
                        overrideSlimefunItemRecipe(item,recipe,SaveMethod.USE_CS);
                        AddUtils.sendMessage(player,"&a覆写SF物品数据成功");
                    }else{
                        AddUtils.sendMessage(player,"&c这不是有效的SF ID,请重新载入粘液物品数据");
                    }
                }),(s->AddUtils.sendMessage(player,s)));
                return false;
            }));
            menu.addMenuClickHandler(createSF,((player, i, itemStack, clickAction) -> {
                player.closeInventory();
                AddUtils.sendMessage(player,"&a输入需要创建的物品ID");
                ChatUtils.awaitInput(player,(string -> {
                    menu.open(player);
                    final String Ustring=string.toUpperCase();
                    setId(menu,Ustring);
                    provideSfInstanceInfoOrFail(menu,((id, stack, group, rtype, recipe, output) -> {
                        createSlimefunItemInstance(id,stack,group,rtype,recipe,output,SaveMethod.USE_ID);
                        AddUtils.sendMessage(player,"&a成功创建物品! ID: &f"+Ustring);
                    }),(s->AddUtils.sendMessage(player,s)));
                }));
                return false;
            }));
            menu.replaceExistingItem(createSF,new CustomItemStack(Material.ENCHANTED_BOOK,"&a创建新的sf实例","在物品槽放入目标物品,并设置必要参数","","&7会将粘液物品存储为粘液id","&7会尝试解析为物品库的id","&7推荐的模式"));
            menu.addMenuClickHandler(createSC,((player, i, itemStack, clickAction) -> {
                player.closeInventory();
                AddUtils.sendMessage(player,"&a输入需要创建的物品ID");
                ChatUtils.awaitInput(player,(string -> {
                    menu.open(player);
                    final String Ustring=string.toUpperCase();
                    setId(menu,Ustring);
                    provideSfInstanceInfoOrFail(menu,((id, stack, group, rtype, recipe, output) -> {
                        createSlimefunItemInstance(id,stack,group,rtype,recipe,output,SaveMethod.USE_CS);
                        AddUtils.sendMessage(player,"&a成功创建物品! ID: &f"+Ustring);
                    }),(s->AddUtils.sendMessage(player,s)));
                }));
                return false;
            }));
            menu.replaceExistingItem(createSC,new CustomItemStack(Material.ENCHANTED_BOOK,"&a创建新的sf实例","在物品槽放入目标物品,并设置必要参数","","&c采取强制保存模式","&c将所有带nbt的物品创建物品库项目保存","&7会生成大量配置文件","&7不推荐的模式"));
            ItemStack clearItem=new CustomItemStack(Material.BARRIER,"&c清除该物品的全部修改","&c并删除配置文件相关内容","&c谨慎点击!");
            ItemStack clearTwice=AddUtils.addLore( new CustomItemStack(Material.BARRIER,"&c清除该物品的全部更改","&e请在5秒内二次确认!","&e请在五秒内二次确认"));

            menu.replaceExistingItem(clear,clearItem);
            final AtomicBoolean pendingClear=new AtomicBoolean(false);
            menu.addMenuClickHandler(clear,((player, i, itemStack, clickAction) -> {
                if(!pendingClear.get()){
                    pendingClear.set(true);
                    menu.replaceExistingItem(clear,clearTwice);
                    AddUtils.sendMessage(player,"&e你点击了清除设置按钮!");
                    AddUtils.sendMessage(player,"&e请在5秒内二次确认你的选择!");
                    AddUtils.sendMessage(player,"&e五秒过后将自动取消清除");
                    SlimefunTools.runSyncLater(()->{pendingClear.set(false);menu.replaceExistingItem(clear,clearItem);AddUtils.sendMessage(player,"&a已取消清除物品的全部更改");},100);
                    return false;
                }else {
                    pendingClear.set(false);
                    menu.replaceExistingItem(clear,clearItem);
                    DataContainer dh=getDataHolder(menu);
                    String id=dh.getString(0);
                    if(id==null){
                        AddUtils.sendMessage(player,"&c请先点击加载键加载SF实例");
                        return false;
                    }
                    deleteSlimefunItemConfig(id);
                    AddUtils.sendMessage(player,"&c成功撤销所有改动!");
                    return false;
                }
            }));
            menu.replaceExistingItem(displayAllSlot,new CustomItemStack(Material.REPEATING_COMMAND_BLOCK,"&c预览全部已修改物品","&7点击打开预览界面"));
            menu.addMenuClickHandler(displayAllSlot,(((player, i, itemStack, clickAction) -> {
                openDisplayAllModifiedMenu(player,menu);
                return false;
            })));
            return menu;

        }
    };
    private Function<RecipeType,ItemStack> rtypeIconGen=(c)->(c.toItem()==null||c.toItem().getType().isAir())?new CustomItemStack(Material.BARRIER,"&cNULL"):AddUtils.addLore(c.toItem(),"","&a当前配置类型: &f"+c.getKey().toString());
    private Function<ItemGroup,ItemStack> itemGroupIconGen=(itemGroup -> {
        ItemStack icon=CustomItemGroup.getItemGroupIcon(itemGroup);
        if(itemGroup instanceof FlexItemGroup){
            icon=AddUtils.addLore(icon,"","&c这是一个FlexItemGroup","&c不可以用来设置物品组");
        }
        return icon;
    });
    
    private CustomMenu openEditorGui3x3(Player executor){
       return ItemRegister.this.PLAYER_EDITORGUI3x3.computeIfAbsent(executor.getUniqueId(),(s)->new CustomMenu(null,1,PRESET3X3));

    }
    private CustomMenu openEditorGui6x6(Player executor){
       return  ItemRegister.this.PLAYER_EDITORGUI6X6.computeIfAbsent(executor.getUniqueId(),(s)->new CustomMenu(null,1,PRESET6x6));
    }
    private void openDisplayAllModifiedMenu(Player player,ChestMenu backMenu){
        MenuUtils.openSelectMenu(player,1,null,itemModifyRecords,(r,p)->{
            int len=r.getInstance().getRecipe().length;
            if(len<=9){
                CustomMenu menu=openEditorGui3x3(p);
                PRESET3X3.loadSlimefunItem(menu.getMenu(),r.getInstance());
                menu.openMenu(p);
            }else {
                CustomMenu menu=openEditorGui6x6(p);
                PRESET6x6.loadSlimefunItem(menu.getMenu(),r.getInstance());
                menu.openMenu(p);
            }
        },(r,p)->{},r->AddUtils.addLore(r.getInstance().getItem(),"","&a粘液ID: &f"+r.getInstance().getId(),"&a点击载入配方编辑界面"),backMenu==null?null:
            (cm,p)->backMenu.open(p));
    }
    private Pair<List<ItemStack>,List<CustomMenuGroup.CustomMenuClickHandler>> getDisplayed(String stats,String filter){
        switch (stats){
            case "rtype":return MenuUtils.getSelector(filter==null?null:filter.toLowerCase(),
                    (s,player)->AddUtils.displayCopyString(player,"单击此处拷贝字符串","点击复制到剪贴板",s.getKey().toString()),(s,player)->{},this.recipes.entrySet().iterator(),(c)->{ return AddUtils.addLore(rtypeIconGen.apply(c),"&a点击拷贝");});
            case "group":return MenuUtils.getSelector(filter==null?null:filter.toLowerCase(),
                    (s,player)->AddUtils.displayCopyString(player,"单击此处拷贝字符串","点击复制到剪贴板",s.getKey().toString()),(s,player)->{Slimefun.getRegistry().getSlimefunGuide(SlimefunGuideMode.SURVIVAL_MODE).openItemGroup(Slimefun.getRegistry().getPlayerProfiles().get(player.getUniqueId()),s,1);},this.groups.entrySet().iterator(),(c)->{ return AddUtils.addLore(itemGroupIconGen.apply(c),"&a点击拷贝");});
            case "sfitem": return MenuUtils.getSelector(filter==null?null:filter.toUpperCase(),
                    (s,player)->{AddUtils.forceGive(player,new ItemStack(s.getItem()),1);},
                    (s,player)->AddUtils.displayCopyString(player,"单击此处拷贝字符串","点击复制到剪贴板","slimefun:"+s.getId().toLowerCase()),this.items.entrySet().iterator(),(c)->{
                        ItemStack stack=c.getItem();
                        stack=(stack==null||stack.getType().isAir())?new CustomItemStack(Material.BARRIER,"&cNULL"):stack;
                        return AddUtils.addLore(stack,"",
                                "&7粘液物品ID: &f"+c.getId(),"&a点击获得该物品","&ashift 点击拷贝Namespace ID");
                    });
            default:return null;
        }
    }
    @Getter
    private LinkedHashSet<SubCommand> subCommands=new LinkedHashSet<>();
    public void registerSub(SubCommand command){
        subCommands.add(command);
    }

    @Override
    public SubCommand getMainCommand() {
        return mainCommand;
    }

    public SubCommand getSubCommand(String name){
        for(SubCommand command:subCommands){
            if(command.getName().equals(name)){
                return command;
            }
        }return null;
    }
    private SubCommand mainCommand=new SubCommand("itemreg",
            new SimpleCommandArgs("_operation"),"/itemreg [_operation] [args]"
    )
            .setTabCompletor("_operation",()->getSubCommands().stream().map(SubCommand::getName).toList());//no

    private SubCommand statsCommand=new SubCommand("stats",
            new SimpleCommandArgs("type","page"),"/itemreg stats [type] [page] 打开[type]粘液统计数据界面的第[page]页"){
        @Override
        public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
            var inputInfo=parseInput(var4).getFirstValue();
            String statsType=inputInfo.nextArg();
            if(statsType!=null&&ItemRegister.this.statsType.contains(statsType)){
                Player executor=isPlayer(var1,true);
                if (executor!=null){
                    openStatsGui(executor,statsType,null, CommandUtils.parseIntOrDefault(inputInfo.nextArg(),1));
                }
            }else {
                AddUtils.sendMessage(var1,"&c无效的统计数据类型");
            }
            return true;
        }
    }
            .setDefault("type","rtype")
            .setTabCompletor("type",()->statsType)
            .setDefault("page","1")
            .setTabCompletor("page",()->List.of("1","2","3","4","5"))
            .register(this);
    private SubCommand editorCommand=new SubCommand("editor",
            new SimpleCommandArgs("type"),"/itemreg editor [type] 打开[type]类的编辑界面"){
        @Override
        public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
            var inputInfo=parseInput(var4).getFirstValue();
            String statsType=inputInfo.nextArg();
            if(statsType!=null){
                Player executor=isPlayer(var1,true);
                if (executor!=null){
                    if("3x3".equalsIgnoreCase(statsType)){
                        openEditorGui3x3(executor).openMenu(executor);;
                    }else if("6x6".equalsIgnoreCase(statsType)){
                        openEditorGui6x6(executor).openMenu(executor);
                    }else if("list".equalsIgnoreCase(statsType)){
                        openDisplayAllModifiedMenu(executor,null);
                    }
                    else {
                        AddUtils.sendMessage(var1,"&c无效的编辑器类型");
                    }
                }
            }else {
                AddUtils.sendMessage(var1,"&c无效的编辑器类型");
            }
            return true;
        }
    }
            .setDefault("type","3X3")
            .setTabCompletor("type",()->List.of("3X3","6X6","list"))
            .register(this);
//    private SubCommand reloadCommand=new SubCommand("reload",
//            new SimpleCommandArgs(),"/itemreg reload 重载ItemReg配置文件"){
//        @Override
//        public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
//            reload();
//            AddUtils.sendMessage(var1,"&a重载完成!");
//            return true;
//        }
//    }
//            .register(this);

    //todo 增加危险操作try catch
    public boolean onCommand( CommandSender var1,  Command var2,  String var3, String[] var4){
        if(var1.hasPermission("slimefuntool.command.itemreg")){
            if(var4.length>=1){
                SubCommand command=getSubCommand(var4[0]);
                if(command!=null){
                    String[] elseArg= Arrays.copyOfRange(var4,1,var4.length);
                    return command.getExecutor().onCommand(var1,var2,var3,elseArg);
                }
            }
            showHelpCommand(var1);
        }else{
            AddUtils.sendMessage(var1,"&c你没有权限使用该指令!");
        }
        return true;
    }
    private void showHelpCommand(CommandSender sender){
        AddUtils.sendMessage(sender,"&a/itemreg 全部指令大全");
        for(SubCommand cmd:subCommands){
            for (String help:cmd.getHelp()){
                AddUtils.sendMessage(sender,"&a"+help);
            }
        }
    }
    public List<String> onTabComplete(CommandSender var1,  Command var2, String var3, String[] var4){
        var re=mainCommand.parseInput(var4);
        if(re.getSecondValue().length==0){
            List<String> provider=re.getFirstValue().getTabComplete();
            return provider==null?new ArrayList<>():provider;
        }else{
            SubCommand subCommand= getSubCommand(re.getFirstValue().nextArg());
            if(subCommand!=null){
                String[] elseArg=re.getSecondValue();
                List<String> tab= subCommand.parseInput(elseArg).getFirstValue().getTabComplete();
                if(tab!=null){
                    return tab;
                }
            }
        }
        return new ArrayList<>();
    }
    private Player isPlayer(CommandSender sender,boolean sendMessage){
        if(sender instanceof Player player){
            return player;
        }else {
            if(sendMessage){
                sender.sendMessage(AddUtils.resolveColor("&c该指令只能在游戏内执行!"));
            }
            return null;
        }
    }
}
