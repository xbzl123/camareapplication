package com.example.camareapplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class CustomObservableManager<T>{
    private HashMap<String, CustomObervable> customObservableHashMap =new HashMap<>();

    private static CustomObservableManager customObserable = null;
    public static CustomObservableManager getInstance(){
        if(customObserable == null){
            synchronized (CustomObservableManager.class){
                if (customObserable == null){
                    customObserable = new CustomObservableManager();
                }
            }
        }
        return customObserable;
    }

    public void addObserver(String tag,Observer o) {
        CustomObervable observable = customObservableHashMap.get(tag);
        if(observable == null){
            observable = new CustomObervable();
            customObservableHashMap.put(tag,observable);
        }

        observable.addObserver(o);
    }

    class CustomObervable extends Observable{

        public void notifyCustomObservers(T msg){
            setChanged();
            notifyObservers(msg);
        }
    }

    public void postMessage(String tag,T msg){
        CustomObervable observable = customObservableHashMap.get(tag);
        if (observable != null){
            observable.notifyCustomObservers(msg);
        }
    }

    public int getObservableHashMapSize() {
        return customObservableHashMap.size();
    }
}
