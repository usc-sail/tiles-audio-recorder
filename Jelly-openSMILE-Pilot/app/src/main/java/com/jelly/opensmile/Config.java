package com.jelly.opensmile;

import java.util.ArrayList;

/**
 * Created by tiantianfeng on 10/18/17.
 */

public class Config {

    public String[] assets = {
            //"liveinput_android_new.conf",
            "liveinput_android_radar.conf",
            "BufferModeRb.conf.inc",
            "BufferModeLive.conf.inc",
            "BufferModeRbLag.conf.inc",
            "BufferMode.conf.inc",
            "messages.conf.inc",
            "features.conf.inc",
            "jelly_vad.conf",
            "message_vad.conf.inc",
            "jelly_arousal_valence.conf",
            //"jelly_arousal_valence.lld.conf.inc",
            //"e_jelly_arousal_valence.lld.conf.inc",
            //"jelly_arousal_valence.func.conf.inc",
            "FrameModeFunctionals.conf.inc",
            "FrameModeFunctionalsLive.conf.inc",
            //"e_jelly_arousal_valence.func.conf.inc",
            "eGeMAPSv01a_core.func.conf.inc",
            "GeMAPSv01a_core.lld.conf.inc",
            "GeMAPSv01a_core.func.conf.inc",
            "eGeMAPSv01a_core.lld.conf.inc",
            "emobase_live4.conf",
            "jelly_vad_opensource.conf",
            "message_vad_pitch.conf.inc",
            "jelly_vad_pitch.conf",
            "jelly_vad_NA.conf",
            "emobase_live4_no_wav.conf"

    };

    //public String mainConf = "liveinput_android.conf";

    public String mainConf = "liveinput_android.conf";

    // VAD
    public String vadConf = "jelly_vad.conf";
    public String vadPitchConf = "jelly_vad_pitch.conf";
    public String vadNAConf = "jelly_vad_NA.conf";
    //public String vadConf = "jelly_vad_opensource.conf";

    // Feature Extraction
    //public String saveDataConf = "liveinput_android_radar.conf";
    public String saveDataConf = "emobase_live4.conf";
    public String saveDataConf_no_Wav = "emobase_live4_no_wav.conf";
    //public String saveDataConf = "jelly_arousal_valence.conf";

    // Debug
    //public String debugConf = "jelly_arousal_valence.conf";
    public String debugConf = "emobase_live4.conf";

    ArrayList plugins = new ArrayList<OpenSmilePlugins>();

    public Config(ArrayList<OpenSmilePlugins> op) {
        //for(int i=0; i < op.size(); i++)
            //plugins.add(op.get(i));
    }

}
