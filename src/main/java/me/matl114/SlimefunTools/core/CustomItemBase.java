package me.matl114.SlimefunTools.core;

import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.Getter;
import me.matl114.SlimefunTools.implement.Debug;
import me.matl114.SlimefunTools.implement.SlimefunTools;
import me.matl114.SlimefunTools.utils.AddUtils;
import me.matl114.SlimefunTools.utils.Utils;
import me.matl114.SlimefunTools.utils.CommandClass.*;
import me.matl114.SlimefunTools.utils.FileUtils;
import me.matl114.SlimefunTools.utils.GuiClass.CustomMenuGroup;
import me.matl114.SlimefunTools.utils.SerializeUtils;
import me.matl114.SlimefunTools.utils.StructureClass.Manager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class CustomItemBase implements ComplexCommandExecutor, Listener, TabCompleter , Manager {
    private HashMap<String, ItemStack> items=new LinkedHashMap<>();
    private boolean registered=false;
    private Plugin plugin;
    @Getter
    private String savePath;
    @Getter
    private static CustomItemBase manager;
    private Random rand = new Random();
    public CustomItemBase(){
        manager = this;
    }
    public CustomItemBase init(Plugin pl,String... basePath){
        this.plugin=pl;
        this.addToRegistry();
        this.savePath = basePath[0];
        File baseDir=new File(basePath[0]);
        if(!baseDir.exists()){
            baseDir.mkdirs();
        }
        File[] files=baseDir.listFiles();
        if(files!=null){
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            for (File file : files) {
                if(file.exists()&&file.isFile()&&file.toPath().toString().endsWith(".yml")){
                    String name=file.getName().replace(".yml", "");
                    Debug.logger("加载自定义物品: "+name);
                    try{
                        String content= Files.readString(file.toPath());
                        ItemStack stack= SerializeUtils.deserializeFromString(content);
                        items.put(name,stack);
                        //Debug.logger("成功加载自定义物品: "+name);
                    }catch (Throwable e){
                        Debug.logger("Error loading Custom Item: ",name);
                        Debug.logger(e);
                    }
                }
            }
        }
        registerFunctional();
        return this;
    }
    public CustomItemBase reload(){
        deconstruct();
        return init(this.plugin,this.savePath);
    }
    public void deconstruct(){
        unregisterFunctional();
        this.items.clear();
        this.removeFromRegistry();
    }
    private CustomItemBase registerFunctional(){
        Validate.isTrue(!registered, "itemBase functional have already been registered!");
        plugin.getServer().getPluginCommand("itembase").setExecutor(this);
        plugin.getServer().getPluginCommand("itembase").setTabCompleter(this);
        this.registered=true;
        return this;
    }
    private CustomItemBase unregisterFunctional(){
        Validate.isTrue(registered, "itemBase functional havem't been unregistered!");
        plugin.getServer().getPluginCommand("itembase").setExecutor(null);
        plugin.getServer().getPluginCommand("itembase").setTabCompleter(null);
        this.registered=false;
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
                        "&a物品名字: &f"+entry.getKey(),
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
    public boolean removeItem(String name){
        if(items.remove(name)!=null){
            File file=new File(savePath,name+".yml");
            if(file.exists()){
                file.delete();
                return true;
            }else {
                Debug.logger("错误!不存在文件 %s".formatted(file.toString()));
            }
        }
        return false;
    }
    public boolean addItem(ItemStack item,String itemName){
        item=new ItemStack(item);
        item.setAmount(1);
        String savedString=SerializeUtils.serializeToString(item);
        try{
            File file= FileUtils.getOrCreateFile(savePath +'/'+itemName+".yml");
            Files.writeString(file.toPath(),savedString);
        }catch (Throwable e){
            Debug.logger(e);
            return false;
        }
        items.put(itemName,SerializeUtils.deserializeFromString(savedString));
        return true;
    }
    public ItemStack getItem(String name){
        String[] split=name.split(":");
        String realName=split[split.length-1];
        return Utils.computeIfPresent(items.get(realName),ItemStack::new);
    }
    public String getItemId(ItemStack item){
        ItemStack stackToCompare=new ItemStack(item);
        for(Map.Entry<String,ItemStack> entry:items.entrySet()){
            if(entry.getValue().isSimilar(stackToCompare)){
                return entry.getKey();
            }
        }
        return null;
    }
    public String getItemIdOrCreate(ItemStack item, Supplier<String> itemNameProvider){
        //change craftItemStack to cleaned bukkit ItemStack
        ItemStack stackToCompare=new ItemStack(item);
        for(Map.Entry<String,ItemStack> entry:items.entrySet()){
            if(entry.getValue().isSimilar(stackToCompare)){
                return entry.getKey();
            }
        }
        String itemName=itemNameProvider.get();
        addItem(stackToCompare,itemName);
        return itemName;
    }
    public String getRandomName(){
        String name;
        do{
            name="_item_"+rand.nextInt(99999);
        }while(items.containsKey(name));
        return name;
    }
    public String getDefaultName(){
        String name;
        do{
            name="_item_"+defaultItemIndex;
            defaultItemIndex++;
        }while(items.containsKey(name));
        return name;
    }
    private int defaultItemIndex=0;
    private SubCommand saveCommand=new SubCommand("save",
            new SimpleCommandArgs("name"),"/itembase save [name] 将物品载入物品库,并存入[name].yml"
    ){
        @Override
        public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
            Player executor;
            var inputInfo=parseInput(var4).getFirstValue();
            if((executor=isPlayer(var1,true))!=null){
                ItemStack item=executor.getInventory().getItemInMainHand();
                if(item!=null&&!item.getType().isAir()){
                    item=new ItemStack(item);
                    item.setAmount(1);
                    String itemName=inputInfo.nextArg();
                    if(itemName!=null){
                        if(addItem(item,itemName)){
                            AddUtils.sendMessage(var1,"&a成功保存物品: &f"+itemName);
                        }else{
                            AddUtils.sendMessage(var1,"&c未知错误!保存失败");
                        }
                    }else {
                        AddUtils.sendMessage(var1,"&c请输入有效的名字!");
                    }
                }else {
                    AddUtils.sendMessage(var1,"&c你手中没有任何物品!");
                }
            }
            return true;
        }
    }
            .setTabCompletor("name",()->items.keySet().stream().toList())
            .register(this);
    private SubCommand giveCommand=new SubCommand("give",
            new SimpleCommandArgs("name","player","count"),"/itembase give [name] [player=yourself] [count=1] 给予[player]物品[name]数量[count]"
    ){
        @Override
        public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
            var inputInfo=parseInput(var4).getFirstValue();
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
            return true;
        }
    }
            .setTabCompletor("name",()->items.keySet().stream().toList())
            .setTabCompletor("player",()->Bukkit.getOnlinePlayers().stream().map(Player::getName).toList())
            .setDefault("count","1")
            .setTabCompletor("count",()->List.of("1","16","64","1145"))
            .register(this);
    private SubCommand listCommand=new SubCommand("list",
            new SimpleCommandArgs(),"/itembase list 列举物品库中的全部物品"
    ){
        @Override
        public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
            AddUtils.sendMessage(var1,"&a当前物品库中的物品列表:");
            int amount=1;
            for(String item:items.keySet()){
                AddUtils.sendMessage(var1,"&a"+amount+":&f "+ item);
            }
            return true;
        }
    }
            .register(this);
    private SubCommand openCommand=new SubCommand("open",new SimpleCommandArgs(),"/itembase open 打开可视化界面"){
        @Override
        public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
            Player executor;
            if((executor=isPlayer(var1,true))!=null){
                openGui(executor);
                AddUtils.sendMessage(var1,"&a成功打开物品库界面");
            }
            return true;
        }
    }
            .register(this);
    private SubCommand reloadCommand=new SubCommand("reload",new SimpleCommandArgs(),"/itembase reload 重载物品组配置"){
        @Override
        public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
            AddUtils.sendMessage(var1,"&a重载物品组中!");
            reload();
            AddUtils.sendMessage(var1,"&a重载物品组成功!");
            return true;
        }
    }
            .register(this);
    private SubCommand removeCommand=new SubCommand("remove",new SimpleCommandArgs("name"),"/itembase remove [name] 删除物品[name]"){
        @Override
        public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
            var inputInfo=parseInput(var4).getFirstValue();
            String removeItem=inputInfo.nextArg();
            if(removeItem!=null&&items.containsKey(removeItem)){
                if(removeItem(removeItem)){
                    AddUtils.sendMessage(var1,"&a成功移除物品: "+removeItem);
                }else {
                    AddUtils.sendMessage(var1,"&c移除物品失败,发生未知错误!");
                }
            }else {
                AddUtils.sendMessage(var1,"&c请输入一个有效的物品名字");
            }
            return true;
        }
    }
            .setTabCompletor("name",()->items.keySet().stream().toList())
            .register(this);
    private SubCommand mainCommand=new SubCommand("itembase",
            new SimpleCommandArgs("_operation"),"/itembase [_operation] [args]"
    )
            .setTabCompletor("_operation",()->getSubCommands().stream().map(SubCommand::getName).toList());//no register,just a functional command for tab completer
    //
    public boolean onCommand( CommandSender var1,  Command var2,  String var3, String[] var4){
        if(var1.hasPermission("slimefuntool.command.itembase")){
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
        AddUtils.sendMessage(sender,"&a/itembase 全部指令大全");
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

}
