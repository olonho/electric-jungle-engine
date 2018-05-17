package universum.engine;

import java.io.*;
import java.util.zip.*;
import universum.util.Util;

class TurnRecorder {
    private int id;
    private ZipOutputStream os;
    private String name;

    TurnRecorder(String name) {
        this.name = name;
        try {
            os = new ZipOutputStream(new FileOutputStream(getName()));
            os.putNextEntry(new ZipEntry("moves"));
            os.setLevel(9);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            os = null;
        }
    }

    String getName() {
        return name;
    }

    void record(int turn, int len, DataBuffer buf) {       
        if (os == null) {
            return;
        }
        try {
            os.write(buf.data(), 0, len);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            buf.release();
        }
    }

    void finishRecording() {
        if (os == null) {
            return;
        }
        try {
            os.closeEntry();
            os.close();
            os = null;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
