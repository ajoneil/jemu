package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.SystemController;

public class NESController<E extends NESEmulator> extends SystemController<E> {

    private static final int A_MASK = 1;
    private static final int B_MASK = 1 << 1;
    private static final int SELECT_MASK = 1 << 2;
    private static final int START_MASK = 1 << 3;
    private static final int UP_MASK = 1 << 4;
    private static final int DOWN_MASK = 1 << 5;
    private static final int LEFT_MASK = 1 << 6;
    private static final int RIGHT_MASK = 1 << 7;

    private int physicalControllerState;

    private boolean strobeSignal;
    private int currentControllerState;
    private int joy1ShiftRegister;

    public NESController(E emulator) {
        super(emulator);
    }
    @Override
    public void onActionPressed(Action action) {
        if (!(action instanceof Actions joypadAction)) {
            return;
        }
        switch (joypadAction) {
            case JOY1_UP -> {
                this.physicalControllerState |= UP_MASK;
                if ((this.physicalControllerState & DOWN_MASK) == 0) {
                    this.currentControllerState |= UP_MASK;
                }
            }
            case JOY1_DOWN -> {
                this.physicalControllerState |= DOWN_MASK;
                if ((this.physicalControllerState & UP_MASK) == 0) {
                    this.currentControllerState |= DOWN_MASK;
                }
            }
            case JOY1_LEFT -> {
                this.physicalControllerState |= LEFT_MASK;
                if ((this.physicalControllerState & RIGHT_MASK) == 0) {
                    this.currentControllerState |= LEFT_MASK;
                }
            }
            case JOY1_RIGHT -> {
                this.physicalControllerState |= RIGHT_MASK;
                if ((this.physicalControllerState & LEFT_MASK) == 0) {
                    this.currentControllerState |= RIGHT_MASK;
                }
            }
            case JOY1_START -> this.currentControllerState |= START_MASK;
            case JOY1_SELECT -> this.currentControllerState |= SELECT_MASK;
            case JOY1_A -> this.currentControllerState |= A_MASK;
            case JOY1_B -> this.currentControllerState |= B_MASK;
        }
    }

    @Override
    public void onActionReleased(Action action) {
        if (!(action instanceof Actions joypadAction)) {
            return;
        }
        switch (joypadAction) {
            case JOY1_UP -> {
                this.physicalControllerState &= ~UP_MASK;
                this.currentControllerState &= ~UP_MASK;
                if ((this.physicalControllerState & DOWN_MASK) != 0) {
                    this.currentControllerState |= DOWN_MASK;
                }
            }
            case JOY1_DOWN -> {
                this.physicalControllerState &= ~DOWN_MASK;
                this.currentControllerState &= ~DOWN_MASK;
                if ((this.physicalControllerState & UP_MASK) != 0) {
                    this.currentControllerState |= UP_MASK;
                }
            }
            case JOY1_LEFT -> {
                this.physicalControllerState &= ~LEFT_MASK;
                this.currentControllerState &= ~LEFT_MASK;
                if ((this.physicalControllerState & RIGHT_MASK) != 0) {
                    this.currentControllerState |= RIGHT_MASK;
                }
            }
            case JOY1_RIGHT -> {
                this.physicalControllerState &= ~RIGHT_MASK;
                this.currentControllerState &= ~RIGHT_MASK;
                if ((this.physicalControllerState & LEFT_MASK) != 0) {
                    this.currentControllerState |= LEFT_MASK;
                }
            }
            case JOY1_START  -> this.currentControllerState &= ~START_MASK;
            case JOY1_SELECT -> this.currentControllerState &= ~SELECT_MASK;
            case JOY1_A -> this.currentControllerState &= ~A_MASK;
            case JOY1_B -> this.currentControllerState &= ~B_MASK;
        }
    }

    public void writeJoy1(int value) {
        this.strobeSignal = (value & 1) != 0;
    }

    public int readJoy1() {
        if (this.strobeSignal) {
            return (this.currentControllerState & 1);
        }
        int bit = this.joy1ShiftRegister & 1;
        this.joy1ShiftRegister = (this.joy1ShiftRegister >> 1) | 0x80;
        return bit;
    }

    public int readJoy2() {
        return 0x00;
    }

    public void cycle() {
        if (this.strobeSignal && this.emulator.getRicohCore().getCurrentApuHalfCycleType() == RP2A03.APUHalfCycleType.GET) {
            this.joy1ShiftRegister = this.currentControllerState;
        }
    }

    public enum Actions implements Action {
        JOY1_UP("Joy1 Up"),
        JOY1_DOWN("Joy1 Down"),
        JOY1_LEFT("Joy1 Left"),
        JOY1_RIGHT("Joy1 Right"),
        JOY1_START("Joy1 Start"),
        JOY1_SELECT("Joy1 Select"),
        JOY1_A("Joy1 A"),
        JOY1_B("Joy1 B"),

        JOY2_UP("Joy2 Up"),
        JOY2_DOWN("Joy2 Down"),
        JOY2_LEFT("Joy2 Left"),
        JOY2_RIGHT("Joy2 Right"),
        JOY2_START("Joy2 Start"),
        JOY2_SELECT("Joy2 Select"),
        JOY2_A("Joy2 A"),
        JOY2_B("Joy2 B");

        private final String label;

        Actions(String label) {
            this.label = label;
        }

        @Override
        public String getLabel() {
            return this.label;
        }
    }

}
