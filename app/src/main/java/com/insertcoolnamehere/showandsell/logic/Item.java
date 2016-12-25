package com.insertcoolnamehere.showandsell.logic;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Item implements Serializable {
    private static ArrayList<Item> allItems = new ArrayList<>();
    private static ArrayList<Item> approvedItems = new ArrayList<>();

    public static ArrayList<Item> itemsToShow = new ArrayList<>();
    public static ArrayList<Item> bookmarkedItems = new ArrayList<>();
    /**
     * TODO deprecate this hash map after we write a better search method for the array list
     */
    private static HashMap<String, Item> items = new HashMap<>();
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
        ArrayList<Item> list = new ArrayList<>();
        Set<Map.Entry<String, Item>> entries = items.entrySet();
        for(Map.Entry<String, Item> entry: entries) {
            list.add(entry.getValue());
        }

        return list;
    }

    public static ArrayList<Item> getApprovedItems() {
        ArrayList<Item> list = new ArrayList<>();
        Set<Map.Entry<String, Item>> entries = items.entrySet();
        for(Map.Entry<String, Item> entry: entries) {
            if(entry.getValue().isApproved())
                list.add(entry.getValue());
        }

        return list;
    }

    private static void attemptAddToItemsToShow(Item item) {
        if(showUnapproved) {
            if(!itemsToShow.contains(item))
                itemsToShow.add(item);
            else {
                // remove old version of item
                int i = itemsToShow.indexOf(item);
                itemsToShow.remove(i);
                // insert at same position
                itemsToShow.add(i, item);
            }

            // sort unapproved items to top of list
            Log.d("Item", "about to start sorting");
            ArrayList<Item> unapproved = new ArrayList<>();
            ArrayList<Item> approved = new ArrayList<>();
            for(int i = 0; i < itemsToShow.size(); i++) {
                if(itemsToShow.get(i).isApproved()) {
                    approved.add(itemsToShow.get(i));
                } else {
                    unapproved.add(itemsToShow.get(i));
                }
            }

            itemsToShow.clear();
            for(int i = 0; i < unapproved.size(); i++) {
                itemsToShow.add(unapproved.get(i));
            }
            for(int i = 0; i < approved.size(); i++) {
                itemsToShow.add(approved.get(i));
            }
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

    public Item(String guid, boolean isBookmark) {
        this.guid = guid;

        if(isBookmark) {
            if(!bookmarkedItems.contains(this))
                bookmarkedItems.add(this);
        } else {
            number = numOfItems;
            numOfItems += 1;
            items.put(guid, this);

            // update allItems with new item
            if (allItems.contains(this)) {
                // remove old and insert new
                int i = allItems.indexOf(this);
                Log.d("Item", "Old item#" + i + " was " + allItems.get(i).isApproved());
                allItems.remove(i);
                allItems.add(i, this);
            } else {
                Log.d("Item", "new item going in");
                allItems.add(this);
            }

            attemptAddToItemsToShow(this);
        }
    }


    /**
     * Getters and Setters
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
        return this.name + " #" + this.guid.substring(0,4);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof  Item && this.guid.equals(((Item)other).guid);
    }
}
