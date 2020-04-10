package com.chirag.RNMail;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.Html;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.Callback;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* NativeModule that allows JS to open emails sending apps chooser.
*/

public class RNMailModule extends ReactContextBaseJavaModule {
  ReactApplicationContext reactContext;

  public RNMailModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNMail";
  }

  /**
    * Converts a ReadableArray to a String array
    *
    * @param r the ReadableArray instance to convert
    *
    * @return array of strings
  */
  private String[] readableArrayToStringArray(ReadableArray r) {
    int length = r.size();
    String[] strArray = new String[length];

    for (int keyIndex = 0; keyIndex < length; keyIndex++) {
      strArray[keyIndex] = r.getString(keyIndex);
    }

    return strArray;
  }

  @ReactMethod
  public void mail(ReadableMap options, Callback callback) {
    try {
      String intentAction = Intent.ACTION_SENDTO;
      ArrayList<Uri> fileAttachmentUriList = getFileAttachmentUriList(options);

      if (1 <= fileAttachmentUriList.size()) {
        intentAction = Intent.ACTION_SEND_MULTIPLE;
      }

      Intent intent = new Intent(intentAction);
      intent.setData(Uri.parse("mailto:"));

      if (hasKeyAndDefined(options, "subject")) {
        intent.putExtra(Intent.EXTRA_SUBJECT, options.getString("subject"));
      }

      // On Android HTML email body results are awful, no <table>,<ol>,etc.
      // Is there really no way to make it work like iOS?
      // http://blog.iangclifton.com/2010/05/17/sending-html-email-with-android-intent/
      // http://www.nowherenearithaca.com/2011/10/some-notes-for-sending-html-email-in.html
      if (hasKeyAndDefined(options, "body")) {
        String body = options.getString("body");
        if (options.hasKey("isHTML") && options.getBoolean("isHTML")) {
          intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(body));
        } else {
          intent.putExtra(Intent.EXTRA_TEXT, body);
        }
      }

      if (hasKeyAndDefined(options, "recipients")) {
        ReadableArray recipients = options.getArray("recipients");
        intent.putExtra(Intent.EXTRA_EMAIL, readableArrayToStringArray(recipients));
      }

      if (hasKeyAndDefined(options, "ccRecipients")) {
        ReadableArray ccRecipients = options.getArray("ccRecipients");
        intent.putExtra(Intent.EXTRA_CC, readableArrayToStringArray(ccRecipients));
      }

      if (hasKeyAndDefined(options, "bccRecipients")) {
        ReadableArray bccRecipients = options.getArray("bccRecipients");
        intent.putExtra(Intent.EXTRA_BCC, readableArrayToStringArray(bccRecipients));
      }

      if (1 <= fileAttachmentUriList.size()) {
        // If multiple attachments setType("plain/text"), else queryIntentActivities fails
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_STREAM, fileAttachmentUriList);
      }

      PackageManager manager = reactContext.getPackageManager();
      List<ResolveInfo> list = manager.queryIntentActivities(intent, 0);
      if (list == null || list.size() == 0) {
        callback.invoke("not_available");
        return;
      }

      Intent chooser = Intent.createChooser(intent, "Send Mail");
      chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      reactContext.startActivity(chooser);

    } catch (Exception ex) {
      callback.invoke("error RNMail:mail");
    }
  }

  /**
  * Checks if a given key is valid
  * @param @{link ReadableMap} readableMap
  * @param @{link String} key
  * @return boolean representing whether the key exists and has a value
  */
  private Boolean hasKeyAndDefined(ReadableMap readableMap, String key) {
    return readableMap.hasKey(key) && !readableMap.isNull(key);
  }

  /**
  * Returned list is empty if no attachments foiund
  * @param @{link ReadableMap} options
  * @return ArrayList<Uri> containing file attachment Uri list
  */
  private ArrayList<Uri> getFileAttachmentUriList(ReadableMap options) {
    ArrayList<Uri> fileAttachmentUriList = new ArrayList<Uri>();

    if (hasKeyAndDefined(options, "attachmentList")) {
      ReadableArray attachmentList = options.getArray("attachmentList");
      int length = attachmentList.size();

      for(int i = 0; i < length; ++i) {
        ReadableMap attachmentItem = attachmentList.getMap(i);
        String fileName = "";
        String path = "";
        String mimeType = "";

        if (hasKeyAndDefined(attachmentItem, "name")) {
          fileName = attachmentItem.getString("name");
        }

        if (hasKeyAndDefined(attachmentItem, "path")) {
          path = attachmentItem.getString("path");
        }

        if (hasKeyAndDefined(attachmentItem, "mimeType")) {
          mimeType = attachmentItem.getString("mimeType");
        }

        boolean fileExists = new File(path).exists();

        if (fileExists) {
          fileAttachmentUriList.add(Uri.parse("file://" + path));
        }
      }
    }
    return fileAttachmentUriList;
  }
}
