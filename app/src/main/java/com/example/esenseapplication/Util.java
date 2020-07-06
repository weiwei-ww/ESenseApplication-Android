package com.example.esenseapplication;

import android.os.Handler;
import android.os.Message;

public final class Util {

    // send a message to handler
    public static void SendMessage(Handler handler, int what, Object obj){
        Message message = new Message();
        message.what = what;
        message.obj = obj;
        handler.sendMessage(message);
    }
}
