package com.venky97vp.android.backgroundnavigation;

/**
 * Created by venky on 21-08-2017.
 */

public class Place {
    public Place(String name, String description, String audio, String SSID) {
        this.name = name;
        this.description = description;
        this.audio = audio;
        this.SSID = SSID;
    }

    public String name;
    public String description;
    public String audio;
    public String SSID;
}
