package io.github.arkosammy12.jemu.frontend.gui.swing.menus;

import com.formdev.flatlaf.util.SystemInfo;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.swing.MenuBarMenu;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.net.URI;

public class HelpMenu extends MenuBarMenu {

    @NotNull
    private String projectName = "unknown";

    @NotNull
    private String authorString = "unknown";

    @NotNull
    private String versionString = "unknown";

    @NotNull
    private String commitIDString = "unknown";

    @NotNull
    private String buildDateString = "unknown";

    @NotNull
    private String projectSourceLink = "unknown";

    @NotNull
    private String projectBugReportLink = "unknown";

    public HelpMenu(MainWindow mainWindow) {

        this.getJMenu().setText("Help");
        this.getJMenu().setMnemonic(KeyEvent.VK_H);

        Runnable showAboutDialog = () -> {

            String message = """
                    Version: %s
                    Build Date: %s
                    Commit ID: %s
                    Author: %s
                    """.formatted(this.versionString, this.buildDateString, this.commitIDString, this.authorString);

            mainWindow.showDialog("About - %s".formatted(this.projectName), message, MainWindow.DialogType.INFORMATION);

        };

        Runnable addAboutItem = () -> {
            JMenuItem aboutItem = new JMenuItem("About");
            aboutItem.setMnemonic(KeyEvent.VK_A);
            aboutItem.addActionListener(_ -> showAboutDialog.run());
            this.getJMenu().add(aboutItem);
        };

        if (SystemInfo.isMacOS) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                desktop.setAboutHandler(_ -> showAboutDialog.run());
            } else {
                addAboutItem.run();
            }
        } else {
            addAboutItem.run();
        }

        JMenuItem sourceItem = new JMenuItem("Source");
        sourceItem.setMnemonic(KeyEvent.VK_S);
        sourceItem.addActionListener(_ -> {
            try {
                if (this.projectSourceLink == null) {
                    throw new IllegalStateException("No project source link specified!");
                }
                Desktop.getDesktop().browse(new URI(this.projectSourceLink));
            } catch (Exception ex) {
                mainWindow.showDialog("Unable to open source link", ex.getMessage(), MainWindow.DialogType.ERROR);
            }
        });

        JMenuItem reportItem = new JMenuItem("Report a Bug");
        reportItem.setMnemonic(KeyEvent.VK_R);
        reportItem.addActionListener(_ -> {
            try {
                if (this.projectBugReportLink == null) {
                    throw new IllegalStateException("No project bug report link specified!");
                }
                Desktop.getDesktop().browse(new URI(this.projectBugReportLink));
            } catch (Exception ex) {
                mainWindow.showDialog("Unable to open bug report link", ex.getMessage(), MainWindow.DialogType.ERROR);
            }
        });

        this.getJMenu().add(sourceItem);
        this.getJMenu().add(reportItem);

    }

    public void setProjectName(@NotNull String projectName) {
        this.projectName = projectName;
    }

    public void setAuthorString(@NotNull String authorString) {
        this.authorString = authorString;
    }

    public void setVersionString(@NotNull String versionString) {
        this.versionString = versionString;
    }

    public void setCommitIDString(@NotNull String commitIdString) {
        this.commitIDString = commitIdString;
    }

    public void setBuildDateString(@NotNull String buildDateString) {
        this.buildDateString = buildDateString;
    }

    public void setProjectSourceLink(@NotNull String projectSourceLink) {
        this.projectSourceLink = projectSourceLink;
    }

    public void setProjectBugReportLink(@NotNull String projectBugReportLink) {
        this.projectBugReportLink = projectBugReportLink;
    }

}
