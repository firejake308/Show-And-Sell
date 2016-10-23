package com.insertcoolnamehere.showandsell.logic;

import android.media.Image;

import java.util.ArrayList;

public class Item {
    private static ArrayList<Item> items = new ArrayList<Item>();
    private static int numOfItems = 0;
    private String itemName;
    private String itemDescription;
    private int itemNumber;
    private String itemCondition;
    private double itemPrice;
    private Image itemPic;

    public Item(/*database input*/) {
        //add setters here
        /*
        setItemName();
        setItemDescription();
        setItemNumber();
        setItemCondition();
        setItemPrice();
        setItemPic();
         */

        itemNumber = numOfItems;
        numOfItems += 1;
        items.add(this);
    }


    /**
     * Getters and Setters
     * @return
     */
    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }
    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public int getItemNumber() {
        return itemNumber;
    }
    public void setItemNumber(int itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getItemCondition() {
        return itemCondition;
    }
    public void setItemCondition(String itemCondition) {
        this.itemCondition = itemCondition;
    }

    public double getItemPrice() {
        return itemPrice;
    }
    public void setItemPrice(double itemPrice) {
        this.itemPrice = itemPrice;
    }

    public Image getItemPic() {
        return itemPic;
    }
    public void setItemPic(Image itemPic) {
        this.itemPic = itemPic;
    }
}
