package cz.brose.sound;

/**
 * Class to store song analysis data.
 * @author Vojtech Bruza
 */
public class AudioAnalyserImpl implements AudioAnalyser {
    //TODO save to file and load file
    //TODO use logarithmic dist (protoze ve vyskach nejsou tak velky hodnoty jako v basech)
    //FFT mi rozlozi hodnotu v jednom case na hodnoty spektra v tom case
    //TODO save fft spectrum for each frame

    /**
     *
     * @param songName
     * @param numberOfFrequencyBands
     */
    AudioAnalyserImpl(String songName, int numberOfFrequencyBands){
        //TODO analyze song and save data (save it also to file to be able to load it after)
    }



    public float[] getSpectrum(int frameCount){
        //TODO
        return null;
    }
}
