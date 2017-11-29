package cz.brose.sound;

/**
 * @author Vojtech Bruza
 */
public interface AudioAnalyser {

    /**
     * Return spectrum for given frame.
     * Size is equal to number of fft spectrum
     * @param frameCount
     */
    public float[] getSpectrum(int frameCount);
}
