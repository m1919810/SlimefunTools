package me.matl114.SlimefunTools.utils.CommandClass;

import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;

import java.util.*;
import java.util.function.Supplier;

public class SimpleCommandArgs {
    public static class Argument implements TabProvider{
        public String argsName;
        public HashSet<String> argsAlias;
        public String defaultValue=null;
        public Supplier<List<String>> tabCompletor=null;
        public Argument(String argsName){
            this.argsName = argsName;
            this.argsAlias = new HashSet<>();
            argsAlias.add(argsName);
            argsAlias.add(argsName.toLowerCase());
            argsAlias.add(argsName.toUpperCase());
            argsAlias.add(argsName.substring(0,1));
            argsAlias.add(argsName.substring(0, 1).toLowerCase());
            argsAlias.add(argsName.substring(0, 1).toUpperCase());
        }
        public boolean isAlias(String arg){
            return argsAlias.contains(arg);
        }
        public List<String> getTab(){
            return tabCompletor.get();
        }
    }
    Argument[] args;
    public SimpleCommandArgs(String... args){
        this.args= Arrays.stream(args).map(Argument::new).toArray(Argument[]::new);
    }
    public void setDefault(String arg,String defaultValue){
        for(Argument a : args){
            if(a.argsName.equals(arg)){
                a.defaultValue=defaultValue;
            }
        }
    }
    public void setTabCompletor(String arg,Supplier<List<String>> tabCompletor){
        for(Argument a : args){
            if(a.argsName.equals(arg)){
                a.tabCompletor=tabCompletor;
            }
        }
    }
    public Pair<SimpleCommandInputStream,String[]> parseInputStream(String[] input){
        List<String> commonArgs=new ArrayList<>();
        final HashMap<Argument,String> argsMap=new HashMap<>();
        Iterator<String > iter= Arrays.stream(input).iterator();
        while(iter.hasNext()){
            String arg=iter.next();
            if(arg.startsWith("-")){
                Argument selected=null;
                for(Argument a:args){
                    if(a.isAlias(arg)){
                        selected=a;
                        break;
                    }
                }
                if(selected!=null){
                    if(iter.hasNext()){
                        String arg2=iter.next();

                        argsMap.put(selected,arg2);
                    }else {
                        //你这跟人机一样，在最后一个加参数 直接跳
                    }
                }
                else {
                    //输入了一个无效参数 加入commonArgs
                    commonArgs.add(arg);
                }
            }else {
                commonArgs.add(arg);
            }
        }

        for(Argument a:args){
            if(!argsMap.containsKey(a)){
                if(!commonArgs.isEmpty()){
                    argsMap.put(a,commonArgs.remove(0));
                }else{
                    argsMap.put(a,null);
                }
            }
        }
        return new Pair<>(new SimpleCommandInputStream() {
            Iterator<Argument> iter= Arrays.stream(args).iterator();
            @Override
            public String nextArg() {
                if(iter.hasNext()){
                    Argument a= iter.next();
                    return argsMap.getOrDefault(a,a.defaultValue);
                }else {
                    throw new RuntimeException("there is no next argument in your command definition!");
                }
            }
            public TabProvider peekUncompleteArg(){
                for(Argument a:args){
                    if(argsMap.get(a)==null){
                        return a;
                    }
                }
                return null;
            }
        },commonArgs.toArray(String[]::new));
    }
}