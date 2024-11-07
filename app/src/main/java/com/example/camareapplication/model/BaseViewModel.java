package com.example.camareapplication.model;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class BaseViewModel implements LifecycleEventObserver {
    //监听activity的生命周期
    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
//        Log.e("onResume","name ="+ changeStringToUpperCase(event.name().toLowerCase().replace("_",""),2));
        Class clazz = this.getClass().getSuperclass();
        String methodName = changeStringToUpperCase(event.name().toLowerCase().replace("_", ""), 2);
        try {
            Method method = clazz.getDeclaredMethod(methodName, LifecycleOwner.class);
            method.setAccessible(true);
            method.invoke(this, source);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }


    private String changeStringToUpperCase(String name, int i) {
        ifNull(null);
        Log.e("ifNull","owner =");
        String beforePart = name.substring(0,i);
        String behindPart = name.substring(i+1);
        String changePart = name.substring(i,i+1);
        return beforePart + changePart.toUpperCase() + behindPart;
    }

    protected void onCreate(LifecycleOwner owner) {
        Log.e("onCreate","owner ="+owner);
    }

    protected void onStart(LifecycleOwner owner) {
        Log.e("onStart","owner ="+owner);
    }

    protected void onResume(LifecycleOwner owner) {
        Log.e("onResume","owner ="+owner);
    }

    protected void onPause(LifecycleOwner owner) {

    }

    protected void onStop(LifecycleOwner owner) {

    }

    protected void onDestroy(LifecycleOwner owner) {

    }

    @Nullable
    public Object ifNull(@NonNull Object object){
//        Method isNull = null;
//        try {
//            isNull = getClass().getDeclaredMethod("isNull", Object.class);
//            Log.e("ifNull","object ="+isNull.getName());
//        } catch (NoSuchMethodException e) {
            Log.e("ifNull","object = "+object);
//            e.printStackTrace();
//        }
        return object;
    }
}
