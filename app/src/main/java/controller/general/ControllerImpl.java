package controller.general;

import controller.storing.ReadFromFileImpl;
import controller.storing.WriteToFile;
import controller.storing.WriteToFileImpl;
import daw.core.clip.ClipNotFoundException;
import daw.engine.Engine;
import daw.engine.RPEngine;
import daw.manager.ChannelLinker;
import daw.manager.ImportException;
import daw.manager.Manager;
import daw.utilities.AudioContextManager;
import net.beadsproject.beads.data.audiofile.FileFormatException;
import planning.Element;
import planning.RPPart;
import planning.RPRole;
import view.common.AlertDispatcher;
import view.common.App;
import view.common.ViewDataImpl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class ControllerImpl implements Controller {

    private final ProjectDownloader downloader;
    private final ProjectLoader loader;
    private Manager manager;
    private App app;
    private final RPEngine engine;
    private File currentProject;
    private final File appSettings = new File(WORKING_DIRECTORY + SEP + APP_SETTINGS);

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
            }
        } catch (IOException | FileFormatException e) {
            this.manager = new Manager();
        }
    }

    @Override
    public void updateView() {

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
        } else {
            roleType = RPRole.RoleType.SOUNDTRACK;
        }
        Optional<String> desc = description.equals("") ? Optional.empty() : Optional.of(description);
        this.manager.addChannel(roleType, title, desc);
        App.getData().addChannel(new ViewDataImpl.Channel(title));
    }

    @Override
    public void newClip(String type, String title, String description, String channel, Double time,
                        Double duration, File content) throws IllegalArgumentException, ImportException {
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
        App.getData().addClip(App.getData().getChannel(channel),new ViewDataImpl.Clip(title, time, duration, time));
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
        return this.manager.getRoleList().stream().map(Element::getTitle).collect(Collectors.toList());
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
    }

    @Override
    public Double getPlaybackTime() {
        return this.engine.getPlaybackTime();
    }

    @Override
    public void updatePlaybackTime(Double time) {
        app.updatePlaybackTime(time);
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
    public void moveClip(String clip, String channel, Double finalTimeIn) throws ClipNotFoundException, ImportException {
        App.getData().removeClip(App.getData().getChannel(channel),App.getData().getClip(channel,clip));
        this.manager.moveClip(clip,channel,finalTimeIn);
        Double time = this.manager.getClipTime(clip,channel);
        Double duration = this.manager.getClipDuration(clip);
        App.getData().addClip(App.getData().getChannel(channel), new ViewDataImpl.Clip(clip, time, duration, time));
    }

    @Override
    public void setClipTimeIn(String clip, String channel, Double finalTimeIn) throws ClipNotFoundException, ImportException {
        App.getData().removeClip(App.getData().getChannel(channel),App.getData().getClip(channel,clip));
        this.manager.setClipTimeIn(clip,channel,finalTimeIn);
        Double time = this.manager.getClipTime(clip,channel);
        Double duration = this.manager.getClipDuration(clip);
        App.getData().addClip(App.getData().getChannel(channel), new ViewDataImpl.Clip(clip, time, duration, time));    }

    @Override
    public void setClipTimeOut(String clip, String channel, Double finalTimeOut) throws ClipNotFoundException, ImportException {
        App.getData().removeClip(App.getData().getChannel(channel),App.getData().getClip(channel,clip));
        this.manager.setClipTimeOut(clip,channel,finalTimeOut);
        Double time = this.manager.getClipTime(clip,channel);
        Double duration = this.manager.getClipDuration(clip);
        App.getData().addClip(App.getData().getChannel(channel), new ViewDataImpl.Clip(clip, time, duration, time));    }

    @Override
    public void splitClip(String clip, String channel, Double splittingTime) throws ClipNotFoundException, ImportException {
        App.getData().removeClip(App.getData().getChannel(channel),App.getData().getClip(channel,clip));
        this.manager.splitClip(clip,channel,splittingTime);
        Double time = this.manager.getClipTime(clip,channel);
        Double duration = this.manager.getClipDuration(clip);
        App.getData().addClip(App.getData().getChannel(channel), new ViewDataImpl.Clip(clip, time, duration, time));
    }

    // ONLY FOR TEMPORARY TESTING PURPOSES
    public Manager getManager() {
        return this.manager;
    }

}
