package com.example.branislavnovak.chatapplication;

/**
 * Created by student on 4.6.2018.
 */

public class Crypto {
    public native String crypt(String message);

    static {
        System.loadLibrary("myJni");
    }
}

