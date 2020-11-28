package com.aelektronik.networktest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PermissionUtils {

    Context context;
    Activity current_activity;

    PermissionResultCallback permissionResultCallback;
    ArrayList<String> permission_list=new ArrayList<>();
    ArrayList<String> listPermissionsNeeded=new ArrayList<>();

    String dialog_content="";
    int req_code;

    public PermissionUtils(Context context)
    {
        this.context=context;
        this.current_activity= (Activity) context;
        permissionResultCallback= (PermissionResultCallback) context;
    }

    public PermissionUtils(Context context, PermissionResultCallback callback)
    {
        this.context=context;
        this.current_activity= (Activity) context;
        permissionResultCallback= callback;
    }


    public void check_permission(ArrayList<String> permissions, String dialog_content, int request_code)
    {
        this.permission_list=permissions;
        this.dialog_content=dialog_content;
        this.req_code=request_code;

        if(Build.VERSION.SDK_INT >= 23)
        {
            if (checkAndRequestPermissions(permissions, request_code))
            {
                permissionResultCallback.PermissionGranted(request_code);
            }
        }
        else
        {
            permissionResultCallback.PermissionGranted(request_code);
        }

    }


    /**
     * Check and request the Permissions
     *
     * @param permissions
     * @param request_code
     * @return
     */

    private  boolean checkAndRequestPermissions(ArrayList<String> permissions,int request_code) {

        if(permissions.size()>0)
        {
            listPermissionsNeeded = new ArrayList<>();

            for(int i=0;i<permissions.size();i++)
            {
                int hasPermission = ContextCompat.checkSelfPermission(current_activity,permissions.get(i));

                if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(permissions.get(i));
                }

            }

            if (!listPermissionsNeeded.isEmpty())
            {
                ActivityCompat.requestPermissions(current_activity, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),request_code);
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case 1:
                if(grantResults.length>0)
                {
                    Map<String, Integer> perms = new HashMap<>();

                    for (int i = 0; i < permissions.length; i++)
                    {
                        perms.put(permissions[i], grantResults[i]);
                    }

                    final ArrayList<String> pending_permissions=new ArrayList<>();

                    for (int i = 0; i < listPermissionsNeeded.size(); i++)
                    {
                        if (perms.get(listPermissionsNeeded.get(i)) != PackageManager.PERMISSION_GRANTED)
                        {
                            if(ActivityCompat.shouldShowRequestPermissionRationale(current_activity,listPermissionsNeeded.get(i)))
                                pending_permissions.add(listPermissionsNeeded.get(i));
                            else
                            {
                                Log.i("Go to settings","and enable permissions");
                                permissionResultCallback.NeverAskAgain(req_code);
                                return;
                            }
                        }

                    }

                    if(pending_permissions.size()>0)
                    {
                        showMessageOKCancel(dialog_content,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        switch (which) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                Log.d("LOG","ON OK BTN CLICK CALLING CHECK PERMISSION METHOD");
                                                check_permission(permission_list,dialog_content,req_code);
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                Log.i("permisson","not fully given");
                                                if(permission_list.size()==pending_permissions.size()){
                                                    Log.d("LOG","ON NO BTN CLICK CALLING Permission Denided METHOD");
                                                    //permissionResultCallback.PermissionDenied(req_code);
                                                    check_permission(permission_list,dialog_content,req_code);
                                                }
                                                else{
                                                    Log.d("LOG","ON NO BTN CLICK CALLING PartialPermissionGranted METHOD");
                                                    permissionResultCallback.PartialPermissionGranted(req_code,pending_permissions);
                                                }

                                                break;
                                        }


                                    }
                                });

                    }
                    else
                    {
                        permissionResultCallback.PermissionGranted(req_code);
                    }
                }
                break;
        }
    }


    /**
     * Explain why the app needs permissions
     *
     * @param message
     * @param okListener
     */
    public void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(current_activity)
                .setMessage(message)
                .setPositiveButton("Ok", okListener)
                .setNegativeButton("Cancel", okListener)
                .setCancelable(false)
                .create()
                .show();
    }

    public interface PermissionResultCallback
    {
        void PermissionGranted(int request_code);
        void PartialPermissionGranted(int request_code, ArrayList<String> granted_permissions);
        void PermissionDenied(int request_code);
        void NeverAskAgain(int request_code);
    }
}
