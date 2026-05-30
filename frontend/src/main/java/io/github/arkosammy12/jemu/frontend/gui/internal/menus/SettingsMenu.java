package io.github.arkosammy12.jemu.frontend.gui.internal.menus;

import io.github.arkosammy12.jemu.frontend.gui.internal.SerializedEntry;
import io.github.arkosammy12.jemu.frontend.gui.internal.events.InternalMuteEvent;
import io.github.arkosammy12.jemu.frontend.gui.internal.events.InternalVolumeChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.swing.MenuBarMenu;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.SettingsManager;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow.tryParseInt;

public class SettingsMenu extends MenuBarMenu implements SettingsManager {

    private final JSlider volumeSlider;
    private final JRadioButtonMenuItem muteButton;

    private volatile int volume = 50;
    private volatile boolean muted = false;

    public SettingsMenu(MainWindow mainWindow) {
        this.getJMenu().setText("Settings");
        this.getJMenu().setMnemonic(KeyEvent.VK_S);

        JMenu volumeMenu = new JMenu("Volume");
        this.volumeSlider = new JSlider(0, 100, this.volume);
        this.volumeSlider.setPaintTrack(true);
        this.volumeSlider.setPaintTicks(true);
        this.volumeSlider.setPaintLabels(true);
        this.volumeSlider.setMajorTickSpacing(25);
        this.volumeSlider.setMinorTickSpacing(5);
        this.volumeSlider.addChangeListener(_ -> {
            this.volume = Math.clamp(this.volumeSlider.getValue(), 0, 100);
            mainWindow.pushEvent(new InternalVolumeChangedEvent(this.volume));
        });
        JPanel volumePanel = new JPanel();
        volumePanel.add(this.volumeSlider);
        volumeMenu.add(volumePanel);

        this.muteButton = new JRadioButtonMenuItem("Mute");
        this.muteButton.addChangeListener(_ -> {
            this.muted = this.muteButton.isSelected();
            mainWindow.pushEvent(new InternalMuteEvent(this.muted));
        });
        this.muteButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK, true));
        this.muteButton.setSelected(this.muted);

        this.getJMenu().add(volumeMenu);
        this.getJMenu().add(muteButton);
        this.getJMenu().addSeparator();

        mainWindow.registerSettingProperty(new SerializedEntry("settings.volume", () -> String.valueOf(this.volumeSlider.getValue()), s -> tryParseInt(s).ifPresent(this.volumeSlider::setValue)));
        mainWindow.registerSettingProperty(new SerializedEntry("settings.muted", () -> String.valueOf(this.muteButton.isSelected()), s -> this.muteButton.setSelected(Boolean.parseBoolean(s))));

    }

    @Override
    public int getVolume() {
        return this.volume;
    }

    @Override
    public boolean getMuted() {
        return this.muted;
    }

}
