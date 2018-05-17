package universum;

import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;

class Tool extends URLClassLoader {
    Tool() throws MalformedURLException {
        super(new URL[0], Main.class.getClassLoader());

        String javaHome = System.getProperty("java.home");
        // java.home points to /jre
        javaHome = new File(javaHome).getParent();
        System.out.println("using JDK "+javaHome);
                 
        addURL(new URL("file:" + javaHome+"/lib/tools.jar"));
    }
     
    @SuppressWarnings("unchecked") int compile(String[] args) throws Exception {
        try {
            Class klazz = loadClass("com.sun.tools.javac.Main");            
            Method compile = klazz.getMethod("compile", String[].class, PrintWriter.class);
            Integer rv = 
                (Integer)compile.invoke(null, 
                                        new Object[] { args, new PrintWriter(System.out) });
            return rv.intValue();
        } catch (Exception e) {
            System.out.println("******************************************");
            System.out.println("Exception invoking compiler: make sure \n"+
                               " you use Sun's JDK (not JRE) version >= 1.5");
            System.out.println("******************************************");
            throw e;
        }
    }
     
    @SuppressWarnings("unchecked") int jar(String[] args) throws Exception {
        Class klazz = loadClass("sun.tools.jar.Main");
        Constructor c = klazz.getConstructor(PrintStream.class, 
                                             PrintStream.class, String.class);
        Object instance = c.newInstance(System.out, System.err, "jar");
        Method run = klazz.getMethod("run", String[].class);
        Boolean rv = (Boolean)run.invoke(instance, new Object[] {args});
        return rv.booleanValue() ? 0 : 1;
    }
     
    void dumpArgs(String[] args) {
        for (String s : args) {
            System.out.println("a="+s);
        }
    }
     
    void rmdir(File dir) {
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    rmdir(file);
                }
                else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
    private static Tool instance;
    static Tool getTool() {
        if (instance == null) {
            try {
                instance = new Tool();
            } catch (MalformedURLException mue) {}
        }
        return instance;
    }
}

