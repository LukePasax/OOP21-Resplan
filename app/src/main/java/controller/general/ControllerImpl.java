package controller.general;

import daw.manager.Manager;

public class ControllerImpl implements Controller {

    private static final String SEP = System.getProperty("file.separator");
    private static final String WORKING_DIRECTORY = System.getProperty("user.dir");

    private final SaveProject project;
    private final Manager manager;

    public ControllerImpl(Manager manager) {
        this.manager = manager;
        this.project = new SaveProjectImpl(this.manager);
    }

    @Override
    public void updateView() {

    }

    @Override
    public void saveProject() {
        this.project.save();
    }

}
