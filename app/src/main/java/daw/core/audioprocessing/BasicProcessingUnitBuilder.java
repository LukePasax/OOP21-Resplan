package daw.core.audioprocessing;

import net.beadsproject.beads.core.UGen;
import java.util.*;

/**
 * This class represents an implementation of {@link ProcessingUnitBuilder} in which an exception upon
 * the call of the method build is raised only if no effects have been specified.
 * Remember that the method sidechaining does not add any effect, as the {@link Sidechaining} is not an effect
 * in the context of this software.
 */
public class BasicProcessingUnitBuilder implements ProcessingUnitBuilder {

    private Optional<Sidechaining> sidechain;
    private final List<RPEffect> effects;

    public BasicProcessingUnitBuilder() {
        this.sidechain = Optional.empty();
        this.effects = new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     * @param u the {@link UGen} which this {@link ProcessingUnit} is sidechained to.
     * @param channels the number of input and output channels for the underlying compressor.
     * @return a reference to this object.
     */
    @Override
    public ProcessingUnitBuilder sidechain(UGen u, int channels) {
        if (this.sidechain.isEmpty()) {
            this.sidechain = Optional.of(new Sidechaining(u, channels));
        }
        return this;
    }

    /**
     * {@inheritDoc}
     * @param channels the number of input and output channels for this effect.
     * @param cutoffFrequency the frequency over which sound gets attenuated or eliminated.
     * @return a reference to this object.
     */
    @Override
    public ProcessingUnitBuilder lowPassFilter(int channels, float cutoffFrequency) {
        this.effects.add(new LowPassFilter(channels, cutoffFrequency));
        return this;
    }

    /**
     * {@inheritDoc}
     * @param channels the number of input and output channels for this effect.
     * @param cutoffFrequency the frequency under which sound gets attenuated or eliminated.
     * @return a reference to this object.
     */
    @Override
    public ProcessingUnitBuilder highPassFilter(int channels, float cutoffFrequency) {
        this.effects.add(new HighPassFilter(channels, cutoffFrequency));
        return this;
    }

    /**
     * {@inheritDoc}
     * @param channels the number of input and output channels for this effect.
     * @return a reference to this object.
     */
    @Override
    public ProcessingUnitBuilder reverb(int channels) {
        this.effects.add(new DigitalReverb(channels));
        return this;
    }

    /**
     * {@inheritDoc}
     * @param channels the number of input and output channels for this effect.
     * @return a reference to this object.
     */
    @Override
    public ProcessingUnitBuilder gate(int channels) {
        this.effects.add(new Gate(channels));
        return this;
    }

    /**
     * {@inheritDoc}
     * @param channels the number of input and output channels for this effect.
     * @return a reference to this object.
     */
    @Override
    public ProcessingUnitBuilder compressor(int channels) {
        this.effects.add(new Compression(channels));
        return this;
    }

    /**
     * {@inheritDoc}
     * @return a {@link ProcessingUnit} with the ordered sequence of effects.
     * @throws IllegalStateException if no effects have been specified through this builder.
     */
    @Override
    public ProcessingUnit build() throws IllegalStateException {
        if (!this.effects.isEmpty()) {
            final var pu = new BasicProcessingUnit(effects);
            this.sidechain.ifPresent(pu::addSidechaining);
            return pu;
        }
        throw new IllegalStateException("Cannot create an empty ProcessingUnit.");
    }

}
