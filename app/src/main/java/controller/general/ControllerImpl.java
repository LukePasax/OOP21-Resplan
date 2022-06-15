package controller.general;

import controller.storing.ReadFromFileImpl;
import controller.storing.WriteToFile;
import controller.storing.WriteToFileImpl;
import daw.core.channel.RPChannel;
import daw.core.clip.ClipNotFoundException;
import daw.core.clip.RPClip;
import daw.core.clip.RPRecorder;
import daw.core.clip.Recorder;
import daw.engine.Engine;
import daw.engine.RPEngine;
import daw.manager.ChannelLinker;
import daw.manager.ImportException;
import daw.manager.Manager;
import daw.utilities.AudioContextManager;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.audiofile.AudioFileType;
import net.beadsproject.beads.data.audiofile.FileFormatException;
import net.beadsproject.beads.ugens.RecordToSample;
import planning.Element;
import planning.RPPart;
import planning.RPRole;
import view.common.AlertDispatcher;
import view.common.App;
import view.common.ViewDataImpl;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ControllerImpl implements Controller {

    private final ProjectDownloader downloader;
    private final ProjectLoader loader;
    private Manager manager;
    private App app;
    private final RPEngine engine;
    private File currentProject;
    private final File appSettings = new File(WORKING_DIRECTORY + SEP + APP_SETTINGS);
    private RPRecorder recorder;
    private RecordToSample exporter;
    private final Set<RPChannel> mutedChannels = new HashSet<>();
    private final Set<RPChannel> soloChannels = new HashSet<>();
    private boolean solo = false;

    /**
     * Sets up the application and initializes a new project (see newProject).
     */
    public ControllerImpl() {
        this.loader = new ProjectLoaderImpl();
        this.downloader = new ProjectDownloaderImpl();
        this.newProject();
        this.manager.getMixer().connectToSystem();
        this.engine = new Engine((ChannelLinker) this.manager.getChannelLinker());
    }

    @Override
    public void setApp(App app) {
        this.app = app;
    }

    /**
     * {@inheritDoc}
     */
    public void newProject() {
        try {
            final var fileName = new ReadFromFileImpl(this.appSettings).read();
            if (fileName.isBlank()) {
                this.currentProject = null;
                this.manager = new Manager();
            } else {
                this.currentProject = new File(fileName);
                this.manager = this.loader.load(this.currentProject);
                this.loadViewData();
            }
        } catch (IOException | FileFormatException e) {
            this.manager = new Manager();
        }
    }

    @Override
    public void loadViewData() {
        this.manager.getRoles().forEach(c -> {
            App.getData().addChannel(new ViewDataImpl.Channel(c.getTitle(), c.getType().name()));
            this.manager.getPartList(c.getTitle()).forEach(p -> {
                final var clip = this.manager.getClip(p.getTitle());
                if (clip.isEmpty()) {
                    App.getData().addClip(App.getData().getChannel(c.getTitle()), new ViewDataImpl.Clip(
                            p.getTitle(), this.manager.getClipTime(p.getTitle(), c.getTitle()), clip.getDuration(),
                            Optional.empty(), Optional.empty()));
                } else {
                    App.getData().addClip(App.getData().getChannel(c.getTitle()), new ViewDataImpl.Clip(
                            p.getTitle(), this.manager.getClipTime(p.getTitle(), c.getTitle()), clip.getDuration(),
                            Optional.of(clip.getContentPosition()), Optional.of(clip.getContentDuration())));
                }
            });
        });
        this.manager.getSections().forEach(s -> {
            App.getData().addSection(new ViewDataImpl.Section(s.getValue().getTitle(), s.getKey()));
        });
    }

    /**
     * {@inheritDoc}
     * @throws DownloadingException if an error has occurred when trying to write to file.
     * @throws IllegalStateException if the current project has never been saved before.
     */
    @Override
    public void save() throws DownloadingException, IllegalStateException {
        if (this.currentProject == null) {
            throw new IllegalStateException("Select a file name and a directory.");
        } else {
            this.saveCurrentProject();
        }
    }

    private void saveCurrentProject() throws DownloadingException {
        try {
            this.downloader.download(this.currentProject, this.manager);
        } catch (IOException | FileFormatException e) {
            throw new DownloadingException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * @param file the file where to save.
     * @throws DownloadingException if an error has occurred when trying to write to file.
     */
    @Override
    public void saveWithName(File file) throws DownloadingException {
        this.currentProject = file;
        this.saveCurrentProject();
    }

    /**
     * {@inheritDoc}
     * @param file the file where to read.
     * @throws LoadingException if an error has occurred when trying to read from file.
     */
    @Override
    public void openProject(File file) throws LoadingException {
        try {
            this.manager = this.loader.load(file);
            this.currentProject = file;
            this.loadViewData();
        } catch (IOException | FileFormatException e) {
            throw new LoadingException(e.getMessage());
        }
    }

    @Override
    public void newChannel(String type, String title, String description) throws IllegalArgumentException {
        RPRole.RoleType roleType;
        if (type.equals("Speaker")) {
            roleType = RPRole.RoleType.SPEECH;
        } else if (type.equals("Effect")) {
            roleType = RPRole.RoleType.EFFECTS;
            type = "Effects";
        } else {
            roleType = RPRole.RoleType.SOUNDTRACK;
        }
        Optional<String> desc = description.equals("") ? Optional.empty() : Optional.of(description);
        this.manager.addChannel(roleType, title, desc);
        App.getData().addChannel(new ViewDataImpl.Channel(title, type));
    }

    @Override
    public void newClip(String type, String title, String description, String channel, Double time,
                        Double duration, File content) throws IllegalArgumentException, ImportException, ClipNotFoundException {
        RPPart.PartType partType;
        if (type.equals("Speaker")) {
            partType = RPPart.PartType.SPEECH;
        } else if (type.equals("Effects")) {
            partType = RPPart.PartType.EFFECTS;
        } else {
            partType = RPPart.PartType.SOUNDTRACK;
        }
        Optional<String> desc = description.equals("") ? Optional.empty() : Optional.of(description);
        Optional<File> file = content == null ? Optional.empty() : Optional.of(content);
        this.manager.addClip(partType, title, desc, channel, time, duration, file);
        RPClip<?> clip = this.manager.getClip(title);
        if (clip.isEmpty()) {
            App.getData().addClip(App.getData().getChannel(channel), new ViewDataImpl.Clip(title, time, duration,
                    Optional.empty(), Optional.empty()));
        } else {
            App.getData().addClip(App.getData().getChannel(channel), new ViewDataImpl.Clip(title, time, duration,
                    Optional.of(clip.getContentPosition()), Optional.of(clip.getContentDuration())));
        }
    }

    @Override
    public void deleteChannel(String title) {
        try {
            this.manager.removeChannel(title);
            App.getData().removeChannel(App.getData().getChannel(title));
        } catch (NoSuchElementException e) {
            AlertDispatcher.dispatchError(e.getLocalizedMessage());
        }
    }

    @Override
    public void deleteClip(String title, String channel, Double time) {
        try {
            this.manager.removeClip(channel, title, time);
            App.getData().removeClip(App.getData().getChannel(channel), App.getData().getClip(channel,title));
        } catch (NoSuchElementException | ClipNotFoundException e) {
            AlertDispatcher.dispatchError(e.getLocalizedMessage());
        }
    }

    @Override
    public List<String> getChannelList() {
        return this.manager.getRoles().stream().map(Element::getTitle).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     * @throws DownloadingException if the writing to file is unsuccessful.
     * @throws IllegalStateException if the current project has never been saved before.
     */
    @Override
    public void setTemplateProject() throws DownloadingException, IllegalStateException {
        final WriteToFile writer = new WriteToFileImpl(this.appSettings);
        if (this.currentProject == null) {
            throw new IllegalStateException("Save project before setting it as template.");
        }
        try {
            writer.write(this.currentProject.getAbsolutePath());
        } catch (IOException e) {
            throw new DownloadingException("Unable to perform this operation. " +
                    "Retry to set this project as the template.");
        }
    }

    @Override
    public List<String> getClipList(String channel) {
        return this.manager.getPartList(channel).stream().map(Element::getTitle).collect(Collectors.toList());
    }

    @Override
    public Double getClipTime(String clip, String channel) {
        return this.manager.getClipTime(clip, channel);
    }

    @Override
    public Double getClipDuration(String clip) {
        return this.manager.getClipDuration(clip);
    }

    @Override
    public void start() {
    	AudioContextManager.getAudioContext().start();
        this.engine.start();
    }

    @Override
    public void pause() {
        this.engine.pause();
        AudioContextManager.getAudioContext().stop();
    }

    @Override
    public void stop() {
        this.engine.stop();
        AudioContextManager.getAudioContext().stop();
    }
    
    @Override
    public void setPlaybackTime(Double time) {
        this.engine.setPlaybackTime(time);
        this.updatePlaybackTime(time);
    }

    @Override
    public Double getPlaybackTime() {
        return this.engine.getPlaybackTime();
    }

    @Override
    public void updatePlaybackTime(Double time) {
        this.app.updatePlaybackTime(time);
    }

    @Override
    public boolean isPaused() {
        return this.engine.isPaused();
    }

    @Override
    public double getProjectLength() {
        return this.manager.getProjectLength();
    }

    @Override
    public void moveClip(String clip, String channel, Double finalTimeIn) throws ClipNotFoundException {
        //App.getData().removeClip(App.getData().getChannel(channel),App.getData().getClip(channel,clip));
        this.manager.moveClip(clip,channel,finalTimeIn);
        //createClipView(clip, channel);
        updateChannelClipsView(channel);
    }

    private void updateChannelClipsView(String channel) {
    	this.manager.getChannelLinker().getTapeChannel(this.manager.getChannelLinker().getRole(channel)).getClipWithTimeIterator().forEachRemaining(cwt->{
    		App.getData().removeClip(App.getData().getChannel(channel),App.getData().getClip(channel,cwt.getValue().getTitle()));
    		createClipView(cwt.getValue().getTitle(), channel);
    	});
    }
    
    private void createClipView(String clip, String channel) {
        Double time = this.manager.getClipTime(clip,channel);
        Double duration = this.manager.getClipDuration(clip);
        RPClip<?> rpClip = this.manager.getClip(clip);
        if (rpClip.isEmpty()) {
            App.getData().addClip(App.getData().getChannel(channel), new ViewDataImpl.Clip(clip, time, duration,
                    Optional.empty(), Optional.empty()));
        } else {
            App.getData().addClip(App.getData().getChannel(channel), new ViewDataImpl.Clip(clip, time, duration,
                    Optional.of(rpClip.getContentPosition()), Optional.of(rpClip.getContentDuration())));
        }
    }

    @Override
    public void setClipTimeIn(String clip, String channel, Double finalTimeIn) throws ClipNotFoundException {
    	//App.getData().removeClip(App.getData().getChannel(channel),App.getData().getClip(channel,clip));
    	this.manager.setClipTimeIn(clip,channel,finalTimeIn);
    	updateChannelClipsView(channel);
        //createClipView(clip, channel);
    }

    @Override
    public void setClipTimeOut(String clip, String channel, Double finalTimeOut) throws ClipNotFoundException {
        //App.getData().removeClip(App.getData().getChannel(channel),App.getData().getClip(channel,clip));
        this.manager.setClipTimeOut(clip,channel,finalTimeOut);
        //createClipView(clip, channel);
        updateChannelClipsView(channel);
    }

    @Override
    public void splitClip(String clip, String channel, Double splittingTime) throws ClipNotFoundException {
        //App.getData().removeClip(App.getData().getChannel(channel),App.getData().getClip(channel,clip));
        this.manager.splitClip(clip,channel,splittingTime);
        //createClipView(clip, channel);
        updateChannelClipsView(channel);
    }

    @Override
    public void addContentToClip(String clip, File content) throws ImportException, ClipNotFoundException {
        this.manager.addFileToClip(clip,content);
    }

    @Override
    public void removeContentFromClip(String clip) throws ClipNotFoundException {
        this.manager.removeFileFromClip(clip);
    }

    @Override
    public void newSection(String title, String description, Double initialTime, Double duration) {
        Optional<String> desc = description.equals("") ? Optional.empty() : Optional.of(description);
        this.manager.addSection(title, desc, initialTime, duration);
        App.getData().addSection(new ViewDataImpl.Section(title, initialTime));
    }

    @Override
    public void deleteSection(Double time) {
        this.manager.removeSection(time);
        App.getData().removeSection(App.getData().getUnmodifiableSections().stream()
                .filter(s -> s.getPosition().equals(time))
                .findFirst().orElseThrow(() -> new NoSuchElementException("No section present at that time")));
    }

    @Override
    public void startRecording() {
        this.recorder = new Recorder();
        this.recorder.record();
    }

    @Override
    public void stopRecording(String clip) throws ImportException, ClipNotFoundException {
        this.recorder.pause();
        this.addContentToClip(clip, new File(this.recorder.getSample().getFileName()));
    }

    @Override
    public void startExport(Double startTime) {
        AudioContext ac = AudioContextManager.getAudioContext();
        Sample sample = new Sample(0);
        this.exporter = new RecordToSample(ac, sample, RecordToSample.Mode.INFINITE);
        this.exporter.addInput(this.manager.getMixer().getMasterChannel().getOutput());
        ac.out.addDependent(this.exporter);
        ac.out.removeAllConnections(this.manager.getMixer().getMasterChannel().getOutput());
        this.setPlaybackTime(startTime);
        this.start();
        this.exporter.start();
    }

    @Override
    public void stopExport(File file) throws IOException {
        AudioContext ac = AudioContextManager.getAudioContext();
        this.exporter.pause(true);
        this.stop();
        ac.out.addInput(this.manager.getMixer().getMasterChannel().getOutput());
        ac.out.removeDependent(this.exporter);
        this.exporter.clip();
        this.exporter.getSample().write(file.getAbsolutePath(), AudioFileType.WAV);
    }

    @Override
    public void setMute(String channel) {
        final RPChannel ch = this.manager.getChannelLinker().getChannel(this.manager.getChannelLinker().getRole(channel));
        final var viewChannel = App.getData().getChannel(channel);
        final var isMuted = viewChannel.isMuted().get();
        if (isMuted) {
            this.mutedChannels.add(ch);
        } else {
            this.mutedChannels.remove(ch);
        }
        viewChannel.setMute(!isMuted);
        if (this.solo) {
            this.manageMuteInSoloEnvironment();
        } else {
            this.manageMuteInNonSoloEnvironment();
        }
    }

    private void manageMuteInNonSoloEnvironment() {
        this.mutedChannels.forEach(RPChannel::disable);
        this.manager.getChannelLinker().getAudioSet().stream()
                .map(i -> (RPChannel) i.getKey())
                .filter(i -> !this.mutedChannels.contains(i))
                .forEach(RPChannel::enable);
    }

    private void manageMuteInSoloEnvironment() {
        this.soloChannels.forEach(RPChannel::enable);
        this.manager.getChannelLinker().getAudioSet().stream()
                .map(i -> (RPChannel) i.getKey())
                .filter(i -> !this.soloChannels.contains(i))
                .forEach(RPChannel::disable);
    }

    @Override
    public void setSolo(String channel) {
        final RPChannel ch = this.manager.getChannelLinker().getChannel(this.manager.getChannelLinker().getRole(channel));
        if (this.mutedChannels.contains(ch)) {
            throw new IllegalStateException("Cannot solo a muted channel. Unmute it first, then call this method.");
        }
        this.solo = true;
        this.soloChannels.add(ch);
        App.getData().getChannel(channel).setSolo(true);
        this.manageMuteInSoloEnvironment();
    }

    @Override
    public void removeSolo(String channel) {
        final RPChannel ch = this.manager.getChannelLinker().getChannel(this.manager.getChannelLinker().getRole(channel));
        this.soloChannels.remove(ch);
        App.getData().getChannel(channel).setSolo(false);
        if (this.soloChannels.isEmpty()) {
            this.solo = false;
            this.manageMuteInNonSoloEnvironment();
        } else {
            this.manageMuteInSoloEnvironment();
        }
    }

    // ONLY FOR TEMPORARY TESTING PURPOSES
    public Manager getManager() {
        return this.manager;
    }

}
