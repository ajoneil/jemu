package io.github.arkosammy12.jemu.core.rca.studioii;

import io.github.arkosammy12.jemu.core.common.SystemController;

public class RCAStudioIIKeypad<E extends RCAStudioIIEmulator> extends SystemController<E> {

    private final boolean[] keypad1Keys = new boolean[10];
    private boolean keypad1Efx;

    private final boolean[] keypad2Keys = new boolean[10];
    private boolean keypad2Efx;

    private int latchedKey;

    public RCAStudioIIKeypad(E emulator) {
        super(emulator);
    }

    @Override
    public void onActionPressed(Action action) {
        if (!(action instanceof Actions studioIIAction)) {
            return;
        }
        switch (studioIIAction.keypad) {
            case KEYPAD_1 -> this.keypad1Keys[studioIIAction.key] = true;
            case KEYPAD_2 -> this.keypad2Keys[studioIIAction.key] = true;
        }
    }

    @Override
    public void onActionReleased(Action action) {
        if (!(action instanceof Actions studioIIAction)) {
            return;
        }
        switch (studioIIAction.keypad) {
            case KEYPAD_1 -> this.keypad1Keys[studioIIAction.key] = false;
            case KEYPAD_2 -> this.keypad2Keys[studioIIAction.key] = false;
        }
    }

    public boolean getKeypad1EFX() {
        return this.keypad1Efx;
    }

    public boolean getKeypad2EFX() {
        return this.keypad2Efx;
    }

    public void setLatchedKey(int value) {
        this.latchedKey = value & 0xF;
    }

    public void cycle() {
        if (this.latchedKey <= 9) {
            this.keypad1Efx = this.keypad1Keys[this.latchedKey];
            this.keypad2Efx = this.keypad2Keys[this.latchedKey];
        } else {
            this.keypad1Efx = false;
            this.keypad2Efx = false;
        }
    }

    public enum Actions implements Action {
        KEYPAD1_0("Keypad 1 key 0", 0x0, Keypad.KEYPAD_1),
        KEYPAD1_1("Keypad 1 key 1", 0x1, Keypad.KEYPAD_1),
        KEYPAD1_2("Keypad 1 key 2", 0x2, Keypad.KEYPAD_1),
        KEYPAD1_3("Keypad 1 key 3", 0x3, Keypad.KEYPAD_1),
        KEYPAD1_4("Keypad 1 key 4", 0x4, Keypad.KEYPAD_1),
        KEYPAD1_5("Keypad 1 key 5", 0x5, Keypad.KEYPAD_1),
        KEYPAD1_6("Keypad 1 key 6", 0x6, Keypad.KEYPAD_1),
        KEYPAD1_7("Keypad 1 key 7", 0x7, Keypad.KEYPAD_1),
        KEYPAD1_8("Keypad 1 key 8", 0x8, Keypad.KEYPAD_1),
        KEYPAD1_9("Keypad 1 key 9", 0x9, Keypad.KEYPAD_1),

        KEYPAD2_0("Keypad 2 key 0", 0x0, Keypad.KEYPAD_2),
        KEYPAD2_1("Keypad 2 key 1", 0x1, Keypad.KEYPAD_2),
        KEYPAD2_2("Keypad 2 key 2", 0x2, Keypad.KEYPAD_2),
        KEYPAD2_3("Keypad 2 key 3", 0x3, Keypad.KEYPAD_2),
        KEYPAD2_4("Keypad 2 key 4", 0x4, Keypad.KEYPAD_2),
        KEYPAD2_5("Keypad 2 key 5", 0x5, Keypad.KEYPAD_2),
        KEYPAD2_6("Keypad 2 key 6", 0x6, Keypad.KEYPAD_2),
        KEYPAD2_7("Keypad 2 key 7", 0x7, Keypad.KEYPAD_2),
        KEYPAD2_8("Keypad 2 key 8", 0x8, Keypad.KEYPAD_2),
        KEYPAD2_9("Keypad 2 key 9", 0x9, Keypad.KEYPAD_2);

        private final String label;
        private final Keypad keypad;
        private final int key;

        Actions(String label, int key, Keypad keypad) {
            this.label = label;
            this.key = key;
            this.keypad = keypad;
        }

        @Override
        public String getLabel() {
            return this.label;
        }

    }

    private enum Keypad {
        KEYPAD_1,
        KEYPAD_2
    }

}
