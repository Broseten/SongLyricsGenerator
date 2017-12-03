 package cz.brose.lyrics;

import org.fredy.jsrt.api.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author Vojtech Bruza
 */
public class SimpleSRTHandler implements SRTHandler {
    ArrayList<SRT> srts;
    private int expectedSrtIndex; //next expected srt index

    public SimpleSRTHandler(String srtFileName) throws IOException {
        SRTInfo inputSRT;
        try {
            inputSRT = SRTReader.read(new File(srtFileName));
            srts = new ArrayList<>();
            for (SRT srt : inputSRT) {
                srts.add(srt);
            }
            expectedSrtIndex = 0;
        } catch (SRTReaderException e) {
            throw new IOException("Could not read");
        }
    }

    public String getNextLyrics(long millis){
        String text = "";
        if(expectedSrtIndex < srts.size()){
            Date timePosition = new Date(millis - ONEHOURINMILLIS);
            for (int i = 0; i < srts.size(); i++) {
                int index = (expectedSrtIndex + i) % srts.size(); //cycle
                SRT srt = srts.get(index);
                if(timePosition.after(srt.startTime) && timePosition.before(srt.endTime)){
                    for (String line : srt.text) {
                        text += line + "\n";
                    }
                }
            }
        }
        return text;
    }
}
