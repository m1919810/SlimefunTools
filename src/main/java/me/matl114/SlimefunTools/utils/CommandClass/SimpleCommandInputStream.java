package me.matl114.SlimefunTools.utils.CommandClass;

import javax.annotation.Nullable;
import java.util.List;

public interface SimpleCommandInputStream {
    String nextArg();
    @Nullable
    List<String> getTabComplete();
}