package tauon.app.ui.containers.session.pages.terminal.snippets;

import java.util.UUID;

/**
 * @author subhro
 */
public class SnippetItem {

    private String name;
    private String command;
    private String id;

    public SnippetItem(String name, String command) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.command = command;
    }
    
    public SnippetItem() {
    
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
