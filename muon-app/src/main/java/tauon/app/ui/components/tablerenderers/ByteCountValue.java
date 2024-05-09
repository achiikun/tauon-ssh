package tauon.app.ui.components.tablerenderers;

import org.jetbrains.annotations.NotNull;

public class ByteCountValue implements Comparable<ByteCountValue>{
    
    private final long value;
    
    public ByteCountValue(long value){
        this.value = value;
    }
    
    public long getValue() {
        return value;
    }
    
    @Override
    public int compareTo(@NotNull ByteCountValue percentageValue) {
        return Long.compare(value,percentageValue.value);
    }
}
