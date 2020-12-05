package com.aelektronik.networktest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoTdscdma;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthTdscdma;
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

    static TelephonyManager telephonyManager;
    static TextView tvStrength, tvCellInfo;
    static long storeTime;
    static int changeCount = 0;
    TextView tvDownloadStatus;
    ProgressBar barDownload;
    DownloadThread downloadThread;
    Button btDownload;
    String TAG = "NetworkTest";

    long timeDownloadStart, timeDownloadEnd;

    WifiManager wifiManager;
    private static String file_url_2G = "https://datahub.io/datahq/1mb-test/r/1mb-test.csv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStrength = findViewById(R.id.txtStrength);
        tvCellInfo = findViewById(R.id.txtCellInfo);
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
                //INITIALIZE DOWNLOAD THREAD
                //MANDATORY: checking if downloadThread is already created
                try {
                    if (downloadThread != null) {
                        downloadThread = null;
                    }
                    downloadThread = new DownloadThread(file_url_2G);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                tvDownloadStatus.setText("Download started, please wait");
                downloadThread.start();

            }
        });

        //TIMER CODE FOR SIGNAL STRENGTH TRACKING

        Handler handlerSignalTrack = new Handler();
        Runnable processSignal = new Runnable() {
            @Override
            public void run() {
                getSignalStrength(this);
                handlerSignalTrack.postDelayed(this, 2000);
            }

        };
        handlerSignalTrack.post(processSignal);

    }


    private void getSignalStrength(Runnable context) {
        long curTime, diffTime;
        curTime = System.currentTimeMillis();
        diffTime = curTime - storeTime;

        storeTime = curTime;

        String strCellInfo = "";
        @SuppressLint("MissingPermission") List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();   //This will give info of all sims present inside your mobile
        String strStrength = "No cells:" + cellInfos.size() + "\n";
        changeCount++;
        strStrength += "Counter: " + changeCount + "\n";
        if (cellInfos != null) {
            for (int i = 0; i < cellInfos.size(); i++) {
                if (cellInfos.get(i).isRegistered()) {
                    strStrength += i + ". " + cellInfos.get(i).toString() + "\n";
                    if (cellInfos.get(i) instanceof CellInfoWcdma) {
                        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfos.get(i);
                        CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
                        strCellInfo += ("Wcdma: " + String.valueOf(cellSignalStrengthWcdma.getDbm()) + ", " + cellSignalStrengthWcdma.getAsuLevel() + "\n");
                    } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                        CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfos.get(i);
                        CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                        strCellInfo += ("GSM: " + String.valueOf(cellSignalStrengthGsm.getDbm()) + ", " + cellSignalStrengthGsm.getAsuLevel() + "\n");
                    } else if (cellInfos.get(i) instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                        CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                        strCellInfo += ("LTE: " + String.valueOf(cellSignalStrengthLte.getDbm()) + ", " + cellSignalStrengthLte.getRsrp() + "\n");
                    } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                        CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfos.get(i);
                        CellSignalStrengthCdma cellSignalStrengthCdma = cellInfoCdma.getCellSignalStrength();
                        strCellInfo += "CDMA: " + (String.valueOf(cellSignalStrengthCdma.getDbm()) + ", " + cellSignalStrengthCdma.getCdmaDbm() + "\n");
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (cellInfos.get(i) instanceof CellInfoTdscdma) {
                            CellInfoTdscdma cellInfoTdscdma = (CellInfoTdscdma) cellInfos.get(i);
                            CellSignalStrengthTdscdma cellSignalStrengthTdscdma = cellInfoTdscdma.getCellSignalStrength();
                            strCellInfo += "TSCDMA: " + (String.valueOf(cellSignalStrengthTdscdma.getDbm()) + ", " + cellSignalStrengthTdscdma.getRscp() + "\n");
                        }
                    } else {
                        strCellInfo += "No connection\n";
                    }
                }

            }
        }
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
                strStrength += "NETWORK_TYPE_UNKNOWN";
                break;
            case NETWORK_TYPE_GPRS:
                strStrength += "NETWORK_TYPE_GPRS";
                break;
            case NETWORK_TYPE_EDGE:
                strStrength += "NETWORK_TYPE_EDGE";
                break;
            case NETWORK_TYPE_UMTS:
                strStrength += "NETWORK_TYPE_UMTS";
                break;
            case NETWORK_TYPE_HSDPA:
                strStrength += "NETWORK_TYPE_HSDPA";
                break;
            case NETWORK_TYPE_HSUPA:
                strStrength += "NETWORK_TYPE_HSUPA";
                break;
            case NETWORK_TYPE_HSPA:
                strStrength += "NETWORK_TYPE_HSPA";
                break;
            case NETWORK_TYPE_CDMA:
                strStrength += "NETWORK_TYPE_CDMA";
                break;
            case NETWORK_TYPE_EVDO_0:
                strStrength += "NETWORK_TYPE_EVDO_0";
                break;
            case NETWORK_TYPE_EVDO_A:
                strStrength += "NETWORK_TYPE_EVDO_A";
                break;
            case NETWORK_TYPE_EVDO_B:
                strStrength += "NETWORK_TYPE_EVDO_B";
                break;
            case NETWORK_TYPE_1xRTT:
                strStrength += "NETWORK_TYPE_1xRTT";
                break;
            case NETWORK_TYPE_IDEN:
                strStrength += "NETWORK_TYPE_IDEN";
                break;
            case NETWORK_TYPE_LTE:
                strStrength += "NETWORK_TYPE_LTE";
                break;
            case NETWORK_TYPE_EHRPD:
                strStrength += "NETWORK_TYPE_EHRPD";
                break;
            case NETWORK_TYPE_HSPAP:
                strStrength += "NETWORK_TYPE_HSPAP";
                break;
            case NETWORK_TYPE_NR:
                strStrength += "NETWORK_TYPE_NR";
                break;
            default:
                strStrength += "NETWORK_TYPE_DEFAULT";
                break;

        }
        tvStrength.setText(strStrength);
        tvCellInfo.setText(strCellInfo);
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

                //CHECK FILESIZE
                File exFile = new File(Environment.getExternalStorageDirectory().toString()
                        + "/test.csv");
                if(exFile.exists()){
                    exFile.delete();
                    Log.d(TAG, "File deleted!");
                }

                Log.d(TAG, "Downlaod started");

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
                float timeDownload = ((float)(timeDownloadEnd - timeDownloadStart))/1000;
                float throughPutkBs = lenghtOfFile/timeDownload/1000;
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
                        tvDownloadStatus.setText("File: " + exFile.getName() +
                                "\nDownloaded: " + lenghtOfFile + " bytes.\n" +
                                "Time: " + timeDownload + " s" +
                                "\nThroughput: " + throughPutkBs + " kBy/s");
                        barDownload.setProgress(100);
                    }
                });


            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

            return;
        }


    }



}