package com.client.voidrecorder.recorder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import com.client.voidrecorder.R;
import com.client.voidrecorder.models.Recording;
import com.client.voidrecorder.utils.Conversions;
import com.client.voidrecorder.utils.Paths;
import com.client.voidrecorder.utils.SharedPreferencesHelper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class RecorderFragment extends Fragment {

    private static final String TAG = "RecorderActivity";

    private TextView timerTextView,serviceStatusTv,recordTV;
    private ImageView startBtn, stopBtn, recordingsBtn, settingsBtn,infoBtn;
    private CountDownTimer countDownTimer;
    private int second = -1, minute, hour;// timer vars
    public static final int PERMISSION_ALL = 0;
    private Intent serviceIntent;
    private Context mContext;
    private SharedPreferences sharedPreferences;



    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recorder, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



        mContext = getActivity();

        //recording service controller
        serviceIntent = new Intent(mContext, RecorderService.class);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());


        bindViews(view);
        requestPermissions();

        if (checkPermissions()) {


            Log.d(TAG, "onViewCreated: Permissions Granted");
            setAudioRecorder();

            if (isServiceRunningInForeground(mContext, RecorderService.class)) {
                //when app is closed completely and then opened again, showRecording, ResumeTimer
                Log.d(TAG, "onCreate: Service is running, toggle to recording");
                showRecordingUI();
                serviceStatusTv.setVisibility(View.VISIBLE);
                serviceStatusTv.setText(getString(R.string.service_recording));


                Log.d(TAG, "onViewCreated: Time Difference : "+Conversions.getTimeDifference((String) SharedPreferencesHelper.get(mContext, getString(R.string.timer_resume), ""), Conversions.getTimeNow()));;

            }

        }

//        requireActivity().registerReceiver(mMessageReceiver,new IntentFilter("timer_tracking"));

    }




    /* */
    private void bindViews(View parentView) {
        timerTextView =  parentView.findViewById(R.id.text);
        startBtn = parentView.findViewById(R.id.start);
        stopBtn =  parentView.findViewById(R.id.stop);
        recordingsBtn = parentView.findViewById(R.id.recordings);
        settingsBtn =  parentView.findViewById(R.id.settingsBtn);
        serviceStatusTv =  parentView.findViewById(R.id.serviceStatusTV);
        recordTV =  parentView.findViewById(R.id.recordTV);
        infoBtn =  parentView.findViewById(R.id.infoBtn);
        serviceStatusTv.setVisibility(View.INVISIBLE);
    }

    /* */
    public void setAudioRecorder() {
        Log.d(TAG, "setAudioRecorder: Called");

        stopBtn.setEnabled(false);
        stopBtn.setBackgroundResource(R.drawable.normal_background);
        stopBtn.setImageResource(R.drawable.noraml_stop);

        startBtn.setOnClickListener(startBtnClickListener);
        stopBtn.setOnClickListener(stopBtnClickListener);
        recordingsBtn.setOnClickListener(recordingsBtnClickListener);
        settingsBtn.setOnClickListener(settingsBtnClickListener);
        infoBtn.setOnClickListener(infoBtnClickListener);

    }


    /* */
    private void showRecordingUI() {
        startBtn.setEnabled(false);
        recordingsBtn.setEnabled(false);
        startBtn.setVisibility(View.INVISIBLE);
        recordTV.setVisibility(View.INVISIBLE);
        stopBtn.setEnabled(true);
        stopBtn.setBackgroundResource(R.drawable.round_shape);
        stopBtn.setImageResource(R.drawable.ic_stop_black_35dp);
        recordingsBtn.setBackgroundResource(R.drawable.normal_background);
        recordingsBtn.setImageResource(R.drawable.normal_menu);
    }


    //display recording time
    public void showTimer() {
        
        countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                second++;
                timerTextView.setText(recorderTime());
            }

            public void onFinish() {

            }
        };
        countDownTimer.start();
    }


    //recorder time
    public String recorderTime() {
        if (second == 60) {
            minute++;
            second = 0;
        }
        if (minute == 60) {
            hour++;
            minute = 0;
        }
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    //runtime permissions check
    public boolean requestPermissions() {
        int RECORD_AUDIO_PERMISSION = ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO);
        int WRITE_EXTERNAL_PERMISSION = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int READ_EXTERNAL_PERMISSION = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE);
        ArrayList<String> PERMISSION_LIST = new ArrayList<>();
        if ((RECORD_AUDIO_PERMISSION != PackageManager.PERMISSION_GRANTED)) {
            PERMISSION_LIST.add(Manifest.permission.RECORD_AUDIO);
        }
        if ((WRITE_EXTERNAL_PERMISSION != PackageManager.PERMISSION_GRANTED)) {
            PERMISSION_LIST.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!PERMISSION_LIST.isEmpty()) {
            requestPermissions(PERMISSION_LIST.toArray(new String[PERMISSION_LIST.size()]), PERMISSION_ALL);
            return false;
        }
        return true;
    }


    private boolean checkPermissions(){
        if (ActivityCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(),
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            //permissions granted
            return true;


        } else {




            return false;
        }
    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean record = false, storage = false;
        Log.d(TAG, "onRequestPermissionsResult: BEFORE PERMISSION_ALL CASE : "+requestCode);
        switch (requestCode) {
            case PERMISSION_ALL: {

                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(Manifest.permission.RECORD_AUDIO)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                record = true;
                            } else {
                                Toast.makeText(mContext, "Please allow Microphone permission", Toast.LENGTH_LONG).show();
                            }
                        } else if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                storage = true;
                            } else {
                                Toast.makeText(mContext, "Please allow Storage permission", Toast.LENGTH_LONG).show();
                            }

                        }
                    }
                }
                if (record && storage) {
                    Log.d(TAG, "onRequestPermissionsResult: Both permissions given");
                    setAudioRecorder();
                }
            }
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //here we save the current exact time in sharedpref and then when the app opens subtract it from
        // that time and show on timer textview3
        if(isServiceRunningInForeground(requireActivity(), RecorderService.class)){
            SharedPreferencesHelper.put(mContext, getString(R.string.timer_resume), Conversions.getTimeNow());
        }

        Log.d(TAG, "onDestroy: Called");


    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Called");
    }




    public static boolean isServiceRunningInForeground(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }

            }
        }
        return false;
    }





    /* ----------------------------LISTENERS--------------------------------------------*/

    View.OnClickListener startBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            showRecordingUI();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mContext.startForegroundService(serviceIntent);
            } else {
                mContext.startService(serviceIntent);
            }

            showTimer();



        }
    };






    View.OnClickListener stopBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //clear timeResume pref
            if(SharedPreferencesHelper.get(mContext, getString(R.string.timer_resume), "") != null){
                SharedPreferencesHelper.remove(mContext, getString(R.string.timer_resume));
            }

            mContext.stopService(serviceIntent);
            //cancel count down timer
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            startBtn.setEnabled(true);
            recordingsBtn.setEnabled(true);
            startBtn.setVisibility(View.VISIBLE);
            recordTV.setVisibility(View.VISIBLE);
            stopBtn.setEnabled(false);
            stopBtn.setBackgroundResource(R.drawable.normal_background);
            stopBtn.setImageResource(R.drawable.noraml_stop);
            recordingsBtn.setBackgroundResource(R.drawable.round_shape);
            recordingsBtn.setImageResource(R.drawable.ic_menu_black_35dp);
            second = -1;
            minute = 0;
            hour = 0;
            timerTextView.setText("00:00:00");
        }
    };

    View.OnClickListener recordingsBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            startBtn.setEnabled(true);
            recordingsBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            stopBtn.setBackgroundResource(R.drawable.normal_background);
            stopBtn.setImageResource(R.drawable.noraml_stop);
            recordingsBtn.setBackgroundResource(R.drawable.round_shape);
            recordingsBtn.setImageResource(R.drawable.ic_menu_black_35dp);
            NavHostFragment.findNavController(RecorderFragment.this)
                        .navigate(R.id.action_RecorderFragment_to_RecordingsFragment);
//            startActivity(new Intent(mContext, RecordingsActivity.class));
        }
    };

    View.OnClickListener settingsBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            NavHostFragment.findNavController(RecorderFragment.this)
                    .navigate(R.id.action_RecorderFragment_to_SettingsFragment);
        }



    };

    View.OnClickListener infoBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            showSelectedOptionsDialog();
        }



    };

    private void showSelectedOptionsDialog() {

        final View view = LayoutInflater.from(mContext).inflate(R.layout.selected_settings_dialog_layout, null);

        AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
        alertDialog.setTitle("Selected Settings");
        alertDialog.setCancelable(false);


        final TextView renameEditText = view.findViewById(R.id.etComments);
        String settingsSelected = "Format : "+sharedPreferences.getString(getString(R.string.output_format_pref), "") + "\nQuality : "+sharedPreferences.getString(getString(R.string.output_quality_pref), "") + "\nDuration : "+sharedPreferences.getString(getString(R.string.max_duration), "") + " Minutes\nSpace Limit : "+sharedPreferences.getString(getString(R.string.max_space_pref), "")+ " MB" ;

        renameEditText.setText(settingsSelected);

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Close", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {


                dialogInterface.dismiss();

            }
        });



        alertDialog.setView(view);
        alertDialog.show();
    }


//    BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
//        @SuppressLint("DefaultLocale")
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            Toast.makeText(mContext, "broad", Toast.LENGTH_LONG).show();
//
//            long millisUntilFinished = intent.getLongExtra("timer", 0);
//
////                String counter = String.format("%02d:%02d:%02d",
////                        TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
////                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
////                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
////                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)));
//
//
//            Log.d(TAG, "onReceive: Timer In Recorder Fragment : "+ millisUntilFinished);
////                timerTextView.setText(get);
//        }
//    };
}