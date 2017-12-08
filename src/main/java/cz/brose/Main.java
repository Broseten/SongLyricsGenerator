package cz.brose;

import cz.brose.lyrics.SRTHandler;
import cz.brose.lyrics.SRTWriterWrapper;
import cz.brose.lyrics.SimpleSRTHandler;
import cz.brose.sound.MinimFileSystemHandler;
import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import ddf.minim.analysis.FFT;
import org.apache.commons.io.FilenameUtils;
import processing.core.*;

import javax.swing.*;
import java.io.IOException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * TODO analyze sound offline (video offline?)
 * TODO better colors
 * TODO fix images... not working in java for some reasons
 *
 * @author Vojtech Bruza
 */
public class Main extends PApplet {
    public static void main(String[] args){
        PApplet.main("cz.brose.Main", args);
    }

    public void settings(){
        fullScreen();
//        size(3508,2480); //300dpi A4
//        size(1280,720); 16:9
        //MINIM
        minim = new Minim(new MinimFileSystemHandler());

        try {
            //Select folder with: song, srt, fontFile, color source, (background and mask image):
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new java.io.File("."));
            chooser.setDialogTitle("Select folder with song files:");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            // disable the "All files" option.
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                folderSelected(chooser.getSelectedFile());
            }
            else {
                System.err.println("No working directory selected.");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    //called when the user folder is selected
    void folderSelected(File selection) {
        if (selection == null) {
            System.err.println("Was not able to open input folder");
            exit();
        } else {
            println("Selected folder: " + selection.getName());
            inputFolderPath = selection.getAbsolutePath();
            File[] folderFiles = selection.listFiles();
            boolean srtFound = false;
            for (File folderFile : folderFiles) {
                String ext = FilenameUtils.getExtension(folderFile.getName());
                switch (ext) {
                    case "mp3": //song
                        if(song == null){
                            song = minim.loadFile(folderFile.getAbsolutePath());
                        } else {
                            System.err.println("Just one mp3 file is supported");
                        }
                        break;
                    case "png": case "jpeg": case "jpg": ///TODO doc accepted formats
                        if(folderFile.getName().contains("background")){
                            if (backgroundImage == null) {
                                backgroundImage = loadImage(folderFile.getAbsolutePath());
                            }
                        } else if(folderFile.getName().contains("mask")){
                            if (maskImage == null) { //TODO loading more of them and be able to cycle through it
                                maskImage = loadImage(folderFile.getAbsolutePath());
                            }
                        } else if (srcImage == null) { //load any other default image as color source image
                            srcImage = loadImage(folderFile.getAbsolutePath());
                        }
                        break;
                    case "srt":
                        srtFound = true;
                        try {
                            srtHandler = new SimpleSRTHandler(folderFile.getAbsolutePath());
                            textToDisplay = "";
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.err.println("Error loading srt file");
                        }
                        break;
                    case "txt": // in lyrics creator mode you can press "w" to write lyrics
                        if(srtHandler == null) {
                            srtHandler = new SRTWriterWrapper(folderFile.getAbsolutePath());
                            textToDisplay = srtHandler.getNextLyrics(0);
                        }
                        break;
                    case "ttf": case "otf": //fontFile
                        fontFile = folderFile;
                        break;
                    default:
                }
            }
            if(!srtFound && srtHandler != null) srtCreatorMode = true; //if no srt file found - use text file to create srt
            if(srcImage == null){
                System.err.println("Folder must contain color source image");
                exit();
                //TODO load default
                System.out.println("User specified color source image must contain \"color\" in file name");
            }
            if(backgroundImage == null){
                //TODO load default
                System.out.println("No bg specified. User specified background image must contain \"background\" in file name");
            }
            if(maskImage == null){
                //TODO load default
                System.out.println("User specified mask image must contain \"mask\" in file name");
            }
            if(fontFile == null){
                System.out.println("No fontFile specified, using default...");
            }
        }
    }

    private PVector center;

    private String inputFolderPath;
    //Song
    private Minim minim;
    private AudioPlayer song;
    private final float songPositionBarHeight = 10;
    //Graphics
    private GraphicArts graphicArts;
    private PImage srcImage; //color source
    private PImage backgroundImage; //color source
    private PImage maskImage; //color source
    private int satureColor; //from source img
    private int notSatureColor; //from source img
    private int maxSatuBrighAlph; //max saturation, brightness and alpha value
    //Lyrics
    private String textToDisplay;
    private SRTHandler srtHandler;
    private boolean srtCreatorMode;
    File fontFile;
    //Recording
    private boolean recording;
    private long recordedFrames;


    public void setup() {
        center = new PVector(width/2,height/2);

        //Default style
        noStroke();
        maxSatuBrighAlph = 100;
        background(0);

        //Graphics
        graphicArts = new GraphicArts();
        srcImage.loadPixels();
        setColors();
        if (maskImage != null) {
            processMask();
        }

        //Text style
        textAlign(CENTER, CENTER);
        if(fontFile == null){
            textSize(32); // only when no fontFile is specified
        } else {
            //TODO load fontFile size from its name (min size 32) - writ it to DOCUMENTATION - https://fontlibrary.org/
            int fontSize;
            try { //TODO remove redundant exception and check the input in some better way
                fontSize = Integer.parseInt(fontFile.getName().replaceAll("[\\D]", ""));
            } catch (NumberFormatException e) {
                System.out.println("You can specify fontFile size by writing it to fontFile name (must not contain any other digits)");
                fontSize = 32;
            }
            PFont font = createFont(fontFile.getAbsolutePath(), fontSize);
            textFont(font);
        }

        //Video settings
        recording = false;
        recordedFrames = 0;
        frameRate(24);

        //Song playing
//        Thread songPlayer = new Thread(() -> song.play()); //TODO play song in another thread, this code is non-sense
//        songPlayer.start();
    }

    private void setColors() {
        satureColor = srcImage.pixels[(int)random(srcImage.pixels.length-1)]; //TODO finding saturated color
        notSatureColor = srcImage.pixels[(int)random(srcImage.pixels.length-1)]; //TODO finding not sature color
    }

    public void draw() {
        clearBG();

        //Analyze
        float[] spectrumAmps = getSpectrum(); //be able do this offline
        //Draw graphics
        graphicArts.display(spectrumAmps);

        //Lyrics //TODO correct text blending and appearing
        drawLyrics(); //SLOWS down the sketch a lot!! //TODO in different thread

        //Mask
//        image(maskImage,0,0); //TODO fix mask render - not working for some random reason

        //Video generator
        if(!song.isPlaying()){
            recording = false;
        }
        if (recording) {
            record();
        }

        diplaySongPositionBar(songPositionBarHeight);
    }

    private void displayRecordingIndic() {
        pushStyle();
        fill(255,0,0);
        ellipse(10,height-10,30,30);
        popStyle();
    }

    /**
     * Processes alpha channel of the image mask (white to transparent, black nontransparent)
     */
    private void processMask(){
        maskImage.resize(width,height);//to resize picture mask to current screen size
        maskImage.loadPixels();
        for (int i = 0; i < maskImage.width; i++){
            for(int j = 0; j < maskImage.height; j++){
                int c = color(maskImage.pixels[i + width*j]);
                maskImage.pixels[i + width*j] = color(0,255-blue(c));
            }
        }
        maskImage.updatePixels();
    }

    private String timeStamp(){
        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new java.util.Date());
    }

    ExecutorService threadService = Executors.newFixedThreadPool(4);
    private void record() {
        String timeStamp = timeStamp();
        threadService.submit(() -> saveFrame(inputFolderPath + "/output/" + timeStamp + "####.png"));
        recordedFrames++;
//        displayRecordingIndic();
    }

    private void diplaySongPositionBar(float barHeight) {
        if(mouseY > height - 2*barHeight) {
            pushStyle();
            fill(200);
            rect(0, height - barHeight, map(song.position(), 0, song.length(), 0, width), height);
            popStyle();
        }
    }

    private void drawLyrics() {
        pushMatrix();
        pushStyle();
        textToDisplay = srtHandler.getNextLyrics(song.position());
        fill(255);
        translate(center.x,center.y);
        if(textWidth(textToDisplay) > width){ //if the text does not fit the screen size, make it two rows long
            for (int i = textToDisplay.length()/2; i < textToDisplay.length(); i++){
                if (textToDisplay.charAt(i) == ' '){ //replace first space after the mid with end of line
                    textToDisplay = textToDisplay.substring(0,i) + "\n" + textToDisplay.substring(i+1);
                    break;
                }
            }
        }
        text(textToDisplay,0,-height/8);
        popStyle();
        popMatrix();
    }

    private void clearBG() {
        pushStyle();
        if(backgroundImage == null){
            fill(0,40);
        } else {
//            image(backgroundImage,0,0); //TODO fix images
        }
        rect(0, 0, width, height);
        popStyle();
    }

    private float[] getSpectrum() {
        FFT fft = new FFT(song.bufferSize(), song.sampleRate());
        fft.window(FFT.GAUSS);
        //        fft.noAverages();
        fft.logAverages(22, 6); //miBandWidth - herz nejnizzi oktavy, vysledek bude obsahovat 10x pocet oktav ruznych prumeru
        fft.forward(song.mix);
        float[] spectrumAmps = new float[fft.avgSize()];
        for (int i = 0; i < spectrumAmps.length; i++){
            spectrumAmps[i] = fft.getAvg(i);
        }
        return spectrumAmps;
    }

    @Override
    public void mousePressed() {
        if(mouseY > height - 2 * songPositionBarHeight){ //setSong position
            song.cue((int) map(mouseX,0, width, 0, song.length()));
        }
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
                song.pause();
                song.rewind();
            }
        } else if(key == ' '){ //pause or play
            if(song.isPlaying()){song.pause();}
            else{song.play();}
        }
        // RECORDING
        else if (key == 'r') { //toggle recording
            if(recording){
                System.out.println("Recorded frames: " + recordedFrames);
            } else {
                System.out.println("Recording started...");
                recordedFrames = 0;
            }
            recording = !recording;
        }
        //WRITING LYRICS
        else if (key == 'w'){
            if(srtCreatorMode) {
                ((SRTWriterWrapper) srtHandler).writeNext(song.position(), song.position() + 2000); //end after half a second
            }
        }
        //COLORS SETTING
        else if(key == 'c'){
            setColors();
        }
    }

    @Override
    public void dispose(){
        threadService.shutdown();
        try {
            threadService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.err.println("Saving frames took too long...");
            e.printStackTrace();
        }
        if(srtCreatorMode){
            ((SRTWriterWrapper) srtHandler).saveFile();
        }
        song.close();
        minim.stop();
    }

    /**
     * To display sound amplitudes
     */
    private class GraphicArts{
        ArrayList<Particle> particles = new ArrayList<>();
        Blob blob = new Blob(center.copy(),100);
        Wave[] waves = new Wave[4];

        public void display(float[] spectrumAmps){
            //TODO switching between graphics

            waves[0] = new Wave(center.x,center.y,width/2,8,spectrumAmps);
            waves[1] = new Wave(center.x,center.y,width/2,-8,spectrumAmps);
            waves[2] = new Wave(center.x,center.y,-width/2,8,spectrumAmps);
            waves[3] = new Wave(center.x,center.y,-width/2,-8,spectrumAmps);
            fill(255);
            for (int i = 0; i < waves.length; i++) {
                waves[i].render();
            }
            for (int i = 0; i < spectrumAmps.length; i++) {
                float amnt = map(spectrumAmps[i], 0, 20f, 0, 1);
                int c = lerpColor(notSatureColor,satureColor,amnt);
//                fill(255);
                stroke(c);
                noFill();
                float x = map(i, 0, spectrumAmps.length,0,width/2);
//                float xStep = w/(float)spectrumAmps.length;
                float lineHeightMult = 6;
                bezier(x, height,x-random(-20,20),height-spectrumAmps[i]*lineHeightMult/2,x+random(-20,20),height-spectrumAmps[i]*lineHeightMult/2,x,height-spectrumAmps[i]*lineHeightMult);
                bezier(width-x, height,width-x+random(-20,20),height-spectrumAmps[i]*lineHeightMult/2,width-x+random(-20,20),height-spectrumAmps[i]*lineHeightMult/2,width-x,height-spectrumAmps[i]*lineHeightMult);
//                if(10 < i && i < 30 && spectrumAmps[i] > 6){
//                particles.add(new Particle(new PVector(center.x, 3*h/4), 20*spectrumAmps[i]));

//                }
            }

            float level = song.mix.level();

            noStroke();
            fill(255);
//            pushMatrix();
//            translate(center.x,center.y);//TODO rotation and translation?
//            rotate(level);
//            popMatrix();

            for(int i = 1; i < 20*level; i++) {
                particles.add(new Particle(new PVector(random(width), height), random(15)));
            }

            for (int i = 0; i < particles.size(); i++) {
                Particle particle = particles.get(i);
                particle.update();
                particle.render();
                if (particle.isDead()) {
                    particles.remove(i);
                }
            }
            pushStyle();
            stroke(lerpColor(notSatureColor,satureColor,map(level,0,0.2f,0,1)));
            fill(0,120);
            blob.render();
            popStyle();
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
            //remove acc to make the sketch run faster
            this.size = size;
            this.pos = pos.copy();

            float vx = randomGaussian()*0.3f;
            float vy = randomGaussian()*0.3f - 1.0f;
            vel = new PVector(vx, vy);

            acc = new PVector(0, 0);
            lifespan = 500 + 650/size;

            color = srcImage.pixels[(int)random(srcImage.pixels.length)];
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
            return lifespan <= 0;
        }
    }

    class Wave {//TODO remake to function - you dont have to initialize it everyTime
        //TODO viz nas plakatek
        float x, y;
        float w, h;

        float[] amps;

        public Wave(float x, float y, float w, float h, float[] amps) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.amps = amps.clone();
            for (int i = 0; i < amps.length; i++) {
                if ((h < 0)) {
                    this.amps[i] *= (- (1.1+i/5)); //revert the amplitude if the wave is upside down and scale it
                } else {
                    this.amps[i] *= (1.1+i/10);
                }
            }
        }

        void render(){
            beginShape();
            vertex(x, y);
            vertex(x,y + h + amps[0]);
            curveVertex(x,y + h + amps[0]);
            for (int i = 0; i < amps.length; i++) {
                float xi = map(i,0,amps.length-1,x,x + w);
                curveVertex(xi,y + h + amps[i]);
            }
            curveVertex(x + w,y + h + amps[amps.length-1]);
            vertex(x + w, y);
            endShape(CLOSE);
        }
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
