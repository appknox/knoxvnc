package com.appknox.vnc;

import android.util.Log;
import android.view.KeyEvent;

import java.io.IOException;

public class InputWrapperService {

    private final KeyboardMap keyboardMap;
    public InputWrapperService() {
        keyboardMap = new KeyboardMap();
    }

    private String retrieveKeyStroke(int keyId) {
        boolean status = keyboardMap.getKeyMaps().containsKey(keyId);
        if (status)
            return keyboardMap.getKeyMaps().get(keyId);

        return "";
    }

    private int retrieveEvent(String key) {
        boolean status = keyboardMap.getKeyEvents().containsKey(key);
        if (status) {
            return keyboardMap.getKeyEvents().get(key);
        }

        return 0;
    }

    public void processKey(int status, long keycode) {
        String code = this.retrieveKeyStroke((int) keycode);

        if (status == 1)
            sendKey(code);
    }

    public void processSystemEvent(int status, int keycode, InputService.InputContext context) {
        int keyCode = this.retrieveEvent(String.valueOf(keycode));

        if (status == 1)
            sendKeyEvent(keyCode, context);

    }

    private static void sendKey(String keycode) {
        try {
            Runtime.getRuntime().exec(new String[] { "su", "-c","input text " + "\"" + keycode + "\"" });
            Log.d("shell_input_key_send", String.valueOf(keycode));
        } catch (IOException ignored) {}
    }

    private static void sendKeyEvent(int keycode, InputService.InputContext inputContext) {
        try {
            if (inputContext.isKeyShiftDown)
                Runtime.getRuntime().exec(new String[] { "su", "-c","input keycombination 59 " + keycode });
            else
                Runtime.getRuntime().exec(new String[] { "su", "-c","input keyevent " + keycode });
            Log.d("KeyMapEv", String.valueOf(keycode));
        } catch (IOException ignored) {}
    }
}
