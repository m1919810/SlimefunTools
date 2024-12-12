package me.matl114.SlimefunTools.tests;

import com.google.common.base.Preconditions;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import lombok.Getter;
import me.matl114.SlimefunTools.Slimefun.CustomSlimefunItem;
import me.matl114.SlimefunTools.core.ItemRegister;
import me.matl114.SlimefunTools.implement.Debug;
import me.matl114.matlib.Implements.Managers.BlockDataCache;
import me.matl114.matlib.Utils.AddUtils;
import me.matl114.matlib.Utils.Command.CommandUtils;
import me.matl114.matlib.Utils.PersistentDataContainer.PdcTypes;
import me.matl114.matlib.Utils.Reflect.FieldAccess;
import me.matl114.matlib.Utils.Reflect.MethodAccess;
import me.matl114.matlib.Utils.Reflect.ReflectUtils;
import me.matl114.matlib.core.Manager;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ScriptTests implements Manager {
    private Plugin plugin;
    @Getter
    private ScriptTests manager;
    public ScriptTests() {
        Preconditions.checkState(ItemRegister.getManager()!=null, "ScriptTests requires ItemRegister to be loaded!.");
        manager=this;
    }
    CustomSlimefunItem testItem;
    private NamespacedKey testkey=new NamespacedKey("sftools","test");
    private NamespacedKey argkey=new NamespacedKey("sftools","args");
    public ScriptTests init(Plugin plugin,String... data){
        Debug.logger("Enabling Script tests");
        this.addToRegistry();
        this.plugin = plugin;
        ItemStack testStack=new CustomItemStack(Material.BAKED_POTATO,"&6测试执行器","&7啥也不是");
        ItemMeta meta=testStack.getItemMeta();
        meta.getPersistentDataContainer().set(testkey, PersistentDataType.STRING,"null");
        meta.getPersistentDataContainer().set(argkey, PdcTypes.STRING_LIST, List.of("demo"));
        testStack.setItemMeta(meta);
        testItem= ItemRegister.getManager().registerSlimefunItem("SFTOOL_SCRIPT_TESTER",testStack,ItemRegister.getManager().getFunctionalGroup(), RecipeType.NULL,null,"default");
        testItem.addHandlerForce(new ItemUseHandler(){
            @Override
            public void onRightClick(PlayerRightClickEvent playerRightClickEvent) {
                ItemStack stack=playerRightClickEvent.getItem();
                String testKey=stack.getItemMeta().getPersistentDataContainer().get(testkey,PersistentDataType.STRING);
                String[] testArhs=stack.getItemMeta().getPersistentDataContainer().get(argkey, PdcTypes.STRING_LIST).toArray(String[]::new);
                if(testKey==null||testKey.equals("null")){
                    AddUtils.sendMessage(playerRightClickEvent.getPlayer(),"&c你未初始化测试的方法名!");
                }else{
                    try{
                        Method[] allMethod= ScriptTests.this.getClass().getDeclaredMethods();
                        for(Method m:allMethod){
                            if(m.getName().equals(testKey)){
                                AddUtils.sendMessage(playerRightClickEvent.getPlayer(),"&a找到了该测试项,开始测试");
                                m.setAccessible(true);
                                try{
                                    m.invoke(ScriptTests.this,playerRightClickEvent,testArhs);
                                }catch(Exception e){
                                    AddUtils.sendMessage(playerRightClickEvent.getPlayer(),"&c测试出现错误!"+e.getMessage());
                                    Debug.logger(e);
                                }
                                return;
                            }
                        }
                        AddUtils.sendMessage(playerRightClickEvent.getPlayer(),"&c找不到你所指定的测试项");
                    }catch (Throwable e){
                        AddUtils.sendMessage(playerRightClickEvent.getPlayer(),"&c未知错误!");
                        Debug.logger(e);
                    }
                }
            }
        });
        return this;
    }
    public void deconstruct(){
        this.removeFromRegistry();
        if(testItem!=null){
            ItemRegister.getManager().unregisterSlimefunItem(testItem);
        }
        Debug.logger("Disabling Script tests");
    }
    public ScriptTests reload(){
        deconstruct();
        return init(plugin,null);
    }
    public void testPlayerInventoryAsync(PlayerRightClickEvent event,String[] args){
        Player player=event.getPlayer();
        Debug.logger("开始玩家背包异步修改测试");

        new BukkitRunnable() {
            public void run() {
                Inventory inventory = player.getInventory();
                Debug.logger(inventory.getSize());
                int slot=CommandUtils.parseIntOrDefault(args[0],0);
                inventory.setItem(slot,new ItemStack(Material.BAKED_POTATO));

            }
        }.runTaskAsynchronously(this.plugin);
    }
    public void testChestInventoryAsync(PlayerRightClickEvent event,String[] args){
        Optional<Block> b= event.getClickedBlock();
        if(b.isPresent()){
            Block bb=b.get();
            if(bb.getState() instanceof InventoryHolder ivHolder){
                Debug.logger(ivHolder.getInventory().getSize());
                Debug.logger(ivHolder.getInventory().getContents().length);
                Inventory inventory = ((InventoryHolder)bb.getState()).getInventory();

                new BukkitRunnable() {
                    public void run() {
                        ItemStack[] stack = inventory.getContents();
                        Debug.logger("place ",((BlockState)ivHolder).isPlaced());
                        for (int i = 0; i < stack.length; ++i) {
                            if (stack[i] == null) {
                                Debug.logger("Item is null at slot ", i);
                                inventory.setItem(i,new ItemStack(Material.BAKED_POTATO));
                            } else {
                                Debug.logger("item is not null");
                                stack[i].setType(Material.BAKED_POTATO);
                            }
                        }

                    }
                }.runTaskAsynchronously(this.plugin);
            }
        }else {
            AddUtils.sendMessage(event.getPlayer(),"&c点击一个方块");
        }
    }
    public void testInventorySlot(PlayerRightClickEvent event,String[] args){
        ItemStack item=event.getItem();
        Optional<Block> b= event.getClickedBlock();
        if(b.isPresent()){
            try{
                Block bb=b.get();
                int a= CommandUtils.parseIntOrDefault(args[0],0);
                if(bb.getState() instanceof InventoryHolder ivHolder){
                    Debug.logger(ivHolder.getInventory().getSize());
                    Debug.logger(ivHolder.getInventory().getContents().length);
                    Inventory inventory = ((InventoryHolder)bb.getState()).getInventory();
                    Debug.logger(inventory.getItem(a));
                    inventory.setItem(a,new ItemStack(Material.BAKED_POTATO));
                }
            }catch (Throwable e){
                Debug.logger(e);
            }
        }else {
            AddUtils.sendMessage(event.getPlayer(),"&c点击一个方块");
        }
    }
    public void testInventoryGetSync(PlayerRightClickEvent event,String[] args){
        Optional<Block> b= event.getClickedBlock();
        if(b.isPresent()){
            Block bb=b.get();
            new BukkitRunnable(){
                @Override
                public void run() {
                    Debug.logger("start async thread simulation");
                    long a=System.nanoTime();
//                    new BukkitRunnable(){
//                        @Override
//                        public void run() {
//                            Inventory inventory = event.getPlayer().getInventory();
//                            ItemStack[] stack = inventory.getContents();
//                            for (int i = 0; i < stack.length; ++i) {
//                                if (stack[i] == null) {
//                                    inventory.setItem(i,new ItemStack(Material.BAKED_POTATO));
//                                } else {
//                                    stack[i].setType(Material.BAKED_POTATO);
//                                }
//                            }
//                            long b=System.nanoTime();
//                            Debug.logger("async thread complete");
//                            Debug.logger("async thread time cost",b-a);
//                            return ;
//                        }
//                    }.runTaskAsynchronously(plugin);
                    CompletableFuture.supplyAsync(()->{
                        Inventory inventory = event.getPlayer().getInventory();
                        ItemStack[] stack = inventory.getContents();
                        for (int i = 0; i < stack.length; ++i) {
                            if (stack[i] == null) {
                                inventory.setItem(i,new ItemStack(Material.BAKED_POTATO));
                            } else {
                                stack[i].setType(Material.BAKED_POTATO);
                            }
                        }
                        long b=System.nanoTime();
                        Debug.logger("async thread complete");
                        Debug.logger("async thread time cost",b-a);
                        return null;
                    });
                    Debug.logger("async thread launched");

                }
            }.runTaskAsynchronously(plugin);

        }else {
            AddUtils.sendMessage(event.getPlayer(),"&c点击一个方块");
        }
    }
    public void testInv(PlayerRightClickEvent event,String[] args){
        Optional<Block> b= event.getClickedBlock();
        if(b.isPresent()){
            event.cancel();
            BlockMenu menu=StorageCacheUtils.getMenu( b.get().getLocation());
            if(menu!=null){
                Debug.logger("inv not null");
                ItemStack tmp=menu.getItemInSlot(31);
                if(tmp==null||tmp.getType()==Material.AIR){
                    Debug.logger("recipe null");
                }else {
                    Debug.logger("recipe not null, tmp" ,tmp);
                }
            }
        }
    }

    public void testRemoveSlimefunBlock(PlayerRightClickEvent event,String[] args){
        Debug.logger("开始删除选中的粘液物品");
        BlockDataCache.getManager().removeAllSlimefunBlockAsync((c)->true,(b)->b.getSfId().endsWith("REMOTE_ACCESSOR"),(i)-> AddUtils.sendMessage(event.getPlayer(),"&a选中的粘液物品已经全部清除,共发现%d个".formatted(i)));
    }
    public void testBlockMainThread(PlayerRightClickEvent event,String[] args){
        Player player=event.getPlayer();
        player.sendMessage("即将开始 5秒倒计时");
        player.sendMessage("鼻炎的");
        new BukkitRunnable(){
            public void run() {
                player.sendMessage("开始");
                Debug.logger("开始");
              //  player.getOpenInventory().setCursor(SlimefunItem.getById("MOMOTECH_FINAL_STAR").getItem());
                try{
                    Thread.sleep(700000l);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                player.sendMessage("结束");Debug.logger("结束");
            }
        }.runTaskLater(plugin,100);
    }
    private MethodAccess getHandleMethodAccess=MethodAccess.ofName("getHandle");
    private MethodAccess getPositionMethodAccess=MethodAccess.ofName("getPosition");
    private MethodAccess getNMSMethodAccess=MethodAccess.ofName("getNMS");
    private MethodAccess getCraftWorldHandleMethodAccess=MethodAccess.ofName("getHandle");
    private MethodAccess getNMSBlockType=MethodAccess.ofName("b");

    private MethodAccess getBlockEntityMethodAccess=new MethodAccess((obj)->{
        var a=ReflectUtils.getMethodsByName(obj.getClass(),"c_");
        return a==null?null:a.getFirstValue();
    });
    private Class tileEntityClass;
    private MethodAccess getBlockEntityTypeMethodAccess=new MethodAccess((obj)->{
        List<Class> cls=getAllSuperClassRecursively(obj.getClass()).stream().filter(s->"TileEntity".equals(s.getSimpleName())).toList();
        Class clazz=cls.get(cls.size()-1);
        Debug.logger(clazz);
        tileEntityClass=clazz;
        var re= Arrays.stream(clazz.getDeclaredMethods()).filter(m->m.getParameterTypes().length==0).filter(m->m.getReturnType()!=boolean.class).filter(s->s.getReturnType().getSimpleName().endsWith("Types")).toList();
        Debug.logger(re);
        return re.isEmpty()?null:re.get(0);
    });
    private Class iTileEntityInterface;
    private MethodAccess getTickerMethodAccess=new MethodAccess((obj)->{
        Method[] methods=iTileEntityInterface.getDeclaredMethods();
        Method a= Arrays.stream(methods).filter((m)->"a".equals(m.getName())).filter(m->m.getParameterTypes().length==3).findFirst().get();
        Debug.logger("get info", Arrays.stream(a.getParameterTypes()).toList(),a.getReturnType());
        return a;
    });
    private Class blockEntityTickerInterface;
    private MethodAccess tickMethodAccess=new MethodAccess((obj)->{
        Method[] a=blockEntityTickerInterface.getDeclaredMethods();
        var re= Arrays.stream(a).filter((m)->m.getParameterTypes().length==4).filter(m->"tick".equals(m.getName())).toList();
        return re.isEmpty()?null:re.get(0);
    });
    public static List<Class> getAllSuperClassRecursively(Class clazz){
        List<Class> fieldList=new ArrayList<>();
        while (clazz!=null){
            fieldList.add(clazz);
            clazz=clazz.getSuperclass();

        };
        return fieldList;
    }
    public static List<Class> getAllInterfacesRecursively(Class clazz){
        List<Class> fieldList=new ArrayList<>();
        if(clazz==null){
            return fieldList;
        }
        Class[] fields=clazz.getInterfaces();
        for(Class f:fields){
            fieldList.add(f);
        }
        fieldList.addAll(getAllInterfacesRecursively(clazz.getSuperclass()));
        return fieldList;
    }
    public void testRunEntityTick(PlayerRightClickEvent event,String[] args){
        Optional<Block> b= event.getClickedBlock();
        if(b.isPresent()){
            Debug.logger("block present");
            Block bb=b.get();
            getHandleMethodAccess.invokeCallback((levelAccess)->{
                getPositionMethodAccess.invokeCallback((blockPos)->{
                    getBlockEntityMethodAccess.invokeCallback((blockEntity)->{
                        if(blockEntity!=null){
                            getNMSMethodAccess.invokeCallback((state)->{
                                getNMSBlockType.invokeCallback((block)->{
                                    getBlockEntityTypeMethodAccess.invokeCallback((type)->{
                                        getCraftWorldHandleMethodAccess.invokeCallback((world)->{
                                            try{
                                                if(iTileEntityInterface==null){
                                                    List<Class> interfaces=getAllInterfacesRecursively(block.getClass());
                                                    Debug.logger(block);
                                                    Debug.logger(block.getClass());
                                                    Debug.logger(interfaces);
                                                    iTileEntityInterface= interfaces.stream().filter(s->s.getSimpleName().endsWith("TileEntity")).findFirst().get();
                                                }
                                                Debug.logger(world);
                                                Debug.logger(state);
                                                Debug.logger(type);
                                                var ticker= getTickerMethodAccess.invokeIgnoreFailure(block,world,state,type);
                                                if(blockEntityTickerInterface==null){
                                                    List<Class> interfaces=getAllInterfacesRecursively(ticker.getClass());
                                                    Debug.logger(ticker);
                                                    Debug.logger(ticker.getClass());
                                                    Debug.logger(interfaces);
                                                    blockEntityTickerInterface= interfaces.stream().filter(s->s.getSimpleName().endsWith("Ticker")).findFirst().get();
                                                }
                                                for (int i=0;i<50;++i){
                                                    tickMethodAccess.invoke(ticker,world,blockPos,state,blockEntity);
                                                }
                                                Debug.logger("successfully invoke!");
                                            }catch (Throwable e){
                                                Debug.logger("failed");
                                                e.printStackTrace();
                                            }
                                        },()->{Debug.logger("failed getCraftWorldHandle");},bb.getWorld());
                                    },()->{Debug.logger("failed getBlockEntityType");},blockEntity);
                                },()->{Debug.logger("failed getNMSBlockType");},state);
                            },()->{Debug.logger("failed getNMS");},bb);
                        }else {
                            Debug.logger("not a tile Entity");
                        }
                    },()->{Debug.logger("failed getBlockEntityMethod");},levelAccess,blockPos);
                },()->{Debug.logger("failed getPositionMethod");},bb);
            },()->{Debug.logger("failed getHandle");},bb);
        }
    }
}
