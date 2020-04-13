package com.chirag.RNMail;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.Html;
import androidx.core.content.FileProvider;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.Callback;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

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
    String intentAction = Intent.ACTION_SENDTO;
    ArrayList<Uri> fileAttachmentUriList = getFileAttachmentUriList(options);

    if (1 <= fileAttachmentUriList.size()) {
      intentAction = Intent.ACTION_SEND_MULTIPLE;
    }

    Intent intent = new Intent(intentAction);
    intent.setData(Uri.parse("mailto:"));

    if (hasProperty(options, "subject")) {
      intent.putExtra(Intent.EXTRA_SUBJECT, options.getString("subject"));
    }

    if (hasProperty(options, "body")) {
      String body = options.getString("body");
      if (options.hasKey("isHTML") && options.getBoolean("isHTML")) {
        intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(body));
      } else {
        intent.putExtra(Intent.EXTRA_TEXT, body);
      }
    }

    if (hasProperty(options, "recipients")) {
      ReadableArray recipients = options.getArray("recipients");
      intent.putExtra(Intent.EXTRA_EMAIL, readableArrayToStringArray(recipients));
    }

    if (hasProperty(options, "ccRecipients")) {
      ReadableArray ccRecipients = options.getArray("ccRecipients");
      intent.putExtra(Intent.EXTRA_CC, readableArrayToStringArray(ccRecipients));
    }

    if (hasProperty(options, "bccRecipients")) {
      ReadableArray bccRecipients = options.getArray("bccRecipients");
      intent.putExtra(Intent.EXTRA_BCC, readableArrayToStringArray(bccRecipients));
    }

    if (1 <= fileAttachmentUriList.size()) {
      intent.setType("text/plain");
      intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileAttachmentUriList);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    PackageManager manager = reactContext.getPackageManager();
    List<ResolveInfo> list = manager.queryIntentActivities(intent, 0);

    if (list == null || list.size() == 0) {
      callback.invoke("not_available");
      return;
    }

    if (list.size() == 1) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      try {
        reactContext.startActivity(intent);
        callback.invoke(fileAttachmentUriList.toString());
      } catch (Exception ex) {
        callback.invoke("error activity", ex.toString(), ex.getMessage());
      }
    } else {
      Intent chooser = Intent.createChooser(intent, "Send Mail");
      chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      try {
        reactContext.startActivity(chooser);
        callback.invoke(fileAttachmentUriList.toString() + options.getArray("attachments").size());
      } catch (Exception ex) {
        callback.invoke("error chooser" + ex.toString() + ex.getMessage());
      }
    }
  }

  /**
  * Checks if a given key is valid
  * @param @{link ReadableMap} readableMap
  * @param @{link String} key
  * @return boolean representing whether the key exists and has a value
  */
  private Boolean hasProperty(ReadableMap readableMap, String key) {
    return readableMap.hasKey(key) && !readableMap.isNull(key);
  }

  /**
  * Returned list is empty if no attachments foiund
  * @param @{link ReadableMap} options
  * @return ArrayList<Uri> containing file attachment Uri list
  */
  private ArrayList<Uri> getFileAttachmentUriList(ReadableMap options) {
    ArrayList<Uri> fileAttachmentUriList = new ArrayList<Uri>();

    if (hasProperty(options, "attachments")) {
      ReadableArray attachmentList = options.getArray("attachments");
      int length = attachmentList.size();

      for(int i = 0; i < length; i++) {
        ReadableMap attachmentItem = attachmentList.getMap(i);
        String path = attachmentItem.getString("path");

        File file = new File(path);
        Uri apkURI = FileProvider.getUriForFile(
                                 reactContext,
                                 reactContext.getApplicationContext()
                                 .getPackageName() + ".provider", file);

        fileAttachmentUriList.add(apkURI);
      }
    }

    return fileAttachmentUriList;
  }
}
