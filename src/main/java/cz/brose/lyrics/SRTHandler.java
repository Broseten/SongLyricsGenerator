package cz.brose.lyrics;

/**
 * @author Vojta_2
 */
public interface SRTHandler {
    /**
     *
     * @param millis
     * @return
     */
    String getNextLyrics(long millis);
}
