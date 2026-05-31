package io.github.arkosammy12.jemu.app.adapters;

import de.gurkenlabs.input4j.InputComponent;
import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.drivers.DefaultAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.DefaultSystemVideoDriver;
import io.github.arkosammy12.jemu.app.drivers.MonoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.drivers.JoypadDriver;
import io.github.arkosammy12.jemu.app.drivers.StereoAudioRendererDriver;
import io.github.arkosammy12.jemu.app.io.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.core.drivers.VideoDriver;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public abstract class AbstractSystemAdapter implements SystemAdapter {

    private final byte[] rom;
    private final Path path;

    private final Emulator emulator;
    private final DefaultAudioRendererDriver audioDriver;
    private final JoypadDriver joypadDriver;

    @Nullable
    private DefaultSystemVideoDriver videoDriver;

    public AbstractSystemAdapter(Jemu jemu, CoreInitializer initializer) {
        Optional<byte[]> rawRomOptional = initializer.getRawRom();
        Optional<Path> romPathOptional = initializer.getRomPath();
        if (rawRomOptional.isEmpty() || romPathOptional.isEmpty()) {
            throw new EmulatorException("Must select a ROM file before starting emulation!");
        }
        byte[] rom = rawRomOptional.get();
        this.rom = Arrays.copyOf(rom, rom.length);
        this.path = romPathOptional.get();

        this.emulator = this.createEmulator();
        this.joypadDriver = new JoypadDriver(this);
        this.audioDriver = this.emulator.getAudioGenerator().isStereo() ? new StereoAudioRendererDriver(jemu, this.emulator.getAudioGenerator()) : new MonoAudioRendererDriver(jemu, this.emulator.getAudioGenerator());
    }

    protected abstract Emulator createEmulator();

    @Nullable
    protected abstract SystemController.Action getActionForKeyCode(int keyCode);

    @Nullable
    public abstract SystemController.Action getActionForJoypadEvent(InputComponent.ID id);

    @Override
    public byte[] getRom() {
        return Arrays.copyOf(this.rom, this.rom.length);
    }

    @Override
    public Path getRomPath() {
        return this.path;
    }

    @Override
    public Emulator getEmulator() {
        return this.emulator;
    }

    @Override
    public Optional<VideoDriver> getVideoDriver() {
        return Optional.ofNullable(this.videoDriver);
    }

    @Override
    public Optional<? extends DefaultAudioRendererDriver> getAudioDriver() {
        return Optional.of(this.audioDriver);
    }

    public Component createAWTComponentVideoDriver() {
        KeyAdapter keyAdapter = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                SystemController.Action action = getActionForKeyCode(keyCode);
                if (action != null) {
                    getEmulator().getSystemController().onActionPressed(action);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                SystemController.Action action = getActionForKeyCode(keyCode);
                if (action != null) {
                    getEmulator().getSystemController().onActionReleased(action);
                }
            }

        };
        this.videoDriver = new DefaultSystemVideoDriver(this.emulator.getVideoGenerator(), keyAdapter);
        return this.videoDriver;
    }

    public void onFrame() {
        if (this.videoDriver != null) {
            this.videoDriver.requestFrame();
        }
        this.joypadDriver.poll();
    }

    @Override
    public void close() {
        if (this.videoDriver != null) {
            this.videoDriver.close();
        }
        try {
            this.joypadDriver.close();
            if (this.emulator != null) {
                this.emulator.close();
            }
        } catch (Exception e) {
            Logger.error("Error attempting to release %s emulator resources: {}".formatted(this.getSystemName()), e);
        }
    }

}
