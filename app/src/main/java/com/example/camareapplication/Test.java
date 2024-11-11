package com.example.camareapplication;

import com.example.camareapplication.Generic.Coordinate;
import com.example.camareapplication.Generic.MyPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class Test {
    public static void main(String[] args){
        CustomObservableManager customObservable = new CustomObservableManager();
        CustomObserver customObserver = new CustomObserver();
        CustomObserver customObserver1 = new CustomObserver();

        customObservable.addObserver("AAA", new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                System.out.println("AAA the result is "+arg+",the id is "+o.hashCode());

            }
        });
        customObservable.addObserver("BBB", new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                System.out.println("BBB the result is "+arg+",the id is "+o.hashCode());

            }
        });
        System.out.println("the size is "+customObservable.getObservableHashMapSize());

//        customObservable.postMessage("BBB","你好，这是觀察者模式");
        customObservable.postMessage("BBB",52013140);

        MyPosition<Integer,Double,Float> myPosition = new MyPosition<Integer,Double,Float>();
        myPosition.setY(520.0);
        myPosition.setZ(250.0f);
        System.out.println("myPosition the result is "+myPosition.getX());
        System.out.println("== jsonArray the result is "+myPosition);

        Coordinate coordinate = new Coordinate();
        coordinate.setX(1314);
        System.out.println("coordinate the result is "+coordinate.getX());

        Integer[] integers = fun(1,2,3,4,5,6);
        System.out.println("== integers the result is "+integers[2]);
        List<Integer> list = ArraysConvertList(integers);
        System.out.println("== list the result is "+ list.get(2));

    }

    public static <T> T[] fun(T...arg){
        return arg;
    }

    public static <T> List<T> ArraysConvertList(T...arg){
        List list = new ArrayList<>();
        for (T t: arg) {
            list.add(t);
        }
        return list;
    }
}
