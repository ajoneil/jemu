package io.github.arkosammy12.jemu.core.common;

// TODO: Expose a reset() method to allow outsiders to reset the system without throwing away the whole instance.
// TODO: Allow cores to not need to be passed a ROM file to initialize
public interface Emulator extends AutoCloseable {

    SystemHost getHost();

    VideoGenerator<?> getVideoGenerator();

    AudioGenerator<?> getAudioGenerator();

    SystemController<?> getSystemController();

    void executeFrame();

    void executeCycle();

    int getFramerate();

}
