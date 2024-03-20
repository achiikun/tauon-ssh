package tauon.app.ui.containers.session.pages.files;

import java.util.Deque;
import java.util.LinkedList;

public class NavigationHistory {
    private final Deque<String> back = new LinkedList<>();
    private final Deque<String> forward = new LinkedList<>();

    public boolean hasPrevElement() {
        return !back.isEmpty();
    }

    public boolean hasNextElement() {
        return !forward.isEmpty();
    }

    public String prevElement() {
        return back.pop();
    }

    public String nextElement() {
        return forward.pop();
    }

    public void addBack(String item) {
        back.push(item);
    }

    public void addForward(String item) {
        forward.push(item);
    }
}
