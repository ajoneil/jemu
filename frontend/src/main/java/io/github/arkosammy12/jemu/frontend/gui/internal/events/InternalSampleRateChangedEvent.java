package io.github.arkosammy12.jemu.frontend.gui.internal.events;

import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.swing.events.SampleRateChangedEvent;

public record InternalSampleRateChangedEvent(SampleRate sampleRate) implements InternalEvent {

    @Override
    public Event getEvent() {
        return new SampleRateChangedEvent(sampleRate);
    }

}
