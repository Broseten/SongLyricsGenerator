package cz.brose;

import cz.brose.lyrics.SRTHandler;
import cz.brose.lyrics.SRTHandlerImpl;
import cz.brose.lyrics.SimpleSRTHandler;
import cz.brose.sound.MinimFileSystemHandler;
import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import ddf.minim.analysis.FFT;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * TODO package
 * TODO display text - some class hierarchy
 * TODO analyze sound - try to do it offline
 * TODO try to generate the video completely offline
 * TODO noise display (plynuly prechody mezi barvama - source and final color a prechodova barva mezi tim)
 *
 * @author Vojtech Bruza
 */
public class Main extends PApplet {
    public static void main(String[] args){
        if (args.length < 1){
            System.err.println("No song name provided. Please gimme some argument!");
            System.exit(2);
        }
        if (args.length > 1){
            System.err.println("Please gimme just ONE song name!");
            System.exit(1);
        } //TODO check input (file name, file content specification...)
        PApplet.main("cz.brose.Main", args);
    }

    public void settings(){
//        fullScreen();
        size(1024,576); //16:9
        inputSongName = args[0];
    }
    private PVector center;

    private Minim minim;
    private AudioPlayer song;
    private String inputSongName;
    //Graphics
    private GraphicArts graphicArts;
    //Lyrics
    private String textToDisplay;
    private SRTHandler srtHandler;
    private long nextLyricsStartMillis;
    private long nextLyricsEndMillis;
    //Recording
    private boolean recording;
    private long recordedFrames;


    public void setup() {
        center = new PVector(width/2,height/2);

        //Default style
        noStroke();
        colorMode(HSB,360,100,100,100);
        background(0);

        graphicArts = new GraphicArts();
        minim = new Minim(new MinimFileSystemHandler());

        //Text style
        textAlign(CENTER);
        PFont font = createFont("src/main/resources/fonts/CANDY.ttf", 128);
        textFont(font);
        textToDisplay = "";

        //Lyrics handling
        srtHandler = new SimpleSRTHandler("src/main/resources/" + inputSongName + ".srt");

        //Video settings
        recording = false;
        recordedFrames = 0;
        frameRate(30);

        //Song playing //TODO remove the path from loadFile
        song = minim.loadFile("src/main/resources/" + inputSongName + ".mp3"); //minim.load(songName + ".mp3", 2048); //TODO pokud nekonci na .mp3, tak pridej mp3
        Thread songPlayer = new Thread(() -> song.play()); //play song in another thread
        songPlayer.start();
    }

    public void draw() {
        clearBG(90);

        //Analyze
        float[] spectrumAmps = getSpectrum(); //be able do this offline
        //Draw graphics
        graphicArts.display(spectrumAmps);

        //Lyrics
        drawLyrics(100);

        //Video generator
        record();

        diplaySongPositionBar(10);
    }

    private void record() {
        if(recording){
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new java.util.Date());
            saveFrame("/output/"+inputSongName+"/"+timeStamp+"####.jpg");
        }
    }

    private void diplaySongPositionBar(int barHeight) {
        if(mouseY > height - 2*barHeight) {
            pushStyle();
            fill(300);
            rect(0, height - barHeight, map(song.position(), 0, song.length(), 0, width), height);
            popStyle();
        }
    }

    private void drawLyrics(int fill) {
        pushStyle();
        textToDisplay = srtHandler.getNextLyrics(song.position());
        blendMode(ADD);
        fill(fill);
        text(textToDisplay,center.x,center.y);
        popStyle();
    }

    private void clearBG(int rgbSubtract) {
        pushStyle();
        blendMode(SUBTRACT);
        fill(rgbSubtract);
        rect(0, 0, width, height);
        popStyle();
    }

    private float[] getSpectrum() {
        FFT fft = new FFT(song.bufferSize(), song.sampleRate());
        fft.window(FFT.GAUSS);
        //        fft.noAverages();
        ////        fft.logAverages(22, 10); //miBandWidth - herz nejnizzi oktavy, vysledek bude obsahovat 10x pocet oktav ruznych prumeru
        fft.forward(song.mix);
        float[] spectrumAmps = new float[fft.specSize()];
        for (int i = 0; i < spectrumAmps.length; i++){
            spectrumAmps[i] = fft.getBand(i);
        }
        return spectrumAmps;
    }

    @Override
    public void keyPressed() {
        // SONG CONTROL
        if (key == '+') { //fast forward
            song.skip(12000);
        } else if (key == '-') { //backward
            song.skip(-12000);
        } else if (key == 's') { //stop playing
            if(song.isPlaying()) {
                song.rewind();
                song.pause();
            }
        } else if(key == ' '){ //pause or play
            if(song.isPlaying()){song.pause();}
            else{song.play();}
        }
        // RECORDING
        else if (key == 'r') { //toggle recording
            recordedFrames = 0;
            if(recording){
                System.out.println("Recorded frames: " + recordedFrames);
            }
            recording = !recording;
        }
    }

    @Override
    public void dispose(){
        song.close();
        minim.stop();
    }

    /**
     * To display sound amplitudes
     */
    private class GraphicArts{
        ArrayList<Thing> things = new ArrayList<>();
        public void display(float[] spectrumAmps){

//            noStroke();
//            for (int i = 0; i < spectrumAmps.length; i++) {
//                float amnt = map(spectrumAmps[i], 0, 6f, 0, 1);
//                int c = lerpColor(color(300,100,100),color(200,100,100),amnt);
////                fill(255);
//                stroke(c);
//                noFill();
//                float x = map(i, 0, spectrumAmps.length,0,width/2);
////                float xStep = width/(float)spectrumAmps.length;
//                bezier(x, height,x-random(-20,20),height-spectrumAmps[i]*30,x+random(-20,20),height-spectrumAmps[i]*30,x,height-spectrumAmps[i]*40);
//                bezier(width-x, height,width-x+random(-20,20),height-spectrumAmps[i]*30,width-x+random(-20,20),height-spectrumAmps[i]*30,width-x,height-spectrumAmps[i]*40);
//                if(10 < i && i < 30 && spectrumAmps[i] > 6){
//                    things.add(new Thing());
//                }
//            }

            noStroke();
            float level = song.mix.level();
            fill(360,120*level);
            pushMatrix();
            translate(center.x,center.y);
            rotate(level);
            popMatrix();
            for (int i = 0; i < things.size(); i++) {
                Thing thing = things.get(i);
                thing.update();
                thing.display();
                if (thing.isOut()) {
                    things.remove(i);
                }
            }

        }
    }

    private class Thing{
        PVector pos;
        PVector vel;
        PVector acc;

        public PVector getAcc() {
            return acc;
        }

        public PVector getPos() {
            return pos;
        }

        float size;

        float getSize() {
            return size;
        }

        Thing(){
            pos = center.copy().add(PVector.random2D().setMag(random(400)));
            vel = pos.copy().sub(center).setMag(random(.2f,.4f));
            acc = vel.copy().setMag(.01f);
            size = random(1,2);
        }

        void update(){
            vel.add(acc);
            pos.add(vel);
            size+=.1f;
        }
        void display(){
            ellipse(pos.x,pos.y,size,size);
        }

        boolean isOut(){
            return pos.x < 0 || pos.x > width || pos.y < 0 || pos.y > height;
        }
    }
}
