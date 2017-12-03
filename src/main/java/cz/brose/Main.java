package cz.brose;

import cz.brose.lyrics.SRTHandler;
import cz.brose.lyrics.SimpleSRTHandler;
import cz.brose.sound.MinimFileSystemHandler;
import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import ddf.minim.analysis.FFT;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.core.PVector;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * TODO analyze sound offline (video offline?)
 * TODO noise display (plynuly prechody mezi barvama - source and final color a prechodova barva mezi tim)
 *
 * TODO!
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
    private PImage rose; //color source
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

        //Graphics
        graphicArts = new GraphicArts();
        rose = loadImage("images/rose.jpeg");
        rose.loadPixels();

        //MINIM
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
        clearBG(50);

        //Analyze
        float[] spectrumAmps = getSpectrum(); //be able do this offline
        //Draw graphics
        graphicArts.display(spectrumAmps);

        //Lyrics
//        drawLyrics(100); //SLOWS down the sketch a lot!! //TODO in different thread

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
        fft.logAverages(22, 4); //miBandWidth - herz nejnizzi oktavy, vysledek bude obsahovat 10x pocet oktav ruznych prumeru
        fft.forward(song.mix);
        float[] spectrumAmps = new float[fft.avgSize()]; //TODO averages size vs spectrum size
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
        ArrayList<Particle> particles = new ArrayList<>();
        Blob blob = new Blob(center.copy(),50);

        public void display(float[] spectrumAmps){
            //TODO have different graphics and switch between them
            noStroke();
            for (int i = 0; i < spectrumAmps.length; i++) {
                float amnt = map(spectrumAmps[i], 0, 6f, 0, 1);
                int c = lerpColor(color(300,100,100),color(200,100,100),amnt);
//                fill(255);
                stroke(c);
                noFill();
                float x = map(i, 0, spectrumAmps.length,0,width/2);
//                float xStep = width/(float)spectrumAmps.length;
                float lineHeightMult = 10;
                bezier(x, height,x-random(-20,20),height-spectrumAmps[i]*lineHeightMult/2,x+random(-20,20),height-spectrumAmps[i]*lineHeightMult/2,x,height-spectrumAmps[i]*lineHeightMult);
                bezier(width-x, height,width-x+random(-20,20),height-spectrumAmps[i]*lineHeightMult/2,width-x+random(-20,20),height-spectrumAmps[i]*lineHeightMult/2,width-x,height-spectrumAmps[i]*lineHeightMult);
//                if(10 < i && i < 30 && spectrumAmps[i] > 6){
//                particles.add(new Particle(new PVector(center.x, 3*height/4), 20*spectrumAmps[i]));

//                }
            }

            fill(200);
            blob.render();

            noStroke();
            float level = song.mix.level();
            fill(360);
            pushMatrix();
            translate(center.x,center.y);//TODO rotation and translation?
            rotate(level);
            popMatrix();

            for(int i = 1; i < 20*level; i++) {
                particles.add(new Particle(new PVector(random(width), height), random(10)));
            }

            for (int i = 0; i < particles.size(); i++) {
                Particle particle = particles.get(i);
                particle.update();
                particle.render();
                if (particle.isDead()) {
                    particles.remove(i);
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

    class Particle { // see https://processing.org/examples/smokeparticlesystem.html
        PVector pos;
        PVector vel;
        PVector acc;
        float lifespan;
        float size;
        int color;

        Particle(PVector pos, float size) {
            //TODO remove acc to make the sketch run faster
            this.size = size;
            this.pos = pos.copy();

            float vx = randomGaussian()*0.3f;
            float vy = randomGaussian()*0.3f - 1.0f;
            vel = new PVector(vx, vy);

            acc = new PVector(0, 0);
            lifespan = 500 + 600/size;

            color = rose.pixels[(int)random(rose.pixels.length)];
        }

        void run() {
            update();
            render();
        }

        // Method to apply a force vector to the Particle object
        // Note we are ignoring "mass" here
        void applyForce(PVector f) {
            acc.add(f);
        }

        // Method to update position
        void update() {
            vel.add(acc);
            pos.add(vel);
            lifespan -= 2.5;
            acc.mult(0); // clear Acceleration
        }

        // Method to display
        void render() {
            imageMode(CENTER);
            tint(255, lifespan);
            fill(color,lifespan);
            noStroke();
            ellipse(pos.x, pos.y,size,size);
        }

        // Is the particle still useful?
        boolean isDead() {
            return lifespan <= 0.0;
        }
    }

    class Wave {
        //TODO
    }

    class Beams {
        //TODO (neco jako jeden obrazek na zacatku videa packing circles)
    }

    class Blob {
        PVector pos;
        float radius;

        public Blob(PVector pos, float radius) {
            this.pos = pos;
            this.radius = radius;
        }

        float yOff = 0;
        public void render(){
            pushMatrix();
            translate(pos.x,pos.y);
            beginShape();
            float xOff = 0;
            for(float angle = 0; angle < TWO_PI; angle += 0.5){
                float r = this.radius + /*noise(xOff, yOff)**/500*song.mix.level(); //TODO correct noise (each vertex should change its value acording to the closest
                float x = r * cos(angle);
                float y = r * sin(angle);
                vertex(x,y);
                xOff+= 0.1;
            }
            yOff += 0.01;
            endShape(CLOSE);
            popMatrix();
        }
    }

}
