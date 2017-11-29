package cz.brose;

import ddf.minim.spi.MinimServiceProvider;

import java.io.*;

/**
 * @author Vojtech Bruza
 */
public class MinimFileSystemHandler {
    public String sketchPath(String fileName ){
        return new File(fileName).getAbsolutePath();
    }
    public InputStream createInput(String fileName ){
        try {
            return new BufferedInputStream(new FileInputStream(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
