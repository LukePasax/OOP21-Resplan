package daw.core.audioprocessing;

import daw.utilities.AudioContextManager;
import net.beadsproject.beads.data.DataBead;
import net.beadsproject.beads.ugens.Compressor;

import java.util.Map;

/**
 * This class represents a gate, which is a tool that reduces or eliminates noise coming from an audio source.
 * Specifically, a gate parses the audio signal and reduces the volume of all the samples that do not reach
 * a certain volume threshold.
 */
public class Gate extends RPEffect {

    private final Compressor compressor;

    /**
     * Constructs a gate and sets its parameters to the current default.
     * @param channels the number of inputs and outputs of this effect.
     */
    public Gate(int channels) {
        super(AudioContextManager.getAudioContext(), channels, channels);
        this.compressor = new Compressor(AudioContextManager.getAudioContext(), channels);
    }

    /**
     * {@inheritDoc}
     * @return a {@link Map} where the keys are the parameters and the values are the
     * current value of each parameter of the effect.
     */
    @Override
    public Map<String, Float> getParameters() {
        return Map.of("threshold", this.compressor.getThreshold(), "ratio", this.compressor.getRatio(),
                "attack", this.compressor.getAttack(), "decay", this.compressor.getDecay());
    }

    /**
     * {@inheritDoc}
     * @param parameters the {@link Map} that contains the parameters that must be modified.
     */
    @Override
    public void setParameters(Map<String, Float> parameters) {
        this.compressor.sendData(new DataBead(parameters));
    }

    /**
     * Allows to get the number of input channels of the effect.
     * @return an integer that represents the number of input.
     */
    @Override
    public int getIns() {
        return this.compressor.getIns();
    }

    /**
     * Allows to get the number of output channels of the effect.
     * @return an integer that represents the number of outputs.
     */
    @Override
    public int getOuts() {
        return this.compressor.getOuts();
    }

    /**
     * {@inheritDoc}
     * @param key a parameter of this effect.
     * @return the floating-point default value of the parameter.
     * @throws IllegalArgumentException if the given string does not match any of the parameters of this effect.
     */
    // TODO
    @Override
    public float getDefaultValue(String key) {
        return 0.0f;
    }

    /**
     * Sets a default value for the parameter specified by the given key. If there is no such parameter,
     * this method does nothing.
     *
     * @param key   the name of a parameter.
     * @param value the value that the key has as default after this method is called.
     */
    @Override
    public void setDefaultValue(String key, float value) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void calculateBuffer() {
        // TODO
    }

}
