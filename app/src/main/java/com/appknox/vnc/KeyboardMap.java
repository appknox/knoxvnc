package com.appknox.vnc;

import android.view.KeyEvent;
import java.util.HashMap;

public class KeyboardMap
{
    private HashMap<Integer, String> keyMaps;
    private HashMap<String, Integer> keyEvents;
    public KeyboardMap() {
        keyMaps = new HashMap<Integer, String>() {};
        keyEvents = new HashMap<String, Integer>() {};

        keyEvents.put("65288", KeyEvent.KEYCODE_DEL);
        keyEvents.put("65293", KeyEvent.KEYCODE_ENTER);

        keyEvents.put("65361", KeyEvent.KEYCODE_DPAD_LEFT);
        keyEvents.put("65363", KeyEvent.KEYCODE_DPAD_RIGHT);
        keyEvents.put("65362", KeyEvent.KEYCODE_DPAD_UP);
        keyEvents.put("65364", KeyEvent.KEYCODE_DPAD_DOWN);

        keyMaps.put(32,  " ");
        keyMaps.put(33,  "!");
        keyMaps.put(34,  "\"");
        keyMaps.put(35,  "#");

        keyMaps.put(36,  "$");
        keyMaps.put(37,  "%");
        keyMaps.put(38,  "&");
        keyMaps.put(39,  "'");

        keyMaps.put(40,  "(");
        keyMaps.put(41,  ")");
        keyMaps.put(42,  "*");
        keyMaps.put(43,  "+");

        keyMaps.put(44,  ",");
        keyMaps.put(45,  "-");
        keyMaps.put(46,  ".");
        keyMaps.put(47,  "/");


        keyMaps.put(48,  "0");
        keyMaps.put(49,  "1");
        keyMaps.put(50,  "2");
        keyMaps.put(51,  "3");

        keyMaps.put(52,  "4");
        keyMaps.put(53,  "5");
        keyMaps.put(54,  "6");
        keyMaps.put(55,  "7");

        keyMaps.put(56,  "8");
        keyMaps.put(57,  "9");

        keyMaps.put(58,  ":");
        keyMaps.put(59,  ";");
        keyMaps.put(60,  "<");
        keyMaps.put(61,  "=");

        keyMaps.put(62,  ">");
        keyMaps.put(63,  "?");
        keyMaps.put(64,  "@");

        keyMaps.put(65,  "A");
        keyMaps.put(66,  "B");
        keyMaps.put(67,  "C");
        keyMaps.put(68,  "D");
        keyMaps.put(69,  "E");
        keyMaps.put(70,  "F");
        keyMaps.put(71,  "G");
        keyMaps.put(72,  "H");
        keyMaps.put(73,  "I");
        keyMaps.put(74,  "J");
        keyMaps.put(75,  "K");
        keyMaps.put(76,  "L");
        keyMaps.put(77,  "M");
        keyMaps.put(78,  "N");
        keyMaps.put(79,  "O");
        keyMaps.put(80,  "P");
        keyMaps.put(81,  "Q");
        keyMaps.put(82,  "R");
        keyMaps.put(83,  "S");
        keyMaps.put(84,  "T");
        keyMaps.put(85,  "U");
        keyMaps.put(86,  "V");
        keyMaps.put(87,  "W");
        keyMaps.put(88,  "X");
        keyMaps.put(89,  "Y");
        keyMaps.put(90,  "Z");

        keyMaps.put(97,   "a");
        keyMaps.put(98,   "b");
        keyMaps.put(99,   "c");
        keyMaps.put(100,  "d");
        keyMaps.put(101,  "e");
        keyMaps.put(102,  "f");
        keyMaps.put(103,  "g");
        keyMaps.put(104,  "h");
        keyMaps.put(105,  "i");
        keyMaps.put(106,  "j");
        keyMaps.put(107,  "k");
        keyMaps.put(108,  "l");
        keyMaps.put(109,  "m");
        keyMaps.put(110,  "n");
        keyMaps.put(111,  "o");
        keyMaps.put(112,  "p");
        keyMaps.put(113,  "q");
        keyMaps.put(114,  "r");
        keyMaps.put(115,  "s");
        keyMaps.put(116,  "t");
        keyMaps.put(117,  "u");
        keyMaps.put(118,  "v");
        keyMaps.put(119,  "w");
        keyMaps.put(120,  "x");
        keyMaps.put(121,  "y");
        keyMaps.put(122,  "z");

        keyMaps.put(96,   "`");
        keyMaps.put(123,  "{");
        keyMaps.put(124,  "|");
        keyMaps.put(125,  "}");
        keyMaps.put(126,  "~");
    }

    public HashMap<Integer, String> getKeyMaps() {
        return keyMaps;
    }

    public HashMap<String, Integer> getKeyEvents() {
        return keyEvents;
    }
}
