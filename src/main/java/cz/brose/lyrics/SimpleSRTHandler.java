 package cz.brose.lyrics;

import org.fredy.jsrt.api.SRT;
import org.fredy.jsrt.api.SRTInfo;
import org.fredy.jsrt.api.SRTReader;
import org.fredy.jsrt.api.SRTReaderException;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author Vojtech Bruza
 */
public class SimpleSRTHandler implements SRTHandler {
    ArrayList<SRT> srts;
    private int expectedSrtIndex; //next expected srt index

    public SimpleSRTHandler(String srtFileName) {
        SRTInfo inputSRT;
        try {
            inputSRT = SRTReader.read(new File(srtFileName));
            srts = new ArrayList<>();
            for (SRT srt : inputSRT) {
                srts.add(srt);
            }
            expectedSrtIndex = 0;
        } catch (SRTReaderException e) {
            System.err.println("Check this file, not able to read it: " + srtFileName);
            System.exit(1);
        }
    }

    final long ONEHOURINMILLIS = 3600000;

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

    //TODO be able to save lyrics when I click - rather in a different class
    public void writeNext(){ //called on clicking some button
        //load next lyrics from a text file

        //show next lyrics

        //click to create new record

    }
}
