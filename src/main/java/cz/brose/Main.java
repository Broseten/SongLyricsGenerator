package cz.brose;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import ddf.minim.analysis.FFT;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;

import java.util.ArrayList;

/**
 * TODO package
 * TODO display text - some class hierarchy
 * TODO analyze sound - try to do it offline
 * TODO try to generate the video completely offline
 * TODO skipping when playing live
 * TODO noise display (plynuly prechody mezi barvama - source and final color a prechodova barva mezi tim)
 *
 * @author Vojtech Bruza
 */
public class Main extends PApplet {
    public static void main(String[] args){
        PApplet.main("cz.brose.Main", args);
    }

    public void settings(){
//        fullScreen();
        size(1024,576); //16:9
    }
    PVector center;

    private Minim minim;
    private AudioPlayer song;
    private GraphicArts graphicArts;
    private String textToDisplay;
    private SRTObject srtHandler;
    long nextLyricsMillis;

    public void setup() {
        center = new PVector(width/2,height/2);
        noStroke();
        colorMode(HSB,360,100,100,100);
        background(0);
        graphicArts = new GraphicArts();
        minim = new Minim(new MinimFileSystemHandler());
        textAlign(CENTER);
        PFont font = createFont("src/main/resources/fonts/CANDY.ttf", 128);
        textFont(font);
        textToDisplay = "";

        srtHandler = new SRTObject(args[0] + ".srt");
        nextLyricsMillis = srtHandler.getNextLyricsStartTime();
        System.out.println("FIRST MILLIS: "+nextLyricsMillis);

        song = minim.loadFile(args[0] + ".mp3"); //minim.load(songName + ".mp3", 2048); //TODO pokud nekonci na .mp3, tak pridej mp3
        Thread songPlayer = new Thread(() -> song.play()); //play song in another thread
        songPlayer.start();
    }
    public void draw() {
        fill(0,28);
        rect(0,0,width,height);

        float[] spectrumAmps = getSpectrum(); //be able do this offline

        graphicArts.display(spectrumAmps);

        //Lyrics
        pushStyle();
//        blendMode(ADD);
        if(song.position() >= nextLyricsMillis) { //TODO let it run in diffrent thread in case of slow down
            textToDisplay = srtHandler.getNextLyrics(song.position());
            nextLyricsMillis = srtHandler.getNextLyricsStartTime();
        }

        fill(20);
        text(textToDisplay,center.x,center.y);
        popStyle();

        //TODO saveframe
        fill(255);
        rect(0,height-10,map(song.position(),0,song.length(),0,width),height);
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
        if (key == '+') { //fast forward
            song.skip(12000);
        } else if (key == '-') { //backward
            song.skip(-12000);
        } else if (key == 'r') { //rewind and play
            if(song.isPlaying()) {
                song.rewind();
            } else {
                song.rewind();
                song.play();
            }
        } else if (key == 's') { //stop playing
            if(song.isPlaying()) {
                song.rewind();
                song.pause();
            }
        } else if(key == ' '){ //pause or play
            if(song.isPlaying()){song.pause();}
            else{song.play();}
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
            for (int i = 0; i < spectrumAmps.length; i++) {
                float amnt = map(spectrumAmps[i], 0, 6f, 0, 1);
                int c = lerpColor(color(300,100,100),color(200,100,100),amnt);
//                fill(255);
                stroke(c);
                noFill();
                float x = map(i, 0, spectrumAmps.length,0,width/2);
//                float xStep = width/(float)spectrumAmps.length;
                bezier(x, height,x-random(-20,20),height-spectrumAmps[i]*30,x+random(-20,20),height-spectrumAmps[i]*30,x,height-spectrumAmps[i]*40);
                bezier(width-x, height,width-x+random(-20,20),height-spectrumAmps[i]*30,width-x+random(-20,20),height-spectrumAmps[i]*30,width-x,height-spectrumAmps[i]*40);
                if(10 < i && i < 30 && spectrumAmps[i] > 6){
                    things.add(new Thing());
                }
            }

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
