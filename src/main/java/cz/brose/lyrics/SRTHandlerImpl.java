package cz.brose.lyrics;

import org.fredy.jsrt.api.SRT;
import org.fredy.jsrt.api.SRTInfo;
import org.fredy.jsrt.api.SRTReader;
import org.fredy.jsrt.api.SRTReaderException;

import java.io.File;
import java.util.ArrayList;

/**
 * @author Vojtech Bruza
 */
public class SRTHandlerImpl implements SRTHandler {
    ArrayList<SRT> srts;
    private int srtIndex;

    public SRTHandlerImpl(String srtFileName) {
        SRTInfo inputSRT;
        try {
            inputSRT = SRTReader.read(new File(srtFileName));
            srts = new ArrayList<>();
            for (SRT srt : inputSRT) {
                srts.add(srt);
            }
            srtIndex = -1;
        } catch (SRTReaderException e) {
            System.err.println("Check this file, not able to read it: " + srtFileName);
            System.exit(1);
        }
    }

    final long ONEHOURINMILLIS = 3600000;

    @Override
    public String getNextLyrics(long millis){
        if(srtIndex + 1 >= srts.size()){
            System.out.println("none");
            return ""; //there are no other lyrics
        }
        StringBuilder nextLyrics = new StringBuilder();
        if(millis >= getNextLyricsStartTime()){ //
            System.out.println("SONG POSITION: " + millis);
            srtIndex++;
            SRT actualSRT = srts.get(srtIndex);
            if(actualSRT == null){
                System.out.println("none index");
                return "";
            }
            while (millis > getNextLyricsEndTime()){
                actualSRT = srts.get(srtIndex);
                srtIndex++;
                if(srtIndex >= srts.size() || actualSRT == null || getNextLyricsEndTime() == Long.MAX_VALUE){
                    System.out.println("none2");
                    return "";
                }
            }
            for (String line : actualSRT.text) {
                nextLyrics.append(line + "\n");
            }
            System.out.println("NEXT LYRICS START: " + getNextLyricsStartTime());
            System.out.println("NEXT LYRICS END: " + getNextLyricsEndTime());
            System.out.print("LYRICS: ");
        }
        System.out.println(nextLyrics.toString());
        System.out.println();
        return nextLyrics.toString();
    }

    public long getNextLyricsStartTime(){
        int nextSRTIndex = srtIndex + 1;
        if(nextSRTIndex >= srts.size()){
            return Long.MAX_VALUE;
        }
        return srts.get(nextSRTIndex).startTime.getTime() + ONEHOURINMILLIS; //Magical constant to make it work
    }

    public long getNextLyricsEndTime(){
        int nextSRTIndex = srtIndex + 1;
        if(nextSRTIndex >= srts.size()){
            return Long.MAX_VALUE;
        }
        return srts.get(nextSRTIndex).endTime.getTime() + ONEHOURINMILLIS;
    }

    //TODO be able to save lyrics when I click - rather in a different class
    public void writeNext(){ //called on clicking some button
        //load next lyrics from a text file

        //show next lyrics

        //click to create new record

    }
}
