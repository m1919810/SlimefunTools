package me.matl114.SlimefunTools.utils;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class Utils {
    public static <T extends Object> T runIfNull(T val, Runnable function){
        if(val==null)function.run();
        return val;
    }
    public static <T,R extends Object> R computeIfPresent(T val, Function<T,R> function){
        if(val == null){return null;}
        else{return function.apply(val);}
    }
    public static <T,R extends Object> R computeTilPresent(T val, Function<T,R>... function){
        R ret = null;
        for(Function<T,R> f : function){
            ret = f.apply(val);
            if(ret != null){return ret;}
        }
        return ret;
    }
    public static <T extends Object > T orDefault(T val, T defaultVal){
        if(val == null){return defaultVal;}
        else{return val;}
    }
    public static <T extends Object > T orCompute(T val, Supplier<T> supplier){
        if(val == null){return supplier.get();}
        else{return val;}
    }
    public static boolean compareMap(HashMap map1, HashMap map2){
        if(map1 == map2){return map1==map2;
        }else if(map1==null){return map2==null||map2.isEmpty();}
        else if(map2==null){return map1.isEmpty();}
        else if(map1.size() != map2.size()){return false;}
        else{
            for(Object key : map1.keySet()){
                if(!map2.containsKey(key)){return false;}
                Object val = map1.get(key);
                if(!Objects.equals(val, map2.remove(key))){return false;}
            }
            return map2.isEmpty();
        }
    }
}
