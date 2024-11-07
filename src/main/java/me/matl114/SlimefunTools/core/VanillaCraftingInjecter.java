package me.matl114.SlimefunTools.core;

import com.google.common.base.Preconditions;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import lombok.Getter;
import me.matl114.SlimefunTools.functional.*;
import me.matl114.SlimefunTools.implement.ConfigLoader;
import me.matl114.SlimefunTools.implement.Debug;
import me.matl114.SlimefunTools.utils.AddUtils;
import me.matl114.SlimefunTools.utils.ReflectUtils;
import me.matl114.SlimefunTools.utils.Settings;
import me.matl114.SlimefunTools.utils.StructureClass.Manager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;

import javax.naming.Name;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VanillaCraftingInjecter implements Manager , Listener {
    Plugin plugin;
    @Getter
    private VanillaCraftingInjecter manager;

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
            .setGetter("output",Recipe::getResult)
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
                return compareShapedRecipeChoice(r1,r2);
            })
            .setType("recipe",RecipeChoice[].class)
            .setGetter("extraData",(i)->null)
            .setSetter("extraData",(i,j)->{})
            .setType("extraData",HashMap.class);
    private Identifier<ShapelessRecipe> shapelessRecipeIdentifier=new Identifier<ShapelessRecipe>(VanillaCraftingInjecter.class)
            .setIdentifier(ShapelessRecipe.class)
            .setGetter("output",Recipe::getResult)
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
            .setGetter("extraData",(i)->null)
            .setSetter("extraData",(i,j)->{})
            .setType("extraData",HashMap.class);

    HashMap<String ,Class> recipeClassMapper=new HashMap<>(){{
        put("shaped",ShapedRecipe.class);
        put("unshaped", ShapelessRecipe.class);
    }};
    HashMap<String, RecipeAccessor.RecipeConstructor> recipeClassConstructor=new HashMap<>(){{
        put("shaped",VanillaCraftingInjecter.this::buildShapedRecipe);
        put("unshaped",VanillaCraftingInjecter.this::buildShapelessRecipe);
    }};
    private RecipeAccessor ofExist(NamespacedKey name){
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
    private RecipeAccessor of(NamespacedKey key,String type,RecipeChoice[] choices,ItemStack output,HashMap elseData){
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
            public Recipe getRecipe(NamespacedKey name,RecipeChoice[] choices,ItemStack otput,HashMap elseData);
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
                    if(enable){
                        Bukkit.removeRecipe(key);
                    }
                    Recipe newRecipe=constructor.getRecipe(key,getRecipeChoice(),getOutput(),getElseData());
                    if(enable){
                        Bukkit.addRecipe(newRecipe);
                    }
                    this.recipe=newRecipe;
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

        public void setExtraData(HashMap<String,String> extraData){

        }
        public ItemStack getOutput(){
            return (ItemStack) (identifier.get(recipe,"output"));
        }
        public void setOutput(ItemStack item){
            identifier.set(recipe,"output",item);
        }
        public RecipeChoice[] getRecipeChoice(){
            return (RecipeChoice[]) identifier.get(recipe,"recipe");
        }
        public void setRecipeChoice(RecipeChoice[] ingred){
            identifier.set(recipe,"recipe",ingred);
        }
        public RecipeAccessor register(){
            return this;
        }
        public HashMap getElseData(){
            return new HashMap();
        }
        public int getEnable(){
            return enable?1:0;
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
            .setComparator("recipe",(r,o1,o2)->r.getIdentifier().differentInternal(r.getRecipe(),"recipe",o1,o2))
            .setType("recipe",RecipeChoice[].class)
            .setGetter("output",RecipeAccessor::getOutput)
            .setSetter("output",(r,t)->r.setOutput((ItemStack)t))
            .setType("output",ItemStack.class)
            .setGetter("enable",RecipeAccessor::getEnable)
            .setSetter("enable",(r,s)->r.setEnabled((Integer)s))
            .setType("enable",Integer.class)
            .setGetter("extraData",RecipeAccessor::getElseData)
            .setSetter("extraData",(r,t)->r.setExtraData((HashMap<String, String>) t))
            .setType("extraData",HashMap.class);

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
        for(int i=0;i<map.length;i++){
            int index=map[i].length();
            for(int j=0;j<index;j++){
                result[3*i+j]=choice.get(map[i].charAt(j));
            }
        }
        return result;
    }
    private ShapedRecipe setShapedRecipeChoices(ShapedRecipe vanillaRecipe,RecipeChoice[] inputChoice){
        ReflectUtils.invokeSetRecursively(vanillaRecipe,ShapedRecipe.class,"ingredients",new HashMap<>());
        inputChoice=Arrays.copyOf(inputChoice,9);
        String[] pattern=new String[3];
        for(int i=0;i<3;i++){
            StringBuilder builder=new StringBuilder();
            for(int j=0;j<3;j++){
                builder.append(inputChoice[3*i+j]==null?" ":String.valueOf(3*i+j).charAt(0));
            }
            pattern[i]=builder.toString();
        }
        vanillaRecipe.shape(pattern);
        for(int i=0;i<9;i++){
            if(inputChoice[i]!=null){
                vanillaRecipe.setIngredient(String.valueOf(i).charAt(0),inputChoice[i]);
            }
        }
        return vanillaRecipe;
    }
    private boolean compareShapedRecipeChoice(RecipeChoice[] r1,RecipeChoice[] r2){
        for(int i=0;i<9;i++){
            if(r1.length<i&&r2.length<i){
                if(!compareRecipeChoice(r1[i],r2[i])){
                    return false;
                }
            }else if(r1.length<i){
                if(r1[i]!=null)return false;
            }else if(r2.length<i){
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
    private Recipe buildShapedRecipe(NamespacedKey id, RecipeChoice[] inputChoice, ItemStack output){
        return buildShapedRecipe(id,inputChoice,output,null);
    }
    private Recipe buildShapedRecipe(NamespacedKey id, RecipeChoice[] inputChoice, ItemStack output,HashMap elseData){
        ShapedRecipe vanillaRecipe=new ShapedRecipe(id,output);
        return setShapedRecipeChoices(vanillaRecipe,inputChoice);
    }
    private Recipe buildShapelessRecipe(NamespacedKey id, RecipeChoice[] inputChoice, ItemStack output){
        return buildShapedRecipe(id,inputChoice,output,null);
    }
    private Recipe buildShapelessRecipe(NamespacedKey id, RecipeChoice[] inputChoice, ItemStack output,HashMap elseData){
        ShapelessRecipe vanillaRecipe=new ShapelessRecipe(id,output);
        Arrays.stream(inputChoice).filter(s->s!=null).forEach(vanillaRecipe::addIngredient);
        return vanillaRecipe;
    }
    final HashMap<NamespacedKey,Recipe> craftingRecipeFromSlimefun = new HashMap<>();
    final HashMap<NamespacedKey,Recipe> registeredRecipe=new HashMap<>();
    final HashMap<NamespacedKey, InstanceModifyRecord<RecipeAccessor>> modifiedRecipeAccessors = new HashMap<>();
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
                HashMap elseData=null;
                if(Bukkit.getRecipe(key)==null){
                    RecipeAccessor accessor=of(key,type,recipe,output,elseData);
                    this.registeredRecipe.put(key,accessor.getRecipe());
                    overrideVanillaRecipe(key,accessor,null,null,null,enable,null,SaveMethod.NONE);
                }else {
                    RecipeAccessor accessor=ofExist(key);

                    overrideVanillaRecipe(key,accessor,type,recipe,output,enable,elseData,SaveMethod.NONE);
                }
            }
        }
    }
    private void overrideVanillaRecipe(NamespacedKey key, RecipeAccessor re, String type, RecipeChoice[] recipe, ItemStack output , Integer enable,HashMap elseData, SaveMethod saveMethod){
        InstanceModifyRecord<RecipeAccessor> record=modifiedRecipeAccessors.computeIfAbsent(key,(i)->new InstanceModifyRecord<RecipeAccessor>(re,this.vanillaCraftingConfig,RecipeAccessor::getNamespacedKey,RecipeAccessor.class,false)
        );
        if(type!=null&&record.checkDifference("type",type)){
            record.executeModification("type",type);
            record.executeDataSave("type",saveMethod);
        }
        if(recipe!=null&&record.checkDifference("recipe",recipe)){
            record.executeModification("recipe",recipe);
            record.executeDataSave("recipe",saveMethod,()->key.getNamespace()+"_"+key.getKey());
        }
        if(output!=null&&record.checkDifference("output",output)){
            record.executeModification("output",output);
            record.executeDataSave("output",saveMethod,()->"_"+key.getNamespace()+"_"+key.getKey()+"_vanilla_crafting_output_");
        }
        if(enable!=null&&record.checkDifference("enable",enable)){
            record.executeModification("enable",enable);
            record.executeDataSave("enable",saveMethod);
        }
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
    public void registerListeners(){
        plugin.getServer().getPluginManager().registerEvents(this,plugin);
    }
    public VanillaCraftingInjecter init(Plugin plugin, String... paths){
        Debug.logger("Enabling VanillaCrafting Injector");
        addToRegistry();
        this.plugin=plugin;
        this.vanillaCraftingPath=paths[0];
        this.vanillaCraftingConfig= ConfigLoader.loadExternalConfig(vanillaCraftingPath);
        sendRecipeFromSlimefunRecipeType();
        loadConfigs();
        registerListeners();
        return this;
    }
    public void deconstruct(){
        this.removeFromRegistry();
        //
        for(InstanceModifyRecord<RecipeAccessor> record:this.modifiedRecipeAccessors.values()){
            record.executeUndoModifications();
        }
        for(Map.Entry<NamespacedKey,Recipe> entry:this.registeredRecipe.entrySet()){
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








    //TODO 预览
    //TODO 普通配方预览 删除
    //TODO 编辑界面 创建界面





}
