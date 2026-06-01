package io.github.arkosammy12.jemu.frontend.gui.swing.events;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;

public record SampleRateChangedEvent(SampleRate sampleRate) implements Event {}
