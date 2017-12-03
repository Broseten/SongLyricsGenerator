package cz.brose.lyrics;

/**
 * @author Vojta_2
 */
public interface SRTHandler {
    long ONEHOURINMILLIS = 3600000;
    /**
     *
     * @param millis
     * @return
     */
    String getNextLyrics(long millis);
}
