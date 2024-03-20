package tauon.app.ui.components.editortablemodel;

public class EditorEntry {
    private String name;
    private String path;
    
    /**
     * DO NOT DELETE
     * Jackson uses to build this object
     */
    @SuppressWarnings("unused")
    public EditorEntry() {
        super();
    }

    public EditorEntry(String name, String path) {
        super();
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
