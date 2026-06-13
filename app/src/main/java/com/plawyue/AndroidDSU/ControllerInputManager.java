package com.plawyue.AndroidDSU;

import android.content.Context;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * ControllerInputManager handles external gamepad/controller input mapping.
 * Maps Android gamepad events to DSU controller state.
 */
public class ControllerInputManager {
    private static final String TAG = "ControllerInputManager";
    
    // Deadzone for analog sticks (0-1 range)
    private static final float STICK_DEADZONE = 0.15f;
    private static final float TRIGGER_DEADZONE = 0.1f;
    
    private DsuCtrlType controllerData;
    private boolean isConnected = false;

    public ControllerInputManager(DsuCtrlType controllerData) {
        this.controllerData = controllerData;
    }

    /**
     * Process motion events from external controller (analog sticks, triggers)
     */
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (!isConnected) {
            return false;
        }

        InputDevice device = InputDevice.getDevice(event.getDeviceId());
        if (device == null || !isGamepad(device)) {
            return false;
        }

        // Process left stick (X=0, Y=1)
        processLeftStick(event);
        
        // Process right stick (Z=11, RZ=14)
        processRightStick(event);
        
        // Process triggers (LTRIGGER=17, RTRIGGER=18)
        processAnalogTriggers(event);

        return true;
    }

    /**
     * Process key events from external controller (buttons)
     */
    public boolean onKeyEvent(KeyEvent event) {
        if (!isConnected) {
            return false;
        }

        InputDevice device = InputDevice.getDevice(event.getDeviceId());
        if (device == null || !isGamepad(device)) {
            return false;
        }

        int keyCode = event.getKeyCode();
        int action = event.getAction();
        boolean isPressed = action == KeyEvent.ACTION_DOWN;

        switch (keyCode) {
            // Face buttons
            case KeyEvent.KEYCODE_BUTTON_A:
                controllerData.A = isPressed ? 255 : 0;
                return true;
            case KeyEvent.KEYCODE_BUTTON_B:
                controllerData.B = isPressed ? 255 : 0;
                return true;
            case KeyEvent.KEYCODE_BUTTON_X:
                controllerData.X = isPressed ? 255 : 0;
                return true;
            case KeyEvent.KEYCODE_BUTTON_Y:
                controllerData.Y = isPressed ? 255 : 0;
                return true;

            // D-Pad
            case KeyEvent.KEYCODE_DPAD_UP:
                controllerData.Dpad_UP = isPressed ? 255 : 0;
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                controllerData.Dpad_Down = isPressed ? 255 : 0;
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                controllerData.Dpad_Left = isPressed ? 255 : 0;
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                controllerData.Dpad_Right = isPressed ? 255 : 0;
                return true;

            // Shoulder buttons
            case KeyEvent.KEYCODE_BUTTON_L1:
                controllerData.L1 = isPressed ? 255 : 0;
                return true;
            case KeyEvent.KEYCODE_BUTTON_R1:
                controllerData.R1 = isPressed ? 255 : 0;
                return true;
            case KeyEvent.KEYCODE_BUTTON_L2:
                controllerData.L2 = isPressed ? 255 : 0;
                return true;
            case KeyEvent.KEYCODE_BUTTON_R2:
                controllerData.R2 = isPressed ? 255 : 0;
                return true;

            // Stick buttons
            case KeyEvent.KEYCODE_BUTTON_THUMBL:
                controllerData.L3 = isPressed ? 1 : 0;
                return true;
            case KeyEvent.KEYCODE_BUTTON_THUMBR:
                controllerData.R3 = isPressed ? 1 : 0;
                return true;

            // Menu buttons
            case KeyEvent.KEYCODE_BUTTON_START:
                controllerData.Option = isPressed ? 1 : 0;
                return true;
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                controllerData.Share = isPressed ? 1 : 0;
                return true;
            case KeyEvent.KEYCODE_BUTTON_MODE:
                controllerData.PS = isPressed ? 1 : 0;
                return true;

            default:
                return false;
        }
    }

    /**
     * Process left analog stick (AXIS_X, AXIS_Y)
     */
    private void processLeftStick(MotionEvent event) {
        float x = getCenteredAxis(event, MotionEvent.AXIS_X);
        float y = getCenteredAxis(event, MotionEvent.AXIS_Y);

        // Apply deadzone
        if (Math.abs(x) < STICK_DEADZONE) x = 0;
        if (Math.abs(y) < STICK_DEADZONE) y = 0;

        // Convert from normalized [-1, 1] to [0, 255]
        controllerData.left_stick_x = (int) ((x + 1.0f) / 2.0f * 255);
        controllerData.left_stick_y = (int) ((y + 1.0f) / 2.0f * 255);
    }

    /**
     * Process right analog stick (AXIS_Z, AXIS_RZ)
     */
    private void processRightStick(MotionEvent event) {
        float x = getCenteredAxis(event, MotionEvent.AXIS_Z);
        float y = getCenteredAxis(event, MotionEvent.AXIS_RZ);

        // Apply deadzone
        if (Math.abs(x) < STICK_DEADZONE) x = 0;
        if (Math.abs(y) < STICK_DEADZONE) y = 0;

        // Convert from normalized [-1, 1] to [0, 255]
        controllerData.right_stick_x = (int) ((x + 1.0f) / 2.0f * 255);
        controllerData.right_stick_y = (int) ((y + 1.0f) / 2.0f * 255);
    }

    /**
     * Process analog triggers (AXIS_LTRIGGER, AXIS_RTRIGGER)
     */
    private void processAnalogTriggers(MotionEvent event) {
        float lTrigger = event.getAxisValue(MotionEvent.AXIS_LTRIGGER);
        float rTrigger = event.getAxisValue(MotionEvent.AXIS_RTRIGGER);

        // Apply deadzone
        if (Math.abs(lTrigger) < TRIGGER_DEADZONE) lTrigger = 0;
        if (Math.abs(rTrigger) < TRIGGER_DEADZONE) rTrigger = 0;

        // Clamp to [0, 1] and convert to [0, 255]
        controllerData.L2 = (int) (Math.max(0, Math.min(1, lTrigger)) * 255);
        controllerData.R2 = (int) (Math.max(0, Math.min(1, rTrigger)) * 255);
    }

    /**
     * Get centered axis value from motion event
     */
    private float getCenteredAxis(MotionEvent event, int axis) {
        InputDevice.MotionRange range = event.getDevice().getMotionRange(axis, InputDevice.TOOL_TYPE_UNKNOWN);
        if (range == null) {
            return 0.0f;
        }

        float flat = range.getFlat();
        float value = event.getAxisValue(axis);

        // Apply flat zone
        if (Math.abs(value) > flat) {
            return value;
        }
        return 0.0f;
    }

    /**
     * Check if device is a gamepad
     */
    private boolean isGamepad(InputDevice device) {
        int sources = device.getSources();
        return (sources & InputDevice.SOURCE_GAMEPAD) != 0 || 
               (sources & InputDevice.SOURCE_JOYSTICK) != 0;
    }

    /**
     * Set controller connected state
     */
    public void setConnected(boolean connected) {
        this.isConnected = connected;
        if (!connected) {
            resetControllerState();
        }
    }

    /**
     * Check if external controller is connected
     */
    public boolean isControllerConnected() {
        return isConnected;
    }

    /**
     * Reset controller state to neutral position
     */
    private void resetControllerState() {
        // Reset buttons
        controllerData.A = 0;
        controllerData.B = 0;
        controllerData.X = 0;
        controllerData.Y = 0;
        controllerData.Dpad_UP = 0;
        controllerData.Dpad_Down = 0;
        controllerData.Dpad_Left = 0;
        controllerData.Dpad_Right = 0;
        controllerData.L1 = 0;
        controllerData.R1 = 0;
        controllerData.L2 = 0;
        controllerData.R2 = 0;
        controllerData.L3 = 0;
        controllerData.R3 = 0;
        controllerData.Option = 0;
        controllerData.Share = 0;
        controllerData.PS = 0;

        // Reset sticks to center
        controllerData.left_stick_x = 128;
        controllerData.left_stick_y = 128;
        controllerData.right_stick_x = 128;
        controllerData.right_stick_y = 128;
    }
}
