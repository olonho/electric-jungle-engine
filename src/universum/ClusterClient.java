package universum;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import java.util.*;
import java.text.DateFormat;

public class ClusterClient {
    String auth, host;
    int port;
    SocketChannel sc;
    ByteBuffer buf;
    String tag, hostname;
    long started;
    
    class CustomRunner extends ContestRunner {
        CustomRunner(String[] args) {
            parseArgs(args);
        }                
        public void destroy() {
            stop();
        }
    }

    ClusterClient(String server) {
        int dog = server.indexOf('@');
        int colon = server.indexOf(':');
        auth = server.substring(0, dog);
        host = server.substring(dog+1, colon);
        port = Integer.parseInt(server.substring(colon+1));       
        buf = ByteBuffer.allocate(640*1024);
        hostname = hostname();
    }


    private byte[] getBytes() {
        int len = buf.getInt();
        byte[] data = new byte[len];
        buf.get(data, 0, len);
        return data;
    }

    private void putString(String s) {
        int len = s.length();
        buf.putInt(len);
        for (int i=0; i<len; i++) {
            buf.putChar(s.charAt(i));
        }
    }

    private String getString() {
        int len = buf.getInt();
        char[] data = new char[len];
        for (int i=0; i<len; i++) {
            data[i] = buf.getChar();
        }
        return new String(data);
    }


    private void startPacket() {
        buf.clear();
        buf.putInt(0x12345678);
        buf.putInt(0); // size placeholder
    }

    private void endPacket() throws IOException {
        int pos = buf.position();
        buf.position(4);
        buf.putInt(pos); // fill in size
        buf.position(pos);
        buf.flip();        
    }

    void connect(String cmd, String[] args) throws IOException {        
        sc = connectTo(host, port);
        
        startPacket();
        putString(auth);
        putString(cmd);
        buf.putInt(args == null ? 0 : args.length);
        if (args != null) {           
            for (String a : args) {
                putString(a);
            }
        }
        endPacket();
        sc.write(buf);
    }

    
    void doCommand(String cmd, String[] args) throws IOException {
        connect(cmd, args);
        // if server not closed connection - auth is OK
        buf.clear();
        buf.limit(8);
        int r = sc.read(buf); 
        if (r < 8) {
            throw new IOException("short read: "+r);
        }        
        buf.flip();
        int magic = buf.getInt();
        if (magic != 0x34125678) {
            throw new IOException("inconsistent magic: "+magic);
        }
        int len = buf.getInt();
        if (len < 8 || len > 635 * 1024) {
            throw new IOException("bad frame: "+len);
        }
        buf.flip();
        buf.limit(len-8);        
        int read = 8;
        do {
            r = sc.read(buf);
            if (r < 0) {
                throw new IOException("cut frame");
            }
            read += r;
        } while (read < len);
        buf.flip();

        sc.close();
    }


    String[] getConfig() throws IOException {
        doCommand("GETCONFIG", new String[] {hostname} );
        
        tag = getString();
        int count = buf.getInt();
        String[] data = new String[count];
        for (int i=0; i<count; i++) {
            data[i] = getString();
        }       
        return data;
    }


    void processDeps(String[] cfg) throws IOException {
        // ugly but must work for now
        for (int i = 0; i < cfg.length; i++) {
            String c = cfg[i];
            if ("--game-kind".equals(c)) {
                i++;
                continue;
            }
            if ("--batch".equals(c)) {
                continue;
            }
            
            if (c.startsWith("--")) {
                System.out.println("HANDLE PARAM: "+c);
            }
            if (i < cfg.length-1) {
                i++;                
                cfg[i] = getJar(cfg[i]);                
            }
        }
    }


    String getJar(String serverName) throws IOException {        
        int slash = serverName.lastIndexOf('/');
        String name = slash < 0 ? serverName : serverName.substring(slash+1);
        System.out.println("getting="+serverName+" "+" as "+name);
        doCommand("GETJAR", new String[]{serverName});
        
        byte[] data = getBytes();
        File jar = new File(name);
        
        FileOutputStream fos = new FileOutputStream(jar);
        fos.write(data);
        fos.close();
        
        String rv =  jar.getCanonicalPath();        
        pathMapper.put(rv, serverName);
        
        return rv;
    }

    Map<String, String> pathMapper = new HashMap<String, String>();

    private static DateFormat FULL_DATE = 
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM,
	                               Locale.US);

    String hostname() {
        try {
            java.net.InetAddress localMachine = 
                java.net.InetAddress.getLocalHost();	
            return localMachine.getHostName();
        }
        catch(java.net.UnknownHostException uhe) {
            return "unknown";
        }
    }
    
    void reportResults(boolean success, String tag, CustomRunner c) throws IOException {
        String cmd;
        String[] results;
        int length;
        if (success) {
            cmd = "RESULT";
            results = c.results();
            length = results.length;
        } else {
            cmd = "DEAD";
            results = null;
            length = 0;
        }
        String[] args = new String[length + 5];
        int idx = 0;
        args[idx++] = tag;
        args[idx++] = Integer.toString(c.getOwner());
        args[idx++] = hostname;
        args[idx++] = Long.toString(System.currentTimeMillis() - started);
        args[idx++] = Integer.toString(success ? c.numTurns() : 0);
        for (int i = 0; i < length; i += 2) {
            args[idx++] = pathMapper.get(results[i]);
            args[idx++] = results[i+1];
        }
        doCommand(cmd, args);
    }

    int start()  {
        try {            
            String[] cfg = getConfig();
            if (tag == null) {
                throw new RuntimeException("NULL tag");
            } else if ("DONE".equals(tag)) {
                return 1;
            }
            processDeps(cfg);
            CustomRunner cr = new CustomRunner(cfg);
            started = System.currentTimeMillis();
            cr.run();
            boolean success = cr.waitCompletion();
            reportResults(success, tag, cr);
        } catch (Exception e) {
            e.printStackTrace();
            return 2;
        }
        return 0;
    }


    public static void main(String args[]) {
        universum.engine.JungleSecurity.setCheckSecurity(true);
        System.out.println("Secure");
        System.exit(runAsClient(args[0]));
    }

    static int runAsClient(String server) {        
        ClusterClient client = new ClusterClient(server);
        int code, i = 0;
        
        do {
            code = client.start();        
        } while (code == 0 && i++ < 20);
        return code;
    }
    

    SocketChannel connectSocks(String host, int port,
                               String proxyHost, int proxyPort) throws IOException {
        boolean isAutentificationNeeded = false;
        String username = null, password = null;
        
        if (username != null && password != null) {           
            isAutentificationNeeded = true;
        }
        
        SocketChannel channel = SocketChannel.open(new InetSocketAddress(proxyHost, proxyPort));

        OutputStream out = channel.socket().getOutputStream();
        InputStream in = channel.socket().getInputStream();
        final DataOutputStream dos = new DataOutputStream(out);
        final BufferedInputStream din = new BufferedInputStream(in);

        byte[] greeting = new byte[]{
            /*  INFORMATION FROM http://en.wikipedia.org/wiki/SOCKS */
            5,      // socks version number, must be 0x05 for this version
            1,      // number of authentication methods supported
            (isAutentificationNeeded)
            ? (byte) 2      // Username/Password
            : (byte) 0      // No authentication
        };
        dos.write(greeting);
        int serverVersion = din.read();
        if (serverVersion != 5) {
            throw new IOException("SOCKS5 protocol error: version: " + serverVersion);
        }
        int authMethod = din.read();
        if (authMethod == 255) {
            throw new IOException("SOCKS5 authentication failure: no supported method acccepted by server");
        }

        int tmp;
        if (authMethod == 2) {
            dos.writeByte(1);
            String uname = username;
            byte[] unamebytes = (uname == null) ? new byte[]{} : uname.getBytes();
            dos.writeByte(unamebytes.length);
            dos.write(unamebytes);
            String pwd = null;
            if (password != null) {
                pwd = password;
            }
            byte[] pwdbytes = (pwd == null) ? new byte[]{} : pwd.getBytes();
            dos.writeByte(pwdbytes.length);
            dos.write(pwdbytes);
            tmp = din.read();
            if (tmp != 1) throw new IOException("socks5.auth.error." + tmp);
            tmp = din.read();
            if (tmp != 0) throw new IOException("socks5.auth.error." + tmp);
        }
        byte[] request = new byte[]{
            /*  INFORMATION FROM http://en.wikipedia.org/wiki/SOCKS */
            5,                        // socks version number, must be 0x05 for this version
            1,                        // command code establish a tcp/ip stream connection
            0,                        // reserved, must be 0x00
            3,                        // Domain name (address field is variable)
            (byte) host.length()      // destination address, 4/16 bytes or 1+domain name length.
        };
        // sending request
        dos.write(request);
        dos.writeBytes(host);
        dos.writeShort(port);         // network byte order port number, 2 bytes
        serverVersion = din.read();
        if (serverVersion != 5) throw new IOException("SOCKS5 protocol error: version: " + serverVersion);
        tmp = din.read();
        if (tmp != 0) {
            switch (tmp) {
            case 1: throw new IOException("SOCKS5 protocol error: general SOCKS server failure");
            case 2: throw new IOException("SOCKS5 protocol error: connection not allowed by ruleset");
            case 3: throw new IOException("SOCKS5 protocol error: Network unreachable");
            case 4: throw new IOException("SOCKS5 protocol error: Host unreachable");
            case 5: throw new IOException("SOCKS5 protocol error: Connection refused");
            case 6: throw new IOException("SOCKS5 protocol error: TTL expired");
            case 7: throw new IOException("SOCKS5 protocol error: Command not supported");
            case 8: throw new IOException("SOCKS5 protocol error: Address type not supported");
            default:
                throw new IOException("SOCKS5 protocol error: unassigned");
            }
        }
        tmp = din.read();             //   read reserved byte
        if (tmp != 0) {
            throw new IOException("SOCKS5 protocol error: reserved 3rd byte");
        }
        int addrType = din.read();              // read address type
        int addrLength = 0;
        switch (addrType) {
        case 1 : addrLength = 4;
            break;
        case 3 : addrLength = 0;
            break;
        case 4 : addrLength = 16;
            break;
        default:
            new IOException("SOCKS5 protocol error: " + addrType);
        }
        if (addrType == 1 || addrType == 4) {
        } else {
            addrLength = din.read();
        }
        for (int i = 0; i < addrLength; i++) { // read address
            tmp = din.read();
        }
        tmp = din.read();   // finaly read port
        tmp = din.read();
        
        return channel;
    }


    SocketChannel connectTo(String host, int port) throws IOException {
        SocketChannel sc = null;
        try {
            sc = SocketChannel.open();
            sc.configureBlocking(true);       
            sc.connect(new InetSocketAddress(host, port));
            sc.finishConnect();
            return sc;
        } catch (Exception ioe) {
            return connectSocks(host, port, "192.18.98.61", 1080);
        }
    }
}
