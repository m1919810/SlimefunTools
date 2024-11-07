package me.matl114.SlimefunTools.functional;

import java.util.HashMap;
import java.util.Map;

public interface DynamicFunctional {
    //these functional classes have to register Instances in other managers
    //these instances are non-static
    //these instances have to be cleared and reload when reload
    static HashMap<DynamicFunctional,Class> instances=new HashMap<>();

    default void registerFunctional(Class clazz){
        instances.put(this,clazz);
    }
    public void onDisable();
    static void onModuleManagerDisable(Class clazz){
        for(Map.Entry<DynamicFunctional,Class> entry:instances.entrySet()){
            if(entry.getValue()==clazz){
                entry.getKey().onDisable();
            }
        }
    }
    static void onFunctionalUnitDisable(){
        instances.keySet().forEach(DynamicFunctional::onDisable);
        instances.clear();
    }
}
