package me.matl114.SlimefunTools.functional;

import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import lombok.Getter;
import me.matl114.SlimefunTools.implement.Debug;
import me.matl114.SlimefunTools.utils.Utils;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class Identifier<T extends Object> implements DynamicFunctional{
    public void onDisable(){
        Identifiers.remove(this.identifier);
    }
    public static Identifier<?> getIdentifier(Class clazz){
        var id=Identifiers.get(clazz);
        if(id==null){
            throw new RuntimeException("Error in Class Identifier sets: class Identifier not registered :"+clazz.getName());
        }
        if(clazz==id.getId()){
            return id;
        }else {
            throw new RuntimeException("Error in Class Identifier sets: unmatched Identifier "+clazz.getName());
        }
    }
    public static HashMap<Class,Identifier<?>> Identifiers=new HashMap<>();
    public Identifier(Class whichClass){
        registerFunctional(whichClass);
    }
    public Class identifier;
    public Class getId(){
        return identifier;
    }
    public Identifier<T> setIdentifier(Class identifier){
        this.identifier = identifier;
        Identifiers.put(identifier,this);
        return this;
    }
    public Identifier<T> setSetter(String attrName, BiConsumer<T,Object> setter){
        setters.put(attrName,setter);
        return this;
    }
    public Identifier<T> setType(String attrName,Class defaultClass){
        type.put(attrName,defaultClass);
        return this;
    }
    public Class getType(String attrName){
        if(type.containsKey(attrName)){
            return type.get(attrName);
        }else {
            throw new RuntimeException("Unregistered attribute "+attrName+" for type in"+identifier.getName());
        }
    }
    public Identifier<T> setGetter(String attrName,Function<T,Object> setter){

        getter.put(attrName,setter);
        return this;
    }
    public interface DifferenceJudger<T>{
        public boolean judge(T valu,Object o1, Object o2);
    }
    public Identifier<T> setComparator(String attrName, DifferenceJudger<T> cmp){
        comparator.put(attrName,cmp);
        return this;
    }
    private HashMap<String, BiConsumer<T,Object>> setters=new HashMap<>();
    private HashMap<String, Function<T,Object>> getter=new HashMap<>();
    private HashMap<String,DifferenceJudger<T>> comparator=new HashMap<>();
    private HashMap<String,Class> type=new HashMap<>();
    private static final HashMap<Class,BiPredicate> defaultJudgers=new HashMap<>(){{
        put(ItemStack.class,(s1,s2)->s1==s2?true:(s1==null?false:((ItemStack)s1).equals((ItemStack)s2 )));
        put(HashMap.class,(s1,s2)-> Utils.compareMap((HashMap)s1,(HashMap)s2));
    }};
    public Object get(T instance,String attrName){
        if(getter.containsKey(attrName)){

            return getter.get(attrName).apply(instance);
        }else {
            throw new RuntimeException("Unregistered attribute "+attrName+" for getter in"+identifier.getName());
        }
    }
    public void set(T instance,String attrName,Object value){
        if(setters.containsKey(attrName)){
            setters.get(attrName).accept(instance,value);
        }else {
            throw new RuntimeException("Unregistered attribute "+attrName+" for setter in"+identifier.getName());
        }
    }
    public boolean differentInternal(T instance1,String  attrName,Object value1,Object value2){
        if(type.containsKey(attrName)){
            Class clazz=getType(attrName);
            if(comparator.containsKey(attrName)){
                DifferenceJudger<T> cmp=comparator.get(attrName);

                if(clazz.isInstance(value1)&&clazz.isInstance(value2)){
                    return !cmp.judge(instance1,value1,value2);
                }else {
                    return true;
                }
            }else {
                //different,not the same
                //should be the opposite
                if(defaultJudgers.containsKey(clazz)){
                    return !defaultJudgers.get(clazz).test(value1,value2);
                }else if(clazz.isArray()){
                    return !Arrays.equals((Object[])value1,(Object[])value2);
                }else {
                    return !Objects.equals(value1,value2);
                }
            }
        }else{
            throw new RuntimeException("Unregistered attribute "+attrName+" for type in"+identifier.getName());
        }

    }
    public boolean different(T instance1,String attrName,Object val){
        Object val1=get(instance1,attrName);
        return different(instance1,attrName,val,val1);
    }
    public boolean different(T instance1,String attrName,Object val,Object val1){
        if(val1==val){
            return false;
        }
        else {
            return differentInternal(instance1,attrName,val,val1);
        }
    }
}