package daw.core.audioprocessing;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import java.util.Map;

/**
 * This class is an extension of {@link UGen}. In the context of this software, this class is the one
 * all effects must extend. Non-abstract subclasses must provide an implementation for method calculateBuffer.
 */
public abstract class RPEffect extends UGen implements AudioElement {

    /**
     * Base constructor for all the effects of this software.
     * @param context the {@link AudioContext} used by this effect.
     * @param ins the number of inputs of this effect.
     * @param outs the number of output of this effect.
     */
    protected RPEffect(AudioContext context, int ins, int outs) {
        super(context, ins, outs);
    }

    /**
     * Allows getting the value associated to each parameter of this particular effect.
     * Parameters influence how the sound is processed by this effect.
     * @return a {@link Map} where the keys are the parameters and the values are the
     * current value of each parameter of the effect.
     */
    public abstract Map<String, Float> getParameters();

    /**
     * Modifies the value of all the parameters specified as keys in the given map. Those parameters
     * will then contain the value associated to them in the map.
     * The keys that do not match any of the effect's parameters are ignored
     * and so are the values associated to them.
     * @param parameters the {@link Map} that contains the parameters that must be modified.
     */
    public abstract void setParameters(Map<String, Float> parameters);

    /**
     * Allows to get the number of input channels of the effect.
     * @return an integer that represents the number of input.
     */
    public abstract int getIns();

    /**
     * Allows to get the number of output channels of the effect.
     * @return an integer that represents the number of outputs.
     */
    public abstract int getOuts();

    /**
     * Allows getting the default value of a certain parameter of this particular effect.
     * @param key a parameter of this effect.
     * @return the floating-point default value of the parameter.
     * @throws IllegalArgumentException if the given string does not match any of the parameters of this effect.
     */
    public abstract float getDefaultValue(String key);

    /**
     * Sets a default value for the parameter specified by the given key. If there is no such parameter,
     * this method does nothing.
     * @param key the name of a parameter.
     * @param value the value that the key has as default after this method is called.
     */
    public abstract void setDefaultValue(String key, float value);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void calculateBuffer();

}
