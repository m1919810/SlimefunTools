package me.matl114.SlimefunTools.implement;

import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class Debug {
    public  static Logger log = Logger.getLogger("SFtools");
    public static Logger testlog=Logger.getLogger("TEST");
    public static boolean start=false;
    public static boolean pos=false;
    public static AtomicBoolean[] breakPoints=null;
    public static Object[] breakValues=null;
    public static AtomicBoolean[] setValues=null;
    public static byte[] lock=new byte[0];
    public static AtomicBoolean getBreakPoint(int i){
        if(breakPoints==null){
            breakPoints=new AtomicBoolean[80];
            breakValues=new Object[80];
            setValues=new AtomicBoolean[80];
            for(int j=0;j<80;j++){
                breakPoints[j]=new AtomicBoolean(false);
                setValues[j]=new AtomicBoolean(false);
            }
        }

        return breakPoints[i];
    }
    public static void setBreakPoint(int i, boolean b){
        synchronized(lock){
            AtomicBoolean boo= getBreakPoint(i);
            setValues[i].set(false);
            breakValues[i]=null;
            breakPoints[i].set(b);
        }
    }
    public static Object getBreakValue(int i){
        synchronized(lock){
            return breakValues[i];
        }
    }
    public static boolean getHasSetValue(int i){
        synchronized(lock){
            return setValues[i].get();
        }
    }
    public static void setBreakValue(int i,Object value){
        synchronized(lock){
            breakValues[i]=value;
            setValues[i].set(true);
        }
    }

    public static  void logger(String message) {
        log.info(message);
    }

    public static  void logger(int message) {
        logger(Integer.toString(message));
    }
    public static void debug(int message) {
        debug(Integer.toString(message));
    }
    public static void logger(Object ... msgs){
        String msg="";
        for(Object m : msgs){
            msg+=" "+m.toString();
        }
        logger(msg);
    }
    public static void logger(Throwable t) {

        t.printStackTrace();

    }
    public static void logger(Supplier<String> str){
        logger(str.get());
    }
    public static void debug(Object ...msgs) {
        String msg="";
        for(Object m : msgs){
            msg+=" "+m.toString();
        }
        debug(msg);
    }


    public static void test(Object ...msgs) {
        if(start){
            logger(msgs);
        }
    }

}
