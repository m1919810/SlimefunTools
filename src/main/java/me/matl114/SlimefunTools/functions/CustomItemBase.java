package me.matl114.SlimefunTools.functions;

import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.Getter;
import me.matl114.SlimefunTools.implement.Debug;
import me.matl114.SlimefunTools.implement.SlimefunTools;
import me.matl114.SlimefunTools.utils.AddUtils;
import me.matl114.SlimefunTools.utils.CommandClass.*;
import me.matl114.SlimefunTools.utils.FileUtils;
import me.matl114.SlimefunTools.utils.GuiClass.CustomMenuGroup;
import me.matl114.SlimefunTools.utils.SerializeUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.IntStream;

public class CustomItemBase implements ComplexCommandExecutor, Listener, TabCompleter {
    HashMap<String, ItemStack> items=new LinkedHashMap<>();
    private boolean registered=false;
    @Getter
    private String savePath;
    @Getter
    protected static CustomItemBase manager;
    public CustomItemBase(){
        manager = this;
    }
    public CustomItemBase init(String basePath){
        this.savePath = basePath;
        File baseDir=new File(basePath);
        if(!baseDir.exists()){
            baseDir.mkdirs();
        }
        File[] files=baseDir.listFiles();
        if(files!=null){
            for (File file : files) {
                if(file.exists()&&file.isFile()&&file.toPath().toString().endsWith(".yml")){
                    String name=file.getName().replace(".yml", "");
                    Debug.logger("加载自定义物品: "+name);
                    try{
                        String content= Files.readString(file.toPath());
                        ItemStack stack= SerializeUtils.deserializeFromString(content);
                        items.put(name,stack);
                        Debug.logger("成功加载自定义物品: "+name);
                    }catch (Throwable e){
                        Debug.logger("Error loading Custom Item: ",name);
                        Debug.logger(e);
                    }
                }
            }
        }
        return this;
    }
    public CustomItemBase reload(){
        this.items.clear();
        return init(this.savePath);
    }
    public CustomItemBase registerCommand(Plugin plugin){
        Validate.isTrue(!registered, "itemBase command have already been registered!");
        plugin.getServer().getPluginCommand("itembase").setExecutor(this);
        plugin.getServer().getPluginCommand("itembase").setTabCompleter(this);
        this.registered=true;
        return this;
    }
    @Getter
    private LinkedHashSet<SubCommand> subCommands=new LinkedHashSet<>();
    public void registerSub(SubCommand command){
        subCommands.add(command);
    }
    public SubCommand getSubCommand(String name){
        for(SubCommand command:subCommands){
            if(command.getName().equals(name)){
                return command;
            }
        }return null;
    }
    public void openGui(Player player){
        new CustomMenuGroup(AddUtils.resolveColor("&a物品库"),54,1 )
                .enableContentPlace(IntStream.range(0,45).toArray())
                .resetItems(getDisplayed())
                .resetHandlers(getHandlers())
                .setPageChangeSlots(46,52)
                .enableOverrides()
                .setOverrideItem(45, ChestMenuUtils.getBackground())
                .setOverrideItem(53,ChestMenuUtils.getBackground())
                .setOverrideItem(47,ChestMenuUtils.getBackground())
                .setOverrideItem(51,ChestMenuUtils.getBackground())
                .openPage(player,1);


    }
    private List<ItemStack> getDisplayed(){
        List<ItemStack> displayed=new ArrayList<>();
        for(Map.Entry<String,ItemStack> entry:items.entrySet()){
            if(entry.getValue()!=null){
                displayed.add(AddUtils.addLore(
                        entry.getValue(),
                        "",
                        "&a左键 获取一个物品",
                        "&a右键 获取一组物品",
                        "&cshift+右键 删除当前物品"
                ));
            }
        }
        return displayed;
    }
    private List<CustomMenuGroup.CustomMenuClickHandler> getHandlers(){
        List<CustomMenuGroup.CustomMenuClickHandler> handlers=new ArrayList<>();
        for(Map.Entry<String,ItemStack> entry:items.entrySet()){
            if(entry.getValue()!=null){
                final ItemStack item=entry.getValue().clone();
                final String name=entry.getKey();
                handlers.add(
                        (e)->{
                    return ((player, i, itemStack, clickAction) -> {
                        if(clickAction.isRightClicked()){
                            if(clickAction.isShiftClicked()){
                                SlimefunTools.runSync(()->{
                                    this.removeItem(name);
                                    e.getParent().resetItems(this.getDisplayed());
                                    e.getParent().resetHandlers(this.getHandlers());
                                    e.requestReload();
                                });
                            }else {
                                AddUtils.forceGive(player,item,64);
                            }
                        }else{
                            if(clickAction.isShiftClicked()){

                            }else{
                                AddUtils.forceGive(player,item,1);
                            }
                        }
                        return false;
                    });
                });
            }
        }
        return handlers;
    }
    public void removeItem(String name){
        items.remove(name);
        File file=new File(savePath,name=".yml");
        if(file.exists()){
            file.delete();
        }else {
            Debug.logger("错误!不存在文件 %s".formatted(file.toString()));
        }

    }
    private SubCommand saveCommand=new SubCommand("save",
            new SimpleCommandArgs("name"),"/itembase save [name] 将物品载入物品库,并存入[name].yml"
    )
            .setTabCompletor("name",()->items.keySet().stream().toList())
            .register(this);
    private SubCommand giveCommand=new SubCommand("give",
            new SimpleCommandArgs("name","player","count"),"/itembase give [name] [player=yourself] [count=1] 给予[player]物品[name]数量[count]"
    )
            .setTabCompletor("name",()->items.keySet().stream().toList())
            .setTabCompletor("player",()->Bukkit.getOnlinePlayers().stream().map(Player::getName).toList())
            .setDefault("count","1")
            .setTabCompletor("count",()->List.of("1","16","64","1145"))
            .register(this);
    private SubCommand listCommand=new SubCommand("list",
            new SimpleCommandArgs(),"/itembase list 列举物品库中的全部物品"
    )
            .register(this);
    private SubCommand openCommand=new SubCommand("open",new SimpleCommandArgs(),"/itembase open 打开可视化界面")
            .register(this);
    private SubCommand reloadCommand=new SubCommand("reload",new SimpleCommandArgs(),"/itembase reload 重载物品组配置")
            .register(this);
    private SubCommand mainCommand=new SubCommand("itembase",
            new SimpleCommandArgs("_operation"),"/itembase [_operation] [args]"
    )
            .setTabCompletor("_operation",()->getSubCommands().stream().map(SubCommand::getName).toList());//no register,just a functional command for tab completer
    public boolean onCommand( CommandSender var1,  Command var2,  String var3, String[] var4){
        Player executor;
        if(var1.hasPermission("slimefuntool.command.itembase")){
            if(var4.length>=1){
                String[] elseArg= Arrays.copyOfRange(var4,1,var4.length);
                SimpleCommandInputStream inputInfo;
                switch (var4[0]){
                    case "save":
                        inputInfo=saveCommand.parseInput(elseArg).getFirstValue();
                        if((executor=isPlayer(var1,true))!=null){
                            ItemStack item=executor.getInventory().getItemInMainHand();
                            if(item!=null&&!item.getType().isAir()){
                                item=new ItemStack(item);
                                item.setAmount(1);
                                String itemName=inputInfo.nextArg();
                                if(itemName!=null){
                                    String savedString=SerializeUtils.serializeToString(item);
                                    try{
                                        File file= FileUtils.getOrCreateFile(savePath +'/'+itemName+".yml");
                                        Files.writeString(file.toPath(),savedString);
                                    }catch (Throwable e){
                                        AddUtils.sendMessage(var1,"&c未知错误!保存失败");
                                        Debug.logger(e);
                                        break;
                                    }
                                    items.put(itemName,SerializeUtils.deserializeFromString(savedString));
                                    AddUtils.sendMessage(var1,"&a成功保存物品: &f"+itemName);

                                }else {
                                    AddUtils.sendMessage(var1,"&c请输入有效的名字!");
                                }
                            }else {
                                AddUtils.sendMessage(var1,"&c你手中没有任何物品!");
                            }
                        }
                        break;
                    case "give":
                        inputInfo=giveCommand.parseInput(elseArg).getFirstValue();
                        String itemName=inputInfo.nextArg();
                        if(itemName!=null&&items.containsKey(itemName)){
                            ItemStack item=items.get(itemName);
                            String playerName=inputInfo.nextArg();
                            Player player= isPlayer(var1,false);
                            if(playerName!=null){
                                player= Bukkit.getPlayerExact(playerName);
                            }
                            if(player!=null){
                                int amount=CommandUtils.parseIntOrDefault(
                                        inputInfo.nextArg()
                                        ,1);
                                AddUtils.forceGive(player,item.clone(),amount);
                                AddUtils.sendMessage(var1,"&a成功给予物品!");
                            }else {
                                AddUtils.sendMessage(var1,"&c请指定一个玩家!");
                            }
                        }else{
                            AddUtils.sendMessage(var1,"&c请输入一个有效的item名");
                        }
                        break;
                    case "list":
                        AddUtils.sendMessage(var1,"&a当前物品库中的物品列表:");
                        int amount=1;
                        for(String item:items.keySet()){
                            AddUtils.sendMessage(var1,"&a"+amount+":&f "+ item);
                        }
                        break;
                    case "reload":
                        AddUtils.sendMessage(var1,"&a重载物品组中!");
                        reload();
                        AddUtils.sendMessage(var1,"&a重载物品组成功!");
                    case "open":
                        if((executor=isPlayer(var1,true))!=null){
                            openGui(executor);
                            AddUtils.sendMessage(var1,"&a成功打开物品库界面");
                        }
                    default:

                }
            }else{
                showHelpCommand(var1);
            }
        }
        return true;
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
    private void showHelpCommand(CommandSender sender){
        AddUtils.sendMessage(sender,"&a/itembase全部指令大全");
        for(SubCommand cmd:subCommands){
            for (String help:cmd.getHelp()){
                AddUtils.sendMessage(sender,"&a"+help);
            }
        }
    }
    public List<String> onTabComplete(CommandSender var1,  Command var2, String var3, String[] var4){
        var re=mainCommand.parseInput(var4);
        TabProvider provider=re.getFirstValue().peekUncompleteArg();
        if(provider!=null){
            return provider.getTab();
        }else{
            SubCommand subCommand= getSubCommand(re.getFirstValue().nextArg());
            if(subCommand!=null){
                String[] elseArg=re.getSecondValue();
                TabProvider provider2=subCommand.parseInput(elseArg).getFirstValue().peekUncompleteArg();
                if(provider2!=null){
                    return provider2.getTab();
                }
            }
        }
        return new ArrayList<>();
    }

}
