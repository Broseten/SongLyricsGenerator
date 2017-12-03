package cz.brose.sound;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import ddf.minim.analysis.FFT;

/**
 * Class to store song analysis data.
 * Always sampling 60fps(this means that you have 60 sampling values per second)
 * @author Vojtech Bruza
 */
public class OnlineAudioAnalyser implements AudioAnalyser {
    //TODO save to file and load file
    //TODO each channel separately
    //use logarithmic dist? (protoze ve vyskach neni tolik ruznych hodnot jako v basech)
    //FFT mi rozlozi hodnotu v jednom case na hodnoty spektra v tom case
    //save fft spectrum for each frame

    int numberOfFrequencyBands;
    int songLength; //can be -1 when length could not be determined
    float[][] spectrumAmps;
    AudioPlayer song;

    //javadoc
    public OnlineAudioAnalyser(Minim minim, AudioPlayer song, int numberOfFrequencyBands){
        this.song = song;
        this.numberOfFrequencyBands = numberOfFrequencyBands;
        //TODO handle -1 song length if the length is unknown
        this.songLength = song.length(); //is there a different way to find out?
//        spectrumAmps = new int[numberOfSamples][];
        analyze(minim,song);
    }

    private void analyze(Minim minim, AudioPlayer song){
//        song.play();
//        for/*eachFrame*{
//            // analyze
//            // save internally
//        }
        for(int i = 0; song.position() < songLength; i++){
//            song.skip(millisecondsBetweenSamples); //TODO use cue - you can use it to offline analysis
            FFT fft = new FFT(song.bufferSize(), song.sampleRate());
            fft.window(FFT.GAUSS);
            //        fft.noAverages();
            ////        fft.logAverages(22, 10); //miBandWidth - herz nejnizzi oktavy, vysledek bude obsahovat 10x pocet oktav ruznych prumeru
            fft.forward(song.mix);
            spectrumAmps[i] = new float[fft.specSize()];
            for(int j = 0; j < fft.specSize(); j++) {
//                 = fft.getBand(j);
            }
        }

//        fft.window(FFT.GAUSS);

//        float[] leftChannel = song.getChannel(AudioSample.LEFT);
//
//        int fftSize = 1024;
//        float[] fftSamples = new float[fftSize];
//        FFT fft = new FFT( fftSize, song.sampleRate() );
//
//        int totalChunks = (leftChannel.length / fftSize) + 1;
//
//        spectra = new float[totalChunks][fftSize/2];
//
//        for(int chunkIdx = 0; chunkIdx < totalChunks; ++chunkIdx)
//        {
//            int chunkStartIndex = chunkIdx * fftSize;
//            int chunkSize = min( leftChannel.length - chunkStartIndex, fftSize );
//
//            // copy first chunk into our analysis array
//            for (int i = 0; i < chunkSize; i++){
//                fftSamples[i] = leftChannel[chunkStartIndex+i];
//            }
//
//            if ( chunkSize < fftSize ){
//                // we use a system call for this
//                Arrays.fill( fftSamples, chunkSize, fftSamples.length - 1, 0.0);
//            }
//
//            // now analyze this buffer
//            fft.forward( fftSamples );
//
//            // and copy the resulting spectrum into our spectra array
//            for(int i = 0; i < 512; ++i){
//                spectra[chunkIdx][i] = fft.getBand(i);
//            }
//        }
//        song.close();
    }

    public void saveToFile(){
        //TODO
    }
    public void loadFromFile(){
        //TODO
    }

    public float[] getSpectrum(int frameCount){
        //TODO
        return null;
    }
}
