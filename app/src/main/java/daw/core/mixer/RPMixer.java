package daw.core.mixer;

import daw.core.channel.RPChannel;
import net.beadsproject.beads.ugens.Gain;

import java.util.List;
import java.util.Optional;

/**
 * This interface models a mixer,
 * which is a controller for {@link RPChannel}.
 */

public interface RPMixer {

    /**
     * A method to create a Basic {@link RPChannel} in the mixer
     * @return the {@link RPChannel} that is created
     */
    RPChannel createBasicChannel();

    /**
     * A method to create a Gated {@link RPChannel} in the mixer
     * @return the {@link RPChannel} that is created
     */
    RPChannel createGatedChannel();

    /**
     * A method to create a Return {@link RPChannel} in the mixer
     * @return the {@link RPChannel} that is created
     */
    RPChannel createReturnChannel();

    /**
     * This method is used to create a sidechained {@link RPChannel}
     * @param channel the {@link RPChannel} to sidechain
     * @return the sidechained {@link RPChannel}
     */
    RPChannel createSidechained(RPChannel channel);

    /**
     * A method that returns the Master channel of the mixer.
     * @return the Master channel contained in the mixer
     */
    RPChannel getMasterChannel();

    /**
     * A method to link the output of a {@link RPChannel}
     * to the Input of a Return channel.
     * @param channel the {@link RPChannel} which output is to be linked
     * @param returnChannel the Return channel which receives the input
     */
    void linkChannel(RPChannel channel, RPChannel returnChannel);

    /**
     * A method to add a {@link RPChannel} to a group
     * @param channel the {@link RPChannel} to be added
     * @param group the Group
     */
    void linkToGroup(RPChannel channel, RPChannel group);

    /**
     * A method to remove a {@link RPChannel} from a group
     * @param channel the {@link RPChannel} to be removed
     * @param group the group to remove the {@link RPChannel} from
     */
    void unlinkFromGroup(RPChannel channel, RPChannel group);

    /**
     * A method to link a sidechained {@link RPChannel}
     * @param channel the {@link RPChannel} to sidechain
     * @param sidechainedChannel the sidechained {@link RPChannel}
     */
    void linkToSidechained(RPChannel channel, RPChannel sidechainedChannel);

}
