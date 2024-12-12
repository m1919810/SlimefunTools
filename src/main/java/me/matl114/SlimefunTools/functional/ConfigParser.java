package me.matl114.SlimefunTools.functional;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import me.matl114.SlimefunTools.core.CustomItemBase;
import me.matl114.SlimefunTools.core.ItemRegister;
import me.matl114.SlimefunTools.implement.Debug;

import me.matl114.matlib.Utils.Algorithm.StringHashMap;
import me.matl114.matlib.Utils.Command.CommandUtils;
import me.matl114.matlib.Utils.Utils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class ConfigParser<T extends Object> implements StaticFunctional{
    /**
     * Most of these functional Units requires ItemRegister load
     * @param str
     * @return
     */
    public static HashMap<Class,ConfigParser> Identifiers=new HashMap<>();
    private static String c(String... str){
        return String.join(".", str);
    }
    private static NamespacedKey ofNSKey(String namespacedKey){
        String[] re= namespacedKey.split(":");
        if(re.length==2){
            return new NamespacedKey(re[0],re[1]);
        }else return null;
    }
    private static ConfigParser<ItemStack> itemParser=new ConfigParser<ItemStack>()
            .setDeserializer(((config, string) -> {
                return ItemRegister.git(config.getString(string));
            }))
            .setSerializer(((config, parent, value,mode,elseData) -> {
                if(mode==SaveMethod.NONE){return;}
                if(value==null){
                    config.setValue(parent,"null");return;
                }
                String defaultName=(elseData==null||elseData.length==0)? CustomItemBase.getManager().getDefaultName() : elseData[0];
                switch (mode){
                    case USE_CS -> {
                        if(value.hasItemMeta()){
                            config.setValue(parent, CustomItemBase.getManager().getItemIdOrCreate(value,()->defaultName));
                        }else {
                            config.setValue(parent,ItemRegister. dmt(value.getType()));
                        }
                    }
                    case USE_ID -> {
                        config.setValue(parent, ItemRegister.ditOa(value,()-> defaultName));
                    }
                    default -> {}
                }
            }))
            .setIdentifier(ItemStack.class);
    private static ConfigParser<ItemGroup> itemGroupParser=new ConfigParser<ItemGroup>()
            .setDeserializer(((config, string) -> {
                String value=config.getString(string);
                NamespacedKey key= ofNSKey(value);
                if(key==null){
                    Debug.logger("Error! in ",config.getFile().toString()," path: ",string,", invalid NamespacedKey: ",value);
                    return null;
                }else {
                    return Utils.runIfNull(ItemRegister.getManager().getGroup(value),()->Debug.logger("ItemGroup not found or not supported: ",value));
                }
            }))
            .setSerializer(((config, parent, value,mode,elseData) -> {
                if(mode==SaveMethod.NONE){return;}
                config.setValue(parent,value.getKey().toString());
            }))
            .setIdentifier(ItemGroup.class);
    private static ConfigParser<ItemStack[]> itemListParser=new ConfigParser<ItemStack[]>()
            .setDeserializer(((config, string) -> {
                List<String> value=config.getStringList(string);
                return value.stream().map(ItemRegister::git).toArray(ItemStack[]::new);
            }))
            .setSerializer(((config, parent, value,mode,elseData) -> {
                if(mode==SaveMethod.NONE){return;}
                if(value==null||value.length==0){
                    config.setValue(parent,new ArrayList<>());return;
                }
                String defaultName=(elseData==null||elseData.length==0)?CustomItemBase.getManager().getDefaultName() : elseData[0];
                List<String> items= new ArrayList<>();
                for(int j=0;j<value.length;++j){
                    final int index=j;
                    ItemStack val=value[index];
                    switch (mode){
                        case USE_CS -> {
                            if(val.hasItemMeta()){
                                items.add( CustomItemBase.getManager().getItemIdOrCreate(val,()->(defaultName+"_"+index)));
                            }else {
                                items.add( ItemRegister.dmt(val.getType()));
                            }
                        }
                        case USE_ID -> {
                            items.add(ItemRegister.ditOa(val,()->(defaultName+"_"+index)));
                        }
                    }
                }
                config.setValue(parent,items);
            }))
            .setIdentifier(ItemStack[].class);
    private static ConfigParser<RecipeType> recipeTypeConfigParser=new ConfigParser<RecipeType>()
            .setDeserializer(((config, string) -> {
                String value=config.getString(string);
                return Utils.runIfNull(ItemRegister.getManager().getRtype(value),()->Debug.logger("RecipeType not found or not supported: ",value) );
            }))
            .setSerializer(((config, parent, value,mode,elseData) -> {
                config.setValue(parent,value.getKey().toString());
            }))
            .setIdentifier(RecipeType.class);
    private static ConfigParser<Integer> integerConfigParser=new ConfigParser<Integer>()
            .setDeserializer(((config, string) -> CommandUtils.parseIntegerOrDefault(config.getString(string),null)))
            .setSerializer(((config, parent, value,mode,elseData) -> config.setValue(parent,String.valueOf((Integer)value))))
            .setIdentifier(Integer.class);
    private static ConfigParser<String> stringConfigParser=new ConfigParser<String>()
            .setDeserializer((Config::getString))
            .setSerializer(((config, parent, value,mode,elseData) -> config.setValue(parent,value)))
            .setIdentifier(String.class);

    private static ConfigParser<RecipeChoice[]> recipeChoiceParser=new ConfigParser<RecipeChoice[]>()
            .setDeserializer((config,string)->{
                return IntStream.range(0,9).mapToObj(i->c(string,String.valueOf(i))).map((s)->{
                    String type=config.getString(c(s,"type"));
                    if(type==null)return null;
                    switch(type){
                        case "exact":
                            List<String> exactChoices=config.getStringList(c(s,"choices"));
                            if(exactChoices==null||exactChoices.isEmpty()){
                                Debug.logger("Error in deserialize! Invalid recipeChoice choices list in ",s);
                                return null;
                            }
                            return new RecipeChoice.ExactChoice(exactChoices.stream().filter(str->str!=null).map(ItemRegister::git).toArray(ItemStack[]::new));
                        case "material":
                            List<String> materialChoices=config.getStringList(c(s,"choices"));
                            if(materialChoices==null||materialChoices.isEmpty()){
                                Debug.logger("Error in deserialize! Invalid recipeChoice choices list in ",s);
                                return null;
                            }
                            return new RecipeChoice.MaterialChoice(materialChoices.stream().filter(str->str!=null).map(str->str.replace("minecraft:","")).map(String::toUpperCase).map(Material::getMaterial).toArray(Material[]::new));
                        default:
                            Debug.logger("Error in deserialize! Invalid recipeChoice type :",type," in ",s);
                            return null;
                    }
                }).toArray(RecipeChoice[]::new);
            })
            .setSerializer(((config, parent, value, saveMethod, elseData) -> {
                RecipeChoice[] recipe=(RecipeChoice[])value;
                List<String> recipeString=new ArrayList<>();
                //清除之前的值
                for(int i=0;i<recipe.length;i++){
                    final String index=String.valueOf(i);
                    RecipeChoice choice=recipe[i];
                    if(choice!=null){
                        if(choice instanceof RecipeChoice.ExactChoice ec) {
                            config.setValue(c(parent,index,"type"),"exact");
                            config.setValue(c(parent,index,"choices"),ec.getChoices().stream().filter(str->str!=null).map(stack ->ItemRegister.ditOa(stack,()->"_"+elseData[0]+"_vanilla_crafting_"+index+"_")).toList());
                        }else if(choice instanceof RecipeChoice.MaterialChoice mc) {
                            config.setValue(c(parent,index,"type"),"material");
                            config.setValue(c(parent,index,"choices"),mc.getChoices().stream().filter(str->str!=null).map(Material::toString).toList());
                        }else{
                            Debug.logger("Error in serialize!Unsupported recipeChoice class :",choice.getClass());
                        }
                    }
                }
            }))
            .setIdentifier(RecipeChoice[].class);
    //only the map of string,not recursively map
    private static ConfigParser<StringHashMap> hashmapParser=new ConfigParser<StringHashMap>()
            .setIdentifier(StringHashMap.class)
            .setDeserializer(((config, string) -> {
                Set<String> keys=config.getKeys(string);
                if(keys==null||keys.isEmpty()){
                    return new StringHashMap();
                }else{
                    StringHashMap valuePair=new StringHashMap();
                    for(String key:keys){
                        String value=config.getString(c(string,key));
                        if(value!=null){
                            valuePair.put(key,value);
                        }
                    }
                    return valuePair;
                }
            }))
            .setSerializer(((config, parent, value, saveMethod, elseData) -> {
                Debug.logger("extraData save called");
                for(Map.Entry<String,String> entry:(value).entrySet()){
                    config.setValue(c(parent,entry.getKey()),entry.getValue());
                }
            }));

    //the map of String to Object? maybe we need a Map.class? or sth else
    //Parsers register moved to here
    public static ConfigParser getIdentifier(Class clazz){
        ConfigParser parser= Identifiers.get(clazz);
        if(parser==null){
            throw new RuntimeException("Error in config parser sets: class parser not registered :"+clazz.getName());
        }
        else if(clazz==parser.getId()){
            return parser;
        }else {
            throw new RuntimeException("Error in config parser sets: unmatched Identifier "+clazz.getName());
        }
    }
    Class identifier;
    public Class getId(){
        return identifier;
    }
    public ConfigParser<T> setIdentifier(Class clazz){
        assert this.configDeserializer!=null&&this.configSerializer != null;
        this.identifier = clazz;
        Identifiers.put(clazz, this);
        return this;
    }
    public interface ConfigSerizalizer<T>{
        public void serialize(Config config, String parent, T value, SaveMethod saveMethod, String... elseData);
        default void serialize(Config config,String parent,T value,SaveMethod saveMethod){
            serialize(config,parent,value,saveMethod,null);
        }
    }
    public T deserialize(Config config,String parent){
        return configDeserializer.apply(config,parent);
    }
    public static void serialize(Config config,String parent,Object value,Class type,SaveMethod saveMethod,String... elseData){
        try{
            assert value==null||type.isInstance(value);
            config.setValue(parent,null);
            if(value!=null) {
                ConfigParser parser=ConfigParser.getIdentifier(type);
                //清除当前值
                //将新值设置上去
                parser.configSerializer.serialize(config,parent,value,saveMethod,elseData);
                config.save();
            }
        }catch(Throwable e){
            Debug.logger("Error in config serialization ");
            Debug.logger(e);
        }
    }
    public static Object deserialize(Config config,String parent,Class type){
        try{
            ConfigParser parser=ConfigParser.getIdentifier(type);
            return parser.configDeserializer.apply(config,parent);
        }catch (Throwable e){
            Debug.logger("Error in config deserialization");
            Debug.logger(e);
            return null;
        }
    }
    BiFunction<Config,String,T> configDeserializer;
    ConfigSerizalizer<T> configSerializer;
    public ConfigParser<T> setDeserializer(BiFunction<Config,String,T> deserializer){
        this.configDeserializer = deserializer;
        return this;
    }
    public ConfigParser<T> setSerializer(ConfigSerizalizer<T> serializer){
        this.configSerializer = serializer;
        return this;
    }
}