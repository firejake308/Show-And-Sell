package com.insertcoolnamehere.showandsell.logic;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;

public class Item implements Serializable {
    /**
     * An ArrayList of all items, both approved and unapproved, in the group managed by this user
     */
    public static ArrayList<Item> managedGroupItems = new ArrayList<>();
    /**
     * An ArrayList of all approved items returned by the server in all groups
     */
    public static ArrayList<Item> allGroupsItems = new ArrayList<>();

    /**
     * An ArrayList of all items that have been bookmarked by this user
     */
    public static ArrayList<Item> bookmarkedItems = new ArrayList<>();
    /**
     * An ArrayList of all approved items in the currently viewed group
     */
    public static ArrayList<Item> currentGroupItems = new ArrayList<>();

    public static final int BROWSE = 0;
    public static final int BOOKMARK = 1;
    public static final int MANAGE = 2;
    public static final int ALL = 3;
    public static final int OTHER = 4;

    private static int numOfItems = 0;

    public static Item getItem(String guid) {
        // search for item in browse items
        for (Item item: allGroupsItems) {
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
        return !allGroupsItems.isEmpty();
    }

    /**
     * Returns a list of all items in either the browse group or the managed group
     * @return all items
     */
    public static ArrayList<Item> getItemsList() {
        ArrayList<Item> list = new ArrayList<>();
        for(Item item: allGroupsItems) {
            list.add(item);
        }

        for(Item item: managedGroupItems) {
            list.add(item);
        }
        return list;
    }

    public static void clearItemsCache() {
        managedGroupItems.clear();
        allGroupsItems.clear();
        currentGroupItems.clear();
        bookmarkedItems.clear();
    }

    public static void clearManagedItems() {
        managedGroupItems.clear();
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
    private String ownerId;
    private String groupId;

    public Item(String guid, int itemType) {
        this.guid = guid;
        mItemType = itemType;

        if(itemType == BOOKMARK) {
            if(!bookmarkedItems.contains(this))
                bookmarkedItems.add(this);
        } else if (itemType == ALL) {
            number = numOfItems;
            numOfItems += 1;

            if(!allGroupsItems.contains(this))
                allGroupsItems.add(this);
        } else if(itemType == MANAGE) {
            // update managedGroupItems with new item
            Log.d("Item", "new item going into manage group items");
            managedGroupItems.add(this);
        } else if (itemType == BROWSE) {
            if (!currentGroupItems.contains(this))
                currentGroupItems.add(this);
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
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
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
