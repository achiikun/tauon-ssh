package tauon.app.ui.containers.session.pages.tools.diskspace;

import tauon.app.ui.components.tablerenderers.ByteCountValue;
import tauon.app.ui.components.tablerenderers.PercentageValue;

public class PartitionEntry {
    private String fileSystem, mountPoint;
    private ByteCountValue totalSize, used, available;
    private PercentageValue usedPercent;

    public PartitionEntry(String fileSystem, String mountPoint, long totalSize,
                          long used, long available, double usedPercent) {
        this.fileSystem = fileSystem;
        this.mountPoint = mountPoint;
        this.totalSize = new ByteCountValue(totalSize);
        this.used = new ByteCountValue(used);
        this.available = new ByteCountValue(available);
        this.usedPercent = new PercentageValue(usedPercent);
    }

    public String getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(String fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }

    public ByteCountValue getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(ByteCountValue totalSize) {
        this.totalSize = totalSize;
    }

    public ByteCountValue getUsed() {
        return used;
    }

    public void setUsed(ByteCountValue used) {
        this.used = used;
    }

    public ByteCountValue getAvailable() {
        return available;
    }

    public void setAvailable(ByteCountValue available) {
        this.available = available;
    }

    public PercentageValue getUsedPercent() {
        return usedPercent;
    }

    public void setUsedPercent(PercentageValue usedPercent) {
        this.usedPercent = usedPercent;
    }
}
