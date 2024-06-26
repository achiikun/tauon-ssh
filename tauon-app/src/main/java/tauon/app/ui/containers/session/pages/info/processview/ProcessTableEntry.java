package tauon.app.ui.containers.session.pages.info.processview;

import tauon.app.ui.components.tablerenderers.ByteCountValue;

public class ProcessTableEntry {
    private String name;
    private String user;
    private String time;
    private String tty;
    private String args;
    private float cpu;
    private ByteCountValue memory;
    private int pid;
    private int ppid;
    private int nice;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTty() {
        return tty;
    }

    public void setTty(String tty) {
        this.tty = tty;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public float getCpu() {
        return cpu;
    }

    public void setCpu(float cpu) {
        this.cpu = cpu;
    }

    public ByteCountValue getMemory() {
        return memory;
    }

    public void setMemory(ByteCountValue memory) {
        this.memory = memory;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getPpid() {
        return ppid;
    }

    public void setPpid(int ppid) {
        this.ppid = ppid;
    }

    public int getNice() {
        return nice;
    }

    public void setNice(int nice) {
        this.nice = nice;
    }
}
