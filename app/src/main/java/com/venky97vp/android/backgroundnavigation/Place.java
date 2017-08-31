package com.venky97vp.android.backgroundnavigation;

/**
 * Created by venky on 21-08-2017.
 */

public class Place {
    public Place(String name, String description, String audio, String SSID, String password, Place nextPlace) {
        this.name = name;
        this.description = description;
        this.audio = audio;
        this.SSID = SSID;
        this.nextPlace = nextPlace;
        this.password = password;
    }

    public String name;
    public String description;
    public String audio;
    public String SSID;
    public Place nextPlace;
    public String password;
}
