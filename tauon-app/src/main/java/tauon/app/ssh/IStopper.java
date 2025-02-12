package tauon.app.ssh;

import java.util.concurrent.atomic.AtomicBoolean;

public interface IStopper {
    
    static Handle of(AtomicBoolean stopFlag) {
        if(stopFlag == null)
            return null;
        return new Handle() {
            @Override
            public void stop() {
                stopFlag.set(true);
            }
            
            @Override
            public boolean isStopped() {
                return stopFlag.get();
            }
        };
    }
    
    interface Handle extends IStopper{
        public void stop();
    }
    
    class Default implements IStopper, Handle {
        
        private boolean stopped = false;
        
        @Override
        public void stop() {
            stopped = true;
        }
        
        @Override
        public boolean isStopped() {
            return stopped;
        }
    }
    
    boolean isStopped();
}
