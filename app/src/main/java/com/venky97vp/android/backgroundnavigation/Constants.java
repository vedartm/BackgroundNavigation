package com.venky97vp.android.backgroundnavigation;

/**
 * Created by venky on 20-08-2017.
 */

public class Constants {
    public static final String ACTION_PLAY = "play";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_REPLAY = "replay";

    public static final String TAG = "venky97vp";

    public static final Place GNV = new Place("Gnana Vihar", "", "gnv", "", "", null);
    public static final Place VDV = new Place("Vidhya Vihar", "", "vdv", "", "", GNV);
    public static final Place CHITH = new Place("Chith Vihar", "", "chith", "Random", "venky97vp", VDV);
    public static final Place TIFAC = new Place("Tifac Core", "", "tifac", "", "", CHITH);
    public static final Place VKJ = new Place("Vishva Karma Joth", "", "vkj", "", "", TIFAC);
    public static final Place VV = new Place("Vidyuth Vihar", "", "vv", "", "", VKJ);
    public static final Place PLACE[] = {CHITH,TIFAC,GNV,VDV,VKJ,VV};
}
