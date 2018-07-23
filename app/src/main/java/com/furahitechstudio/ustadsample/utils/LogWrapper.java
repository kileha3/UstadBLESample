package com.furahitechstudio.ustadsample.utils;

import android.util.Log;

public class LogWrapper {

  private static final String TAG = "UstadGoogleAPI";

  public static void log(boolean isError, String logMessage){
    if(isError){
      Log.e(TAG,logMessage);
    }else{
      Log.d(TAG,logMessage);
    }
  }
}
