package universum.engine;

import java.security.*;
import java.util.Set;
import java.util.HashSet;

/* 
 * This is a very special class, which should have no engine class dependencies
 * as in fact must be loaded once by upper level classloader, to avoid situations
 * where we have multiple engine instances trying to install conflicting security 
 * managers
 */
public class JungleSecurity extends SecurityManager {
    Set<String> allowed, whitelist;

    private JungleSecurity() {
        super();

        try {
            if (true) {
                checkPermission(ENGINE_PERMISSION);
            }
        } catch (java.security.AccessControlException se) {
            System.err.println("Make sure you updated your java.policy "+
                               "according to comment in JungleSecurity.java!");
            throw se;
        }
        //System.out.println("new SM!");
        
        allowed = new HashSet<String>();
        whitelist = new HashSet<String>();
        
	allowed.add("universum.bi");
        allowed.add("universum.util");        
        allowed.add("java.lang");
        allowed.add("java.lang.annotation");
        allowed.add("java.lang.ref");
        allowed.add("java.util");
        allowed.add("java.util.regex");
        allowed.add("java.text");
        allowed.add("java.util.logging");
        allowed.add("java.util.concurrent");
        allowed.add("java.util.concurrent.atomic");
        allowed.add("java.util.concurrent.locks");
        allowed.add("java.math");
        allowed.add("java.io"); // to allow use of System.out.println()

        whitelist.add("sun.reflect");
        //whitelist.add("sun.reflect.misc");
        //whitelist.add("sun.text.resources");
    }
    
    public synchronized void checkPackageAccess(String pkg) {
        // XXX?
	// kinda kludgy code, but for whatever reason newInstance
	// sometimes requires to access classes in sun.reflect, what is 
        // disabled by default 
        //System.err.println("to "+pkg);	
        try {
          if (whitelist.contains(pkg)) {
              checkPermission(ENGINE_PERMISSION);
              return;
           }

          super.checkPackageAccess(pkg);
       
          if (!isAllowed(pkg)) {
              //System.err.println("really "+pkg);
              checkPermission(ENGINE_PERMISSION);
          }
        } catch (java.security.AccessControlException ace) {
          System.out.println("FAILED ACCESS TO "+pkg);
          throw ace;
        }
    }
    
    // this method has to be public, as security manager in
    // JSP is loaded by different classloader,
    // thus call from BeingLoader will be cross package :(
    public synchronized boolean addAllowed(String pkg) {
        if (pkg.startsWith("com.sun") ||
            pkg.startsWith("sun") ||
            pkg.startsWith("org.apache") ||
            pkg.startsWith("java") ||
            pkg.startsWith("javax") ||            
            (pkg.startsWith("universum") &&
             !pkg.startsWith("universum.beings"))){
            // those names are bad
            return false;
        }
        //System.err.println("allowed "+pkg);
        allowed.add(pkg);
        return true;
    }

    private boolean isAllowed(String str) {
        if (allowed.contains(str)) {
            return true;
        }
        
        if (str == null) {
            return true;
        }

        for (String allowedElement : allowed) {
            if (str.startsWith(allowedElement+".")) {
                return addAllowed(str);
            }
        } 
        return false;
    }

    public void checkAccess(ThreadGroup g) {
        checkPermission(ENGINE_PERMISSION);
    }
    
    /*
    public void checkPropertyAccess(String key) {
        checkPermission(ENGINE_PERMISSION);
    }

    public void checkPropertiesAccess() {
        checkPermission(ENGINE_PERMISSION);
    }

    public void checkAwtEventQueueAccess() {
        checkPermission(ENGINE_PERMISSION);
        } */

    /**
     * This variable will be set to true or real runs, to avoid unfair tricks
     * like fiddling with sun.misc.Unsafe, and if you want to check that your 
     * creature will run well - copy java.policy to ~/.java.policy (or just
     * temporary add its content to jre/lib/security/java.security)
     * and set CHECK_SECURITY to 'true', or run with -secure command line arg.
     * WARNING: it's recommended to remove modification from Java policy,
     * as it gives any application
     *
     * NB: we could parasite on some existing runtime permission granted to
     * everybody, but this solution is cleaner
     **/
    private static boolean CHECK_SECURITY = false;

    public static void setCheckSecurity(boolean param) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null && sm instanceof JungleSecurity) {
            sm.checkPermission(ENGINE_PERMISSION);
        }
	CHECK_SECURITY = param;
        updateSecurity();
    }
    public static boolean getCheckSecurity() {
	//checkPerms();
	return CHECK_SECURITY;
    }

     // permission for engine, not given to beings
    private static final RuntimePermission ENGINE_PERMISSION 
        = new RuntimePermission("ElectricJungleEnginePermission");

    
    public static synchronized void updateSecurity() {
        if (CHECK_SECURITY) {
            SecurityManager sm = System.getSecurityManager();
            if (sm == null || !(sm instanceof JungleSecurity)) {
                System.setSecurityManager(new JungleSecurity());
            }
        }
    }

    public void checkEnginePermission() {
	checkPermission(ENGINE_PERMISSION);
    }
}
