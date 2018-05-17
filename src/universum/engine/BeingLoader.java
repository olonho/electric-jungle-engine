package universum.engine;

import java.net.*;

/**
 *
 * @author nike
 */
class BeingLoader extends URLClassLoader {
    public BeingLoader(Universe u) {
        super(new URL[0], u.getClass().getClassLoader());
    }

    void addClassPath(String path) {
        try {
            addURL(new URL("file:" + path));
        } catch (MalformedURLException e) {}
    }

    Class loadBeing(String name, String path) 
        throws ClassNotFoundException {
        System.err.println("loading " +path);
        if (path != null && !path.equals("")) {
            addClassPath(path);
        }

        // that is kinda hacky, but allows us to load being classes
        // dynamically
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot > 0 ) {                
                try {
                    JungleSecurity js = 
                        (JungleSecurity)System.getSecurityManager();
                    if (js != null) {
                        js.addAllowed(name.substring(0, dot));
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        return loadClass(name);
    }   
}
