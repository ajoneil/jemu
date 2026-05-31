package io.github.arkosammy12.jemu.app.drivers;

import de.gurkenlabs.input4j.InputComponent;
import de.gurkenlabs.input4j.InputDevice;
import io.github.arkosammy12.jemu.app.util.MavenProperties;
import io.github.arkosammy12.jemu.app.adapters.AbstractSystemAdapter;

import de.gurkenlabs.input4j.InputDevices;
import de.gurkenlabs.input4j.components.XInput;
import io.github.arkosammy12.jemu.core.common.SystemController;

import java.io.IOException;

public class JoypadDriver implements AutoCloseable {
    private static final int POLL_DEVICES_SLEEP_TIME_MS = 1000; // 1 second
    private static final int POLL_DEVICE_SLEEP_TIME_MS = 16/2;  // ~1/2 frame

    private static float JOYSTICK_ENGAGED_THRESH = 0.5f;

    // Keep JOYSTICK_DISENGAGED_THRESH <= JOYSTICK_ENGAGED_THRESH.
    // If JOYSTICK_DISENGAGED_THRESH < JOYSTICK_ENGAGED_THRESH there is a hysteresis effect which seems more natural
    // when using a joystick in place of a DPAD
    private static float JOYSTICK_DISENGAGED_THRESH = 0.45f;
    private boolean joyUp = false;
    private boolean joyDown = false;
    private boolean joyLeft = false;
    private boolean joyRight = false;

    private InputDevice device;
    private final Thread pollThread;
    private volatile boolean running = true;

    private final AbstractSystemAdapter systemAdapter;

    public JoypadDriver(AbstractSystemAdapter systemAdapter) {
        this.systemAdapter = systemAdapter;

        this.pollThread = new Thread(this::pollLoop, "%s-joypad-poll-thread".formatted(MavenProperties.ARTIFACT_ID));
        this.pollThread.setDaemon(true);
        this.pollThread.start();
    }

    private void pressButton(InputComponent.ID id)
    {
        SystemController.Action action = systemAdapter.getActionForJoypadEvent(id);

        if(action != null)
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
    }

    private void releaseButton(InputComponent.ID id)
    {
        SystemController.Action action = systemAdapter.getActionForJoypadEvent(id);

        if(action != null)
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
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
        if(val >= JOYSTICK_ENGAGED_THRESH && !joyUp){
            joyUp = true;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_UP);
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
        }
        else if (val < JOYSTICK_DISENGAGED_THRESH && joyUp){
            joyUp = false;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_UP);
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
        }

        if(val <= -JOYSTICK_ENGAGED_THRESH && !joyDown){
            joyDown = true;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_DOWN);
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
        }
        else if (val > -JOYSTICK_DISENGAGED_THRESH && joyDown){
            joyDown = false;
            SystemController.Action action = systemAdapter.getActionForJoypadEvent(XInput.DPAD_DOWN);
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
        }
    }

    private void threadSleep(long ms)
    {
        try{
            Thread.sleep(POLL_DEVICE_SLEEP_TIME_MS);
        } catch (InterruptedException e) {
            running = false;
            Thread.currentThread().interrupt();
        }
    }

    private void pollLoop() {
        while (running)
        {
            try (var devices = InputDevices.init()) {
                // For some reason the onDeviceConnected event doesn't work as a hot swap so is rather useless...
                // We must call InputDevices.init() to check for newly connected controllers
                devices.onDeviceDisconnected(this::detachDevice);
                devices.getAll().stream().findFirst().ifPresent(this::attachDevice);

                while(running && device != null)
                {
                    device.poll();
                    threadSleep(POLL_DEVICE_SLEEP_TIME_MS);
                }
            } catch (IOException e) {
                // Do nothing; loop will re-initialize
            }

            threadSleep(POLL_DEVICES_SLEEP_TIME_MS);
        }
    }

    private void detachDevice(InputDevice inputDevice) {
        // TODO: multiple controller support
        if (this.device == inputDevice) {
            this.device = null;
        }
    }

    private void attachDevice(InputDevice inputDevice) {
        // TODO: multiple controller support
        if (this.device != null)
            return;

        this.device = inputDevice;

        this.device.onButtonPressed(XInput.DPAD_UP, () -> pressButton(XInput.DPAD_UP));
        this.device.onButtonPressed(XInput.DPAD_DOWN, () -> pressButton(XInput.DPAD_DOWN));
        this.device.onButtonPressed(XInput.DPAD_LEFT, () -> pressButton(XInput.DPAD_LEFT));
        this.device.onButtonPressed(XInput.DPAD_RIGHT, () -> pressButton(XInput.DPAD_RIGHT));
        this.device.onButtonPressed(XInput.START, () -> pressButton(XInput.START));
        this.device.onButtonPressed(XInput.BACK, () -> pressButton(XInput.BACK));
        this.device.onButtonPressed(XInput.X, () -> pressButton(XInput.X));
        this.device.onButtonPressed(XInput.A, () -> pressButton(XInput.A));

        this.device.onButtonReleased(XInput.DPAD_UP, () -> releaseButton(XInput.DPAD_UP));
        this.device.onButtonReleased(XInput.DPAD_DOWN, () -> releaseButton(XInput.DPAD_DOWN));
        this.device.onButtonReleased(XInput.DPAD_LEFT, () -> releaseButton(XInput.DPAD_LEFT));
        this.device.onButtonReleased(XInput.DPAD_RIGHT, () -> releaseButton(XInput.DPAD_RIGHT));
        this.device.onButtonReleased(XInput.START, () -> releaseButton(XInput.START));
        this.device.onButtonReleased(XInput.BACK, () -> releaseButton(XInput.BACK));
        this.device.onButtonReleased(XInput.X, () -> releaseButton(XInput.X));
        this.device.onButtonReleased(XInput.A, () -> releaseButton(XInput.A));

        this.device.onAxisChanged(XInput.LEFT_THUMB_X, this::xAxisChange);
        this.device.onAxisChanged(XInput.LEFT_THUMB_Y, this::yAxisChange);
    }

    @Override
    public void close() {
        running = false;
        pollThread.interrupt();

        try {
            pollThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
