package me.matl114.SlimefunTools.functional;

import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Supplier;

public class InstanceModifyRecord<T extends Object>{
    private String c(String... str){
        return String.join(".", str);
    }
    private HashMap<String,Object> record=new HashMap<>();
    private boolean refreshRecord;
    private Identifier<T> identifier;
    @Getter
    private T instance;
    Config database;
    Function<T,String> pathParser;
    public InstanceModifyRecord(T instance, Config dataBase, Function<T,String> pathParsor, Class identifier, boolean refreshRecord){
        assert instance.getClass().isAssignableFrom(identifier);
        this.database=dataBase;
        this.pathParser=pathParsor;
        this.instance=instance;
        this.refreshRecord=refreshRecord;
        Identifier<?> id=Identifier.getIdentifier(identifier);
        assert identifier==id.getId();
        this.identifier=(Identifier<T>)id;
    }
    public boolean checkDifference(String attribute ,Object val){
        return this.identifier.different(instance,attribute,val);
    }
    public void executeModification(String attribute,Object value){
        if(!record.containsKey(attribute)||refreshRecord){
            Object newObject=this.identifier.get(instance,attribute);
            record.put(attribute,newObject);
        }
        this.identifier.set(this.instance,attribute,value);
    }
    public void executeUndoModification(String attribute){
        this.identifier.set(this.instance,attribute,this.record.get(attribute));
        this.record.remove(attribute);
    }
    public void executeUndoAllModifications(){
        HashSet<String> attributeSet=new HashSet<>(record.keySet());
        for(String attribute:attributeSet){
            executeUndoModification(attribute);
        }
    }
    public void executeDataDelete(String attribute){
        executeUndoModification(attribute);
        String parentPath=pathParser.apply(instance);
        this.database.setValue(c(parentPath,attribute),null);
        this.database.save();
    }
    public void executeAllDataDelete(){
        HashSet<String> attrNames=new HashSet<>(record.keySet());
        for(String attrName:attrNames){
            executeDataDelete(attrName);
        }
        this.database.setValue(pathParser.apply(instance),null);
        this.database.save();
    }
    public void executeDataSave(String attribute ,SaveMethod mode){
        executeDataSave(attribute,mode,()->null);
    }
    public void executeDataSave(String attribute , SaveMethod mode, Supplier<String> elseData){
        if(mode!=SaveMethod.NONE&&this.database!=null){
            Object value=this.identifier.get(instance,attribute);
            //execute config update
            Object record=this.record.get(attribute);
            String parentPath=this.pathParser.apply(instance);
            if(refreshRecord|| this.identifier.different(instance,attribute,value,record)){
                ConfigParser.serialize(this.database,c(parentPath,attribute),value,identifier.getType(attribute),mode,elseData.get());
            }else{
                //如果历史记录不是持续刷新 说明config调了个寂寞 相当于没调,所以顺便就删了吧
                this.database.setValue(c(parentPath,attribute),null);
            }
            this.database.save();

        }
    }
    //todo execute reload
}