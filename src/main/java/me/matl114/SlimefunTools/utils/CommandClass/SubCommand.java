package me.matl114.SlimefunTools.utils.CommandClass;

import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;


public class SubCommand{
    public interface SubCommandCaller{
        public void registerSub(SubCommand command);
    }
    @Getter
    String[] help;
    SimpleCommandArgs template;
    @Getter
    String name;
    public SubCommand(String name,SimpleCommandArgs argsTemplate,String... help){
        this.name = name;
        this.template=argsTemplate;
        this.help = help;
    }
    public SubCommand register(SubCommandCaller caller){
        caller.registerSub(this);
        return this;
    }
    @Nonnull
    public Pair<SimpleCommandInputStream,String[]> parseInput(String[] args){
        return template.parseInputStream(args);
    }
    public SubCommand setDefault(String arg,String val){
        this.template.setDefault(arg,val);
        return this;
    }
    public SubCommand setTabCompletor(String arg, Supplier<List<String>> completions){
        this.template.setTabCompletor(arg,completions);
        return this;
    }

}