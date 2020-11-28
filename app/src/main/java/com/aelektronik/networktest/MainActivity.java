package com.aelektronik.networktest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoTdscdma;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthTdscdma;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    class MyPhoneStateListener extends PhoneStateListener {
        long storeTime, curTime;
        int changeCount = 0;
        MyPhoneStateListener(){
            storeTime = System.currentTimeMillis();
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            //get diff time from last change
            curTime = System.currentTimeMillis();
            long diffTime = curTime - storeTime;

            super.onSignalStrengthsChanged(signalStrength);

            List<CellSignalStrength> l = new ArrayList<CellSignalStrength>();
            changeCount++;

            //get strength
            String str = signalStrength.toString();
            str += ("\n" + signalStrength.getGsmSignalStrength());
            str += ("\n" + signalStrength.getEvdoEcio());
            str += ("\n" + signalStrength.getEvdoDbm());
            str += ("\n" + signalStrength.getCdmaEcio());
            str += ("\n" + signalStrength.getCdmaDbm());
            str += ("\n" + signalStrength.getLevel());
            str += ("\n" + changeCount + ". "+ diffTime);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                /*l = signalStrength.getCellSignalStrengths();
                str += ("\n" + l.get(0).getDbm());
                str += ("\n" + l.get(0).getAsuLevel());*/
            } else {

            }
            tvStrength.setText(str + "\n");
            storeTime = curTime;

            String strength = "";
            @SuppressLint("MissingPermission") List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();   //This will give info of all sims present inside your mobile
            if(cellInfos != null) {
                for (int i = 0 ; i < cellInfos.size() ; i++) {
                    if (cellInfos.get(i).isRegistered()) {
                        if (cellInfos.get(i) instanceof CellInfoWcdma) {
                            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfos.get(i);
                            CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
                            strength += ("Wcdma: " + String.valueOf(cellSignalStrengthWcdma.getDbm()) + ", " + cellSignalStrengthWcdma.getAsuLevel()+ "\n");
                        } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                            CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfos.get(i);
                            CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                            strength += ("GSM: " + String.valueOf(cellSignalStrengthGsm.getDbm()) + ", " + cellSignalStrengthGsm.getAsuLevel()+ "\n");
                        } else if (cellInfos.get(i) instanceof CellInfoLte) {
                            CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                            CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                            strength += ("LTE: " + String.valueOf(cellSignalStrengthLte.getDbm()) + ", " + cellSignalStrengthLte.getRsrp() +"\n");
                        } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                            CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfos.get(i);
                            CellSignalStrengthCdma cellSignalStrengthCdma = cellInfoCdma.getCellSignalStrength();
                            strength += "CDMA: " + (String.valueOf(cellSignalStrengthCdma.getDbm()) + ", " + cellSignalStrengthCdma.getCdmaDbm()+ "\n");
                        }
                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (cellInfos.get(i) instanceof CellInfoTdscdma){
                                CellInfoTdscdma cellInfoTdscdma = (CellInfoTdscdma) cellInfos.get(i);
                                CellSignalStrengthTdscdma cellSignalStrengthTdscdma = cellInfoTdscdma.getCellSignalStrength();
                                strength += "TSCDMA: " + (String.valueOf(cellSignalStrengthTdscdma.getDbm()) + ", " + cellSignalStrengthTdscdma.getRscp()+ "\n");
                            }
                        }
                    }
                    else{
                        //strength += "No connection";
                    }
                }
            }
            tvCellInfo.setText(strength);
        }

        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            super.onCallStateChanged(state, phoneNumber);

        }
    }

    TelephonyManager telephonyManager;
    MyPhoneStateListener listener;
    TextView tvStrength, tvCellInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStrength = findViewById(R.id.txtStrength);
        tvCellInfo = findViewById(R.id.txtCellInfo);


        listener = new MyPhoneStateListener();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(getApplicationContext(),"No location permission", Toast.LENGTH_SHORT).show();
            return;
        }

/*
        try{
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setClassName("com.android.settings", "com.android.settings.RadioInfo");
            startActivity(intent);
        } catch(Exception e){
            Toast.makeText(getApplicationContext(), " Device not supported" , Toast.LENGTH_LONG).show();
        }

 */
    }


    private static String getSignalStrength(Context context) throws SecurityException {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String strength = "Cell: ";
        List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();   //This will give info of all sims present inside your mobile
        if(cellInfos != null) {
            for (int i = 0 ; i < cellInfos.size() ; i++) {
                if (cellInfos.get(i).isRegistered()) {
                    if (cellInfos.get(i) instanceof CellInfoWcdma) {
                        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfos.get(i);
                        CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
                        strength += (String.valueOf(cellSignalStrengthWcdma.getDbm()) + "\n");
                    } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                        CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfos.get(i);
                        CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                        strength += (String.valueOf(cellSignalStrengthGsm.getDbm()) + "\n");
                    } else if (cellInfos.get(i) instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                        CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                        strength += (String.valueOf(cellSignalStrengthLte.getDbm()) + "\n");
                    } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                        CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfos.get(i);
                        CellSignalStrengthCdma cellSignalStrengthCdma = cellInfoCdma.getCellSignalStrength();
                        strength += (String.valueOf(cellSignalStrengthCdma.getDbm()) + "\n");
                    }
                }
            }
        }
        return strength;
    }

}