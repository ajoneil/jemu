package io.github.arkosammy12.jemu.app.drivers;

import de.gurkenlabs.input4j.InputComponent;
import de.gurkenlabs.input4j.InputDevice;
import de.gurkenlabs.input4j.InputDevicePlugin;
import io.github.arkosammy12.jemu.app.util.MavenProperties;
import io.github.arkosammy12.jemu.app.adapters.AbstractSystemAdapter;

import de.gurkenlabs.input4j.InputDevices;
import de.gurkenlabs.input4j.components.XInput;
import io.github.arkosammy12.jemu.core.common.SystemController;
import org.tinylog.Logger;

import java.io.IOException;


public class JoypadDriver implements AutoCloseable {

    private static final float JOYSTICK_ENGAGED_THRESH = 0.5f;

    // Keep JOYSTICK_DISENGAGED_THRESH <= JOYSTICK_ENGAGED_THRESH.
    // If JOYSTICK_DISENGAGED_THRESH < JOYSTICK_ENGAGED_THRESH there is a hysteresis effect which seems more natural
    // when using a joystick in place of a DPAD
    private static final float JOYSTICK_DISENGAGED_THRESH = 0.45f;

    private boolean joyUp = false;
    private boolean joyDown = false;
    private boolean joyLeft = false;
    private boolean joyRight = false;

    private final AbstractSystemAdapter systemAdapter;

    private final Thread pollThread;
    private final Object joypadLock = new Object();
    private volatile boolean running = true;

    private InputDevicePlugin inputDevicePlugin;

    public JoypadDriver(AbstractSystemAdapter systemAdapter) {
        this.systemAdapter = systemAdapter;

        this.pollThread = new Thread(this::pollLoop, "%s-joypad-poll-thread".formatted(MavenProperties.ARTIFACT_ID));
        this.pollThread.setDaemon(true);
        this.pollThread.start();
    }

    public void poll() {
        synchronized (this.joypadLock) {
            this.joypadLock.notify();
        }
    }

    private void pollLoop() {
        this.tryInitDevices();

        while (this.running) {
            try {
                synchronized (this.joypadLock) {
                    this.joypadLock.wait();
                    if (!this.running) {
                        break;
                    }
                    this.tryInitDevices();
                    if (this.inputDevicePlugin != null) {
                        this.inputDevicePlugin.getAll().forEach(InputDevice::poll);
                    }
                }
            } catch (InterruptedException e) {}
        }

        if (this.inputDevicePlugin != null) {
            try {
                this.inputDevicePlugin.close();
            } catch (IOException e) {
                Logger.error("Error releasing joypad driver resources: {}", e);
            }
        }
    }

    private void registerCallbacks(InputDevice device) {
        device.onButtonPressed(XInput.DPAD_UP, () -> pressButton(XInput.DPAD_UP));
        device.onButtonPressed(XInput.DPAD_DOWN, () -> pressButton(XInput.DPAD_DOWN));
        device.onButtonPressed(XInput.DPAD_LEFT, () -> pressButton(XInput.DPAD_LEFT));
        device.onButtonPressed(XInput.DPAD_RIGHT, () -> pressButton(XInput.DPAD_RIGHT));
        device.onButtonPressed(XInput.START, () -> pressButton(XInput.START));
        device.onButtonPressed(XInput.BACK, () -> pressButton(XInput.BACK));
        device.onButtonPressed(XInput.X, () -> pressButton(XInput.X));
        device.onButtonPressed(XInput.A, () -> pressButton(XInput.A));

        device.onButtonReleased(XInput.DPAD_UP, () -> releaseButton(XInput.DPAD_UP));
        device.onButtonReleased(XInput.DPAD_DOWN, () -> releaseButton(XInput.DPAD_DOWN));
        device.onButtonReleased(XInput.DPAD_LEFT, () -> releaseButton(XInput.DPAD_LEFT));
        device.onButtonReleased(XInput.DPAD_RIGHT, () -> releaseButton(XInput.DPAD_RIGHT));
        device.onButtonReleased(XInput.START, () -> releaseButton(XInput.START));
        device.onButtonReleased(XInput.BACK, () -> releaseButton(XInput.BACK));
        device.onButtonReleased(XInput.X, () -> releaseButton(XInput.X));
        device.onButtonReleased(XInput.A, () -> releaseButton(XInput.A));

        device.onAxisChanged(XInput.LEFT_THUMB_X, this::xAxisChange);
        device.onAxisChanged(XInput.LEFT_THUMB_Y, this::yAxisChange);
    }

    private void pressButton(InputComponent.ID id) {
        SystemController.Action action = systemAdapter.getActionForJoypadEvent(id);
        if (action != null) {
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
        }
    }

    private void releaseButton(InputComponent.ID id) {
        SystemController.Action action = systemAdapter.getActionForJoypadEvent(id);
        if (action != null) {
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
        }
    }

    private void xAxisChange(float val) {
        if(val >= JOYSTICK_ENGAGED_THRESH && !joyRight){
            joyRight = true;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_RIGHT);
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
        }
        else if (val < JOYSTICK_DISENGAGED_THRESH && joyRight){
            joyRight = false;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_RIGHT);
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
        }

        if(val <= -JOYSTICK_ENGAGED_THRESH && !joyLeft){
            joyLeft = true;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_LEFT);
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
        }
        else if (val > -JOYSTICK_DISENGAGED_THRESH && joyLeft){
            joyLeft = false;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_LEFT);
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
        }
    }

    private void yAxisChange(float val) {
        if (val >= JOYSTICK_ENGAGED_THRESH && !joyUp) {
            joyUp = true;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_UP);
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
        } else if (val < JOYSTICK_DISENGAGED_THRESH && joyUp) {
            joyUp = false;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_UP);
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
        }

        if (val <= -JOYSTICK_ENGAGED_THRESH && !joyDown) {
            joyDown = true;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_DOWN);
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
        } else if (val > -JOYSTICK_DISENGAGED_THRESH && joyDown) {
            joyDown = false;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_DOWN);
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
        }
    }

    private void tryInitDevices() {
        if (this.inputDevicePlugin != null) {
            return;
        }
        this.inputDevicePlugin = InputDevices.init();
        this.inputDevicePlugin.getAll().forEach(this::registerCallbacks);
        this.inputDevicePlugin.onDeviceConnected(this::registerCallbacks);
    }

    @Override
    public void close() {
        this.running = false;
        synchronized (this.joypadLock) {
            this.joypadLock.notifyAll();
        }
        try {
            this.pollThread.join();
        } catch (InterruptedException _) {}
    }

}
