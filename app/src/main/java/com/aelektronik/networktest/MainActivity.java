package com.aelektronik.networktest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import static android.telephony.TelephonyManager.*;


public class MainActivity extends AppCompatActivity {
    String TAG = "NetworkTest";
    static TelephonyManager telephonyManager;

    //GUI global vars
    static TextView tvStrength, tvNetworkDataType;
    TextView tvDownloadStatus;
    ProgressBar barDownload;
    DownloadThread downloadThread;
    Button btDownload;

    //Control - main thread vars
    int downloadActive = 0;
    int downloadStarted = 0;
    int counterDownload = 0;
    private static String file_url_2G = "https://datahub.io/datahq/1mb-test/r/1mb-test.csv";
    private static String file_url_3G = "https://datahub.io/datahq/1mb-test/r/1mb-test.csv";
    private static String file_url_LTE = "https://datahub.io/datahq/1mb-test/r/1mb-test.csv";

    //Network status thread vars
    int powerDBM = 0;
    String strCellInfo;
    String strNetworkDataType;


    //Download thread vars
    long timeDownloadStart = 0, timeDownloadEnd = 0;
    float timeDownload = 0, throughPutkBs = 0;
    float timeDownload2G = 0, throughPutkBs2G = 0;
    float timeDownload3G = 0, throughPutkBs3G = 0;
    float timeDownloadLTE = 0, throughPutkBsLTE = 0;

    //PASS/FAIL vars
    int pass2G = 1;
    int pass3G = 1;
    int passLTE = 1;

    WifiManager wifiManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStrength = findViewById(R.id.txtStrength);
        tvNetworkDataType = findViewById(R.id.txtCellInfo);
        tvDownloadStatus = findViewById(R.id.txtDownloadStatus);
        barDownload = findViewById(R.id.barDownload);
        btDownload = findViewById(R.id.btStartDownload);

        //TELEPHONY MANAGER + PERMISSION CHECK
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(getApplicationContext(), "No location permission", Toast.LENGTH_SHORT).show();
            return;
        }

        //WIFI ENABLED/DISABLED CODE
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);


        //BUTTON LISTENER FOR STARTING DOWNLOAD
        btDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadActive = 1;
                btDownload.setVisibility(View.GONE);
            }
        });

        //TIMER CODE FOR NETWORK PARAMETER CONTROL AND CHECK SIGNAL STRENGHT
        Handler handlerCheckConditions = new Handler();
        Runnable processSignal = new Runnable() {
            @Override
            public void run() {
                //CHECK NETWORK STATUS
                getSignalStrength();
                getNetworkType();
                tvNetworkDataType.setText(strNetworkDataType);
                tvStrength.setText(strCellInfo + ", " + powerDBM +" dbm");
                //CONTROL DOWNLOAD
                if(downloadActive > 0){ //download is or it should be activated
                    if(downloadStarted == 0 ){     //downlload should be started
                        //INITIALIZE DOWNLOAD THREAD
                        //MANDATORY: checking if downloadThread is already created
                        if((counterDownload % 3) == 0) {
                            try {
                                if (downloadThread != null) {
                                    downloadThread = null;
                                }
                                downloadThread = new DownloadThread(file_url_2G);
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }

                            tvDownloadStatus.setText("Download 2G started, please wait");
                            downloadThread.start();
                            counterDownload++;
                        }
                        else if((counterDownload % 3) == 1){ //2G is over, start 3G
                            //STORE 2G VARS
                            timeDownload2G = timeDownload;
                            throughPutkBs2G = throughPutkBs;
                            //START 3G
                            try {
                                if (downloadThread != null) {
                                    downloadThread = null;
                                }
                                downloadThread = new DownloadThread(file_url_3G);
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                            tvDownloadStatus.setText("Download 3G started, please wait");
                            downloadThread.start();
                            counterDownload++;
                        }
                        else if ((counterDownload %3 )== 2){
                            timeDownload3G = timeDownload;
                            throughPutkBs3G = throughPutkBs;
                            try {
                                if (downloadThread != null) {
                                    downloadThread = null;
                                }
                                downloadThread = new DownloadThread(file_url_LTE);
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                            tvDownloadStatus.setText("Download LTE started, please wait");
                            downloadThread.start();
                            counterDownload++;
                            downloadActive = 0;
                        }
                        else{
                            //IMPOSSIBLE TO GET HERE!!!
                            downloadActive = 0;
                        }
                    }
                    else{                            //download is already in charge, we check status

                        if(counterDownload %3 == 1){    //2G
                            if(powerDBM > -70){
                                pass2G = 0;
                            }
                        }
                        else if(counterDownload %3 == 2){   //3G
                            if(powerDBM > -70){
                                pass3G = 0;
                            }
                        }
                        else if (counterDownload %3 == 0){  //LTE
                            if(powerDBM > -96){
                                passLTE = 0;
                            }
                        }
                        else{
                            //IMPOSSIBLE TO GET HERE
                        }


                    }

                }
                else{   //downloadActive <= 0
                    if(downloadStarted == 0 && ((counterDownload % 3) == 0)){
                        timeDownloadLTE = timeDownload;
                        throughPutkBsLTE = throughPutkBs;
                        tvDownloadStatus.setText("Time: " + timeDownload2G + " s" +
                                "    Throughput: " + throughPutkBs2G + " kBy/s\n" +
                                "Time: " + timeDownload3G + " s" +
                                "    Throughput: " + throughPutkBs3G + " kBy/s\n" +
                                "Time: " + timeDownloadLTE + " s" +
                                "    Throughput: " + throughPutkBsLTE + " kBy/s\n" +
                                 + pass2G + pass3G + passLTE);
                        btDownload.setVisibility(View.VISIBLE);
                    }
                }

                //call this func again in 2000ms
                handlerCheckConditions.postDelayed(this, 2000);
            }

        };
        handlerCheckConditions.post(processSignal);

    }


    private void getSignalStrength() {
        @SuppressLint("MissingPermission") List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();   //This will give info of all sims present inside your mobile
        if (cellInfos != null) {
            for (int i = 0; i < cellInfos.size(); i++) {
                if (cellInfos.get(i).isRegistered()) {
                    if (cellInfos.get(i) instanceof CellInfoWcdma) {
                        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfos.get(i);
                        CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
                        strCellInfo = "WCDMA";
                        powerDBM = cellSignalStrengthWcdma.getDbm();
                    } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                        CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfos.get(i);
                        CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                        strCellInfo = "GSM";
                        powerDBM = cellSignalStrengthGsm.getDbm();
                    } else if (cellInfos.get(i) instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                        CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                        strCellInfo = "LTE";
                        powerDBM = cellSignalStrengthLte.getDbm();
                    } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                        CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfos.get(i);
                        CellSignalStrengthCdma cellSignalStrengthCdma = cellInfoCdma.getCellSignalStrength();
                        strCellInfo = "CDMA";
                        powerDBM = cellSignalStrengthCdma.getDbm();
                    } else {
                        strCellInfo = "No connection\n";
                    }
                }

            }
        }
        return;
    }

    
    private void getNetworkType(){

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        int networkType = telephonyManager.getDataNetworkType();


        switch (networkType){
            case NETWORK_TYPE_UNKNOWN:
                strNetworkDataType = "NETWORK_TYPE_UNKNOWN";
                break;
            case NETWORK_TYPE_GPRS:
                strNetworkDataType = "NETWORK_TYPE_GPRS";
                break;
            case NETWORK_TYPE_EDGE:
                strNetworkDataType = "NETWORK_TYPE_EDGE";
                break;
            case NETWORK_TYPE_UMTS:
                strNetworkDataType = "NETWORK_TYPE_UMTS";
                break;
            case NETWORK_TYPE_HSDPA:
                strNetworkDataType = "NETWORK_TYPE_HSDPA";
                break;
            case NETWORK_TYPE_HSUPA:
                strNetworkDataType = "NETWORK_TYPE_HSUPA";
                break;
            case NETWORK_TYPE_HSPA:
                strNetworkDataType = "NETWORK_TYPE_HSPA";
                break;
            case NETWORK_TYPE_CDMA:
                strNetworkDataType = "NETWORK_TYPE_CDMA";
                break;
            case NETWORK_TYPE_EVDO_0:
                strNetworkDataType = "NETWORK_TYPE_EVDO_0";
                break;
            case NETWORK_TYPE_EVDO_A:
                strNetworkDataType = "NETWORK_TYPE_EVDO_A";
                break;
            case NETWORK_TYPE_EVDO_B:
                strNetworkDataType = "NETWORK_TYPE_EVDO_B";
                break;
            case NETWORK_TYPE_1xRTT:
                strNetworkDataType = "NETWORK_TYPE_1xRTT";
                break;
            case NETWORK_TYPE_IDEN:
                strNetworkDataType = "NETWORK_TYPE_IDEN";
                break;
            case NETWORK_TYPE_LTE:
                strNetworkDataType = "NETWORK_TYPE_LTE";
                break;
            case NETWORK_TYPE_EHRPD:
                strNetworkDataType = "NETWORK_TYPE_EHRPD";
                break;
            case NETWORK_TYPE_HSPAP:
                strNetworkDataType = "NETWORK_TYPE_HSPAP";
                break;
            case NETWORK_TYPE_NR:
                strNetworkDataType = "NETWORK_TYPE_NR";
                break;
            default:
                strNetworkDataType = "NETWORK_TYPE_DEFAULT";
                break;

        }
        return;
    }






    class DownloadThread extends Thread{
        URL urlDownload = null;

        //Constructor with checking whether is urlDownload not null
        //without this, the same object is created twice
        DownloadThread(String url) throws MalformedURLException {
            if(urlDownload != null){
                urlDownload = null;
            }
            this.urlDownload = new URL(url);
        }

        public void setUrlDownload(String url) throws MalformedURLException {
            if(urlDownload != null){
                urlDownload = null;
            }
            this.urlDownload = new URL(url);
        }

        @Override
        public void run(){
            int count;
            downloadStarted = 1;
            try {

                //SET PROGRESS BAR TO 0
                barDownload.post(new Runnable() {
                    @Override
                    public void run() {
                        barDownload.setProgress(0);
                    }
                });

                //GET FILESIZE
                URLConnection connection = urlDownload.openConnection();
                connection.connect();
                long lenghtOfFile = connection.getContentLengthLong();

                //CHECK IF FILE ALREADY EXISTS
                File exFile = new File(Environment.getExternalStorageDirectory().toString()
                        + "/test.csv");
                if(exFile.exists()){
                    exFile.delete();
                    Log.d(TAG, "File deleted!");
                }

                Log.d(TAG, "Download started");

                //DOWNLOAD
                timeDownloadStart = System.currentTimeMillis();
                InputStream input = new BufferedInputStream(urlDownload.openStream(),
                        8192);

                OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory().toString()
                        + "/test.csv");

                byte data[] = new byte[1024];

                long total = 0;
                long updateTotal = 0;
                long progress = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    final long  uiTotal = total;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    if((100*(total - updateTotal)/lenghtOfFile) >= 1){
                        barDownload.post(new Runnable() {
                            @Override
                            public void run() {
                                barDownload.setProgress((int) (100*uiTotal/lenghtOfFile));
                            }
                        });
                        updateTotal = total;
                    }
                    // writing data to file
                    output.write(data, 0, count);
                }
                timeDownloadEnd = System.currentTimeMillis();
                timeDownload = ((float)(timeDownloadEnd - timeDownloadStart))/1000;
                throughPutkBs = lenghtOfFile/timeDownload/1000;
                // flushing output
                output.flush();
                Log.d(TAG, "File downloaded!");
                // closing streams
                output.close();
                input.close();
                //SET FINAL UI DOWNLOAD
                tvDownloadStatus.post(new Runnable() {
                    @Override
                    public void run() {
                        barDownload.setProgress(100);
                    }
                });


            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            downloadStarted = 0;
            return;
        }
    }
}