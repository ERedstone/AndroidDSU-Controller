package com.plawyue.AndroidDSU;

import android.content.Context;
import android.os.Handler;
import android.view.InputDevice;
import android.view.InputManager;

/**
 * ControllerDetectionListener detects external controller connection/disconnection events.
 */
public class ControllerDetectionListener implements InputManager.InputDeviceListener {
    private InputManager inputManager;
    private OnControllerStateChangedListener stateChangeListener;
    private boolean controllerConnected = false;

    public interface OnControllerStateChangedListener {
        void onControllerConnected(InputDevice device);
        void onControllerDisconnected();
    }

    public ControllerDetectionListener(Context context, OnControllerStateChangedListener listener) {
        this.inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        this.stateChangeListener = listener;
    }

    public void startListening() {
        if (inputManager != null) {
            inputManager.registerInputDeviceListener(this, new Handler());
            checkForConnectedController();
        }
    }

    public void stopListening() {
        if (inputManager != null) {
            inputManager.unregisterInputDeviceListener(this);
        }
    }

    private void checkForConnectedController() {
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice device = InputDevice.getDevice(deviceId);
            if (isGamepad(device) && !controllerConnected) {
                controllerConnected = true;
                if (stateChangeListener != null) {
                    stateChangeListener.onControllerConnected(device);
                }
                break;
            }
        }
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        if (isGamepad(device) && !controllerConnected) {
            controllerConnected = true;
            if (stateChangeListener != null) {
                stateChangeListener.onControllerConnected(device);
            }
        }
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        int[] deviceIds = InputDevice.getDeviceIds();
        boolean gamepadFound = false;
        for (int id : deviceIds) {
            InputDevice device = InputDevice.getDevice(id);
            if (isGamepad(device)) {
                gamepadFound = true;
                break;
            }
        }

        if (!gamepadFound && controllerConnected) {
            controllerConnected = false;
            if (stateChangeListener != null) {
                stateChangeListener.onControllerDisconnected();
            }
        }
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        if (isGamepad(device) && controllerConnected) {
            if (stateChangeListener != null) {
                stateChangeListener.onControllerConnected(device);
            }
        }
    }

    private boolean isGamepad(InputDevice device) {
        if (device == null) {
            return false;
        }
        int sources = device.getSources();
        return (sources & InputDevice.SOURCE_GAMEPAD) != 0 || 
               (sources & InputDevice.SOURCE_JOYSTICK) != 0;
    }

    public boolean isControllerConnected() {
        return controllerConnected;
    }
}
