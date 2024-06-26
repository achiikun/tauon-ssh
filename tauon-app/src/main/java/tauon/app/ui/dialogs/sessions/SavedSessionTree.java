package tauon.app.ui.dialogs.sessions;

import tauon.app.settings.SessionFolder;

public class SavedSessionTree {
    private SessionFolder folder;
    private String lastSelection;

    public synchronized SessionFolder getFolder() {
        return folder;
    }

    public synchronized void setFolder(SessionFolder folder) {
        this.folder = folder;
    }

    public synchronized String getLastSelection() {
        return lastSelection;
    }

    public synchronized void setLastSelection(String lastSelection) {
        this.lastSelection = lastSelection;
    }
}
