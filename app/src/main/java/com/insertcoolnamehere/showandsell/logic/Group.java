package com.insertcoolnamehere.showandsell.logic;

import java.util.ArrayList;

/**
 * Created by hassa on 5/6/2017.
 */

public class Group {

    public static ArrayList<Group> availableGroups = new ArrayList<>();
    public static ArrayList<Group> unselectedGroups = new ArrayList<>();

    public static Group getGroup(String id) {
        for(Group group: availableGroups) {
            if (group.id.equals(id))
                return group;
        }

        return null;
    }

    public static void clearGroups() {
        availableGroups.clear();
        unselectedGroups.clear();
    }

    private String name;
    private String id;
    private String pickupAddress;
    private String extraInfo;
    private double rating;

    public Group(String name, String id, String pickupAddress, String extraInfo, double rating) {
        this.name = name;
        this.id = id;
        this.pickupAddress = pickupAddress;
        this.extraInfo = extraInfo;
        this.rating = rating;

        availableGroups.add(this);
        unselectedGroups.add(this);
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public double getRating() {
        return rating;
    }
}
