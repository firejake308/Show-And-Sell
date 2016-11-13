package com.insertcoolnamehere.showandsell.logic;

import android.graphics.Bitmap;
import android.media.Image;

import java.util.ArrayList;

public class Item {
    private static ArrayList<Item> items = new ArrayList<Item>();
    private static int numOfItems = 0;
    private String name;
    private String description;
    private int number;
    private String condition;
    private double price;
    private Bitmap pic;

    public Item(/*database input*/) {
        //add setters here
        /*
        setName();
        setDescription();
        setNumber();
        setCondition();
        setPrice();
        setPic();
         */

        number = numOfItems;
        numOfItems += 1;
        items.add(this);
    }


    /**
     * Getters and Setters
     * @return
     */
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public int getNumber() {
        return number;
    }
    public void setNumber(int number) {
        this.number = number;
    }

    public String getCondition() {
        return condition;
    }
    public void setCondition(String condition) {
        this.condition = condition;
    }

    public double getPrice() {
        return price;
    }
    public void setPrice(double price) {
        this.price = price;
    }

    public Bitmap getPic() {
        return pic;
    }
    public void setPic(Bitmap pic) {
        this.pic = pic;
    }
}
