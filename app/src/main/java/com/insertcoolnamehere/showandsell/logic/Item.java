package com.insertcoolnamehere.showandsell.logic;

import android.graphics.Bitmap;
import android.media.Image;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Item implements Serializable{
    private static HashMap<String, Item> items = new HashMap<String, Item>();
    private static int numOfItems = 0;

    public static Item getItem(String guid) {
        return items.get(guid);
    }

    public static boolean hasItems() {
        return !items.isEmpty();
    }
    public static ArrayList<Item> getItemsList() {
        ArrayList<Item> list = new ArrayList<Item>();
        Item[] array = new Item[items.size()];
        Set<Map.Entry<String, Item>> entries = items.entrySet();
        for(Map.Entry<String, Item> entry: entries) {
            list.add(entry.getValue());
        }

        return list;
    }

    private String name;
    private String description;
    private int number;
    private final String guid;
    private String condition;
    private double price;
    private Bitmap pic;

    public Item(String guid) {
        this.guid = guid;

        number = numOfItems;
        numOfItems += 1;
        items.put(guid, this);
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

    public String getGuid() {
        return guid;
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
