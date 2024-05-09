package tauon.app.ui.components.tablerenderers;

import org.jetbrains.annotations.NotNull;

public class PercentageValue implements Comparable<PercentageValue>{
    
    private final double value;
    
    public PercentageValue(double value){
        this.value = value;
    }
    
    public double getValue() {
        return value;
    }
    
    @Override
    public int compareTo(@NotNull PercentageValue percentageValue) {
        return Double.compare(value, percentageValue.value);
    }
}
