package com.example.esenseapplication;

import android.os.Binder;

public class ObjectWrapperForBinder extends Binder {

    private final Object data;

    public ObjectWrapperForBinder(Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }
}