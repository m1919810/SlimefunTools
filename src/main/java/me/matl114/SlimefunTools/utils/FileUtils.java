package me.matl114.SlimefunTools.utils;

import me.matl114.SlimefunTools.implement.Debug;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;

public class FileUtils {
    public static File getOrCreateFile(String path) throws IOException {
        File file = new File(path);
        if(!file.getParentFile().exists()){
            Files.createDirectories(file.getParentFile().toPath());
        }
        if(!file.exists()){
            if(file.createNewFile()){
                return file;
            }else {
                throw new IOException(path + " create failed");
            }
        }else{
            return file;
        }
    }
    public static void ensureParentDir(File file) throws IOException {
        if(!file.getParentFile().exists()){
            Files.createDirectories(file.getParentFile().toPath());
        }
    }
    public static void copyFile(File from ,String to) throws IOException {
        if(from.exists()){
            File toFile = new File(to);
            ensureParentDir(toFile);
            Files.copy(from.toPath(),toFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }else {
            throw new IOException(from + " does not exist");
        }
    }
    public static void copyFile(String resource, String to) throws IOException {
        File toFile = new File(to);
        ensureParentDir(toFile);

        Files.copy(FileUtils.class.getResourceAsStream("/"+resource),toFile.toPath(),StandardCopyOption.REPLACE_EXISTING);

    }
    public static void copyFolderRecursively(File fromPath, String toPath) throws IOException {

    }
    public static void copyFolderRecursively(String from,String toPath) throws IOException {
        ClassLoader classLoader = FileUtils.class.getClassLoader();
        URI uri=null;
        try {
            uri = classLoader.getResource(from).toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage());
        } catch (NullPointerException e){
            throw new IOException(e.getMessage());
        }

        if(uri == null){
            throw new IOException("something is wrong directory or files missing");
        }

        /** jar case */
        URL jar = FileUtils.class.getProtectionDomain().getCodeSource().getLocation();
        //jar.toString() begins with file:
        //i want to trim it out...
        Path jarFile = Paths.get(URLDecoder.decode(jar.toString(), StandardCharsets.UTF_8) .substring("file:".length()));
        FileSystem fs = FileSystems.newFileSystem(jarFile, Map.of());
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fs.getPath(from));
        Path to=new File(toPath).toPath();
        for(Path p: directoryStream){
            Debug.logger(p);
            InputStream is = FileUtils.class.getResourceAsStream("/"+p.toString()) ;
            Path target=to.resolve(p.toString());

            if(!Files.exists(target)){
                if(!Files.exists(target.getParent())){
                    Files.createDirectories(target.getParent());
                }
                Files.copy(is,target);
            }
        }
    }

}
