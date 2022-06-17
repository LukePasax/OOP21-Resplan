package view.edit;

import Resplan.Starter;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.util.StringConverter;
import view.common.App;
import view.common.ChannelsView;
import view.common.NumberFormatConverter;
import view.common.TimeAxisSetter;
import view.common.ToolBarSetter;
import view.common.ViewDataImpl.Channel;
import view.common.ViewDataImpl.Clip;

public class EditChannelsView extends ChannelsView {
	
	private final static Paint muteColor = Paint.valueOf("#FF3333");
	private final static Paint soloColor = Paint.valueOf("#B8D4FF");
	
	public EditChannelsView(TimeAxisSetter timeAxisSetter, VBox channelsContentPane, VBox channelsInfoPane, ToolBarSetter toolBarSetter) {
		super(timeAxisSetter, channelsContentPane, channelsInfoPane, toolBarSetter);
	}
	
	@Override
	public Node drawClip(Clip clip) {
		Label title = new Label(clip.getTitle());
		Label fileName = clip.isEmpty() ? new Label("Empty clip") : new Label("content: " + clip.getContentName());
		Label contentPos = clip.isEmpty() ? new Label("") : new Label("cut: " + new NumberFormatConverter().toString(clip.getContentPosition()));
		contentPos.setFont(Font.font(10));
		return new VBox(title, fileName, contentPos);
	}

	@Override
	public Node drawInfos(Channel ch) {
		//buttons
		Button muteButton = new Button("M");
		Button soloButton = new Button("S");
		muteButton.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
		soloButton.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
		muteButton.setOnAction(e->Starter.getController().setMute(ch.getTitle()));
		soloButton.setOnAction(e->Starter.getController().setSolo(ch.getTitle()));
		App.getData().getChannel(ch.getTitle()).isMuted().addListener((obs,old,n)->{
			if (n) {
				muteButton.setBackground(new Background(new BackgroundFill(muteColor, null, null)));
			} else {
				muteButton.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
			}
		});
		App.getData().getChannel(ch.getTitle()).isSolo().addListener((obs, old, n)->{
			if (n) {
				soloButton.setBackground(new Background(new BackgroundFill(soloColor, null, null)));
			} else {
				soloButton.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
			}
		});
		//label
		Label groupLabel = new Label(ch.getGroup());
		groupLabel.setTextFill(Color.GRAY);
		groupLabel.setFont(Font.font(12));
		//pan
		Slider pan = new Slider(-1.0, 1.0, 0.0);
		pan.setOnMouseClicked(e->{
			if(e.getClickCount() == 2) {
				pan.setValue(0.0);
				Starter.getController().setPan(ch.getTitle(), Double.valueOf(pan.getValue()).floatValue());
			}
		});
		pan.setMaxWidth(50);
		pan.setMajorTickUnit(50);
		pan.setShowTickMarks(true);
		pan.setShowTickLabels(true);
		pan.setLabelFormatter(new StringConverter<Double>() {

			@Override
			public String toString(Double object) {
				return object < 0 ? "L" : object > 0 ? "R" : "C";
			}

			@Override
			public Double fromString(String string) {
				return null;
			}
			
		});
		pan.setOnMouseReleased(e->Starter.getController().setPan(ch.getTitle(), Double.valueOf(pan.getValue()).floatValue()));
		//volume
		Slider volume = new Slider(0, 100, 100);
		volume.setOnMouseClicked(e->{
			if(e.getClickCount() == 2) {
				volume.setValue(100);
				Starter.getController().setVolume(ch.getTitle(), Double.valueOf(volume.getValue()).intValue());
			}
		});
		volume.setMaxWidth(120);
		volume.setMajorTickUnit(100);
		volume.setShowTickMarks(true);
		volume.setShowTickLabels(true);
		volume.setLabelFormatter(new StringConverter<Double>() {

			@Override
			public String toString(Double object) {
				return object == 0 ? "-∞" : object == 100 ? "0.0" : ""; 
			}

			@Override
			public Double fromString(String string) {
				return null;
			}
			
		});
		volume.setOnMouseReleased(e->Starter.getController().setVolume(ch.getTitle(), Double.valueOf(volume.getValue()).intValue()));
		var pane = new VBox(groupLabel, new FlowPane(muteButton, soloButton, pan), volume);
		pane.setMinHeight(100);
		return pane;
	}
	
}
