package com.plawyue.dsucontroller;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.util.HashMap;
import java.util.Map;

public class ExternalControllerManager {
    private Context context;
    private UsbManager usbManager;
    private Map<Integer, ControllerState> controllerStates;
    private ControllerListener controllerListener;
    private static final String ACTION_USB_PERMISSION = "com.example.USB_PERMISSION";
    
    // Controller button mappings
    public static final int BUTTON_A = KeyEvent.KEYCODE_BUTTON_A;
    public static final int BUTTON_B = KeyEvent.KEYCODE_BUTTON_B;
    public static final int BUTTON_X = KeyEvent.KEYCODE_BUTTON_X;
    public static final int BUTTON_Y = KeyEvent.KEYCODE_BUTTON_Y;
    public static final int BUTTON_L1 = KeyEvent.KEYCODE_BUTTON_L1;
    public static final int BUTTON_R1 = KeyEvent.KEYCODE_BUTTON_R1;
    public static final int BUTTON_L2 = KeyEvent.KEYCODE_BUTTON_L2;
    public static final int BUTTON_R2 = KeyEvent.KEYCODE_BUTTON_R2;
    public static final int BUTTON_START = KeyEvent.KEYCODE_BUTTON_START;
    public static final int BUTTON_SELECT = KeyEvent.KEYCODE_BUTTON_SELECT;
    
    // D-pad keys
    public static final int DPAD_UP = KeyEvent.KEYCODE_DPAD_UP;
    public static final int DPAD_DOWN = KeyEvent.KEYCODE_DPAD_DOWN;
    public static final int DPAD_LEFT = KeyEvent.KEYCODE_DPAD_LEFT;
    public static final int DPAD_RIGHT = KeyEvent.KEYCODE_DPAD_RIGHT;
    
    public interface ControllerListener {
        void onControllerConnected(int controllerId);
        void onControllerDisconnected(int controllerId);
        void onButtonPressed(int controllerId, int button);
        void onButtonReleased(int controllerId, int button);
        void onJoystickMoved(int controllerId, int axis, float value);
        void onDpadPressed(int controllerId, int direction);
        void onDpadReleased(int controllerId, int direction);
    }
    
    private class ControllerState {
        boolean[] buttons = new boolean[20];
        float[] axes = new float[6];
        boolean isConnected = false;
    }
    
    public ExternalControllerManager(Context context) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.controllerStates = new HashMap<>();
        
        registerUSBReceiver();
        detectConnectedControllers();
    }
    
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            handleControllerConnected(device.getDeviceId());
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && isGamepad(device)) {
                    requestUSBPermission(device);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    handleControllerDisconnected(device.getDeviceId());
                }
            }
        }
    };
    
    private void registerUSBReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(usbReceiver, filter);
    }
    
    private void requestUSBPermission(UsbDevice device) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
            context, 0, new Intent(ACTION_USB_PERMISSION), 
            PendingIntent.FLAG_IMMUTABLE
        );
        usbManager.requestPermission(device, permissionIntent);
    }
    
    private boolean isGamepad(UsbDevice device) {
        // Check if device is a gamepad/controller
        int deviceClass = device.getDeviceClass();
        int deviceSubclass = device.getDeviceSubclass();
        
        // USB HID class
        return deviceClass == 3 || 
               // Some controllers might use vendor-specific class
               (deviceClass == 0 && deviceSubclass == 0);
    }
    
    private void detectConnectedControllers() {
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (isGamepad(device)) {
                if (usbManager.hasPermission(device)) {
                    handleControllerConnected(device.getDeviceId());
                } else {
                    requestUSBPermission(device);
                }
            }
        }
    }
    
    private void handleControllerConnected(int deviceId) {
        ControllerState state = new ControllerState();
        state.isConnected = true;
        controllerStates.put(deviceId, state);
        
        if (controllerListener != null) {
            controllerListener.onControllerConnected(deviceId);
        }
    }
    
    private void handleControllerDisconnected(int deviceId) {
        controllerStates.remove(deviceId);
        
        if (controllerListener != null) {
            controllerListener.onControllerDisconnected(deviceId);
        }
    }
    
    public boolean handleGenericMotionEvent(MotionEvent event) {
        // Handle joystick/trigger movements
        int deviceId = event.getDeviceId();
        
        if (!controllerStates.containsKey(deviceId)) {
            // Auto-detect and add controller
            InputDevice device = event.getDevice();
            if (device != null) {
                int sources = device.getSources();
                if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
                    handleControllerConnected(deviceId);
                }
            }
        }
        
        if (controllerStates.containsKey(deviceId) && 
            controllerStates.get(deviceId).isConnected) {
            
            ControllerState state = controllerStates.get(deviceId);
            
            // Process joystick axes
            float axisX = event.getAxisValue(MotionEvent.AXIS_X);
            float axisY = event.getAxisValue(MotionEvent.AXIS_Y);
            float axisZ = event.getAxisValue(MotionEvent.AXIS_Z);
            float axisRZ = event.getAxisValue(MotionEvent.AXIS_RZ);
            float axisLTRIGGER = event.getAxisValue(MotionEvent.AXIS_LTRIGGER);
            float axisRTRIGGER = event.getAxisValue(MotionEvent.AXIS_RTRIGGER);
            float axisHATX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
            float axisHATY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
            
            // Left stick
            if (axisX != 0 || axisY != 0) {
                state.axes[0] = axisX;
                state.axes[1] = axisY;
                if (controllerListener != null) {
                    controllerListener.onJoystickMoved(deviceId, 0, axisX); // Left stick X
                    controllerListener.onJoystickMoved(deviceId, 1, axisY); // Left stick Y
                }
            }
            
            // Right stick
            if (axisZ != 0 || axisRZ != 0) {
                state.axes[2] = axisZ;
                state.axes[3] = axisRZ;
                if (controllerListener != null) {
                    controllerListener.onJoystickMoved(deviceId, 2, axisZ);  // Right stick X
                    controllerListener.onJoystickMoved(deviceId, 3, axisRZ); // Right stick Y
                }
            }
            
            // Triggers
            if (axisLTRIGGER != state.axes[4]) {
                state.axes[4] = axisLTRIGGER;
                if (controllerListener != null) {
                    controllerListener.onJoystickMoved(deviceId, 4, axisLTRIGGER); // L2
                }
            }
            if (axisRTRIGGER != state.axes[5]) {
                state.axes[5] = axisRTRIGGER;
                if (controllerListener != null) {
                    controllerListener.onJoystickMoved(deviceId, 5, axisRTRIGGER); // R2
                }
            }
            
            // D-pad
            handleDpadInput(deviceId, axisHATX, axisHATY);
            
            return true;
        }
        return false;
    }
    
    public boolean handleKeyEvent(KeyEvent event) {
        int deviceId = event.getDeviceId();
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        
        // Auto-detect and register controller
        if (!controllerStates.containsKey(deviceId)) {
            InputDevice device = event.getDevice();
            if (device != null) {
                int sources = device.getSources();
                if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
                    (sources & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) {
                    handleControllerConnected(deviceId);
                }
            }
        }
        
        if (controllerStates.containsKey(deviceId) && 
            controllerStates.get(deviceId).isConnected &&
            (event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            
            ControllerState state = controllerStates.get(deviceId);
            
            switch (action) {
                case KeyEvent.ACTION_DOWN:
                    state.buttons[keyCode % state.buttons.length] = true;
                    if (controllerListener != null) {
                        controllerListener.onButtonPressed(deviceId, keyCode);
                    }
                    return true;
                    
                case KeyEvent.ACTION_UP:
                    state.buttons[keyCode % state.buttons.length] = false;
                    if (controllerListener != null) {
                        controllerListener.onButtonReleased(deviceId, keyCode);
                    }
                    return true;
            }
        }
        return false;
    }
    
    private void handleDpadInput(int deviceId, float hatX, float hatY) {
        int dpadState = 0;
        
        if (hatX < -0.5f) dpadState = DPAD_LEFT;
        else if (hatX > 0.5f) dpadState = DPAD_RIGHT;
        else if (hatY < -0.5f) dpadState = DPAD_UP;
        else if (hatY > 0.5f) dpadState = DPAD_DOWN;
        
        if (controllerListener != null) {
            if (dpadState != 0) {
                controllerListener.onDpadPressed(deviceId, dpadState);
            } else {
                // Release all dpad directions
                controllerListener.onDpadReleased(deviceId, DPAD_UP);
                controllerListener.onDpadReleased(deviceId, DPAD_DOWN);
                controllerListener.onDpadReleased(deviceId, DPAD_LEFT);
                controllerListener.onDpadReleased(deviceId, DPAD_RIGHT);
            }
        }
    }
    
    public void setControllerListener(ControllerListener listener) {
        this.controllerListener = listener;
    }
    
    public void cleanup() {
        try {
            context.unregisterReceiver(usbReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }
        controllerStates.clear();
    }
}
