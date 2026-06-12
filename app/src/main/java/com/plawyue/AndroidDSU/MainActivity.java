package com.plawyue.AndroidDSU;

import static com.google.android.material.internal.ContextUtils.getActivity;
import static com.plawyue.AndroidDSU.DsuComManage.MotorLevel;

import androidx.annotation.IntRange;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.shy.rockerview.MyRockerView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener, View.OnLongClickListener {
    private SensorManager sensorManager;
    private Sensor sensor;

    static double PI = 3.1415926535897932;
    Button L3,R3,OPkey,SHARE,Colorx;
    Boolean locked=false;
    Button button_A,button_B,button_DUP,button_DDOWN,button_DLEFT,button_DRight,button_PLUS,button_DEDUCE,BUTTON_home,L2,R2,Touch,button_X,button_Y;
    float Gyrosensitvity=0.9f;
    float Accsensitvity=1f;
    Boolean isEditMode=false;
    Dialog yourDialog;
    Dialog EdittextDialog;
    SeekBar yourDialogSeekBar;
    EditText Editbtntext;
    SeekBar accseekbar;


    static float METER_PER_SECOND_SQUARED_TO_G = (float) 9.8066;

    static DsuCtrlType gs_DsuCtrlUIData;        //UI关联的手柄信息

    private  DsuComManage mDsuComManage ;       //DSU协议管理器

    // ==================== ADDED FOR EXTERNAL CONTROLLER SUPPORT ====================
    private static final String TAG = "ExternalController";
    private boolean externalControllerConnected = false;
    private String connectedControllerName = null;
    private TextView controllerStatusText;
    private BroadcastReceiver controllerReceiver;
    private UsbManager usbManager;
    
    // Button mapping for external controller (maps gamepad buttons to DSU buttons)
    private Map<Integer, Integer> controllerButtonMap = new HashMap<>();
    
    // Joystick deadzone (0.0 to 1.0)
    private static final float JOYSTICK_DEADZONE = 0.15f;
    // ==================== END OF ADDED CODE ====================

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化相关
        UIInit();
        ButtonInit();
        GlobalVarInit();
        JoystickInit();
        WIFIDisplay();
        SensorInit();
        UIRefresh();
        
        // ==================== ADDED: Initialize external controller support ====================
        initExternalControllerSupport();
        // ==================== END OF ADDED CODE ====================
        
        //通信管理器创建
        mDsuComManage = new DsuComManage();
        mDsuComManage.StartServer();

    }
    
    // ==================== ADDED: External Controller Methods ====================
    
    private void initExternalControllerSupport() {
        // Initialize USB manager for USB-C telescopic controllers
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        
        // Setup controller status text view
        controllerStatusText = new TextView(this);
        controllerStatusText.setId(View.generateViewId());
        controllerStatusText.setTextColor(Color.parseColor("#00FF00"));
        controllerStatusText.setTextSize(12);
        
        // Add to toolbar or status area
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.addView(controllerStatusText, new Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ));
        }
        
        // Broadcast receiver for controller connection/disconnection
        controllerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_INPUT_DEVICE_CHANGED.equals(intent.getAction())) {
                    checkConnectedControllers();
                }
            }
        };
        
        // Register receiver
        IntentFilter filter = new IntentFilter(Intent.ACTION_INPUT_DEVICE_CHANGED);
        registerReceiver(controllerReceiver, filter);
        
        // Initial check for connected controllers
        checkConnectedControllers();
        
        // Initialize button mappings (maps standard Android gamepad buttons to DSU)
        initControllerButtonMappings();
    }
    
    private void initControllerButtonMappings() {
        // Standard button mappings
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_A, 0);      // Maps to A button
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_B, 1);      // Maps to B button
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_X, 2);      // Maps to X button
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_Y, 3);      // Maps to Y button
        
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_L1, 4);     // Maps to L1
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_R1, 5);     // Maps to R1
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_L2, 6);     // Maps to L2 (analog)
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_R2, 7);     // Maps to R2 (analog)
        
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_THUMBL, 8); // Left stick click (L3)
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_THUMBR, 9); // Right stick click (R3)
        
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_START, 10);  // Start/Options
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_SELECT, 11); // Select/Share
        
        controllerButtonMap.put(KeyEvent.KEYCODE_BUTTON_MODE, 12);   // Home/PS button
        
        controllerButtonMap.put(KeyEvent.KEYCODE_DPAD_UP, 13);       // D-pad Up
        controllerButtonMap.put(KeyEvent.KEYCODE_DPAD_DOWN, 14);     // D-pad Down
        controllerButtonMap.put(KeyEvent.KEYCODE_DPAD_LEFT, 15);     // D-pad Left
        controllerButtonMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, 16);    // D-pad Right
    }
    
    private void checkConnectedControllers() {
        int[] deviceIds = InputDevice.getDeviceIds();
        boolean foundController = false;
        
        for (int id : deviceIds) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null && (device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
                foundController = true;
                connectedControllerName = device.getName();
                Log.d(TAG, "Controller connected: " + connectedControllerName);
                
                // Show toast on connection
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, 
                        getString(R.string.controller_connected, connectedControllerName), 
                        Toast.LENGTH_SHORT).show();
                    updateControllerStatusUI(true);
                });
                break;
            }
        }
        
        if (!foundController && externalControllerConnected) {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, 
                    getString(R.string.controller_disconnected), 
                    Toast.LENGTH_SHORT).show();
                updateControllerStatusUI(false);
            });
        }
        
        externalControllerConnected = foundController;
        if (!foundController) {
            connectedControllerName = null;
        }
    }
    
    private void updateControllerStatusUI(boolean connected) {
        if (controllerStatusText != null) {
            if (connected && connectedControllerName != null) {
                controllerStatusText.setText("🎮 " + connectedControllerName);
                controllerStatusText.setTextColor(Color.parseColor("#00FF00"));
            } else {
                controllerStatusText.setText(getString(R.string.no_controller));
                controllerStatusText.setTextColor(Color.parseColor("#888888"));
            }
        }
    }
    
    private void handleControllerJoystick(MotionEvent event) {
        if (locked) return; // Don't process if locked
        
        // Get left stick values (range: -1.0 to 1.0)
        float leftX = event.getAxisValue(MotionEvent.AXIS_X);
        float leftY = event.getAxisValue(MotionEvent.AXIS_Y);
        
        // Get right stick values
        float rightX = event.getAxisValue(MotionEvent.AXIS_Z);
        float rightY = event.getAxisValue(MotionEvent.AXIS_RZ);
        
        // Apply deadzone
        leftX = applyDeadzone(leftX);
        leftY = applyDeadzone(leftY);
        rightX = applyDeadzone(rightX);
        rightY = applyDeadzone(rightY);
        
        // Convert from -1..1 range to 0..255 range (DSU expects 0-255)
        int leftStickX = (int) ((leftX + 1) * 127.5f);
        int leftStickY = (int) ((leftY + 1) * 127.5f);
        int rightStickX = (int) ((rightX + 1) * 127.5f);
        int rightStickY = (int) ((rightY + 1) * 127.5f);
        
        // Clamp values
        leftStickX = Math.min(255, Math.max(0, leftStickX));
        leftStickY = Math.min(255, Math.max(0, leftStickY));
        rightStickX = Math.min(255, Math.max(0, rightStickX));
        rightStickY = Math.min(255, Math.max(0, rightStickY));
        
        // Update DSU data
        gs_DsuCtrlUIData.left_stick_x = leftStickX;
        gs_DsuCtrlUIData.left_stick_y = leftStickY;
        gs_DsuCtrlUIData.right_stick_x = rightStickX;
        gs_DsuCtrlUIData.right_stick_y = rightStickY;
        
        // Handle analog triggers (L2/R2)
        float l2Value = event.getAxisValue(MotionEvent.AXIS_LTRIGGER);
        float r2Value = event.getAxisValue(MotionEvent.AXIS_RTRIGGER);
        
        if (l2Value > 0.1f) {
            int l2Int = (int) (l2Value * 255);
            gs_DsuCtrlUIData.L2 = Math.min(255, l2Int);
        } else {
            gs_DsuCtrlUIData.L2 = 0;
        }
        
        if (r2Value > 0.1f) {
            int r2Int = (int) (r2Value * 255);
            gs_DsuCtrlUIData.R2 = Math.min(255, r2Int);
        } else {
            gs_DsuCtrlUIData.R2 = 0;
        }
    }
    
    private float applyDeadzone(float value) {
        if (Math.abs(value) < JOYSTICK_DEADZONE) {
            return 0f;
        }
        // Rescale remaining range
        float sign = value > 0 ? 1f : -1f;
        return sign * ((Math.abs(value) - JOYSTICK_DEADZONE) / (1f - JOYSTICK_DEADZONE));
    }
    
    private void handleControllerButtonPress(int keyCode, boolean pressed) {
        if (locked && keyCode != KeyEvent.KEYCODE_BACK) return;
        
        // Map the key code to DSU button action
        Integer mappedAction = controllerButtonMap.get(keyCode);
        
        if (mappedAction != null) {
            switch (mappedAction) {
                case 0: // A button
                    gs_DsuCtrlUIData.A = pressed ? 255 : 0;
                    updateButtonVisual(button_A, pressed);
                    break;
                case 1: // B button
                    gs_DsuCtrlUIData.B = pressed ? 255 : 0;
                    updateButtonVisual(button_B, pressed);
                    break;
                case 2: // X button
                    gs_DsuCtrlUIData.X = pressed ? 255 : 0;
                    updateButtonVisual(button_X, pressed);
                    break;
                case 3: // Y button
                    gs_DsuCtrlUIData.Y = pressed ? 255 : 0;
                    updateButtonVisual(button_Y, pressed);
                    break;
                case 4: // L1
                    gs_DsuCtrlUIData.L1 = pressed ? 255 : 0;
                    break;
                case 5: // R1
                    gs_DsuCtrlUIData.R1 = pressed ? 255 : 0;
                    updateButtonVisual(button_PLUS, pressed);
                    break;
                case 6: // L2 (analog, but we handle digital press)
                    if (pressed) gs_DsuCtrlUIData.L2 = 255;
                    else if (gs_DsuCtrlUIData.L2 > 0 && event.getAxisValue(MotionEvent.AXIS_LTRIGGER) == 0) gs_DsuCtrlUIData.L2 = 0;
                    updateButtonVisual(L2, pressed);
                    break;
                case 7: // R2 (analog)
                    if (pressed) gs_DsuCtrlUIData.R2 = 255;
                    else if (gs_DsuCtrlUIData.R2 > 0 && event.getAxisValue(MotionEvent.AXIS_RTRIGGER) == 0) gs_DsuCtrlUIData.R2 = 0;
                    updateButtonVisual(R2, pressed);
                    break;
                case 8: // L3
                    gs_DsuCtrlUIData.L3 = pressed ? 1 : 0;
                    updateButtonVisual(L3, pressed);
                    break;
                case 9: // R3
                    gs_DsuCtrlUIData.R3 = pressed ? 1 : 0;
                    updateButtonVisual(R3, pressed);
                    break;
                case 10: // Start/Options
                    gs_DsuCtrlUIData.Option = pressed ? 1 : 0;
                    updateButtonVisual(OPkey, pressed);
                    break;
                case 11: // Select/Share
                    gs_DsuCtrlUIData.Share = pressed ? 1 : 0;
                    updateButtonVisual(SHARE, pressed);
                    break;
                case 12: // Home/PS
                    gs_DsuCtrlUIData.PS = pressed ? 1 : 0;
                    updateButtonVisual(BUTTON_home, pressed);
                    break;
                case 13: // D-pad Up
                    gs_DsuCtrlUIData.Dpad_UP = pressed ? 255 : 0;
                    updateButtonVisual(button_DUP, pressed);
                    break;
                case 14: // D-pad Down
                    gs_DsuCtrlUIData.Dpad_Down = pressed ? 255 : 0;
                    updateButtonVisual(button_DDOWN, pressed);
                    break;
                case 15: // D-pad Left
                    gs_DsuCtrlUIData.Dpad_Left = pressed ? 255 : 0;
                    updateButtonVisual(button_DLEFT, pressed);
                    break;
                case 16: // D-pad Right
                    gs_DsuCtrlUIData.Dpad_Right = pressed ? 255 : 0;
                    updateButtonVisual(button_DRight, pressed);
                    break;
            }
        }
        
        // Handle volume buttons as controller buttons (existing functionality)
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            gs_DsuCtrlUIData.A = pressed ? 255 : 0;
            updateButtonVisual(button_A, pressed);
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            gs_DsuCtrlUIData.B = pressed ? 255 : 0;
            updateButtonVisual(button_B, pressed);
        }
    }
    
    private void updateButtonVisual(Button button, boolean pressed) {
        if (button != null) {
            if (pressed) {
                button.setBackground(getDrawable(R.drawable.pressed));
                button.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).start();
            } else {
                button.setBackground(getDrawable(R.drawable.unpress));
                button.animate().scaleX(1f).scaleY(1f).setDuration(50).start();
            }
        }
    }
    
    // ==================== END OF ADDED EXTERNAL CONTROLLER CODE ====================

    /******************************** PART 0  初始化  ************************************/
    public void UIInit()
    {
        Window window =this.getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
        window.setStatusBarColor(Color.TRANSPARENT);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.senseseekbar, (ViewGroup) findViewById(R.id.elementseek));
        Button yourDialogButton = (Button) layout.findViewById(R.id.your_dialog_button);
        yourDialogSeekBar = layout.findViewById(R.id.your_dialog_seekbar);
        accseekbar=layout.findViewById(R.id.seekBar);
        TextView acctext = layout.findViewById(R.id.acctext);
        accseekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                layout.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                acctext.setText(getString(R.string.Acctips)+ i/10.0);
                Accsensitvity= (float) (i/10.0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        yourDialog = new Dialog(this);
        yourDialog.setContentView(layout);
        yourDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        TextView lighttext = layout.findViewById(R.id.lighttext);
        SeekBar.OnSeekBarChangeListener yourSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //add code here
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //add code here

            }

            @Override
            public void onProgressChanged(SeekBar seekBark, int progress, boolean fromUser) {
                //add code here
                layout.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                lighttext.setText(getString(R.string.gyrotips)+ + progress/10.0);
                Gyrosensitvity= (float) (progress/10.0);
            }
        };
        yourDialogButton.setOnClickListener(this);
        yourDialogSeekBar.setOnSeekBarChangeListener(yourSeekBarListener);
        yourDialog.setCanceledOnTouchOutside(false);
        yourDialog.setCancelable(false);
        LayoutInflater inflater2 = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout2 = inflater2.inflate(R.layout.editlayout, (ViewGroup) findViewById(R.id.elementseek2));
        EdittextDialog = new Dialog(this);
        EdittextDialog.setContentView(layout2);
        EdittextDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        Button yourDialogButton2 = (Button) layout2.findViewById(R.id.okbtn);
        Button canclebtn=layout2.findViewById(R.id.cancelbtn);
        canclebtn.setOnClickListener(this);
        Editbtntext=layout2.findViewById(R.id.btncontent);
        yourDialogButton2.setOnClickListener(this);
        EdittextDialog.setCanceledOnTouchOutside(false);
        EdittextDialog.setCancelable(false);
        getWindow().setNavigationBarColor(ContextCompat.getColor(MainActivity.this, R.color.background));
        VirtualTouchpadView touchpad = findViewById(R.id.touchpad);
        touchpad.setTouchDataCallback(data -> {
            // 这里处理触摸数据（例如发送到网络）
            gs_DsuCtrlUIData.TouchStruce = Arrays.copyOf(data,12);
        });
    }
    
    // ==================== OVERRIDE METHODS FOR CONTROLLER INPUT ====================
    
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        // Check if this is a gamepad/joystick event
        if ((event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
            
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                handleControllerJoystick(event);
                return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        
        // Check if this is a gamepad button
        if ((event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            boolean isActionDown = event.getAction() == KeyEvent.ACTION_DOWN;
            
            // Provide haptic feedback for button presses
            if (isActionDown && !locked) {
                getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            } else if (!isActionDown && !locked) {
                getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE);
            }
            
            handleControllerButtonPress(keyCode, isActionDown);
            return true;
        }
        
        // Handle back button for unlocking
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (locked) {
                // Simulate unlock when back is pressed while locked
                View lockView = findViewById(R.id.LOCK);
                if (lockView != null) {
                    lockView.performLongClick();
                }
                return true;
            }
        }
        
        return super.dispatchKeyEvent(event);
    }
    
    // ==================== END OF OVERRIDE METHODS ====================
    
    public void ButtonInit()
    {
        TextView Locktips=findViewById(R.id.LOCK);
        Locktips.setOnClickListener(this);
        Locktips.setOnLongClickListener(this);
        Locktips.setOnTouchListener(this);
        Touch=findViewById(R.id.Button_touchlock);
        Touch.setOnClickListener(this);
        button_A=findViewById(R.id.button_A);
        button_X=findViewById(R.id.Square);
        button_Y=findViewById(R.id.Tri);
        button_B=findViewById(R.id.button_B);
        button_DUP=findViewById(R.id.button_dUp);
        button_DDOWN=findViewById(R.id.button_Ddown);
        button_DLEFT=findViewById(R.id.button_Dleft);
        button_DRight=findViewById(R.id.button_Dright);
        button_PLUS=findViewById(R.id.button_PLUS);
        button_DEDUCE=findViewById(R.id.button_DEDUCE);
        BUTTON_home=findViewById(R.id.button_home);
        L3=findViewById(R.id.buttonL3);
        R3=findViewById(R.id.buttonR3);
        SHARE=findViewById(R.id.buttonShare);
        OPkey=findViewById(R.id.buttonOption);
        button_Y.setOnTouchListener(this);
        button_X.setOnTouchListener(this);
        L3.setOnTouchListener(this);
        R3.setOnTouchListener(this);
        SHARE.setOnTouchListener(this);
        OPkey.setOnTouchListener(this);
        L2=findViewById(R.id.button_L2);
        R2=findViewById(R.id.button_R2);
        L2.setOnTouchListener(this);
        R2.setOnTouchListener(this);
        button_A.setOnTouchListener(this);
        button_B.setOnTouchListener(this);
        button_DUP.setOnTouchListener(this);
        button_DDOWN.setOnTouchListener(this);
        button_DLEFT.setOnTouchListener(this);
        button_DRight.setOnTouchListener(this);
        button_PLUS.setOnTouchListener(this);
        button_DEDUCE.setOnTouchListener(this);
        BUTTON_home.setOnTouchListener(this);

        button_Y.setOnClickListener(this);
        button_X.setOnClickListener(this);
        L3.setOnClickListener(this);
        R3.setOnClickListener(this);
        SHARE.setOnClickListener(this);
        OPkey.setOnClickListener(this);
        L2.setOnClickListener(this);
        R2.setOnClickListener(this);
        button_A.setOnClickListener(this);
        button_B.setOnClickListener(this);
        button_DUP.setOnClickListener(this);
        button_DDOWN.setOnClickListener(this);
        button_DLEFT.setOnClickListener(this);
        button_DRight.setOnClickListener(this);
        button_PLUS.setOnClickListener(this);
        button_DEDUCE.setOnClickListener(this);
        BUTTON_home.setOnClickListener(this);
        loadbutton();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister controller receiver to prevent memory leaks
        if (controllerReceiver != null) {
            try {
                unregisterReceiver(controllerReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver wasn't registered
            }
        }
    }
    
    public void GlobalVarInit()
    {
        gs_DsuCtrlUIData = new DsuCtrlType();
        gs_DsuCtrlUIData.right_stick_x=128;
        gs_DsuCtrlUIData.left_stick_y=128;
        gs_DsuCtrlUIData.left_stick_x=128;
        gs_DsuCtrlUIData.right_stick_y=128;
        SharedPreferences userInfo = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        SharedPreferences.Editor editor = userInfo.edit();//获取Editor
        if(userInfo.contains("sensisavin")==false){
            editor.putFloat("sensisavin",0.9F);
            editor.commit();
        }
        if(userInfo.contains("accsensisavin")==false){
            editor.putFloat("accsensisavin",1F);
            editor.commit();
        }
        Accsensitvity=userInfo.getFloat("accsensisavin",1f);
        Gyrosensitvity=userInfo.getFloat("sensisavin",0.9F);
        accseekbar.setProgress((int)(Accsensitvity*10));
        yourDialogSeekBar.setProgress((int) (Gyrosensitvity*10));
    }
    public void WIFIDisplay()
    {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = (ipAddress & 0xff) + "." + (ipAddress>>8 & 0xff) + "." + (ipAddress>>16 & 0xff) + "." + (ipAddress >> 24 & 0xff);
        Toolbar mtoorbar=findViewById(R.id.toolbar);
        mtoorbar.setOnMenuItemClickListener(onMenuItemClick);
        mtoorbar.inflateMenu(R.menu.menu);
        mtoorbar.setFitsSystemWindows(true);
        mtoorbar.setTitle(ip);
        mtoorbar.setTitleTextColor(getResources().getColor(R.color.spical));
        TextView mtext = findViewById(R.id.Serveripaddress);
        mtext.setText(ip);
        Drawable drawable = mtoorbar.getOverflowIcon();
        if (drawable != null) {
            DrawableCompat.setTint(drawable, getResources().getColor(R.color.spical));
            mtoorbar.setOverflowIcon(drawable);
        }


    }

    public void SensorInit()
    {

        registerReceiver(mBatInfoReveiver, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        SensorEventListener sensorListener = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
            public void onSensorChanged(SensorEvent event) {
                Boolean conver=true;
                Boolean noconver=false;
                float accX = -Accsensitvity*event.values[2] * METER_PER_SECOND_SQUARED_TO_G / 100;
                float accY =  -Accsensitvity*event.values[0] * METER_PER_SECOND_SQUARED_TO_G / 100;
                float accZ =Accsensitvity*event.values[1] * METER_PER_SECOND_SQUARED_TO_G / 100;
                gs_DsuCtrlUIData.accelX= accY;
                gs_DsuCtrlUIData.accelY=accX;
                gs_DsuCtrlUIData.accelZ=accZ;


            }
        };
        SensorEventListener gyrolinster=new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {

                gs_DsuCtrlUIData.gyroR= (float) (radToDeg(sensorEvent.values[1]) * Gyrosensitvity);
                gs_DsuCtrlUIData.gyroY= (float) (-radToDeg(sensorEvent.values[2]) *Gyrosensitvity);
                gs_DsuCtrlUIData.gyroP= (float) (radToDeg(sensorEvent.values[0]) * Gyrosensitvity);
            }
            double radToDeg(double radians) {
                return radians * 180 / PI;
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        sensorManager.registerListener(sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(gyrolinster,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_GAME);


    }


    /******************************** PART 1  电池信息传递  ************************************/
    private void onBatteryInfoReceiver(int intLevel, int intScale) {
        // TODO Auto-generated method stub
        int percent = intLevel*100/ intScale;
        if(percent > 98){
            gs_DsuCtrlUIData.DeviceBattery=DsuCtrlType.BAT_FULL_OR_ALMOST;
        }
        else if(percent>90){
            gs_DsuCtrlUIData.DeviceBattery=DsuCtrlType.BAT_HIGH;
        }
        else if(percent >80){
            gs_DsuCtrlUIData.DeviceBattery=DsuCtrlType.BAT_HIGH;
        }
        else if(percent>40){
            gs_DsuCtrlUIData.DeviceBattery=DsuCtrlType.BAT_MEDIUM;
        }
        else if(percent>20){
            gs_DsuCtrlUIData.DeviceBattery=DsuCtrlType.BAT_LOW;
        }
        else {
            gs_DsuCtrlUIData.DeviceBattery=DsuCtrlType.BAT_DYING;
        }
    };
    //创建BroadcastReceiver
    private BroadcastReceiver mBatInfoReveiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //如果捕捉到的Action是ACTION_BATTERY_CHANGED则运行onBatteryInforECEIVER()
            if(intent.ACTION_BATTERY_CHANGED.equals(action))
            {
                //获得当前电量
                int intLevel = intent.getIntExtra("level",0);
                //获得手机总电量
                int intScale = intent.getIntExtra("scale",100);
                // 在下面会定义这个函数，显示手机当前电量
                onBatteryInfoReceiver(intLevel, intScale);
            }
        }
    };
    /******************************** PART 2  按钮控制相关代码  ************************************/
    private Button buttontemp;
    private String Buttonnamex;
    private void Inputbox(Button mbutton,String Buttonname){
        EdittextDialog.show();
        View viewx = findViewById(R.id.linearLayout);
        Bitmap bitmap2 = Bitmap.createBitmap(viewx.getWidth(), viewx.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap bitmap=Bitmap.createBitmap(viewx.getWidth(),viewx.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvasback=new Canvas(bitmap);
        canvasback.drawColor(ContextCompat.getColor(MainActivity.this, R.color.background));
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap2);
        viewx.draw(canvas);
        bitmap2=blur(bitmap2,25);
        bitmap2=mergeBitmap(bitmap,bitmap2);
        Animation alphaAnimation = new AlphaAnimation(0f, 1f);
        alphaAnimation.setDuration(100);//设置动画持续时间为500毫秒
        alphaAnimation.setFillAfter(false);//设置动画结束后保持当前的位置（即不返回到动画开始前的位置）
        ImageView backg=findViewById(R.id.imageView);
        backg.setAnimation(alphaAnimation);
        backg.setImageBitmap(bitmap2);
        backg.setVisibility(View.VISIBLE);
        Editbtntext.setText(mbutton.getText());
        buttontemp=mbutton;
        Buttonnamex=Buttonname;

    }
    private void loadbutton(){
        loadbuttontext(button_A,"ButtonA");
        loadbuttontext(button_B,"ButtonB");
        loadbuttontext(button_X,"ButtonX");
        loadbuttontext(button_Y,"ButtonY");
        loadbuttontext(button_DUP,"ButtonDup");
        loadbuttontext(button_DDOWN,"ButtonDdown");
        loadbuttontext(button_DLEFT,"ButtonDleft");
        loadbuttontext(button_DRight,"ButtonDright");
        loadbuttontext(button_PLUS,"Buttonplus");
        loadbuttontext(button_DEDUCE,"Buttondeduce");
        loadbuttontext(BUTTON_home,"Buttonhome");
        loadbuttontext(L2,"ButtonL2");
        loadbuttontext(R2,"ButtonR2");
        loadbuttontext(R3,"ButtonR3");
        loadbuttontext(L3,"ButtonL3");
        loadbuttontext(OPkey,"ButtonOP");
        loadbuttontext(SHARE,"ButtonSh");
    }
    private void loadbuttontext(Button mbutton,String Buttonname){
        SharedPreferences userInfo = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String text;
        text=userInfo.getString(Buttonname,"NonexistFLAG@");
        String Dont="NonexistFLAG@";
        if(text.equals(Dont)){
            return;
        }
        mbutton.setText(text);
    }
    Timer timerx = new Timer();
    @Override
    public boolean onLongClick(View view) {
        if(view.getId()==R.id.LOCK){
            if(locked){
                getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                Touch.setText(getString(R.string.lockbutton));
                ImageView backg=findViewById(R.id.imageView);
                Animation alphaAnimation = new AlphaAnimation(1f, 0f);
                alphaAnimation.setDuration(100);//设置动画持续时间为500毫秒
                alphaAnimation.setFillAfter(false);//设置动画结束后保持当前的位置（即不返回到动画开始前的位置）
                backg.setAnimation(alphaAnimation);
                backg.setVisibility(View.INVISIBLE);
                timerx.cancel();
                ConstraintLayout mview= findViewById(R.id.main);
                mview.scrollTo(0,0);
                Paint paint = new Paint();
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(1);
                paint.setColorFilter(new ColorMatrixColorFilter(cm));
                getWindow().getDecorView().setLayerType(View.LAYER_TYPE_HARDWARE, paint);
//                findViewById(R.id.sensitivelay).setVisibility(View.VISIBLE);
                findViewById(R.id.LOCK).startAnimation(alphaAnimation);
                findViewById(R.id.LOCK).setVisibility(View.INVISIBLE);
//                findViewById(R.id.editmode).setVisibility(View.VISIBLE);
                Toolbar mtool=findViewById(R.id.toolbar);
                mtool.setVisibility(View.VISIBLE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                locked=!locked;
            }
        }
        return false;
    }
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(view.getId()==R.id.LOCK&&motionEvent.getAction() == MotionEvent.ACTION_DOWN){
            view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(200).start();

        }else if(view.getId()==R.id.LOCK&&motionEvent.getAction() == MotionEvent.ACTION_UP){
            view.animate().scaleX(1f).scaleY(1f).setDuration(200).start();

        }
        if(locked==false){
            if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

                switch (view.getId()){
                    case R.id.button_A: button_A.setBackground(getDrawable(R.drawable.pressed));gs_DsuCtrlUIData.A=255;break;
                    case R.id.button_B: button_B.setBackground(getDrawable(R.drawable.pressed)); gs_DsuCtrlUIData.B=255;break;
                    case R.id.Square: button_X.setBackground(getDrawable(R.drawable.pressed)); gs_DsuCtrlUIData.X=255;break;
                    case R.id.Tri: button_Y.setBackground(getDrawable(R.drawable.pressed)); gs_DsuCtrlUIData.Y=255;break;
                    case R.id.button_dUp: button_DUP.setBackground(getDrawable(R.drawable.pressed)); gs_DsuCtrlUIData.Dpad_UP=255;break;
                    case R.id.button_Ddown: button_DDOWN.setBackground(getDrawable(R.drawable.pressed)); gs_DsuCtrlUIData.Dpad_Down=255;break;
                    case R.id.button_Dleft: button_DLEFT.setBackground(getDrawable(R.drawable.pressed)); gs_DsuCtrlUIData.Dpad_Left=255;break;
                    case R.id.button_Dright:button_DRight.setBackground(getDrawable(R.drawable.pressed)); gs_DsuCtrlUIData.Dpad_Right=255;break;
                    case R.id.button_PLUS:button_PLUS.setBackground(getDrawable(R.drawable.pressed)); gs_DsuCtrlUIData.R1=255;break;
                    case R.id.button_DEDUCE:button_DEDUCE.setBackground(getDrawable(R.drawable.pressed)); gs_DsuCtrlUIData.L1=255;break;
                    case R.id.button_home:BUTTON_home.setBackground(getDrawable(R.drawable.pressed));gs_DsuCtrlUIData.PS=1;break;
                    case R.id.button_L2:L2.setBackground(getDrawable(R.drawable.pressed));gs_DsuCtrlUIData.L2=255;break;
                    case R.id.button_R2:R2.setBackground(getDrawable(R.drawable.pressed));gs_DsuCtrlUIData.R2=255;break;
                    case R.id.buttonR3:R3.setBackground(getDrawable(R.drawable.pressed));gs_DsuCtrlUIData.R3=1;break;
                    case R.id.buttonL3:L3.setBackground(getDrawable(R.drawable.pressed));gs_DsuCtrlUIData.L3=1;break;
                    case R.id.buttonOption:OPkey.setBackground(getDrawable(R.drawable.pressed));gs_DsuCtrlUIData.Option=1;break;
                    case R.id.buttonShare:SHARE.setBackground(getDrawable(R.drawable.pressed));gs_DsuCtrlUIData.Share=1;break;
                }
            }
            else if( motionEvent.getAction() == MotionEvent.ACTION_UP){
                view.animate().scaleX(1).scaleY(1).setDuration(100).start();
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE);
                switch (view.getId()){
                    case R.id.button_A: button_A.setBackground(getDrawable(R.drawable.unpress));gs_DsuCtrlUIData.A=0;break;
                    case R.id.button_B: button_B.setBackground(getDrawable(R.drawable.unpress)); gs_DsuCtrlUIData.B=0;break;
                    case R.id.Square: button_X.setBackground(getDrawable(R.drawable.unpress)); gs_DsuCtrlUIData.X=0;break;
                    case R.id.Tri: button_Y.setBackground(getDrawable(R.drawable.unpress)); gs_DsuCtrlUIData.Y=0;break;
                    case R.id.button_dUp: button_DUP.setBackground(getDrawable(R.drawable.unpress)); gs_DsuCtrlUIData.Dpad_UP=0;break;
                    case R.id.button_Ddown: button_DDOWN.setBackground(getDrawable(R.drawable.unpress)); gs_DsuCtrlUIData.Dpad_Down=0;break;
                    case R.id.button_Dleft: button_DLEFT.setBackground(getDrawable(R.drawable.unpress)); gs_DsuCtrlUIData.Dpad_Left=0;break;
                    case R.id.button_Dright: button_DRight.setBackground(getDrawable(R.drawable.unpress)); gs_DsuCtrlUIData.Dpad_Right=0;break;
                    case R.id.button_PLUS:button_PLUS.setBackground(getDrawable(R.drawable.unpress)); gs_DsuCtrlUIData.R1=0;break;
                    case R.id.button_DEDUCE:button_DEDUCE.setBackground(getDrawable(R.drawable.unpress)); gs_DsuCtrlUIData.L1=0;break;
                    case R.id.button_home:BUTTON_home.setBackground(getDrawable(R.drawable.unpress));gs_DsuCtrlUIData.PS=0;break;
                    case R.id.button_L2:L2.setBackground(getDrawable(R.drawable.unpress));gs_DsuCtrlUIData.L2=0;break;
                    case R.id.button_R2:R2.setBackground(getDrawable(R.drawable.unpress));gs_DsuCtrlUIData.R2=0;break;
                    case R.id.buttonR3:R3.setBackground(getDrawable(R.drawable.unpress));gs_DsuCtrlUIData.R3=0;break;
                    case R.id.buttonL3:L3.setBackground(getDrawable(R.drawable.unpress));gs_DsuCtrlUIData.L3=0;break;
                    case R.id.buttonOption:OPkey.setBackground(getDrawable(R.drawable.unpress));gs_DsuCtrlUIData.Option=0;break;
                    case R.id.buttonShare:SHARE.setBackground(getDrawable(R.drawable.unpress));gs_DsuCtrlUIData.Share=0;break;
                }
            }
        }
        return false;
    }
    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.your_dialog_button){
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            SharedPreferences userInfo = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
            SharedPreferences.Editor editor = userInfo.edit();//获取Editor
            ImageView backg=findViewById(R.id.imageView);
            backg.setVisibility(View.INVISIBLE);
            yourDialog.cancel();

            editor.putFloat("sensisavin", Gyrosensitvity);
            editor.putFloat("accsensisavin",Accsensitvity);
            editor.commit();

            Toast.makeText(MainActivity.this,getString(R.string.gainsaved), Toast.LENGTH_LONG).show();

        }
        if(view.getId()==R.id.Button_touchlock){
            if(locked==true){

            }else{
                getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CONFIRM);

                //锁状态
                Touch.setText(getString(R.string.lockbutton));
                ImageView backg=findViewById(R.id.imageView);
                Button lockbtn=findViewById(R.id.Button_touchlock);

                View viewx = findViewById(R.id.linearLayout);
                Bitmap bitmap2 = Bitmap.createBitmap(viewx.getWidth(), viewx.getHeight(), Bitmap.Config.ARGB_8888);
                Bitmap bitmap=Bitmap.createBitmap(viewx.getWidth(),viewx.getHeight(),Bitmap.Config.ARGB_8888);
                Canvas canvasback=new Canvas(bitmap);
                canvasback.drawColor(ContextCompat.getColor(MainActivity.this, R.color.background));
                Canvas canvas = new Canvas();
                canvas.setBitmap(bitmap2);
                viewx.draw(canvas);
                bitmap2=blur(bitmap2,25);
                bitmap2=mergeBitmap(bitmap,bitmap2);
                bitmap2=handleImageEffect(bitmap2,0.2f);


                Animation alphaAnimation = new AlphaAnimation(0f, 1f);
                alphaAnimation.setDuration(100);//设置动画持续时间为500毫秒
                alphaAnimation.setFillAfter(false);//设置动画结束后保持当前的位置（即不返回到动画开始前的位置）
                backg.setAnimation(alphaAnimation);
                backg.setImageBitmap(bitmap2);
                backg.setVisibility(View.VISIBLE);

                Paint paint = new Paint();
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                paint.setColorFilter(new ColorMatrixColorFilter(cm));
                getWindow().getDecorView().setLayerType(View.LAYER_TYPE_HARDWARE, paint);
//                findViewById(R.id.editmode).setVisibility(View.INVISIBLE);
                findViewById(R.id.LOCK).setVisibility(View.VISIBLE);
                findViewById(R.id.LOCK).startAnimation(alphaAnimation);
//                findViewById(R.id.sensitivelay).setVisibility(View.INVISIBLE);
                Toolbar mtool=findViewById(R.id.toolbar);
                mtool.setVisibility(View.INVISIBLE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                locked=!locked;
            }

        }
        if(view.getId()==R.id.okbtn){
            String result =Editbtntext.getText().toString();
            buttontemp.setText(result);
            SharedPreferences userInfo = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
            SharedPreferences.Editor editor = userInfo.edit();//获取Editor
            editor.putString(Buttonnamex,result);
            editor.commit();
            ImageView backg=findViewById(R.id.imageView);
            backg.setVisibility(View.INVISIBLE);
            EdittextDialog.cancel();
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        }
        if(view.getId()==R.id.cancelbtn){
            ImageView backg=findViewById(R.id.imageView);
            backg.setVisibility(View.INVISIBLE);
            EdittextDialog.cancel();
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        if(isEditMode&&locked==false) {
            switch (view.getId()) {
                case R.id.button_A:Inputbox(button_A,"ButtonA");break;
                case R.id.button_B:Inputbox(button_B,"ButtonB");break;
                case R.id.Square: Inputbox(button_X,"ButtonX");break;
                case R.id.Tri: Inputbox(button_Y,"ButtonY");break;
                case R.id.button_dUp: Inputbox(button_DUP,"ButtonDup");break;
                case R.id.button_Ddown: Inputbox(button_DDOWN,"ButtonDdown");break;
                case R.id.button_Dleft: Inputbox(button_DLEFT,"ButtonDleft");break;
                case R.id.button_Dright:Inputbox(button_DRight,"ButtonDright");break;
                case R.id.button_PLUS:Inputbox(button_PLUS,"Buttonplus");break;
                case R.id.button_DEDUCE:Inputbox(button_DEDUCE,"Buttondeduce");break;
                case R.id.button_home:Inputbox(BUTTON_home,"Buttonhome");break;
                case R.id.button_L2:Inputbox(L2,"ButtonL2");break;
                case R.id.button_R2:Inputbox(R2,"ButtonR2");break;
                case R.id.buttonR3:Inputbox(R3,"ButtonR3");break;
                case R.id.buttonL3:Inputbox(L3,"ButtonL3");break;
                case R.id.buttonOption:Inputbox(OPkey,"ButtonOP");break;
                case R.id.buttonShare:Inputbox(SHARE,"ButtonSh");break;
            }
        }
    }
    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event) {
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        switch (keyCode) {
// 音量减小
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                button_A.setBackground(getDrawable(R.drawable.pressed));gs_DsuCtrlUIData.A=255;
                return true;
// 音量增大
            case KeyEvent.KEYCODE_VOLUME_UP:
                button_B.setBackground(getDrawable(R.drawable.pressed)); gs_DsuCtrlUIData.B=255;
                return true;
            case KeyEvent.KEYCODE_BACK:
                if(locked){
                    return true;}
        }
        return super.onKeyDown (keyCode, event);
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event){
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                button_A.setBackground(getDrawable(R.drawable.unpress));gs_DsuCtrlUIData.A=0;
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                button_B.setBackground(getDrawable(R.drawable.unpress)); gs_DsuCtrlUIData.B=0;
                return true;
        }
        return super.onKeyUp (keyCode, event);
    }
    /******************************** PART 3  界面特效相关代码  ************************************/
    boolean edit_flag=true;     //编辑模式标识
    public static Bitmap handleImageEffect(Bitmap bitmap,float lum) {
        //传进来的bitmap默认不能修改  所以再创建一个bm
        Bitmap bm = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        //画布
        Canvas canvas = new Canvas(bm);
        //抗锯齿
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //修改亮度
        ColorMatrix lumMatrix = new ColorMatrix();
        //r g b a    1 表示全不透明
        lumMatrix.setScale(lum, lum, lum, 1);
        //组合Matrix
        ColorMatrix imageMatrix = new ColorMatrix();
        imageMatrix.postConcat(lumMatrix);
        //为画笔设置颜色过滤器
        paint.setColorFilter(new ColorMatrixColorFilter(imageMatrix));
        //在canvas上照着bitmap画
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bm;
    }
    private Bitmap blur(Bitmap bmp, @IntRange(from = 1, to = 25) int radius) {
        RenderScript rs = RenderScript.create(this);
        Allocation allocFromBmp = Allocation.createFromBitmap(rs, bmp);
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, allocFromBmp.getElement());
        blur.setInput(allocFromBmp);
        blur.setRadius(radius);
        blur.forEach(allocFromBmp);
        allocFromBmp.copyTo(bmp);
        rs.destroy();
        return bmp;
    }
    public static Bitmap mergeBitmap(Bitmap backBitmap, Bitmap frontBitmap) {

        if (backBitmap == null || backBitmap.isRecycled()
                || frontBitmap == null || frontBitmap.isRecycled()) {
            return null;
        }
        Bitmap bitmap = backBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Rect baseRect  = new Rect(0, 0, backBitmap.getWidth(), backBitmap.getHeight());
        Rect frontRect = new Rect(0, 0, frontBitmap.getWidth(), frontBitmap.getHeight());
        canvas.drawBitmap(frontBitmap, frontRect, baseRect, null);
        return bitmap;
    }

    private Toolbar.OnMenuItemClickListener onMenuItemClick = new Toolbar.OnMenuItemClickListener() {


        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {


            switch (menuItem.getItemId()) {
                case R.id.sensitivelay:
                    getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                    yourDialog.show();

                    View viewx = findViewById(R.id.linearLayout);
                    Bitmap bitmap2 = Bitmap.createBitmap(viewx.getWidth(), viewx.getHeight(), Bitmap.Config.ARGB_8888);
                    Bitmap bitmap = Bitmap.createBitmap(viewx.getWidth(), viewx.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvasback = new Canvas(bitmap);
                    canvasback.drawColor(ContextCompat.getColor(MainActivity.this, R.color.background));
                    Canvas canvas = new Canvas();
                    canvas.setBitmap(bitmap2);
                    viewx.draw(canvas);
                    bitmap2 = blur(bitmap2, 25);
                    bitmap2 = mergeBitmap(bitmap, bitmap2);
                    Animation alphaAnimation = new AlphaAnimation(0f, 1f);
                    alphaAnimation.setDuration(100);//设置动画持续时间为500毫秒
                    alphaAnimation.setFillAfter(false);//设置动画结束后保持当前的位置（即不返回到动画开始前的位置）
                    ImageView backg = findViewById(R.id.imageView);
                    backg.setAnimation(alphaAnimation);
                    backg.setImageBitmap(bitmap2);
                    backg.setVisibility(View.VISIBLE);
                    break;
                case R.id.app_bar_switch:
                    if (edit_flag) {
                        isEditMode = true;
                        menuItem.setTitle(getString(R.string.Menu_Editsave));

                        Toast.makeText(MainActivity.this, getString(R.string.editmodeent), Toast.LENGTH_LONG).show();
                        edit_flag = !edit_flag;
                    } else {
                        isEditMode = false;
                        menuItem.setTitle(getString(R.string.Menu_Edit));
                        Toast.makeText(MainActivity.this, getString(R.string.btnSaved), Toast.LENGTH_LONG).show();
                        edit_flag = !edit_flag;
                    }
                    break;
                case R.id.joystick_sw:
                    LinearLayout Joysticklayout=findViewById(R.id.joystick_layout);
                    if( Joysticklayout.getVisibility()==View.VISIBLE){
                        TranslateAnimation   mHiddenAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                                0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                                -1.0f);
                        mHiddenAction.setDuration(200);
                        mHiddenAction.setInterpolator(new AccelerateInterpolator());
                        Joysticklayout.startAnimation(mHiddenAction);
                        Joysticklayout.setVisibility(View.GONE);
                    }else{
                        TranslateAnimation mShowAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                                -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
                        mShowAction.setDuration(200);
                        mShowAction.setInterpolator(new AccelerateInterpolator());
                        Joysticklayout.startAnimation(mShowAction);
                        Joysticklayout.setVisibility(View.VISIBLE);
                    }
                    break;
                case R.id.touchpad_sw:
                    VirtualTouchpadView touchpad = findViewById(R.id.touchpad);
                    if( touchpad.getVisibility()==View.VISIBLE){
                        TranslateAnimation   mHiddenAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                                0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                                -1.0f);
                        mHiddenAction.setDuration(200);
                        mHiddenAction.setInterpolator(new AccelerateInterpolator());
                        touchpad.startAnimation(mHiddenAction);
                        touchpad.setVisibility(View.GONE);
                    }else{
                        TranslateAnimation mShowAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                                -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
                        mShowAction.setDuration(200);
                        mShowAction.setInterpolator(new AccelerateInterpolator());
                        touchpad.startAnimation(mShowAction);
                        touchpad.setVisibility(View.VISIBLE);
                    }
                    break;
                case R.id.about_menu:
                    Toast.makeText(MainActivity.this,getString(R.string.Content_about),Toast.LENGTH_LONG).show();
                    break;
            }
            return true;

        }
    };
    //oled防烧代码
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler(){
        public void handleMessage(Message message){
            Random r = new Random();
            ConstraintLayout mview= findViewById(R.id.main);
            switch (message.what){
                case 1:
                    mview.scrollTo( r.nextInt(18)+3,0);
                    break;
                case 2:
                    mview.scrollTo(0,r.nextInt(18)+3);
                    break;
                case 3:
                    mview.scrollTo( r.nextInt(21) - 20,0);
                    break;
                case 4:
                    mview.scrollTo(0,r.nextInt(21) - 20);
                    break;
            }
        }
    };

    /******************************** PART 4  摇杆相关代码  ************************************/

    public void JoystickInit(){
        final double[] angle_all = new double[1];
        final int[] distance = new int[1];
        MyRockerView myRockerView=findViewById(R.id.rocker_view);
        myRockerView.setOnShakeListener(MyRockerView.DirectionMode.DIRECTION_8, new MyRockerView.OnShakeListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void direction(MyRockerView.Direction direction) {
                switch (direction){
                    case DIRECTION_LEFT:
                        break;
                    case DIRECTION_RIGHT:
                        break;
                    case DIRECTION_UP:
                        break;
                    case DIRECTION_DOWN:
                        break;
                    case DIRECTION_UP_LEFT:
                        break;
                    case DIRECTION_UP_RIGHT:
                        break;
                    case DIRECTION_DOWN_LEFT:
                        break;
                    case DIRECTION_DOWN_RIGHT:
                        break;
                    case DIRECTION_CENTER:
                        calculator_joystick(0,0);
                        break;
                }
            }

            @Override
            public void onFinish() {

            }
        });
        myRockerView.setOnAngleChangeListener(new MyRockerView.OnAngleChangeListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void angle(double angle) {
                calculator_joystick(distance[0], angle);
            }

            @Override
            public void onFinish() {

            }
        });
        myRockerView.setOnDistanceLevelListener(new MyRockerView.OnDistanceLevelListener() {
            @Override
            public void onDistanceLevel(int level) {
                distance[0] =level;
                calculator_joystick(level, angle_all[0]);

            }
        });
        MyRockerView myRockerView_2=findViewById(R.id.rocker_view_2);
        myRockerView_2.setOnShakeListener(MyRockerView.DirectionMode.DIRECTION_8, new MyRockerView.OnShakeListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void direction(MyRockerView.Direction direction) {
                switch (direction){
                    case DIRECTION_LEFT:
                        break;
                    case DIRECTION_RIGHT:
                        break;
                    case DIRECTION_UP:
                        break;
                    case DIRECTION_DOWN:
                        break;
                    case DIRECTION_UP_LEFT:
                        break;
                    case DIRECTION_UP_RIGHT:
                        break;
                    case DIRECTION_DOWN_LEFT:
                        break;
                    case DIRECTION_DOWN_RIGHT:
                        break;
                    case DIRECTION_CENTER:
                        calculator_joystick_2(0,0);
                        break;
                }
            }

            @Override
            public void onFinish() {

            }
        });
        myRockerView_2.setOnAngleChangeListener(new MyRockerView.OnAngleChangeListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void angle(double angle) {
                calculator_joystick_2(distance[0], angle);
            }

            @Override
            public void onFinish() {

            }
        });
        myRockerView_2.setOnDistanceLevelListener(new MyRockerView.OnDistanceLevelListener() {
            @Override
            public void onDistanceLevel(int level) {
                distance[0] =level;
                calculator_joystick_2(level, angle_all[0]);

            }
        });
    }
    public void calculator_joystick(int distance,double angle){
        if(locked){
            return;
        }
        double radians = Math.toRadians(angle);
        double joystick_x=(int) (Math.sin(radians)*distance+128);
        double joystick_y=(int) (Math.cos(radians)*distance+128);
        gs_DsuCtrlUIData.left_stick_x= (int) joystick_x;
        gs_DsuCtrlUIData.left_stick_y= (int) joystick_y;
    }
    public void calculator_joystick_2(int distance,double angle){
        if(locked){
            return;
        }
        double radians = Math.toRadians(angle);
        double joystick_x=(int) (Math.sin(radians)*distance+128);
        double joystick_y=(int) (Math.cos(radians)*distance+128);
        gs_DsuCtrlUIData.right_stick_x= (int) joystick_x;
        gs_DsuCtrlUIData.right_stick_y= (int) joystick_y;
    }

    /*********************************Part 5 界面刷新 ***************************************/
    @SuppressLint("HandlerLeak")
    private final Handler uirefresh_handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ImageView ContStateUI = findViewById(R.id.ConnectState_image);
            switch (msg.what) {

                case 1:
                   // getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                    ContStateUI.setBackground(getResources().getDrawable(R.drawable.baseline_cloud_off_24));
                    break;
                case 0:
                  //  getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                    ContStateUI.setBackground(getResources().getDrawable(R.drawable.baseline_cloud_done_24));
                    break;
            }
        }
    };

    public void UIRefresh()
    {
        new Thread() {
            @Override
            public void run() {
                while (true)
                {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }


                    if(DsuComManage.ServerConnectFlag == 0)
                    {
                        Message msg = Message.obtain();
                        msg.what = 1;
                        uirefresh_handler.sendMessage(msg);
                    }else
                    {
                        Message msg = Message.obtain();
                        msg.what = 0;
                        uirefresh_handler.sendMessage(msg);
                    }
                    if(MotorLevel != 0)
                    {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(55, MotorLevel));
                        }else
                        {
                            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(new long[]{0, MotorLevel * 4 }, 0);
                        }
                    }else{
                        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).cancel();
                    }

                }


            }
        }.start();
    }
}
