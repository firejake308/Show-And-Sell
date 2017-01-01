package com.insertcoolnamehere.showandsell.logic;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;

public class Item implements Serializable {
    /**
     * An ArrayList of all items, both approved and unapproved, in the selected default group
     */
    public static ArrayList<Item> managedGroupItems = new ArrayList<>();
    /**
     * An ArrayList of all approved items returned by the server in the selected default group
     */
    public static ArrayList<Item> browseGroupItems = new ArrayList<>();

    /**
     * ArrayList of all items that have been bookmarked by this user
     */
    public static ArrayList<Item> bookmarkedItems = new ArrayList<>();

    public static final int BROWSE = 0;
    public static final int BOOKMARK = 1;
    public static final int MANAGE = 2;

    private static int numOfItems = 0;

    public static Item getItem(String guid) {
        // search for item in browse items
        for (Item item: browseGroupItems) {
            if (item.guid.equals(guid))
                return item;
        }

        // search for item in manage group items
        for (Item item: managedGroupItems) {
            if (item.guid.equals(guid))
                return item;
        }

        // search for item in bookmarked items, whic h could be from any group
        for (Item item: bookmarkedItems) {
            if(item.guid.equals(guid))
                return item;
        }

        // if the item is not found, return null
        return null;
    }

    public static boolean hasBrowseItems() {
        return !browseGroupItems.isEmpty();
    }

    /**
     * Returns a list of all items in either the browse group or the managed group
     * @return
     */
    public static ArrayList<Item> getItemsList() {
        ArrayList<Item> list = new ArrayList<>();
        for(Item item: browseGroupItems) {
            list.add(item);
        }

        for(Item item: managedGroupItems) {
            list.add(item);
        }
        return list;
    }

    public static void clearItemsCache() {
        managedGroupItems.clear();
        browseGroupItems.clear();
        bookmarkedItems.clear();
    }

    private String name;
    private String description;
    private int number;
    private final String guid;
    private String condition;
    private double price;
    private Bitmap pic;
    private boolean approved;
    private int mItemType;

    public Item(String guid, int itemType) {
        this.guid = guid;
        mItemType = itemType;

        if(itemType == BOOKMARK) {
            if(!bookmarkedItems.contains(this))
                bookmarkedItems.add(this);
        } else if (itemType == BROWSE) {
            number = numOfItems;
            numOfItems += 1;

            // don't add this item to browse feed unless and until it is approved
        } else if(itemType == MANAGE) {
            // update managedGroupItems with new item
            if (managedGroupItems.contains(this)) {
                // remove old and insert new
                int i = managedGroupItems.indexOf(this);
                Log.d("Item", "Old item#" + i + " was " + managedGroupItems.get(i).isApproved());
                managedGroupItems.remove(i);
                managedGroupItems.add(i, this);
            } else {
                Log.d("Item", "new item going in");
                managedGroupItems.add(this);
            }
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

        if(mItemType != BROWSE)
            return;

        // update approved items list
        if(approved && !browseGroupItems.contains(this)) {
            browseGroupItems.add(this);
        } else if(!approved && browseGroupItems.contains(this)) {
            browseGroupItems.remove(this);
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
