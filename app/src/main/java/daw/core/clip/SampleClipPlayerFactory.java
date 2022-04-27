package daw.core.clip;

import java.io.IOException;

import daw.core.channel.RPChannel;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.audiofile.FileFormatException;
import net.beadsproject.beads.data.audiofile.OperationUnsupportedException;
import net.beadsproject.beads.ugens.SamplePlayer;
import net.beadsproject.beads.ugens.SamplePlayer.LoopType;

public class SampleClipPlayerFactory implements ClipPlayerFactory {

	@Override
	public RPClipPlayer createClipPlayer(RPClip clip, RPChannel channel) throws IOException, OperationUnsupportedException, FileFormatException {
		if(!clip.getClass().equals(SampleClip.class)) {
			throw new IllegalArgumentException("The supplied clip must be a Sample Clip");
		}
		SampleClipPlayer player = new SampleClipPlayer((SampleClip) clip);
		channel.connectSource(player.getUGen());
		return player;
	}

	@Override
	public RPClipPlayer createClipPlayerWithActiveCut(RPClip clip, RPChannel channel, double cut) throws IOException, OperationUnsupportedException, FileFormatException {
		var player = this.createClipPlayer(clip, channel);
		player.setCut(cut);
		return player;
	}
	
	public class SampleClipPlayer implements RPClipPlayer {
		
		private final SamplePlayer player;
		private final double contentPosition;
		private double cutTime;
		private boolean isCutActive;
		
		private SampleClipPlayer(SampleClip sampleClip) throws IOException, OperationUnsupportedException, FileFormatException {
			this.player = new SamplePlayer(new Sample(sampleClip.getContent().getAbsolutePath()));
			this.contentPosition = sampleClip.getContentPosition();
			this.player.setPosition(this.contentPosition);
			this.player.setLoopType(LoopType.NO_LOOP_FORWARDS);
			this.isCutActive = false;
			this.stop();
		}

		@Override
		public void play() {
			this.player.start();
		}

		@Override
		public void pause() {
			this.player.pause(true);
		}

		@Override
		public void stop() {
			this.pause();
			if(isCutActive) {
				this.setPlaybackPosition(cutTime);
			} else {
				this.setPlaybackPosition(contentPosition);
			}	
		}

		@Override
		public void setPlaybackPosition(double milliseconds) {
			this.player.setPosition(milliseconds);
		}

		@Override
		public double getPlaybackPosition() {
			return this.player.getPosition();
		}

		@Override
		public void setCut(double time) {
			this.cutTime = time;
			this.isCutActive = true;
		}

		@Override
		public void disableCut() {
			this.isCutActive = false;
		}

		@Override
		public UGen getUGen() {
			return this.player;
		}

		@Override
		public boolean isPaused() {
			return this.player.isPaused();
		}

		@Override
		public boolean isCutActive() {
			return this.isCutActive;
		}

		@Override
		public double getCutTime() {
			if(!this.isCutActive) {
				throw new IllegalStateException("The cut is not active for this player");
			}
			return this.cutTime;
		}

	}


}
