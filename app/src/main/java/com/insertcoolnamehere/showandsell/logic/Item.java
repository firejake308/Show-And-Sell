package com.insertcoolnamehere.showandsell.logic;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Item implements Serializable {

    public static ArrayList<Item> allItems = new ArrayList<>();
    public static ArrayList<Item> approvedItems = new ArrayList<>();
    public static ArrayList<Item> itemsToShow = new ArrayList<>();
    /**
     * TODO deprecate this hash map
     */
    private static HashMap<String, Item> items = new HashMap<String, Item>();
    private static int numOfItems = 0;

    private static boolean showUnapproved = false;

    public static void setShowUnapproved(boolean show) {
        // if this changes, we have to re-check our list of items to show
        if(showUnapproved != show) {
            // first reset <code>showUnapproved</code>
            showUnapproved = show;
            // then clear list
            itemsToShow.clear();
            // then recheck all items to make sure they're approved
            for(Item item: allItems) {
                attemptAddToItemsToShow(item);
            }
        }
    }

    public static Item getItem(String guid) {
        return items.get(guid);
    }

    public static boolean hasItems() {
        return !items.isEmpty();
    }
    public static ArrayList<Item> getItemsList() {
        ArrayList<Item> list = new ArrayList<Item>();
        Set<Map.Entry<String, Item>> entries = items.entrySet();
        for(Map.Entry<String, Item> entry: entries) {
            list.add(entry.getValue());
        }

        return list;
    }

    public static ArrayList<Item> getApprovedItems() {
        ArrayList<Item> list = new ArrayList<Item>();
        Set<Map.Entry<String, Item>> entries = items.entrySet();
        for(Map.Entry<String, Item> entry: entries) {
            if(entry.getValue().isApproved())
                list.add(entry.getValue());
        }

        return list;
    }

    public static void attemptAddToItemsToShow(Item item) {
        if(showUnapproved) {
            if(!itemsToShow.contains(item))
                itemsToShow.add(item);
        } else {
            Log.d("Item", "Adding only approved items");
            if(item.isApproved() && itemsToShow.contains(item))
                itemsToShow.add(item);
        }
    }

    private String name;
    private String description;
    private int number;
    private final String guid;
    private String condition;
    private double price;
    private Bitmap pic;
    private boolean approved;

    public Item(String guid) {
        this.guid = guid;

        number = numOfItems;
        numOfItems += 1;
        items.put(guid, this);
        if(allItems.contains(this))
            allItems.add(this);

        // debug
        if(number == 2) {
            setApproved(true);
        }

        attemptAddToItemsToShow(this);
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

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;

        // update approved items list
        if(approved && !approvedItems.contains(this)) {
            approvedItems.add(this);
            attemptAddToItemsToShow(this);
        } else if(!approved && approvedItems.contains(this)) {
            approvedItems.remove(this);
        }
    }

    @Override
    public String toString() {
        return this.guid;
    }

    @Override
    public boolean equals(Object other) {
        if(this.guid.equals(((Item)other).guid))
            return true;
        else
            return false;
    }
}
