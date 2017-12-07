package cz.brose.lyrics;

import org.fredy.jsrt.api.SRT;
import org.fredy.jsrt.api.SRTInfo;
import org.fredy.jsrt.api.SRTWriter;
import org.fredy.jsrt.api.SRTWriterException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.ListIterator;
import java.util.Scanner;

/**
 * @author Vojtech Bruza
 */
public class SRTWriterWrapper implements SRTHandler {
    SRTInfo srtInfo;
    private ArrayList<String> inputLines;
    ListIterator<String> currentLine;
    String originalFilePath;

    public SRTWriterWrapper(String lyricsTextFile){
        originalFilePath = lyricsTextFile;
        Scanner input = null;
        try {
            File file = new File(lyricsTextFile);
            input = new Scanner(file);
        } catch (IOException e) {
            System.err.println("Not able to read text file: " + lyricsTextFile);
            e.printStackTrace();
            System.exit(2);
        }
        inputLines = new ArrayList<>();
        while (input.hasNextLine()) {
            inputLines.add(input.nextLine());
        }
        input.close();
        currentLine = inputLines.listIterator();
        srtInfo = new SRTInfo();
    }

    //called when pressing some key
    public void writeNext(long startMillis, long endMillis){
        String line = "";
        if(currentLine.hasNext()){
            line = currentLine.next();
        } else return;
        //TODO allow some way to show next lyrics on the screen
        SRT srt = new SRT(currentLine.previousIndex(), new Date(startMillis - SRTHandler.ONEHOURINMILLIS), new Date(endMillis - SRTHandler.ONEHOURINMILLIS), line);
        srtInfo.add(srt);
    }

    public void saveFile(){
        File out = new File(originalFilePath + ".srt");
        try {
            out.createNewFile();
            SRTWriter.write(out, srtInfo);
        } catch (IOException e) {
            System.err.println("File already exists: " + out.getPath());
            e.printStackTrace();
        } catch (SRTWriterException e) {
            System.err.println("Could not save srt file.");
            e.printStackTrace();
        }
    }

    @Override
    public String getNextLyrics(long millis) {
        if(currentLine.hasNext()){
            return inputLines.get(currentLine.nextIndex());
        } else return "";
    }
}
