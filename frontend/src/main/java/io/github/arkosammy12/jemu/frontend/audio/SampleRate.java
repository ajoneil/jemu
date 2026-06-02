package io.github.arkosammy12.jemu.frontend.audio;

public enum SampleRate {
    HZ_44100(0),
    HZ_48000(1);

    private final int id;

    SampleRate(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

}
