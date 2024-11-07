package com.example.camareapplication;

import java.util.Observable;
import java.util.Observer;

public class CustomObserver implements Observer {
    @Override
    public void update(Observable o, Object arg) {
        System.out.println("the result is "+arg+",the id is "+o.hashCode());
    }
}
