package io.github.arkosammy12.jemu.app;

import io.github.arkosammy12.jemu.app.adapters.AbstractSystemAdapter;
import io.github.arkosammy12.jemu.app.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.app.drivers.DefaultAudioRendererDriver;
import io.github.arkosammy12.jemu.app.io.CLIArgs;
import io.github.arkosammy12.jemu.app.io.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.app.util.MavenProperties;
import io.github.arkosammy12.jemu.frontend.audio.AudioChannels;
import io.github.arkosammy12.jemu.frontend.audio.AudioEngine;
import io.github.arkosammy12.jemu.frontend.gui.swing.commands.*;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.MuteEvent;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.VolumeChangedEvent;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.HelpManager;
import net.harawata.appdirs.AppDirsFactory;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.sound.sampled.LineUnavailableException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public final class Jemu {

    private static final Path APP_DIR = Path.of(AppDirsFactory.getInstance().getUserDataDir(MavenProperties.ARTIFACT_ID, null, null));

    private volatile AbstractSystemAdapter currentSystem = null;
    private volatile State currentState = State.STOPPED;

    private final MainWindow mainWindow;
    private final AudioEngine audioEngine;

    private final Thread emulatorCommandListenerThread;
    private final Thread uiEventListenerThread;

    private volatile boolean running;
    private final Object systemLock = new Object();
    private volatile boolean shutdownStarted = false;

    public Jemu(@Nullable CLIArgs cliArgs) throws Exception {
        try {

            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                Logger.error("Uncaught exception in thread {}: {}", thread.getName(), throwable, throwable.getStackTrace());
                try {
                    this.onShutdown();
                } catch (Exception e) {}
            });

            this.mainWindow = new MainWindow(MavenProperties.ARTIFACT_ID, APP_DIR, Arrays.stream(System.values()).toList());
            this.initMainWindow();

            this.audioEngine = new AudioEngine("%s-audio-thread".formatted(MavenProperties.ARTIFACT_ID));
            this.initAudioEngine();

            this.emulatorCommandListenerThread = new Thread(this::emulatorCommandListenerLoop, "%s-emulator-command-listener-thread".formatted(MavenProperties.ARTIFACT_ID));
            this.uiEventListenerThread = new Thread(this::eventListenerLoop, "%s-event-listener-thread".formatted(MavenProperties.ARTIFACT_ID));

            if (cliArgs != null) {
                Optional<System> system = cliArgs.getSystem();
                this.mainWindow.getFileManager().loadFile(cliArgs.getRomPath(), system.isPresent());
                system.ifPresent(s -> this.mainWindow.getEmulatorManager().setCurrentSystemDescriptor(s));
            }

            this.mainWindow.show();

        } catch (Exception e) {
            this.onShutdown();
            throw new RuntimeException("Failed to initialize %s: ".formatted(MavenProperties.ARTIFACT_ID), e);
        }
    }

    public AudioEngine getAudioEngine() {
        return this.audioEngine;
    }

    public void start() {
        this.running = true;
        if (this.uiEventListenerThread != null) {
            this.uiEventListenerThread.start();
        }
        if (this.emulatorCommandListenerThread != null) {
            this.emulatorCommandListenerThread.start();
        }
    }

    private void eventListenerLoop() {
        while (this.running) {
            try {
                Event uiEvent = this.mainWindow.waitEvent();
                switch (uiEvent) {
                    case MuteEvent(boolean mute) -> this.audioEngine.setMuted(mute);
                    case VolumeChangedEvent(int newVolume) -> this.audioEngine.setVolume(newVolume);
                    case null, default -> {}
                }
            } catch (InterruptedException _) {

            } catch (Exception e) {
                Logger.error("Unexpected error in event listener loop: {}", e);
            }
        }
    }

    private void emulatorCommandListenerLoop() {
        while (this.running) {
            try {
                EmulatorCommand enqueuedEmulatorCommand = this.mainWindow.waitEmulatorCommand();
                synchronized (this.systemLock) {
                    State enqueuedState = switch (enqueuedEmulatorCommand) {
                        case ResetEmulatorCommand resetEvent -> this.onEmulatorResetCommand(resetEvent);
                        case StopEmulatorCommand _ -> this.onEmulatorStopCommand();
                        case PauseEmulatorCommand pauseEmulatorCommand -> this.onEmulatorPauseCommand(pauseEmulatorCommand);
                        case StepFrameEmulatorCommand _ -> this.onEmulatorStepFrameCommand();
                        case StepCycleEmulatorCommand _ -> this.onEmulatorStepCycleCommand();
                        case null -> null;
                    };
                    if (enqueuedState == null) {
                        continue;
                    }
                    this.currentState = enqueuedState;
                }
            } catch (EmulatorException e) {
                Logger.error("Error initializing emulator: {}", e);
                this.onEmulatorException(e);
            } catch (InterruptedException _) {

            } catch (Exception e) {
                Logger.error("Unexpected error while processing emulator command: {}", e);
                this.onEmulatorException(new EmulatorException("Unexpected error while processing emulator command!", e));
            }
        }
    }

    private void onEmulatorIdle() {
    }

    private void onEmulatorRunning() {
        if (currentSystem == null) {
            return;
        }
        this.currentSystem.getEmulator().executeFrame();
        this.mainWindow.getTitleManager().update(this.currentSystem.getRomTitle().orElse("No title"));
    }

    private void onEmulatorSteppingFrame() {
        if (this.currentSystem == null) {
            return;
        }
        this.currentSystem.getEmulator().executeFrame();
        this.currentState = State.PAUSED;
    }

    private void onEmulatorSteppingCycle() {
        if (this.currentSystem == null) {
            return;
        }
        this.currentSystem.getEmulator().executeCycle();
        this.currentState = State.PAUSED;
    }

    private State onEmulatorResetCommand(ResetEmulatorCommand resetEvent) throws Exception {
        if (this.currentSystem != null) {
            this.currentSystem.close();
        }

        EmulatorInitializer emulatorInitializer = new EmulatorInitializer() {

            @Override
            public Optional<Path> getRomPath() {
                return mainWindow.getFileManager().getSelectedRomPath();
            }

            @Override
            public Optional<byte[]> getRawRom() {
                return this.getRomPath().map(SystemAdapter::readRawRom);
            }

            @Override
            public Optional<System> getSystem() {
                return Optional.ofNullable(resetEvent.getSystemDescriptor().orElse(null) instanceof System system ? system : null);
            }

        };

        // TODO: If there was a current emulator running before initializing a new one, just reset the current one and update its loaded ROM if any
        // TODO: Do not require a ROM to be selected to initialize it
        this.currentSystem = System.getSystemAdapter(this, emulatorInitializer);
        this.audioEngine.setFramerate(this.currentSystem.getEmulator().getFramerate());
        this.audioEngine.setAudioChannels(this.currentSystem.getEmulator().getAudioGenerator().isStereo() ? AudioChannels.STEREO : AudioChannels.MONO);
        this.audioEngine.start();
        this.mainWindow.getSystemViewport().setSystemDisplay(() -> this.currentSystem.createAWTComponentVideoDriver());

        boolean resetIntoPaused = resetEvent.resetIntoPaused();
        this.audioEngine.setPaused(resetIntoPaused);
        return resetIntoPaused ? State.PAUSED : State.RUNNING;
    }

    private State onEmulatorStopCommand() {
        if (this.currentSystem != null) {
            this.currentSystem.close();
            this.currentSystem = null;
        }
        this.audioEngine.stop();
        this.audioEngine.setPaused(true);
        return State.STOPPED;
    }

    private State onEmulatorPauseCommand(PauseEmulatorCommand pauseEmulatorCommand) {
        boolean stopped = this.currentSystem == null;
        if (pauseEmulatorCommand.pause()) {
            this.audioEngine.setPaused(true);
            return stopped ? State.PAUSE_STOPPED : State.PAUSED;
        } else {
            if (stopped) {
                return State.STOPPED;
            } else {
                this.audioEngine.setPaused(false);
                return State.RUNNING;
            }
        }
    }

    private State onEmulatorStepFrameCommand() {
        return State.STEPPING_FRAME;
    }

    private State onEmulatorStepCycleCommand() {
        return State.STEPPING_CYCLE;
    }

    private void initMainWindow() {
        this.mainWindow.setClosingHook(() -> {
            this.running = false;
            try {
                this.onShutdown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        HelpManager helpManager = this.mainWindow.getHelpManager();
        helpManager.setProjectName(MavenProperties.ARTIFACT_ID);
        helpManager.setAuthorString(MavenProperties.AUTHOR);
        helpManager.setVersionString(MavenProperties.VERSION);
        helpManager.setCommitIDString(Version.COMMIT_ID);
        helpManager.setBuildDateString(MavenProperties.BUILD_DATE);
        helpManager.setProjectSourceLink("https://github.com/ArkoSammy12/jemu");
        helpManager.setProjectBugReportLink("https://github.com/ArkoSammy12/jemu/issues");
    }

    private void initAudioEngine() {
        this.audioEngine.setSampleFrameCallback(() -> {
            try {
                synchronized (this.systemLock) {
                    if (this.currentSystem == null) {
                        return null;
                    }

                    switch (this.currentState) {
                        case STOPPED, PAUSED, PAUSE_STOPPED -> this.onEmulatorIdle();
                        case RUNNING -> this.onEmulatorRunning();
                        case STEPPING_FRAME -> this.onEmulatorSteppingFrame();
                        case STEPPING_CYCLE -> this.onEmulatorSteppingCycle();
                    }

                    this.currentSystem.onFrame();
                    return this.currentSystem.getAudioDriver().map(DefaultAudioRendererDriver::getSampleFrame).orElse(null);
                }
            } catch (Exception e) {
                Logger.error("Unexpected error while running emulator: {}", e);
                this.onEmulatorException(new EmulatorException("Unexpected error while running emulator!", e));
                return null;
            }
        });
    }

    private void onEmulatorException(Exception e) {
        this.mainWindow.showCoreError(e);
        synchronized (this.systemLock) {
            if (this.currentSystem != null) {
                try {
                    this.currentSystem.close();
                } catch (Exception _) {
                }
                this.mainWindow.getSystemViewport().setSystemDisplay(null);
                this.currentSystem = null;
            }
        }
        this.mainWindow.submitEmulatorCommand(new StopEmulatorCommand());
    }

    private void onShutdown() {
        if (this.shutdownStarted) {
            return;
        }
        this.shutdownStarted = true;
        try {
            if (this.emulatorCommandListenerThread != null) {
                this.emulatorCommandListenerThread.interrupt();
                this.emulatorCommandListenerThread.join();
            }
        } catch (InterruptedException _) {}

        try {
            if (this.uiEventListenerThread != null) {
                this.uiEventListenerThread.interrupt();
                this.uiEventListenerThread.join();
            }
        } catch (InterruptedException _) {}

        synchronized (this.systemLock) {
            if (this.currentSystem != null) {
                this.currentSystem.close();
                this.currentSystem = null;
            }
        }

        if (this.mainWindow != null) {
            this.mainWindow.close();
        }
        if (this.audioEngine != null) {
            this.audioEngine.close();
        }
    }

    private enum State {
        STOPPED,
        PAUSE_STOPPED,
        RUNNING,
        PAUSED,
        STEPPING_FRAME,
        STEPPING_CYCLE
    }

}