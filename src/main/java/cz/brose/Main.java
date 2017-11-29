package cz.brose;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import processing.core.PApplet;

/**
 * TODO package
 * TODO draw text - some class hierarchy
 * TODO analyze sound - try to do it offline
 * TODO try to generate the video completely offline
 * TODO skipping when playing live
 * TODO noise draw (plynuly prechody mezi barvama - source and final color a prechodova barva mezi tim)
 *
 * @author Vojtech Bruza
 */
public class Main extends PApplet {
    public static void main(String[] args){
        PApplet.main("Main", args);
    }

    public void settings(){
//        fullScreen();
        size(1024,576);
    }

    private Minim minim;
    private AudioPlayer song;

    public void setup() {
        noStroke();
        background(0);
        minim = new Minim(new MinimFileSystemHandler());
        song = minim.loadFile(args[0]);
        song.play();
    }

    public void draw() {
    }

}
