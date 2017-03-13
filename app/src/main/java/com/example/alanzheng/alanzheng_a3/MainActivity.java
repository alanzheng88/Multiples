package com.example.alanzheng.alanzheng_a3;

import android.content.ClipData;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, View.OnDragListener {

    private final String TAG = MainActivity.this.getClass().getSimpleName();
    private static final String API_URL = "https://qrng.anu.edu.au/API/jsonI.php?length=4&type=uint8";
    private static final int NUMBERS_COUNT = 4;
    private static final int CONNECTION_READ_TIMEOUT = 10000;
    private static final int CONNECTION_TIMEOUT = 40000;
    private static final int CONNECTION_INPUT_STREAM_LINE_LENGTH = 5000;

    private TextView mRandomNumber1TextView;
    private TextView mRandomNumber2TextView;
    private TextView mRandomNumber3TextView;
    private TextView mRandomNumber4TextView;

    private TextView mMultipleOf2TextView;
    private TextView mMultipleOf3TextView;
    private TextView mMultipleOf5TextView;
    private TextView mMultipleOf10TextView;

    private ConnectivityManager mConnectMgr;

    private String readIt(InputStream stream, int len) throws IOException {
        Reader reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        reader.read(buffer);
        return new String(buffer);
    }

    private String readJsonData(String remoteUrl) throws IOException {
        InputStream is = null;

        URL url = new URL(remoteUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setReadTimeout(CONNECTION_READ_TIMEOUT);
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            if (conn.getResponseCode() == 200) {
                is = conn.getInputStream();
                return readIt(is, CONNECTION_INPUT_STREAM_LINE_LENGTH);
            }
        } finally {
            if (is != null) {
                is.close();
                conn.disconnect();
            }
        }

        return null;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, "event is: " + MotionEvent.actionToString(event.getAction()));
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                ClipData clipData = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDragAndDrop(clipData, shadowBuilder, v, 0);
                return true;
        }
        return false;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {

        TextView dragView;
        TextView targetTextView;

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                dragView = (TextView) event.getLocalState();
                dragView.setVisibility(View.INVISIBLE);
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                setBackgroundColorToView(v);
                break;
            case DragEvent.ACTION_DRAG_EXITED:
                removeBackgroundColorFromView(v);
                break;
            case DragEvent.ACTION_DROP:
                Log.d(TAG, "drag dropped");

                dragView = (TextView) event.getLocalState();
                String dragViewText = dragView.getText().toString();
                String underline = getResources().getString(R.string.underline);
                if (dragViewText.equals(underline)) {
                    dragView.setVisibility(View.VISIBLE);
                    return false;
                }

                int number;
                try {
                    number = Integer.parseInt(dragViewText);
                } catch (NumberFormatException e) {
                    return false;
                }

                boolean isDropSuccessful = false;
                targetTextView = (TextView) v;
                isDropSuccessful = processMultiples(number, isDropSuccessful, targetTextView);

                if (!isDropSuccessful) {
                    Log.d(TAG, "status: not dropped");
                    dragView.setVisibility(View.VISIBLE);
                    return false;
                }

                break;
            case DragEvent.ACTION_DRAG_ENDED:
                if (!event.getResult()) {
                    dragView = (TextView) event.getLocalState();
                    dragView.setVisibility(View.VISIBLE);
                }

                removeBackgroundColorFromView(v);
                break;
        }

        return true;
    }

    private void setBackgroundColorToView(View v) {
        TextView targetTextView = (TextView) v;
        int color = ResourcesCompat.getColor(getResources(), R.color.colorNavyBlue, null);
        targetTextView.setBackgroundColor(color);
    }

    private void removeBackgroundColorFromView(View v) {
        TextView targetTextView = (TextView) v;
        int color = ResourcesCompat.getColor(getResources(), R.color.colorGrey, null);
        targetTextView.setBackgroundColor(color);
    }

    private boolean processMultiples(int number, boolean dropped, TextView targetTextView) {
        if (targetTextView.getId() == mMultipleOf2TextView.getId()) {
            dropped = processMultiplesOf2(dropped, number, targetTextView);
        } else if (targetTextView.getId() == mMultipleOf3TextView.getId()) {
            dropped = processMultiplesOf3(dropped, number, targetTextView);
        } else if (targetTextView.getId() == mMultipleOf5TextView.getId()) {
            dropped = processMultiplesOf5(dropped, number, targetTextView);
        } else if (targetTextView.getId() == mMultipleOf10TextView.getId()) {
            dropped = processMultiplesOf10(dropped, number, targetTextView);
        }
        return dropped;
    }

    private boolean processMultiplesOf2(boolean dropped, int number, TextView targetTextView) {
        Log.d(TAG, "in here: multiple of 2");
        boolean isMultipleOf2 = number % 2 == 0;
        if (isMultipleOf2) {
            Log.d(TAG, "dropped onto multiple of 2");
            insertNumberIntoTextView(targetTextView, number);
            dropped = true;
        }
        return dropped;
    }

    private boolean processMultiplesOf3(boolean dropped, int number, TextView targetTextView) {
        Log.d(TAG, "in here: multiple of 3");
        boolean isMultipleOf3 = number % 3 == 0;
        if (isMultipleOf3) {
            insertNumberIntoTextView(targetTextView, number);
            Log.d(TAG, "dropped onto multiple of 3");
            dropped = true;
        }
        return dropped;
    }

    private boolean processMultiplesOf5(boolean dropped, int number, TextView targetTextView) {
        Log.d(TAG, "in here: multiple of 5");
        boolean isMultipleOf5 = number % 5 == 0;
        if (isMultipleOf5) {
            insertNumberIntoTextView(targetTextView, number);
            Log.d(TAG, "dropped onto multiple of 5");
            dropped = true;
        }
        return dropped;
    }

    private boolean processMultiplesOf10(boolean dropped, int number, TextView targetTextView) {
        Log.d(TAG, "in here: multiple of 10");
        boolean isMultipleOf10 = number % 10 == 0;
        if (isMultipleOf10) {
            insertNumberIntoTextView(targetTextView, number);
            Log.d(TAG, "dropped onto multiple of 10");
            dropped = true;
        }
        return dropped;
    }

    private void insertNumberIntoTextView(TextView tv, int number) {
        String previousText = tv.getText().toString();
        String newText = previousText.isEmpty() ? String.valueOf(number) : previousText + "\n" + number;
        tv.setText(newText);
    }

    private class GetRandomNumbersTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                Log.d(TAG, "Fetching json data from url");
                return readJsonData(urls[0]);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String jsonData) {
            try {
                if (jsonData != null) {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONArray jsonArray = jsonObject.getJSONArray("data");
                    String[] currentNumbers = new String[NUMBERS_COUNT];
                    if (jsonArray.length() == 4) {
                        mRandomNumber1TextView.setVisibility(View.VISIBLE);
                        mRandomNumber2TextView.setVisibility(View.VISIBLE);
                        mRandomNumber3TextView.setVisibility(View.VISIBLE);
                        mRandomNumber4TextView.setVisibility(View.VISIBLE);
                        String randomNumber1 = jsonArray.get(0).toString();
                        String randomNumber2 = jsonArray.get(1).toString();
                        String randomNumber3 = jsonArray.get(2).toString();
                        String randomNumber4 = jsonArray.get(3).toString();
                        currentNumbers[0] = randomNumber1;
                        currentNumbers[1] = randomNumber2;
                        currentNumbers[2] = randomNumber3;
                        currentNumbers[3] = randomNumber4;
                        mRandomNumber1TextView.setText(randomNumber1);
                        mRandomNumber2TextView.setText(randomNumber2);
                        mRandomNumber3TextView.setText(randomNumber3);
                        mRandomNumber4TextView.setText(randomNumber4);
                    }
                    Toast.makeText(MainActivity.this,
                            TextUtils.join(", ", currentNumbers),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Connection timed out. JSON data could not be retrieved from server.",
                            Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRandomNumber1TextView = (TextView) findViewById(R.id.random_number1_textview);
        mRandomNumber1TextView.setOnTouchListener(this);
        mRandomNumber2TextView = (TextView) findViewById(R.id.random_number2_textview);
        mRandomNumber2TextView.setOnTouchListener(this);
        mRandomNumber3TextView = (TextView) findViewById(R.id.random_number3_textview);
        mRandomNumber3TextView.setOnTouchListener(this);
        mRandomNumber4TextView = (TextView) findViewById(R.id.random_number4_textview);
        mRandomNumber4TextView.setOnTouchListener(this);

        mMultipleOf2TextView = (TextView) findViewById(R.id.multiple_of_2_textview);
        mMultipleOf2TextView.setOnDragListener(this);
        mMultipleOf3TextView = (TextView) findViewById(R.id.multiple_of_3_textview);
        mMultipleOf3TextView.setOnDragListener(this);
        mMultipleOf5TextView = (TextView) findViewById(R.id.multiple_of_5_textview);
        mMultipleOf5TextView.setOnDragListener(this);
        mMultipleOf10TextView = (TextView) findViewById(R.id.multiple_of_10_textview);
        mMultipleOf10TextView.setOnDragListener(this);

        mConnectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private boolean hasInternetConnection() {
        NetworkInfo networkInfo = mConnectMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public void retrieveRandomNumbers(View v) {
        if (!hasInternetConnection()) {
            Toast.makeText(this,
                    "Please ensure you have an internet connection",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        GetRandomNumbersTask task = new GetRandomNumbersTask();
        task.execute(API_URL);
    }

    public void clearAll(View v) {
        String underline = getResources().getString(R.string.underline);
        mRandomNumber1TextView.setText(underline);
        mRandomNumber2TextView.setText(underline);
        mRandomNumber3TextView.setText(underline);
        mRandomNumber4TextView.setText(underline);
        mMultipleOf2TextView.setText("");
        mMultipleOf3TextView.setText("");
        mMultipleOf5TextView.setText("");
        mMultipleOf10TextView.setText("");
    }
}
