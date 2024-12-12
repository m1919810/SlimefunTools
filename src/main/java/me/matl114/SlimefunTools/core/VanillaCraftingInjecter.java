package me.matl114.SlimefunTools.core;

import com.google.common.base.Preconditions;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.Getter;
import me.matl114.SlimefunTools.functional.*;
import me.matl114.SlimefunTools.implement.Configs;
import me.matl114.SlimefunTools.implement.Debug;
import me.matl114.SlimefunTools.implement.SlimefunTools;
import me.matl114.matlib.Utils.AddUtils;
import me.matl114.matlib.Utils.Algorithm.StringHashMap;
import me.matl114.matlib.Utils.Command.CommandGroup.ComplexCommandExecutor;
import me.matl114.matlib.Utils.Command.CommandGroup.SubCommand;
import me.matl114.matlib.Utils.Command.CommandUtils;
import me.matl114.matlib.Utils.Command.Params.SimpleCommandArgs;
import me.matl114.matlib.Utils.ConfigLoader;
import me.matl114.matlib.Utils.Menu.MenuClickHandler.DataContainer;
import me.matl114.matlib.Utils.Menu.MenuGroup.CustomMenu;
import me.matl114.matlib.Utils.Menu.MenuGroup.CustomMenuGroup;
import me.matl114.matlib.Utils.Menu.MenuUtils;
import me.matl114.matlib.Utils.Reflect.ReflectUtils;
import me.matl114.matlib.Utils.Settings;
import me.matl114.matlib.core.Manager;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class VanillaCraftingInjecter implements Manager, Listener, ComplexCommandExecutor, TabCompleter {
    Plugin plugin;
    @Getter
    private VanillaCraftingInjecter manager;
    private boolean registered=false;
    String vanillaCraftingPath;
    Config vanillaCraftingConfig;
    public VanillaCraftingInjecter() {
        Preconditions.checkState(ItemRegister.getManager()!=null, "VanillaCraftingInjecter requires ItemRegister to be loaded!.");
        manager = this;
    }
    private String c(String... str){
        return String.join(".", str);
    }
    private NamespacedKey ofNSKey(String namespacedKey){
        if(namespacedKey==null)return null;
        String[] re= namespacedKey.split(":");
        if(re.length==2){
            return new NamespacedKey(re[0],re[1]);
        }else return null;
    }
    private Identifier<ShapedRecipe> shapedRecipeIdentidier=new Identifier<ShapedRecipe>(VanillaCraftingInjecter.class)
            .setIdentifier(ShapedRecipe.class)
            .setGetter("output",r->new ItemStack(r.getResult()))
            .setSetter("output",(r,i)->{
                ItemStack stack= (ItemStack) ReflectUtils.invokeGetRecursively(r, CraftingRecipe.class, Settings.FIELD,"output");
                if(stack==null){
                    Debug.logger("Failed to override shapedRecipe output ",r.getKey());
                }else{
                    AddUtils.copyItem((ItemStack) i,stack);
                }
            })
            .setType("output",ItemStack.class)
            .setGetter("recipe",this::getShapedRecipeChoices)
            .setSetter("recipe",(i,j)->setShapedRecipeChoices(i,(RecipeChoice[]) j))
            .setComparator("recipe",(r,o1,o2)->{
                RecipeChoice[] r1=(RecipeChoice[])o1;
                RecipeChoice[] r2=(RecipeChoice[])o2;
                boolean result= compareShapedRecipeChoice(r1,r2);
                return result;
            })
            .setType("recipe",RecipeChoice[].class)
            .setGetter("extraData",this::getShapedRecipeShpae)
            .setSetter("extraData",(i,j)->{
                resizeShapeRecipeChoices(i,(StringHashMap) j);
            })
            .setType("extraData", StringHashMap.class);
    private Identifier<ShapelessRecipe> shapelessRecipeIdentifier=new Identifier<ShapelessRecipe>(VanillaCraftingInjecter.class)
            .setIdentifier(ShapelessRecipe.class)
            .setGetter("output",r->new ItemStack(r.getResult()))
            .setSetter("output",(r,i)->{
                ItemStack stack= (ItemStack) ReflectUtils.invokeGetRecursively(r, CraftingRecipe.class, Settings.FIELD,"output");
                if(stack==null){
                    Debug.logger("Failed to override shapelessRecipe output ",r.getKey());
                }else {
                    AddUtils.copyItem((ItemStack) i,stack);
                }
            })
            .setType("output",ItemStack.class)
            .setGetter("recipe",this::getShapelessRecipeChoices)
            .setSetter("recipe",(i,j)->{setShapelessRecipeChoices(i,(RecipeChoice[]) j);})
            .setComparator("recipe",(r,i,j)->compareShapelessRecipeChoice((RecipeChoice[]) i,(RecipeChoice[]) j))
            .setType("recipe",RecipeChoice[].class)
            .setGetter("extraData",(i)->StringHashMap.ofNewMap(null))
            .setSetter("extraData",(i,j)->{})
            .setType("extraData",StringHashMap.class);
    HashSet<String> supportedRecipeType=new HashSet<>(){{
        add("shaped");
        add("unshaped");
    }};
    HashMap<String ,Class> recipeClassMapper=new HashMap<>(){{
        put("shaped",ShapedRecipe.class);
        put("unshaped", ShapelessRecipe.class);
    }};
    HashMap<String, RecipeAccessor.RecipeConstructor> recipeClassConstructor=new HashMap<>(){{
        put("shaped",VanillaCraftingInjecter.this::buildShapedRecipe);
        put("unshaped",VanillaCraftingInjecter.this::buildShapelessRecipe);
    }};
    private RecipeAccessor ofExist(NamespacedKey name){
        if(modifiedRecipeAccessors.containsKey(name)){
            return modifiedRecipeAccessors.get(name).getInstance();
        }
        Recipe recipe=Bukkit.getRecipe(name);
        if(recipe!=null){
            RecipeAccessor accessor=new RecipeAccessor();
            boolean found=false;
            for(Map.Entry<String, Class> classMapper:recipeClassMapper.entrySet()){
                if(classMapper.getValue().isInstance(recipe)){
                    accessor.setTypeInternal(classMapper.getKey());
                    found=true;
                    break;
                }
            }
            if(!found){
                Debug.logger("RecipeAccessor: Error while creating RecipeAccessor! ",accessor.getClass()," not supported yet");
                return null;
            }
            accessor.recipe=recipe;
            accessor.id=name.toString();
            accessor.newCreated=false;
            //已经启用
            accessor.enable=true;
            return accessor;
        }return null;
    }

    private RecipeAccessor of(NamespacedKey key,String type,RecipeChoice[] choices,ItemStack output,StringHashMap elseData){
        RecipeAccessor accessor=ofExist(key);
        if(accessor!=null)return accessor;
        RecipeAccessor.RecipeConstructor constructor=recipeClassConstructor.get(type);
        if(constructor==null){
            Debug.logger("RecipeAccessor: Error while fetching recipe constructor! ",type," not supported");
            return null;
        }
        Recipe recipe=constructor.getRecipe(key,choices,output,elseData);
        accessor=new RecipeAccessor();
        accessor.setTypeInternal(type);
        accessor.recipe=recipe;
        accessor.id=key.toString();
        accessor.newCreated=true;
        //暂未启用
        accessor.enable=false;
        return accessor;
    }

    private class RecipeAccessor{
        @Getter
        Recipe recipe;
        String id;
        @Getter
        private String type;
        @Getter
        Identifier identifier;
        boolean newCreated=false;
        boolean enable=false;
        private interface RecipeConstructor{
            public Recipe getRecipe(NamespacedKey name,RecipeChoice[] choices,ItemStack otput,StringHashMap elseData);
        }
        public boolean setTypeInternal(String type){
            Preconditions.checkArgument(recipeClassMapper.containsKey(type));
            this.type=type;
            this.identifier=Identifier.getIdentifier(recipeClassMapper.get(type));
            return false;
        }
        public boolean changeType(String type){
            if(recipeClassMapper.containsKey(type)&&recipeClassConstructor.containsKey(type)){
                if(this.type.equals(type)){
                    return false;
                }else {
                    RecipeConstructor constructor=recipeClassConstructor.get(type);
                    NamespacedKey key=ofNSKey(id);
                    Bukkit.removeRecipe(key);
                    Recipe newRecipe=constructor.getRecipe(key,getRecipeChoice(),getOutput(), getExtraData());
                    if(enable){
                        Bukkit.addRecipe(newRecipe);
                    }
                    this.recipe=newRecipe;
                    setTypeInternal(type);
                    //set Identifier after new recipe created
                    return true;
                }
            }else {
                Debug.logger("RecipeAccessor: Error in changing recipe type ! ",type," not supported");
                return false;
            }
        }
        public String getNamespacedKey(){
            return id;
        }

        public void setExtraData(StringHashMap extraData){
            identifier.set(recipe,"extraData",extraData);
        }
        public ItemStack getOutput(){
            return new ItemStack( (ItemStack) (identifier.get(recipe,"output")));
        }
        public void setOutput(ItemStack item){
            identifier.set(recipe,"output",item);
        }
        public RecipeChoice[] getRecipeChoice(){
            return ((RecipeChoice[]) identifier.get(recipe,"recipe") ).clone();
        }
        public void setRecipeChoice(RecipeChoice[] ingred){
            identifier.set(recipe,"recipe",ingred);
        }
        public RecipeAccessor register(){
            return this;
        }
        public StringHashMap getExtraData(){
            StringHashMap map= (StringHashMap) identifier.get(recipe,"extraData");
            return StringHashMap.ofNewMap(map);
        }
        public int getEnable(){
            return enable?1:0;
        }

        /**
         * important to apply changes to game
         */
        public void reloadRecipe(){
            NamespacedKey key=ofNSKey(id);
            Bukkit.removeRecipe(key);
            if(this.enable){
                Bukkit.addRecipe(recipe);
            }
        }
        public void setEnabled(int enable){
            if(enable==0){
                if(this.enable){
                    Bukkit.removeRecipe(ofNSKey(id));
                }
                this.enable=false;
            }else {
                if(!this.enable){
                    Bukkit.addRecipe(recipe);
                }
                this.enable=true;
            }
        }
    }
    private Identifier<RecipeAccessor> recipeAccessorIdentifier=new Identifier<RecipeAccessor>(VanillaCraftingInjecter.class)
            .setIdentifier(RecipeAccessor.class)
            .setGetter("type",RecipeAccessor::getType)
            .setSetter("type",(r,t)->r.changeType((String)t))
            .setType("type",String.class)
            .setGetter("recipe",RecipeAccessor::getRecipeChoice)
            .setSetter("recipe",(r,t)->r.setRecipeChoice((RecipeChoice[])t))
            .setComparator("recipe",(r,o1,o2)->!r.getIdentifier().differentInternal(r.getRecipe(),"recipe",o1,o2))
            .setType("recipe",RecipeChoice[].class)
            .setGetter("output",RecipeAccessor::getOutput)
            .setSetter("output",(r,t)->r.setOutput((ItemStack)t))
            .setType("output",ItemStack.class)
            .setGetter("enable",RecipeAccessor::getEnable)
            .setSetter("enable",(r,s)->r.setEnabled((Integer)s))
            .setType("enable",Integer.class)
            .setGetter("extraData",RecipeAccessor::getExtraData)
            .setSetter("extraData",(r,t)->r.setExtraData((StringHashMap) t))
            .setType("extraData",StringHashMap.class);

    private ItemStack PARSE_FAILED=new CustomItemStack(Material.STONE,"&c物品解析失败!","&e请检查配置文件");
    private Function<String,RecipeChoice> nouseMapper=(s)->{
        NamespacedKey key=ofNSKey(s);
        if(key!=null){
            Tag tag= Bukkit.getTag("items",ofNSKey(s),Material.class);
            if(tag!=null){
                return new RecipeChoice.MaterialChoice(tag);
            }
        }
        ItemStack stack=ItemRegister.getManager().git(s);
        if(stack==null){
            return null;
        }else return new RecipeChoice.ExactChoice(stack);
    };



    private Recipe addRecipeToCraftingTable(ItemStack[] recipe, ItemStack output, NamespacedKey id){
        if(recipe.length>9){
            for(int i=9;i<recipe.length;i++){
                if(recipe[i]!=null){
                    Debug.logger("CraftingTable: this recipe can not be sent to Crafting Table: ",id.toString());
                    return null;
                }
            }
            Debug.logger("CraftingTable: warning:recipe length can be resized to 9");
            recipe= Arrays.copyOf(recipe, 9);
        }
        if(Bukkit.getRecipe(id)!=null){
            Debug.logger("CraftingTable: this recipe is already registered: ",id.toString());
            return Bukkit.getRecipe(id);
        }
        RecipeChoice[] inputChoice=new RecipeChoice[9];
        for(int i=0;i<recipe.length;i++){
            inputChoice[i]=recipe[i]==null?null: new RecipeChoice.ExactChoice(recipe[i]);
        }
        Recipe vanillaRecipe= buildShapedRecipe(id,inputChoice,output);
        Bukkit.addRecipe(vanillaRecipe);
        return vanillaRecipe;
    }
    private RecipeChoice[] getShapelessRecipeChoices(ShapelessRecipe recipe){
        return recipe.getChoiceList().toArray(RecipeChoice[]::new);
    }
    private ShapelessRecipe setShapelessRecipeChoices(ShapelessRecipe recipe,RecipeChoice[] choiceList){
        List<RecipeChoice> recipeChoiceList=recipe.getChoiceList();
        for(RecipeChoice choice:recipeChoiceList){
            if(choice!=null){
                recipe.removeIngredient(choice);
            }
        }
        for(RecipeChoice choice:choiceList){
            if(choice!=null){
                recipe.addIngredient(choice);
            }
        }
        return recipe;
    }
    private boolean compareShapelessRecipeChoice(RecipeChoice[] r1,RecipeChoice[] r2){
        Set<RecipeChoice> r1Set= Arrays.stream(r1).collect(Collectors.toSet());
        Set<RecipeChoice> r2Set= Arrays.stream(r2).collect(Collectors.toSet());
        for(RecipeChoice choice:r1Set){
            Iterator<RecipeChoice> it1=r2Set.iterator();
            boolean found=false;
            while(it1.hasNext()){
                RecipeChoice choice1=it1.next();
                if(compareRecipeChoice(choice1,choice)){
                    it1.remove();
                    found=true;
                    break;
                }
            }
            if(!found)return false;
        }
        return r2Set.isEmpty();
    }
    private RecipeChoice[] getShapedRecipeChoices(ShapedRecipe recipe){
        Map<Character,RecipeChoice> choice= recipe.getChoiceMap();
        String[] map=recipe.getShape();
        RecipeChoice[] result=new RecipeChoice[9];
        int index=0;
        for(int i=0;i<map.length;i++){
            index=map[i].length();
            for(int j=0;j<index;j++){
                result[index*i+j]=choice.get(map[i].charAt(j));
            }
        }
        result=Arrays.copyOf(result, index*map.length);
        return result;
    }
    private StringHashMap getShapedRecipeShpae(ShapedRecipe vanillaRecipe){
        String[] pattern=vanillaRecipe.getShape();
        if(pattern!=null&&pattern.length!=0){
            StringHashMap  size=new StringHashMap();
            if(pattern.length!=3){
                size.put("height",String.valueOf(pattern.length));
            }
            if(pattern[0].length()!=3){
                size.put("width",String.valueOf(pattern[0].length()));
            }
            return size;
        }
        return null;
    }
    private ShapedRecipe resizeShapeRecipeChoices(ShapedRecipe vanillaRecipe,int width,int height){
        return setShapedRecipeChoices(vanillaRecipe,getShapedRecipeChoices(vanillaRecipe),width,height);
    }
    private ShapedRecipe resizeShapeRecipeChoices(ShapedRecipe vanillaRecipe ,StringHashMap map){
        if(map.containsKey("width")||map.containsKey("height")){
            var re=getShapeFromExtraData(map);
            return resizeShapeRecipeChoices(vanillaRecipe,re.getSecondValue(),re.getFirstValue());
        }
        return vanillaRecipe;
    }
    private ShapedRecipe setShapedRecipeChoices(ShapedRecipe vanillaRecipe,RecipeChoice[] inputChoice){
        return setShapedRecipeChoices(vanillaRecipe,inputChoice,3,3);
    }
    private ShapedRecipe setShapedRecipeChoices(ShapedRecipe vanillaRecipe,RecipeChoice[] inputChoice,int width,int height){
        ReflectUtils.setFieldRecursively(vanillaRecipe,ShapedRecipe.class,"ingredients",new HashMap<>());
        inputChoice=Arrays.copyOf(inputChoice,width*height);
        String[] pattern=new String[height];
        for(int i=0;i<height;i++){
            StringBuilder builder=new StringBuilder();
            for(int j=0;j<width;j++){
                builder.append(inputChoice[width*i+j]==null?" ":String.valueOf(width*i+j).charAt(0));
            }
            pattern[i]=builder.toString();
        }
        vanillaRecipe.shape(pattern);
        for(int i=0;i<inputChoice.length;i++){
            if(inputChoice[i]!=null){
                vanillaRecipe.setIngredient(String.valueOf(i).charAt(0),inputChoice[i]);
            }
        }
        return vanillaRecipe;
    }
    private boolean compareShapedRecipeChoice(RecipeChoice[] r1,RecipeChoice[] r2){
        for(int i=0;i<9;i++){
            if(r1.length>i&&r2.length>i){
                if(!compareRecipeChoice(r1[i],r2[i])){
                    return false;
                }
            }else if(r1.length>i){
                if(r1[i]!=null)return false;
            }else if(r2.length>i){
                if(r2[i]!=null)return false;
            }
        }
        return true;
    }
    private boolean compareRecipeChoice(RecipeChoice r1,RecipeChoice r2){
        if(r1==null||r2==null){
            return r1==r2;
        }else if(r1 instanceof RecipeChoice.ExactChoice ec1){
            if(r2 instanceof RecipeChoice.ExactChoice ec2){
                HashSet<ItemStack> stack1= new HashSet<>(ec1.getChoices());
                HashSet<ItemStack> stack2= new HashSet<>(ec2.getChoices());
                for(ItemStack test1:stack1){
                    if(!stack2.remove(test1)){
                        return false;
                    }
                }
                return stack2.isEmpty();
            }else return false;
        }else if(r1 instanceof RecipeChoice.MaterialChoice mc1){
            if(r2 instanceof RecipeChoice.MaterialChoice mc2){
                HashSet<Material> mat1=new HashSet<>(mc1.getChoices());
                HashSet<Material> mat2=new HashSet<>(mc2.getChoices());
                for (Material mat : mat1) {
                    if(!mat2.remove(mat)){
                        return false;
                    }
                }
                return mat2.isEmpty();
            }else return false;
        }
        return r1.equals(r2);
    }
    private RecipeChoice[][] getShapedRecipeChoiceArrayTo2Dim(RecipeChoice[] recipe,int height,int width
                                                              ){
        RecipeChoice[][] result=new RecipeChoice[height][width];
        for(int i=0;i<height;i++){
            for(int j=0;j<width;j++){
                int index=i*width+j;
                if(recipe.length>index){
                    result[i][j]=recipe[index];
                }else {
                    result[i][j]=null;
                }
            }
        }
        return result;
    }
    private RecipeChoice[] getShapedRecipChoiceArrayFrom2Diim(RecipeChoice[][] recipe){
        List<RecipeChoice> result=new ArrayList<>();
        for(int i=0;i<recipe.length;i++){
            RecipeChoice[] choice=recipe[i];
            for(int j=0;j<choice.length;j++){
                result.add(choice[j]);
            }
        }
        return result.toArray(RecipeChoice[]::new);
    }

    /**
     * pair<height,width> range from 1-3
     */
    private Pair<Integer,Integer> getShapeFromExtraData(StringHashMap extraData){
        return new Pair<>(CommandUtils.validRange(CommandUtils.parseIntOrDefault(extraData.get("height"),3),1,3),CommandUtils.validRange( CommandUtils.parseIntOrDefault(extraData.get("width"),3),1,3));
    }
    private Recipe buildShapedRecipe(NamespacedKey id, RecipeChoice[] inputChoice, ItemStack output){
        return buildShapedRecipe(id,inputChoice,output,null);
    }
    private Recipe buildShapedRecipe(NamespacedKey id, RecipeChoice[] inputChoice, ItemStack output,StringHashMap elseData){
        ShapedRecipe vanillaRecipe=new ShapedRecipe(id,output);
        return setShapedRecipeChoices(vanillaRecipe,inputChoice);
    }
    private Recipe buildShapelessRecipe(NamespacedKey id, RecipeChoice[] inputChoice, ItemStack output){
        return buildShapedRecipe(id,inputChoice,output,null);
    }
    private Recipe buildShapelessRecipe(NamespacedKey id, RecipeChoice[] inputChoice, ItemStack output,StringHashMap elseData){
        ShapelessRecipe vanillaRecipe=new ShapelessRecipe(id,output);
        Arrays.stream(inputChoice).filter(s->s!=null).forEach(vanillaRecipe::addIngredient);
        return vanillaRecipe;
    }
    final HashMap<NamespacedKey,Recipe> craftingRecipeFromSlimefun = new LinkedHashMap<>();
    final HashMap<NamespacedKey,RecipeAccessor> registeredRecipe=new LinkedHashMap<>();
    final HashMap<NamespacedKey, InstanceModifyRecord<RecipeAccessor>> modifiedRecipeAccessors = new LinkedHashMap<>();
    private void loadConfigs(){
        Set<String> keys=this.vanillaCraftingConfig.getKeys();
        for(String k:keys){
            NamespacedKey key=ofNSKey(k);
            if(key==null){
                Debug.logger("原版配方 ",k,": unknown NamespacedKey format: ",k);
            }else {
                String type=this.vanillaCraftingConfig.getString(c(k,"type"));
                RecipeChoice[] recipe=(RecipeChoice[]) ConfigParser.deserialize(this.vanillaCraftingConfig,c(k,"recipe"),RecipeChoice[].class);
                ItemStack output=(ItemStack) ConfigParser.deserialize(this.vanillaCraftingConfig,c(k,"output"),ItemStack.class);
                Integer enable= (Integer) ConfigParser.deserialize(this.vanillaCraftingConfig,c(k,"enable"),Integer.class);
                //add support to elseData
                StringHashMap elseData=(StringHashMap) ConfigParser.deserialize(this.vanillaCraftingConfig,c(k,"extraData"),StringHashMap.class);
                if(Bukkit.getRecipe(key)==null){
                    RecipeAccessor accessor=of(key,type,recipe,output,elseData);
                    this.registeredRecipe.put(key,accessor);
                    overrideVanillaRecipe(key,accessor,null,null,null,enable,null,SaveMethod.NONE);
                }else {
                    RecipeAccessor accessor=ofExist(key);

                    overrideVanillaRecipe(key,accessor,type,recipe,output,enable,elseData,SaveMethod.NONE);
                }
            }
        }
        Debug.logger("原版配方增改成功");
        //防止修改之时remove失效
        SlimefunTools.runSync(()->{
            Debug.logger("VcInjector: 重载补丁启动");
            Debug.logger("重新注入原版配方中...");
            this.modifiedRecipeAccessors.forEach((i,j)->j.getInstance().reloadRecipe());
        });
    }
    private void overrideVanillaRecipe(NamespacedKey key, RecipeAccessor re, String type, RecipeChoice[] recipe, ItemStack output , Integer enable,StringHashMap elseData, SaveMethod saveMethod){
        InstanceModifyRecord<RecipeAccessor> record=modifiedRecipeAccessors.computeIfAbsent(key,(i)->new InstanceModifyRecord<RecipeAccessor>(re,this.vanillaCraftingConfig,RecipeAccessor::getNamespacedKey,RecipeAccessor.class,false)
        );
        boolean changeTaken=false;
        if(type!=null&&record.checkDifference("type",type)){
            record.executeModification("type",type);
            record.executeDataSave("type",saveMethod);
            changeTaken=true;
        }
        if(recipe!=null&&record.checkDifference("recipe",recipe)){
            record.executeModification("recipe",recipe);
            record.executeDataSave("recipe",saveMethod,()->key.getNamespace()+"_"+key.getKey()+"_vanila_crafting_recipeChoice_");
            changeTaken=true;
        }
        if(output!=null&&record.checkDifference("output",output)){
            record.executeModification("output",output);
            record.executeDataSave("output",saveMethod,()->"_"+key.getNamespace()+"_"+key.getKey()+"_vanilla_crafting_output_");
            changeTaken=true;
        }
        if(elseData!=null&&record.checkDifference("extraData",elseData)){
            record.executeModification("extraData",elseData);
            record.executeDataSave("extraData",saveMethod);
            changeTaken=true;
        }
        if(enable!=null&&record.checkDifference("enable",enable)){
            record.executeModification("enable",enable);
            record.executeDataSave("enable",saveMethod);
            changeTaken=true;
        }

        if(changeTaken){
            record.getInstance().reloadRecipe();
        }
    }

    private void createVanillaRecipe(NamespacedKey key,  String type, RecipeChoice[] recipe, ItemStack output , Integer enable,StringHashMap elseData, SaveMethod saveMethod){
        RecipeAccessor accessor=of(key,type,recipe,output,elseData);
        this.registeredRecipe.put(key,accessor);
        //register to bukkit
        accessor.setEnabled(enable);
        InstanceModifyRecord<RecipeAccessor> record=new InstanceModifyRecord<>(accessor,this.vanillaCraftingConfig,i->i.getNamespacedKey().toString(),RecipeAccessor.class,false);
        modifiedRecipeAccessors.put(key,record);
        record.executeDataSave("type",saveMethod);
        record.executeDataSave("recipe",saveMethod,()->key.getNamespace()+"_"+key.getKey()+"_vanila_crafting_recipeChoice_");
        record.executeDataSave("output",saveMethod,()->"_"+key.getNamespace()+"_"+key.getKey()+"_vanilla_crafting_output_");
        record.executeDataSave("enable",saveMethod);
        record.executeDataSave("extraData",saveMethod);
    }
    public void deleteVanillaRecipeModification(NamespacedKey id){
        if(this.modifiedRecipeAccessors.containsKey(id)){
            InstanceModifyRecord<RecipeAccessor> record=this.modifiedRecipeAccessors.remove(id);
            record.executeAllDataDelete();
        }
        if(registeredRecipe.containsKey(id)){
            registeredRecipe.remove(id);
            unregisterVanillaRecipe(id);
        }
    }
    public void unregisterVanillaRecipe(NamespacedKey id){
        Bukkit.removeRecipe(id);
    }
    private void deleteVanillaRecipeConfig(NamespacedKey key){
        deleteVanillaRecipeModification(key);
        this.vanillaCraftingConfig.setValue(key.toString(),null);
        this.vanillaCraftingConfig.save();
    }
    private static Pattern keyRe=Pattern.compile("[a-z0-9/._-]");
    private String validKey(String id){
        return id.toLowerCase().replaceAll("[^a-zA-Z0-9/._-]","");
    }
    private void sendRecipeFromSlimefunRecipeType(){
        ItemRegister.getManager().VANILLA_CRAFT_TABLE.relatedTo((a,b)->{
            for(SlimefunItem item: Slimefun.getRegistry().getAllSlimefunItems()){
                if(item.getRecipeType()==ItemRegister.getManager().VANILLA_CRAFT_TABLE&&Arrays.equals(a,item.getRecipe())&&b.equals(item.getRecipeOutput())){
                    String id=item.getId();
                    NamespacedKey key=new NamespacedKey("slimefuntools",validKey(id));
                    Recipe recipe=addRecipeToCraftingTable(a,b,key);
                    if(recipe!=null){
                        craftingRecipeFromSlimefun.put(key,recipe);
                    }
                    return;
                }
            }
            var ido =Slimefun.getItemDataService().getItemData(b);
            if(ido.isPresent()){
                String id=ido.get();
                NamespacedKey key=new NamespacedKey("slimefuntools",validKey(id));
                Recipe recipe=addRecipeToCraftingTable(a,b,key);
                if(recipe!=null){
                    craftingRecipeFromSlimefun.put(key,recipe);
                }
                return;
            }else{
                Debug.logger("Error While Registering item to RecipeType! No SlimefunItem Match These inputs and outputs!");
            }
        },(a,b)->{
            for(SlimefunItem item: Slimefun.getRegistry().getAllSlimefunItems()){
                if(item.getRecipeType()==ItemRegister.getManager().VANILLA_CRAFT_TABLE&&Arrays.equals(a,item.getRecipe())&&b.equals(item.getRecipeOutput())){
                    String id=item.getId();
                    NamespacedKey key=new NamespacedKey("slimefuntools",validKey(id));
                    Bukkit.removeRecipe(key);
                    this.craftingRecipeFromSlimefun.remove(key);
                    return;
                }
                var ido =Slimefun.getItemDataService().getItemData(b);
                if(ido.isPresent()){
                    String id=ido.get();
                    NamespacedKey key=new NamespacedKey("slimefuntools",validKey(id));
                    Bukkit.removeRecipe(key);
                    this.craftingRecipeFromSlimefun.remove(key);
                    return;
                }else{
                    Debug.logger("Error UnWhile Registering item to RecipeType! No SlimefunItem Match These inputs and outputs!");
                }
            }
            Debug.logger("Error While UnRegistering item to RecipeType! No SlimefunItem Match These inputs and outputs!");
        });
        //furnace need impl
    }
    private VanillaCraftingInjecter registerFunctional(){
        Validate.isTrue(!registered, "vanillaCraftingInjector functional have already been registered!");
        plugin.getServer().getPluginManager().registerEvents(this,plugin);
        plugin.getServer().getPluginCommand("vcinject").setExecutor(this);
        plugin.getServer().getPluginCommand("vcinject").setTabCompleter(this);
        this.registered=true;
        return this;
    }
    private VanillaCraftingInjecter unregisterFunctional(){
        Validate.isTrue(registered, "vanillaCraftingInjector functional haven't been registered!");
        HandlerList.unregisterAll(this);
        plugin.getServer().getPluginCommand("vcinject").setExecutor(null);
        plugin.getServer().getPluginCommand("vcinject").setTabCompleter(null);
        this.registered=false;
        return this;
    }
    public VanillaCraftingInjecter init(Plugin plugin, String... paths){
        Debug.logger("Enabling VanillaCrafting Injector");
        addToRegistry();
        this.plugin=plugin;
        this.vanillaCraftingPath=paths[0];
        this.vanillaCraftingConfig= ConfigLoader.loadExternalConfig(vanillaCraftingPath);
        sendRecipeFromSlimefunRecipeType();
        loadConfigs();
        registerFunctional();
        return this;
    }
    public void deconstruct(){
        this.unregisterFunctional();
        this.removeFromRegistry();
        //
        for(InstanceModifyRecord<RecipeAccessor> record:this.modifiedRecipeAccessors.values()){
            record.executeUndoAllModifications();
        }
        for(Map.Entry<NamespacedKey,RecipeAccessor> entry:this.registeredRecipe.entrySet()){
            Bukkit.removeRecipe(entry.getKey());
        }
        for(Map.Entry<NamespacedKey,Recipe> entry:this.craftingRecipeFromSlimefun.entrySet()){
            Bukkit.removeRecipe(entry.getKey());
        }
        ItemRegister.getManager().VANILLA_CRAFT_TABLE.unregisterRecipeType();
        ItemRegister.getManager().VANILLA_FURNACE.unregisterRecipeType();
        this.modifiedRecipeAccessors.clear();
        this.registeredRecipe.clear();
        this.craftingRecipeFromSlimefun.clear();
        DynamicFunctional.onModuleManagerDisable(VanillaCraftingInjecter.class);
        Debug.logger("Disabling VanillaCrafting Injector");
    }
    public VanillaCraftingInjecter reload(){
        deconstruct();
        return init(this.plugin,this.vanillaCraftingPath);
    }






    //allowing modify crafting recipes to be crafted
    @EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = false)
    public void onCraftingTableCraft(CraftItemEvent e) {
        if(e.getRecipe() instanceof CraftingRecipe sr) {
            NamespacedKey key = sr.getKey();
            if(modifiedRecipeAccessors.containsKey(key)){
                e.setResult(Event.Result.ALLOW);
            }else if(craftingRecipeFromSlimefun.containsKey(key)){
                e.setResult(Event.Result.ALLOW);
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = false)
    public void onCraftingTablePrepareCraft(PrepareItemCraftEvent e) {
        if(e.getRecipe() instanceof CraftingRecipe sr) {
            NamespacedKey key = sr.getKey();
            if(modifiedRecipeAccessors.containsKey(key)) {
                e.getInventory().setResult(sr.getResult());
            }else if(craftingRecipeFromSlimefun.containsKey(key)) {
                e.getInventory().setResult(sr.getResult());
            }
        }
    }



    private HashSet<String> statsType=new LinkedHashSet<>(){{
        add("shaped");
        add("unshaped");
    }};
    private void openStatsGui(Player player,String type,String filter,int page){
        if(page<=0){
            AddUtils.sendMessage(player,"&c无效的页数!");
            return;
        }
        CustomMenuGroup menuGroup= new CustomMenuGroup(AddUtils.resolveColor("&a配方信息统计"),54,1 )
                .enableContentPlace(IntStream.range(0,45).toArray())
                .setPageChangeSlots(46,52)
                .enableOverrides();
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
        int index=47;
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
    private HashMap<String,ShapelessRecipe> getUnshapedRecipeMap(){
        HashMap<String,ShapelessRecipe> map=new HashMap<>();
        Iterator<Recipe> recipeIterator=plugin.getServer().recipeIterator();
        while(recipeIterator.hasNext()){
            Recipe recipe=recipeIterator.next();
            if (recipe instanceof ShapelessRecipe shaped) {
                map.put(shaped.getKey().toString(),shaped);
            }
        }
        return map;
    }
    private HashMap<String,ShapedRecipe> getShapedRecipeMap(){
        HashMap<String,ShapedRecipe> map=new HashMap<>();
        Iterator<Recipe> recipeIterator=plugin.getServer().recipeIterator();
        while(recipeIterator.hasNext()){
            Recipe recipe=recipeIterator.next();
            if (recipe instanceof ShapedRecipe shaped) {
                map.put(shaped.getKey().toString(),shaped);
            }
        }
        return map;
    }
    private HashMap<String,ItemStack> typeIcon=new HashMap<>(){{
        put("shaped",new CustomItemStack(Material.CRAFTING_TABLE,"&a有序工作台配方"));
        put("unshaped",new CustomItemStack(Material.CRAFTING_TABLE,"&a无序工作台配方"));
    }};
    private ItemStack typeNotSupported=new CustomItemStack(Material.BARRIER,"&c不支持的配方类型!");
    private ItemStack typeIconLost=new CustomItemStack(Material.STRUCTURE_VOID,"&c图标缺失!");
    private List<ItemStack> getRecipeChoiceIconList(RecipeChoice choice){
        if(choice!=null){
            List<ItemStack> choiceSamples;
            if(choice instanceof RecipeChoice.MaterialChoice materialChoice){
                choiceSamples=materialChoice.getChoices().stream().map(ItemStack::new).toList();
            }else if(choice instanceof RecipeChoice.ExactChoice exactChoice){
                choiceSamples=exactChoice.getChoices();
            }else {
                choiceSamples=List.of(choice.getItemStack());
            }
            return choiceSamples;
        }else{
            return null;
        }
    }
    private ItemStack getElseDataIcon(StringHashMap map){
        List<String> lores=new ArrayList<>();
        lores.add("&7额外信息显示");
        lores.add("&7点击以修改额外信息");
        for(Map.Entry<String,String> entry:map.entrySet()){
            lores.add("&a"+entry.getKey()+" : &f"+entry.getValue());
        }
        return new CustomItemStack(Material.BOOK,"&a配方额外信息显示",lores);
    }
    private void openRecipeChoiceView(List<ItemStack> choiceSamples, Player player, Consumer<Player> backPath){
        HashMap<String,ItemStack> mappedChoice=new HashMap<>();
        for (int k=0;k<choiceSamples.size();++k){
            mappedChoice.put(String.valueOf(k),choiceSamples.get(k));
        }
        MenuUtils.openSelectMenu(player,1,null,mappedChoice,(cm)->(it,pl)->{},(cm)->(it,pl)->{},item->AddUtils.addLore(item,"","多匹配配方选项"),(cm,p)->backPath.accept(p));
    }
    private RecipeChoice choiceBuilder(Stream<ItemStack> choiceStream,String type){
        switch (type){
            case "material":{
                List<Material> materials=choiceStream.filter(s->s!=null).map(ItemStack::getType).collect(Collectors.toSet()).stream().toList();
                if(materials.isEmpty()){
                    return null;
                }else {
                    return new RecipeChoice.MaterialChoice(materials);
                }
            }
            case "exact":{
                List<ItemStack> exacts=choiceStream.filter(s->s!=null).map(ItemStack::new).peek(s->s.setAmount(1)).collect(Collectors.toSet()).stream().toList();
                if(exacts.isEmpty()){
                    return null;
                }else {
                    return new RecipeChoice.ExactChoice(exacts);
                }
            }
            default:return null;
        }
    }
    private CustomMenu getRecipeChoiceBuilder(List<ItemStack> originlist,BiConsumer<Player,RecipeChoice> resultCallback,Consumer<Player> fallback){
        return new CustomMenu(null,1,(i__)->{
            ChestMenu menu=new ChestMenu("&a配方选项构建器");
            menu.setEmptySlotsClickable(true);
            menu.setPlayerInventoryClickable(true);
            IntStream.range(0,45).forEach(i->menu.addItem(i,null,((player, i1, itemStack, clickAction) -> true)));
            IntStream.range(45,54).forEach(i->menu.addItem(i,ChestMenuUtils.getBackground(),ChestMenuUtils.getEmptyClickHandler()));
            ItemStack info=new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE,"&a可在上方修改配方选项","&7并可以选择构建材质选项或是精确选项");
            menu.replaceExistingItem(49,info);
            ItemStack materialChoice=new CustomItemStack(Material.OAK_WOOD,"&e构建材质选项","&7只会保存物品的材质","&7相同材质的物品均可匹配","&7包括粘液物品","&c不推荐");
            menu.replaceExistingItem(47,materialChoice);
            menu.addMenuClickHandler(47,((player, i, itemStack, clickAction) -> {
                RecipeChoice choice=choiceBuilder(IntStream.range(0,45).mapToObj(menu::getItemInSlot),"material");
                AddUtils.sendMessage(player,choice!=null?"&a成功构建材料配方选项":"&a成功删除配方选项");
                resultCallback.accept(player,choice);
                return false;
            }));
            ItemStack exactChoice=new CustomItemStack(Material.BEACON,"&a构建精确选项","&7会保存物品本身","&7只有完全相同的物品可以匹配");
            menu.replaceExistingItem(51,exactChoice);
            menu.addMenuClickHandler(51,((player, i, itemStack, clickAction) -> {
                RecipeChoice choice=choiceBuilder(IntStream.range(0,45).mapToObj(menu::getItemInSlot),"exact");
                AddUtils.sendMessage(player,choice!=null?"&a成功构建精确配方选项":"&a成功删除配方选项");
                resultCallback.accept(player,choice);

                return false;
            }));
            menu.replaceExistingItem(53,MenuUtils.getBackButton(fallback==null?"&7没有返回的路":""));
            menu.addMenuClickHandler(53,((player, i, itemStack, clickAction) -> {
                fallback.accept(player);
                return false;
            }));
            int len=Math.min(45,originlist==null?0: originlist.size());
            for (int i=0;i<len;++i){
                menu.replaceExistingItem(i,originlist.get(i));
            }
            return menu;
        });
    }
    private ItemStack nullSlotIcon=new CustomItemStack(Material.BARRIER," "," ");

    private class RecipeInfoGui implements IntFunction<ChestMenu> {
        int size=45;
        int[][] recipeSlots=new int[][]{
            new int[]{11,12,13}, new int[]{20,21,22},new int[]{29,30,31}
        };
        int outputSlot=25;
        int typeSlot=23;
        int elseDataSlot=24;
        RecipeAccessor accessor;
        public RecipeInfoGui(RecipeAccessor accessor){
            this.accessor=accessor;
        }
        public ChestMenu apply(int i____) {
            RecipeChoice[] choices=accessor.getRecipeChoice();
            StringHashMap elseData=accessor.getExtraData();
            int height= CommandUtils.parseIntOrDefault(elseData.get("height"),3);
            int width= CommandUtils.parseIntOrDefault(elseData.get("width"),3);
            ChestMenu menu=new ChestMenu("配方信息显示");

            IntStream.range(0,45).forEach((i)->menu.addItem(i,ChestMenuUtils.getBackground(),ChestMenuUtils.getEmptyClickHandler()));
            final HashMap<Integer,List<ItemStack>> multiChoicePlace=new HashMap<>();
            for(int i=0;i<3;++i){
                for(int j=0;j<3;++j){
                    if(i<height&&j<width){
                        int choiceIndex=i*width+j;
                        RecipeChoice choice=choiceIndex<choices.length? choices[choiceIndex]:null;
                        if(choice!=null){
                            List<ItemStack> choiceSamples=getRecipeChoiceIconList(choice);
                            if(choiceSamples.isEmpty()){
                                menu.replaceExistingItem(recipeSlots[i][j],null);
                            }
                            else if(choiceSamples.size()==1){
                                menu.replaceExistingItem(recipeSlots[i][j],choiceSamples.get(0));
                            }else{
                                multiChoicePlace.put(recipeSlots[i][j],choiceSamples);

                                menu.addMenuClickHandler(recipeSlots[i][j],((player, i1, itemStack, clickAction) -> {
                                    openRecipeChoiceView(choiceSamples,player,(player1 -> menu.open(player1)));
                                    return false;
                                }));
                            }
                        }else{
                            menu.replaceExistingItem(recipeSlots[i][j],null);
                        }
                    }else{
                        menu.replaceExistingItem(recipeSlots[i][j],nullSlotIcon);
                    }
                }
            }
            final Runnable asyncMultiChoice=new BukkitRunnable() {
                AtomicInteger counter=new AtomicInteger(0);
                public void run() {
                    int value=counter.getAndAdd(1);
                    for(Map.Entry<Integer,List<ItemStack>> entry:multiChoicePlace.entrySet()){
                        menu.replaceExistingItem(entry.getKey(),entry.getValue().get(value%entry.getValue().size()));
                    };
                }
            };
            AtomicReference<BukkitRunnable> running=new AtomicReference<>(null);
            menu.addMenuOpeningHandler(player -> {if(running.get()==null){
                BukkitRunnable runnable= new BukkitRunnable(){
                    public void run(){
                        asyncMultiChoice.run();
                    }
                };
                running.set(runnable);
                runnable.runTaskTimerAsynchronously(plugin,0,20);
            }});
            menu.addMenuCloseHandler(player -> {
                BukkitRunnable runnable=running.getAndSet(null);
                if(runnable!=null&&!runnable.isCancelled()){
                    runnable.cancel();
                }

            });
            menu.replaceExistingItem(outputSlot,accessor.getOutput());
            menu.replaceExistingItem(typeSlot,typeIcon.get(accessor.getType()));
            menu.replaceExistingItem(elseDataSlot,getElseDataIcon(elseData));
            return menu;
        }
    }
    private interface RecipeBuilder{
        void build(NamespacedKey namespacedKey,String type,RecipeChoice[] choice,ItemStack output,StringHashMap extraData,boolean enable);
    }
    private class RecipeEditorGui implements IntFunction<ChestMenu>{
        int[][] recipeSlots=new int[][]{
                new int[]{10,12,14}, new int[]{19,21,23},new int[]{28,30,32}
        };
        int[][] editorSlots=new int[][]{
                new int[]{9,11,13}, new int[]{18,20,22},new int[]{27,29,31}
        };
        int dataHolderSlot=1;
        int typeSlot=15;
        int outputSlot=24;
        int extraDataSlot=33;
        int saveSF=16;
        int saveSC=34;
        int loadRecipe=25;
        int createSF=17;
        int selectSlot=26;
        int createSC=35;
        int clearSlot=8;
        int selectAllModifies=0;
        int enableSlot=44;
        public DataContainer getDataHolder(ChestMenu menu){
            ChestMenu.MenuClickHandler handler=menu.getMenuClickHandler(dataHolderSlot);
            if(handler instanceof DataContainer dh){
                return dh;
            }else {
                DataContainer dh=new DataContainer() {
                    //type
                    String type;
                    //recipeChoices
                    Object recipeChoices=new RecipeChoice[3][3];
                    Object elseData;
                    String namespacedKey;
                    public Object getObject(int val) {return val==0?recipeChoices:elseData;}
                    public void setObject(int val, Object val2) {
                        if(val==0) recipeChoices=val2;else elseData=val2;
                    }
                    public void setString(int val,String val2){
                        if(val==0)type=val2;else namespacedKey=val2;
                    }
                    public String getString(int val){return val==0?type:namespacedKey;}
                    public boolean onClick(Player player, int i, ItemStack itemStack, ClickAction clickAction) {return false;}
                };
                menu.addMenuClickHandler(dataHolderSlot,dh );
                return dh;
            }
        }
        public RecipeChoice[][] getRecipeChoice(ChestMenu menu){
            RecipeChoice[][] choice=(RecipeChoice[][]) getDataHolder(menu).getObject(0);
            return choice!=null?choice:new RecipeChoice[3][3];
        }
        public void setRecipeChoice(ChestMenu menu,RecipeChoice[][] recipeChi){
            getDataHolder(menu).setObject(0,recipeChi);
            for(int i=0;i<recipeChi.length;i++){
                RecipeChoice[] choice=recipeChi[i];
                for(int j=0;j<choice.length;j++){
                    setPositionRecipeChoice(menu,choice[j],i,j,false);
                }
                for(int j=choice.length;j<3;++j){
                    setPositionRecipeChoice(menu,null,i,j,true);
                }
            }
            for(int i=recipeChi.length;i<3;i++){
                for(int j=0;j<3;++j){
                    setPositionRecipeChoice(menu,null,i,j,true);
                }
            }
        }
        public boolean setPositionRecipeChoice(ChestMenu menu,RecipeChoice choice,int height,int width,boolean replaceDataHolder){
            var re=getShapeFromExtraData(getExtraData(menu));
            if(height<re.getFirstValue()&&width<re.getSecondValue()){
                if(replaceDataHolder){
                    try{
                        getRecipeChoice(menu)[height][width]=choice;
                    }catch (Throwable e){}
                }
                menu.replaceExistingItem(recipeSlots[height][width],choice==null?null:choice.getItemStack());
                return true;
            }else {
                menu.replaceExistingItem(recipeSlots[height][width],nullSlotIcon);
            }
            return false;
        }
        public RecipeChoice getPositionRecipeChoice(ChestMenu menu,int i,int j){
            var re=getShapeFromExtraData(getExtraData(menu));
            if(i<re.getFirstValue()&&j<re.getSecondValue()){
                try{
                    return getRecipeChoice(menu)[i][j];
                }catch(Throwable e){}
            }
            return null;
        }
        public String getType(ChestMenu menu){
            return getDataHolder(menu).getString(0);
        }
        public StringHashMap getExtraData(ChestMenu menu){
            return StringHashMap.ofNewMap((StringHashMap)getDataHolder(menu).getObject(1));
        }
        public void resizeRecipeChoice(ChestMenu menu,StringHashMap oldData,StringHashMap newData){
            var re1=getShapeFromExtraData(oldData);
            var re2=getShapeFromExtraData(newData);
            if(re1.getSecondValue()!=re2.getSecondValue()||re1.getFirstValue()!=re2.getFirstValue()){
                RecipeChoice[][] recipeChoices=getRecipeChoice(menu);
                setRecipeChoice(menu,getShapedRecipeChoiceArrayTo2Dim(getShapedRecipChoiceArrayFrom2Diim(recipeChoices),re2.getFirstValue(),re2.getSecondValue()));
            }
        }
        public void setExtraData(ChestMenu menu,StringHashMap map){
            StringHashMap map1=getExtraData(menu);
            getDataHolder(menu).setObject(1,map);
            menu.replaceExistingItem(extraDataSlot,getElseDataIcon(map));
            resizeRecipeChoice(menu,map1,map);
        }
        public void setType(ChestMenu menu,String value){getDataHolder(menu).setString(0,value);
        menu.replaceExistingItem(typeSlot,AddUtils.addLore(typeIcon.getOrDefault(value,typeIconLost),"&7点击切换"));}
        public void loadRecipe(ChestMenu menu, NamespacedKey key){
            getDataHolder(menu).setString(1,key.toString());
            RecipeAccessor accessor=ofExist(key);
            setType(menu,accessor.getType());
            StringHashMap map=accessor.getExtraData();
            setExtraData(menu,map);
            var re=getShapeFromExtraData(map);
            setRecipeChoice(menu,getShapedRecipeChoiceArrayTo2Dim(accessor.getRecipeChoice(),re.getFirstValue(),re.getSecondValue()));
            menu.replaceExistingItem(outputSlot,accessor.getOutput());
            setEnable(menu,accessor.getEnable()!=0);
        }
        ItemStack enable=new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE,"&6配方状态: &a启用");
        ItemStack disable=new CustomItemStack(Material.RED_STAINED_GLASS_PANE,"&6配方状态: &c禁用");
        public boolean getEnable(ChestMenu menu){
            ItemStack stack=menu.getItemInSlot(enableSlot);
            return stack!=null&&stack.getType()==Material.GREEN_STAINED_GLASS_PANE;
        }
        public void setEnable(ChestMenu menu,boolean enable){
            menu.replaceExistingItem(enableSlot,enable?this.enable:this.disable);
        }
        public void runAsyncRecipeChoiceIconChange(ChestMenu menu,int index){
            RecipeChoice[][] choices=getRecipeChoice(menu);
            for(int i=0;i<choices.length;++i){
                RecipeChoice[] choice=choices[i];
                for(int j=0;j<choice.length;++j){
                    List<ItemStack> stack=getRecipeChoiceIconList(choice[j]);
                    if(stack!=null&&stack.size()>1){
                        menu.replaceExistingItem(recipeSlots[i][j],stack.get(index%stack.size()));
                    }
                }
            }
        }
        public void provideRecipeInstanceInfoOrFail(ChestMenu menu,RecipeBuilder builder,Consumer<String> errorMessage){
            NamespacedKey key=ofNSKey(getDataHolder(menu).getString(1));
            if(key==null){
                errorMessage.accept("&c无效的ID");
                return;
            }
            String type=getType(menu);
            if(!supportedRecipeType.contains(type)){
                errorMessage.accept("&c无效的配方类型");
                return;
            }
            RecipeChoice[] choice=getShapedRecipChoiceArrayFrom2Diim( getRecipeChoice(menu));
            ItemStack output=menu.getItemInSlot(outputSlot);
            if(output==null){
                errorMessage.accept("&c无效的输出");
                return;
            }
            StringHashMap map=getExtraData(menu);
            boolean enable=getEnable(menu);
            builder.build(key,type,choice,output,map,enable);
        }
        ItemStack editor=new CustomItemStack(Material.DEBUG_STICK,"&a修改按钮","&7修改该按钮右侧的配方选项信息");
        public ChestMenu apply(int i____){
            ChestMenu menu=new ChestMenu("&a原版配方编辑器");
            menu.setEmptySlotsClickable(true);
            menu.setPlayerInventoryClickable(true);
            IntStream.range(0,45).forEach(i->menu.addItem(i,ChestMenuUtils.getBackground(),ChestMenuUtils.getEmptyClickHandler()));
            for (int i=0;i<3;++i){
                for(int j=0;j<3;++j){
                    final int i_=i;
                    final int j_=j;
                    menu.replaceExistingItem(recipeSlots[i][j],null);
                    menu.addMenuClickHandler(recipeSlots[i][j],((player, i1, itemStack, clickAction) -> {
                        List<ItemStack> choices=getRecipeChoiceIconList(getPositionRecipeChoice(menu,i_,j_));
                        if(choices!=null)
                            openRecipeChoiceView(choices,player,(player1 -> menu.open(player1)));
                        else {
                            AddUtils.sendMessage(player,"&a此处无选项,无法预览");
                        }
                        return false;
                    }));
                    menu.replaceExistingItem(editorSlots[i][j],editor);
                    menu.addMenuClickHandler(editorSlots[i][j],((player, i1, itemStack, clickAction) -> {
                        //todo open edit RecipeChoice page!
                        RecipeChoice oldRecipeChoice=getPositionRecipeChoice(menu,i_,j_);
                        List<ItemStack> choices=getRecipeChoiceIconList(oldRecipeChoice);
                        choices=choices==null?new ArrayList<>():choices;

                        getRecipeChoiceBuilder(choices,(player1,choice) -> {
                            if(!compareRecipeChoice(oldRecipeChoice,choice)){
                                setPositionRecipeChoice(menu,choice,i_,j_,true);
                            }else {
                                AddUtils.sendMessage(player1,"&c新的配方选项相对于先前的配方选项来说无效");
                            }
                            menu.open(player1);
                        },(player1 -> menu.open(player1))).openMenu(player);

                        return false;
                    }));
                }
            }
            menu.replaceExistingItem(outputSlot,null);
            menu.addMenuClickHandler(outputSlot,((player, i, itemStack, clickAction) -> true));
            menu.replaceExistingItem(saveSF,new CustomItemStack(Material.BOOK,"&a保存数据到配置文件","会尽可能的解析物品为id","&7会将粘液物品存储为粘液id","&7会尝试解析为物品库的id","&7推荐的模式"));
            menu.addMenuClickHandler(saveSF,((player, i, itemStack, clickAction) -> {
                provideRecipeInstanceInfoOrFail(menu,((id,type,choice,output,extraData,enable) -> {
                    RecipeAccessor accessor;
                    if((accessor=ofExist(id))!=null){
                        overrideVanillaRecipe(id,accessor,type,choice,output,enable?1:0,extraData,SaveMethod.USE_ID);
                        AddUtils.sendMessage(player,"&c保存成功");
                    }
                    else{
                        AddUtils.sendMessage(player,"&c这不是有效的配方ID,请检查ID或者点击创建新配方");
                    }
                }),(s->AddUtils.sendMessage(player,s)));
                return false;
            }));
            menu.replaceExistingItem(saveSC,new CustomItemStack(Material.BOOK,"&c强制保存模式","将所有带nbt的物品创建物品库项目保存","&7会生成大量配置文件","&7不推荐的模式"));
            menu.addMenuClickHandler(saveSC,((player, i, itemStack, clickAction) -> {
                provideRecipeInstanceInfoOrFail(menu,((id,type,choice,output,extraData,enable) -> {
                    RecipeAccessor accessor;
                    if((accessor=ofExist(id))!=null){
                        overrideVanillaRecipe(id,accessor,type,choice,output,enable?1:0,extraData,SaveMethod.USE_CS);
                        AddUtils.sendMessage(player,"&c保存成功");
                    }
                    else{
                        AddUtils.sendMessage(player,"&c这不是有效的配方ID,请检查ID或者点击创建新配方");
                    }
                }),(s->AddUtils.sendMessage(player,s)));
                return false;
            }));
            menu.addMenuClickHandler(createSF,((player, i, itemStack, clickAction) -> {
                player.closeInventory();
                AddUtils.sendMessage(player,"&a输入需要创建的配方ID");
                ChatUtils.awaitInput(player,(string -> {
                    menu.open(player);
                    final String Ustring=string.toUpperCase();
                    getDataHolder(menu).setString(1,Ustring);
                    provideRecipeInstanceInfoOrFail(menu,((id,type,choice,output,extraData,enable) -> {
                        createVanillaRecipe(id,type,choice,output,enable?1:0,extraData,SaveMethod.USE_ID);
                        AddUtils.sendMessage(player,"&a成功创建配方! ID: &f"+Ustring);
                    }),(s->AddUtils.sendMessage(player,s)));
                }));
                return false;
            }));
            menu.replaceExistingItem(createSF,new CustomItemStack(Material.ENCHANTED_BOOK,"&a创建新的配方","在物品槽放入目标物品,并设置必要参数","","&7会将粘液物品存储为粘液id","&7会尝试解析为物品库的id","&7推荐的模式"));
            menu.addMenuClickHandler(createSC,((player, i, itemStack, clickAction) -> {
                player.closeInventory();
                AddUtils.sendMessage(player,"&a输入需要创建的配方ID");
                ChatUtils.awaitInput(player,(string -> {
                    menu.open(player);
                    final String Ustring=string.toUpperCase();
                    getDataHolder(menu).setString(1,Ustring);
                    provideRecipeInstanceInfoOrFail(menu,((id,type,choice,output,extraData,enable) -> {
                        createVanillaRecipe(id,type,choice,output,enable?1:0,extraData,SaveMethod.USE_ID);
                        AddUtils.sendMessage(player,"&a成功创建配方! ID: &f"+Ustring);
                    }),(s->AddUtils.sendMessage(player,s)));
                }));
                return false;
            }));
            menu.replaceExistingItem(createSC,new CustomItemStack(Material.ENCHANTED_BOOK,"&a创建新的配方","在物品槽放入目标物品,并设置必要参数","","&c采取强制保存模式","&c将所有带nbt的物品创建物品库项目保存","&7会生成大量配置文件","&7不推荐的模式"));
            menu.replaceExistingItem(loadRecipe,new CustomItemStack(Material.BEACON,"&a点击加载一个配方实例",
                    "","&7也可以选择在下方按钮从全部配方中选取一个"));
            menu.addMenuClickHandler(loadRecipe,((player, i, itemStack, clickAction) -> {
                player.closeInventory();
                AddUtils.sendMessage(player,"&e请输入NamespacedKey");
                ChatUtils.awaitInput(player,(string -> {
                    NamespacedKey key=ofNSKey(string);
                    if(key!=null&&Bukkit.getRecipe(key)!=null){
                        loadRecipe(menu,key);
                    }else if(modifiedRecipeAccessors.containsKey(key)){
                        loadRecipe(menu,key);
                    }
                    else {
                        AddUtils.sendMessage(player,"&c无效的ID");
                    }
                    menu.open(player);
                }));
                return false;
            }));
            menu.replaceExistingItem(selectSlot,new CustomItemStack(Material.KNOWLEDGE_BOOK,"&a从所有的支持的配方中选择一个","&7使用前请先设置配方类型"));
            menu.addMenuClickHandler(selectSlot,((player, i, itemStack, clickAction) -> {
                String type=getType(menu);
                if(!supportedRecipeType.contains(type)){
                    AddUtils.sendMessage(player,"&c不支持的配方类型");
                }else {
                    getRecipeSelectMenu(player,type,(p,key)->{
                        loadRecipe(menu,key);
                        menu.open(player);
                    },(p)->menu.open(player));
                }
                return false;
            }));
            final List<String> typeSeq=new ArrayList<>(supportedRecipeType);
            final AtomicInteger index=new AtomicInteger(0);
            setType(menu,typeSeq.get(0));
            menu.addMenuClickHandler(typeSlot,((player, i, itemStack, clickAction) -> {
                int next=index.incrementAndGet();
                setType(menu,typeSeq.get(next%typeSeq.size()));
                return false;
            }));
            setEnable(menu,false);
            menu.addMenuClickHandler(enableSlot,((player, i, itemStack, clickAction) -> {
                setEnable(menu,!getEnable(menu));return false;
            }));
            setExtraData(menu,StringHashMap.ofNewMap(null));
            menu.addMenuClickHandler(extraDataSlot,((player, i, itemStack, clickAction) -> {
                player.closeInventory();
                AddUtils.sendMessage(player,"输入键名,请使用英文,输入\"取消\"则取消");
                ChatUtils.awaitInput(player,(string -> {
                    if(Pattern.matches("[a-zA-Z_0-9]*",string)){
                        AddUtils.sendMessage(player,"输入值:(输入null删除键)");
                        ChatUtils.awaitInput(player,(string1 -> {
                            StringHashMap map= getExtraData(menu);
                            if(string1!=null&&!"null".equals(string1))
                                map.put(string,string1);
                            else
                                map.remove(string);
                            setExtraData(menu,map);
                            menu.open(player);
                        }));
                    }else {
                        AddUtils.sendMessage(player,"&a已取消");
                    }
                }));
                return false;
            }));
            ItemStack clearItem=new CustomItemStack(Material.BARRIER,"&c清除该配方的全部修改","&c并删除配置文件相关内容","&c谨慎点击!");
            ItemStack clearTwice=AddUtils.addLore( new CustomItemStack(Material.BARRIER,"&c清除该配方的全部更改","&e请在5秒内二次确认!","&e请在五秒内二次确认"));
            menu.replaceExistingItem(clearSlot,clearItem);
            final AtomicBoolean pendingClear=new AtomicBoolean(false);
            menu.addMenuClickHandler(clearSlot,((player, i, itemStack, clickAction) -> {
                if(!pendingClear.get()){
                    pendingClear.set(true);
                    menu.replaceExistingItem(clearSlot,clearTwice);
                    AddUtils.sendMessage(player,"&e你点击了清除设置按钮!");
                    AddUtils.sendMessage(player,"&e请在5秒内二次确认你的选择!");
                    AddUtils.sendMessage(player,"&e五秒过后将自动取消清除");
                    SlimefunTools.runSyncLater(()->{pendingClear.set(false);menu.replaceExistingItem(clearSlot,clearItem);AddUtils.sendMessage(player,"&a已取消清除物品的全部更改");},100);
                    return false;
                }else {
                    pendingClear.set(false);
                    menu.replaceExistingItem(clearSlot,clearItem);
                    DataContainer dh=getDataHolder(menu);
                    NamespacedKey id=ofNSKey( dh.getString(1));
                    if(id==null){
                        AddUtils.sendMessage(player,"&c请先加载一个有效的id");
                        return false;
                    }
                    deleteVanillaRecipeConfig(id);
                    AddUtils.sendMessage(player,"&c成功撤销所有改动!");
                    return false;
                }
            }));
            menu.replaceExistingItem(selectAllModifies,new CustomItemStack(Material.REPEATING_COMMAND_BLOCK,"&c预览全部已修改物品","&7点击打开预览界面"));
            menu.addMenuClickHandler(selectAllModifies,(((player, i, itemStack, clickAction) -> {
                openDisplayAllModifiedMenu(player);
                return false;
            })));
            AtomicReference<BukkitRunnable> asyncRefreshRecipeChoiceThread=new AtomicReference<>(null);
            menu.addMenuOpeningHandler(player -> {
                BukkitRunnable r=asyncRefreshRecipeChoiceThread.get();
                if (r != null&&!r.isCancelled()) {
                    r.cancel();
                }
                r=new BukkitRunnable() {
                    int index=0;
                    public void run() {
                        runAsyncRecipeChoiceIconChange(menu,index++);
                    }
                };
                r.runTaskTimerAsynchronously(plugin,0,20);
                asyncRefreshRecipeChoiceThread.set(r);
            });
            menu.addMenuCloseHandler(player -> {
                BukkitRunnable runnable=asyncRefreshRecipeChoiceThread.getAndSet(null);
                if(runnable!=null&&!runnable.isCancelled()){
                    runnable.cancel();
                }
            });
            return menu;
        }
    }
    private RecipeEditorGui RECIPEEDITOR_GUI_PRESET=new RecipeEditorGui();
    private HashMap<UUID,CustomMenu> PLAYER_EDITORGUI=new HashMap<>();
    private CustomMenu openEditorGui(Player player){
        return PLAYER_EDITORGUI.computeIfAbsent(player.getUniqueId(),i->new CustomMenu(null,1,RECIPEEDITOR_GUI_PRESET));
    }
    private CustomMenu opeKeyedRecipeInfo(Recipe recipe){
        if(recipe instanceof Keyed keyed){
            return new CustomMenu(null,1,new RecipeInfoGui(ofExist(keyed.getKey())));
        }else{
            return null;
        }
    }
    private void openDisplayAllModifiedMenu(Player player){
        ChestMenu menu=openEditorGui(player).getMenu();
        HashMap<String,InstanceModifyRecord<RecipeAccessor>> dataMap=new HashMap<>();
        modifiedRecipeAccessors.forEach((s,j)->dataMap.put(s.toString(),j));
        MenuUtils.openSelectMenu(player,1,null,dataMap,(cm)->(r,p)->{
                    RECIPEEDITOR_GUI_PRESET.loadRecipe(menu,ofNSKey(r.getInstance().getNamespacedKey()));
                    menu.open(p);
                },
                (cm)->  (r,p)->{
                    CustomMenu menu1= opeKeyedRecipeInfo(r.getInstance().getRecipe());
                    menu1.getMenu().addMenuClickHandler(0,((player1, i1, itemStack1, clickAction1) ->{ cm.getMenu().open(player1);return false;}));
                    menu1.getMenu().replaceExistingItem(0,MenuUtils.getBackButton("&f返回列表"));
                    menu1.openMenu(player);
                },
                r->AddUtils.addLore(r.getInstance().getOutput(),"","&a配方ID: &f"+r.getInstance().getNamespacedKey(),"&a点击载入配方编辑界面"),(cm,p)->menu.open(p));
    }
    private void getRecipeSelectMenu(Player player,String type,BiConsumer<Player,NamespacedKey> selected,Consumer<Player> fallback){
        switch (type){
            case "shaped":{
                MenuUtils.openSelectMenu(player,1,null,getShapedRecipeMap(),
                        (cm)->(s,p)->{
                            selected.accept(p,s.getKey());
                        },
                        (cm)->(s,p)->{
                            CustomMenu menu= opeKeyedRecipeInfo(s);
                            menu.getMenu().addMenuClickHandler(0,((player1, i, itemStack, clickAction) ->{ cm.getMenu().open(player1);return false;}));
                            menu.getMenu().replaceExistingItem(0,MenuUtils.getBackButton("&f返回列表"));
                            menu.openMenu(player);
                        },
                        (s)->AddUtils.addLore(shapedIconGen.apply(s),"&a点击选择","&ashift点击查看详细信息"),
                        ((customMenu, player1) -> fallback.accept(player1))
                        );
                break;
            }
            case "unshaped":{
                MenuUtils.openSelectMenu(player,1,null,getUnshapedRecipeMap(),
                        (cm)->(s,p)->{
                            selected.accept(p,s.getKey());
                        },
                        (cm)->(s,p)->{
                            CustomMenu menu= opeKeyedRecipeInfo(s);
                            menu.getMenu().addMenuClickHandler(0,((player1, i, itemStack, clickAction) ->{ cm.getMenu().open(player1);return false;}));
                            menu.getMenu().replaceExistingItem(0,MenuUtils.getBackButton("&f返回列表"));
                            menu.openMenu(player);
                        },
                        (s)->AddUtils.addLore(unshapedIconGen.apply(s),"&a点击选择","&ashift点击查看详细信息"),
                        ((customMenu, player1) -> fallback.accept(player1))
                        );
                break;
            }
        }
    }
    private Function<ShapedRecipe,ItemStack> shapedIconGen=(sr)->AddUtils.addLore(sr.getResult(),"","&e有序配方","&a命名空间ID: &f"+sr.getKey().toString());
    private Function<ShapelessRecipe,ItemStack> unshapedIconGen=(sr)->AddUtils.addLore(sr.getResult(),"","&e无序配方","&a命名空间ID: &f"+sr.getKey().toString());
    private Pair<List<ItemStack>,List<CustomMenuGroup.CustomMenuClickHandler>> getDisplayed(String stats, String filter){
        switch (stats){
            case "shaped":return MenuUtils.getSelector(filter==null?null:filter.toLowerCase(),
                    (cm)->(s,player)->AddUtils.displayCopyString(player,"单击此处拷贝字符串","点击复制到剪贴板",s.getKey().toString()),
                    (cm)->(s,player)->{
                        CustomMenu menu= opeKeyedRecipeInfo(s);
                        menu.getMenu().addMenuClickHandler(0,((player1, i, itemStack, clickAction) ->{ cm.getMenu().open(player1);return false;}));
                        menu.getMenu().replaceExistingItem(0,MenuUtils.getBackButton("&f返回列表"));
                        menu.openMenu(player);
                    },
                    getShapedRecipeMap().entrySet().iterator(),
                    (c)->{ return AddUtils.addLore(shapedIconGen.apply(c),"&a点击拷贝","&ashift点击查看详细信息");
            });
            case "unshaped":return MenuUtils.getSelector(filter==null?null:filter.toLowerCase(),
                    (cm)->(s,player)-> AddUtils.displayCopyString(player,"单击此处拷贝字符串","点击复制到剪贴板",s.getKey().toString()),
                    (cm)->(s,player)->{
                        CustomMenu menu= opeKeyedRecipeInfo(s);
                        menu.getMenu().addMenuClickHandler(0,((player1, i, itemStack, clickAction) ->{ cm.getMenu().open(player1);return false;}));
                        menu.getMenu().replaceExistingItem(0, MenuUtils.getBackButton("&f返回列表"));
                        menu.openMenu(player);
                    },
                    getUnshapedRecipeMap().entrySet().iterator(),
                    (c)->{ return AddUtils.addLore(unshapedIconGen.apply(c),"&a点击拷贝","&ashift点击查看详细信息");
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
    private SubCommand mainCommand=new SubCommand("vcinject",
            new SimpleCommandArgs("_operation"),"/vcinject [_operation] [args]"
    )
            .setTabCompletor("_operation",()->getSubCommands().stream().map(SubCommand::getName).toList());//no

    private SubCommand statsCommand=new SubCommand("stats",
            new SimpleCommandArgs("type","page"),"/vninject stats [type] [page] 打开[type]粘液统计数据界面的第[page]页"){
        @Override
        public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
            var inputInfo=parseInput(var4).getFirstValue();
            String statsType=inputInfo.nextArg();
            if(statsType!=null&&VanillaCraftingInjecter.this.statsType.contains(statsType)){
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
            .setTabCompletor("type",()->new ArrayList<>(statsType))
            .setTabCompletor("page",()->List.of("1","2","3","4","5","6"))
            .register(this);
    private SubCommand editorCommand=new SubCommand("editor",
            new SimpleCommandArgs("type"),"/vninject editor [type] 打开[type]型编辑界面"){
        @Override
        public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
            var inputInfo=parseInput(var4).getFirstValue();
            String statsType=inputInfo.nextArg();
            if(statsType!=null){
                Player executor=isPlayer(var1,true);
                if (executor!=null){
                    if("list".equalsIgnoreCase(statsType)){
                        //
                        openDisplayAllModifiedMenu(executor);
                    }else {
                        openEditorGui(executor).openMenu(executor);;
                    }
                }
            }else {
                AddUtils.sendMessage(var1,"&c无效的编辑器类型");
            }
            return true;
        }
    }
            .setDefault("type","3X3")
            .setTabCompletor("type",()->List.of("3X3","list"))
            .register(this);
    //TODO 预览
    //TODO 普通配方预览 删除
    //TODO 编辑界面 创建界面

    public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4){
        if(var1.hasPermission("slimefuntool.command.vcinject")){
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
    private Player isPlayer(CommandSender sender, boolean sendMessage){
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
