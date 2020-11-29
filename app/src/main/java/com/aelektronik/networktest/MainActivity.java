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
import android.os.Environment;
import android.os.Handler;
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
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    static TelephonyManager telephonyManager;
    static TextView tvStrength, tvCellInfo;
    static long storeTime;
    static int changeCount = 0;
    TextView tvDownloadStatus;
    ProgressBar barDownload;
    DownloadThread downloadThread;
    Button btDownload;
    private static String file_url = "https://datahub.io/datahq/1mb-test/r/1mb-test.csv";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStrength = findViewById(R.id.txtStrength);
        tvCellInfo = findViewById(R.id.txtCellInfo);
        tvDownloadStatus = findViewById(R.id.txtDownloadStatus);
        barDownload = findViewById(R.id.barDownload);
        btDownload = findViewById(R.id.btStartDownload);


        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
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

        //INITIALIZE DOWNLOAD THREAD
        
        try {
            downloadThread = new DownloadThread(file_url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        //BUTTON LISTENER FOR STARTING DOWNLOAD

        btDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadThread.start();
                tvDownloadStatus.setText("Download started, please wait");
            }
        });

        //TIMER CODE FOR SIGNAL STRENGTH TRACKING

        Handler handlerSignalTrack= new Handler();
        Runnable processSignal = new Runnable() {
            @Override
            public void run() {
                getSignalStrength(this);
                handlerSignalTrack.postDelayed(this,2000);
            }
        };

        handlerSignalTrack.post(processSignal);

    }


    private static String getSignalStrength(Runnable context)  {
        long curTime, diffTime;
        curTime = System.currentTimeMillis();
        diffTime = curTime - storeTime;

        storeTime = curTime;

        String strCellInfo = "";
        @SuppressLint("MissingPermission") List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();   //This will give info of all sims present inside your mobile
        String strStrength = "No cells:" + cellInfos.size() + "\n";
        changeCount++;
        strStrength += "Counter: " + changeCount + "\n";
        if(cellInfos != null) {
            for (int i = 0 ; i < cellInfos.size() ; i++) {
                if (cellInfos.get(i).isRegistered()) {
                    strStrength += i + ". " + cellInfos.get(i).toString() + "\n";
                    if (cellInfos.get(i) instanceof CellInfoWcdma) {
                        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfos.get(i);
                        CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
                        strCellInfo += ("Wcdma: " + String.valueOf(cellSignalStrengthWcdma.getDbm()) + ", " + cellSignalStrengthWcdma.getAsuLevel()+ "\n");
                    } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                        CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfos.get(i);
                        CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                        strCellInfo += ("GSM: " + String.valueOf(cellSignalStrengthGsm.getDbm()) + ", " + cellSignalStrengthGsm.getAsuLevel()+ "\n");
                    } else if (cellInfos.get(i) instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                        CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                        strCellInfo += ("LTE: " + String.valueOf(cellSignalStrengthLte.getDbm()) + ", " + cellSignalStrengthLte.getRsrp() +"\n");
                    } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                        CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfos.get(i);
                        CellSignalStrengthCdma cellSignalStrengthCdma = cellInfoCdma.getCellSignalStrength();
                        strCellInfo += "CDMA: " + (String.valueOf(cellSignalStrengthCdma.getDbm()) + ", " + cellSignalStrengthCdma.getCdmaDbm()+ "\n");
                    }
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (cellInfos.get(i) instanceof CellInfoTdscdma){
                            CellInfoTdscdma cellInfoTdscdma = (CellInfoTdscdma) cellInfos.get(i);
                            CellSignalStrengthTdscdma cellSignalStrengthTdscdma = cellInfoTdscdma.getCellSignalStrength();
                            strCellInfo += "TSCDMA: " + (String.valueOf(cellSignalStrengthTdscdma.getDbm()) + ", " + cellSignalStrengthTdscdma.getRscp()+ "\n");
                        }
                    }
                    else{
                        strCellInfo += "No connection\n";
                    }
                }

            }
        }
        tvStrength.setText(strStrength);
        tvCellInfo.setText(strCellInfo);
        return strCellInfo;
    }



    class DownloadThread extends Thread{
        URL urlDownload;
        DownloadThread(String url) throws MalformedURLException {
            this.urlDownload = new URL(url);
        }

        public void run(){
            int count;
            try {

                URLConnection connection = urlDownload.openConnection();
                connection.connect();

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                long lenghtOfFile = connection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(urlDownload.openStream(),
                        8192);

                // Output stream
                OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory().toString()
                        + "/test.csv");

                byte data[] = new byte[1024];

                long total = 0;
                long updateTotal = 0;
                long progress = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    if(100*(total - updateTotal)/lenghtOfFile > 1){
                        barDownload.setProgress((int) (100*(total/lenghtOfFile)));
                        updateTotal = total;
                    }
                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("NetworkTest error: ", e.getMessage());
                //tvDownloadStatus.setText("Something went wrong: " + e.getMessage());
            }

            return;
        }
    }


}