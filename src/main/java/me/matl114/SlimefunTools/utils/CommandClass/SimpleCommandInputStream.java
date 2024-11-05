package me.matl114.SlimefunTools.utils.CommandClass;

import javax.annotation.Nullable;

public interface SimpleCommandInputStream {
    String nextArg();
    @Nullable
    TabProvider peekUncompleteArg();
}