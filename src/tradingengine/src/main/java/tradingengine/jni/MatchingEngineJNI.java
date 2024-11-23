package tradingengine.jni;

// import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class MatchingEngineJNI {
    public final String libraryPath;

    public MatchingEngineJNI(@Value("${native.library.path}") String libraryPath) {
        this.libraryPath = libraryPath;
        // loadlibrary();
    }

    public native long createMatchingEngine(String symbol);  // Returns pointer to native instance

    public native String insertOrder(long handle, String orderId, String side, long price, long quantity);

    public native String getMatchingEngineSummary(long handle);

    public long nativePointer;

    public native void deleteMatchingEngine(long pointer);

    @PreDestroy
    @Override
    public void close() {
        if (nativePointer != 0) {
            deleteMatchingEngine(nativePointer);
            nativePointer = 0;
        }
    }

}
