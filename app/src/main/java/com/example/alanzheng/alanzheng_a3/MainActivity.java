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
    private static final int CONNECTION_READ_TIMEOUT = 15000;
    private static final int CONNECTION_TIMEOUT = 50000;
    private static final int CONNECTION_INPUT_STREAM_LINE_LENGTH = 5000;

    private String mUnderline;

    private TextView mRandomNumber1TextView;
    private TextView mRandomNumber2TextView;
    private TextView mRandomNumber3TextView;
    private TextView mRandomNumber4TextView;

    private TextView mMultipleOf2TextView;
    private TextView mMultipleOf3TextView;
    private TextView mMultipleOf5TextView;
    private TextView mMultipleOf10TextView;

    private ConnectivityManager mConnectMgr;

    /**
     * Reads an input stream from a connection of specified length
     * @param stream obtained through a connection
     * @param len number of characters to be read from input stream
     * @return result of the input stream
     * @throws IOException
     */
    private String readIt(InputStream stream, int len) throws IOException {
        Reader reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        // write the data from the stream into the buffer
        reader.read(buffer);
        return new String(buffer);
    }

    /**
     *
     * @param remoteUrl url of the API to read information from
     * @return a string with the results of the API
     * @throws IOException
     */
    private String readJsonData(String remoteUrl) throws IOException {
        InputStream is = null;

        URL url = new URL(remoteUrl);
        // parse url, finds protocol and creates the HttpURLConnection object
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            // configure request for API
            conn.setReadTimeout(CONNECTION_READ_TIMEOUT);
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // conn.connect() is not required as getResponseCode()
            // will implicitly perform the connection
            if (conn.getResponseCode() == 200) {
                is = conn.getInputStream();
                // CONNECTION_INPUT_STREAM_LINE_LENGTH length is assumed to be greater
                // than the length of the result coming from the stream
                return readIt(is, CONNECTION_INPUT_STREAM_LINE_LENGTH);
            }
        } finally {
            if (is != null) {
                // clean up connection now that we're done
                is.close();
                conn.disconnect();
            }
        }

        // this should never be reached
        return null;
    }

    /**
     * Handles the case where the TextView for the random numbers are dragged
     * and displays a shadow for the TextView being dragged
     * @param v the TextView being dragged
     * @param event the actions being performed on the TextView
     * @return true if actions are successful, false otherwise
     */
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

    /**
     * Handles drag events when a random number TextView is dragged
     * over one of the TextViews displaying a list of multiples of numbers
     * @param v the target view which responds to events when something is dragged over it
     * @param event holds information for the object being dragged
     * @return true if drag is successful, false otherwise
     */
    @Override
    public boolean onDrag(View v, DragEvent event) {

        TextView dragView;
        TextView targetTextView;

        switch (event.getAction()) {
            // triggered when drag is initiated
            case DragEvent.ACTION_DRAG_STARTED:
                break;
            // triggered when an object drag enters a target view (entering any of the 4 multiples boxes)
            case DragEvent.ACTION_DRAG_ENTERED:
                // change the background color when we enter the target view
                setBackgroundColorToView(v);
                break;
            // triggered when an object drag exits the target view (leaving any of the 4 multiples boxes)
            case DragEvent.ACTION_DRAG_EXITED:
                // remove the background color when we leave the target view
                removeBackgroundColorFromView(v);
                break;
            case DragEvent.ACTION_DROP:
                Log.d(TAG, "drag dropped");
                // retrieve information for the TextView being dragged
                dragView = (TextView) event.getLocalState();
                String dragViewText = dragView.getText().toString();
                // quick sanity check to make sure that we're not going to
                // end up dropping an empty TextView
                // an mUnderline -> _____ indicates the TextView is empty
                if (dragViewText.equals(mUnderline)) {
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
                    // drag is not successful as it was dragged to
                    // an invalid multiple (e.g: the number 67 dragged to 'Multiples of 3')
                    dragView.setVisibility(View.VISIBLE);
                    return false;
                }

                // drop is successful and made inside of target view
                // (inside one of the multiples boxes)
                dragView.setVisibility(View.INVISIBLE);
                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                // reset background color of target view when we finish dragging
                removeBackgroundColorFromView(v);

                dragView = (TextView) event.getLocalState();
                dragViewText = dragView.getText().toString();
                if (dragViewText.equals(mUnderline)) {
                    // reset the TextView to its original state
                    // (The TextView has an underline -> ______ as its value)
                    // if it is being dragged
                    dragView.setVisibility(View.VISIBLE);
                    return false;
                }

                // getResult() returns true if dragged object enters and drops
                // inside target view (inside the Multiple boxes), false otherwise
                if (!event.getResult()) {
                    // drop is unsuccessful and made outside of target view
                    // (outside of the multiples boxes)
                    dragView.setVisibility(View.VISIBLE);
                    return false;
                }

                break;
        }

        return true;
    }

    /**
     * Sets the background color to a particular color when
     * a TextView for the random numbers is dragged over
     * the target view (the Multiples boxes)
     * @param v the target view
     */
    private void setBackgroundColorToView(View v) {
        TextView targetTextView = (TextView) v;
        int color = ResourcesCompat.getColor(getResources(), R.color.colorNavyBlue, null);
        targetTextView.setBackgroundColor(color);
    }

    /**
     * Removes the background color when a TextView leaves
     * the target view
     * @param v the target view
     */
    private void removeBackgroundColorFromView(View v) {
        TextView targetTextView = (TextView) v;
        int color = ResourcesCompat.getColor(getResources(), R.color.colorGrey, null);
        targetTextView.setBackgroundColor(color);
    }

    /**
     * Handles different cases for the different target views
     * @param number the number associated to the TextView being dragged
     * @param isDropSuccessful determines whether the drop has been successful
     * @param targetTextView the target view
     * @return true if the drop is successful meaning that the number has
     * been dropped in the box associated with the correct multiple, false otherwise
     */
    private boolean processMultiples(int number, boolean isDropSuccessful, TextView targetTextView) {
        if (targetTextView.getId() == mMultipleOf2TextView.getId()) {
            isDropSuccessful = processMultiplesOf2(isDropSuccessful, number, targetTextView);
        } else if (targetTextView.getId() == mMultipleOf3TextView.getId()) {
            isDropSuccessful = processMultiplesOf3(isDropSuccessful, number, targetTextView);
        } else if (targetTextView.getId() == mMultipleOf5TextView.getId()) {
            isDropSuccessful = processMultiplesOf5(isDropSuccessful, number, targetTextView);
        } else if (targetTextView.getId() == mMultipleOf10TextView.getId()) {
            isDropSuccessful = processMultiplesOf10(isDropSuccessful, number, targetTextView);
        }
        return isDropSuccessful;
    }


    private boolean processMultiplesOf2(boolean isDropSuccessful, int number, TextView targetTextView) {
        Log.d(TAG, "in here: multiple of 2");
        boolean isMultipleOf2 = number % 2 == 0;
        if (isMultipleOf2) {
            Log.d(TAG, "dropped onto multiple of 2");
            insertNumberIntoTextView(targetTextView, number);
            isDropSuccessful = true;
        }
        return isDropSuccessful;
    }

    private boolean processMultiplesOf3(boolean isDropSuccessful, int number, TextView targetTextView) {
        Log.d(TAG, "in here: multiple of 3");
        boolean isMultipleOf3 = number % 3 == 0;
        if (isMultipleOf3) {
            insertNumberIntoTextView(targetTextView, number);
            Log.d(TAG, "dropped onto multiple of 3");
            isDropSuccessful = true;
        }
        return isDropSuccessful;
    }

    private boolean processMultiplesOf5(boolean isDropSuccessful, int number, TextView targetTextView) {
        Log.d(TAG, "in here: multiple of 5");
        boolean isMultipleOf5 = number % 5 == 0;
        if (isMultipleOf5) {
            insertNumberIntoTextView(targetTextView, number);
            Log.d(TAG, "dropped onto multiple of 5");
            isDropSuccessful = true;
        }
        return isDropSuccessful;
    }

    private boolean processMultiplesOf10(boolean isDropSuccessful, int number, TextView targetTextView) {
        Log.d(TAG, "in here: multiple of 10");
        boolean isMultipleOf10 = number % 10 == 0;
        if (isMultipleOf10) {
            insertNumberIntoTextView(targetTextView, number);
            Log.d(TAG, "dropped onto multiple of 10");
            isDropSuccessful = true;
        }
        return isDropSuccessful;
    }

    private void insertNumberIntoTextView(TextView tv, int number) {
        String previousText = tv.getText().toString();
        String newText = previousText.isEmpty() ? String.valueOf(number) : previousText + "\n" + number;
        tv.setText(newText);
    }

    /**
     * Asynchronous task for fetching random numbers in the background
     * and updating the associated random number TextViews when the
     * information is ready
     */
    private class GetRandomNumbersTask extends AsyncTask<String, Void, String> {

        /**
         * Fetch information from the API provided by urls[0]
         * @param urls a list of urls but in our case we only care
         *             about the first one
         * @return a json string representing data retrieved from the API
         */
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

        /**
         * Process the json data and sets the corresponding TextViews
         * for the random numbers
         * @param jsonData the information being returned from doInBackground()
         */
        @Override
        protected void onPostExecute(String jsonData) {
            try {
                if (jsonData != null) {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONArray jsonArray = jsonObject.getJSONArray("data");
                    String[] currentNumbers = new String[NUMBERS_COUNT];
                    // quick sanity check to make sure all 4 random
                    // numbers are present
                    if (jsonArray.length() == 4) {
                        // make all TextViews for the random numbers
                        // visible in case they were hidden before
                        setAllRandomNumberTextVisible();
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
                    // display the numbers retrieved from the API as a Toast message
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

        // retrieve 'underline' string resource for later use
        mUnderline = getResources().getString(R.string.underline);

        // text views which holds the random numbers and are dragged
        mRandomNumber1TextView = (TextView) findViewById(R.id.random_number1_textview);
        mRandomNumber1TextView.setOnTouchListener(this);
        mRandomNumber2TextView = (TextView) findViewById(R.id.random_number2_textview);
        mRandomNumber2TextView.setOnTouchListener(this);
        mRandomNumber3TextView = (TextView) findViewById(R.id.random_number3_textview);
        mRandomNumber3TextView.setOnTouchListener(this);
        mRandomNumber4TextView = (TextView) findViewById(R.id.random_number4_textview);
        mRandomNumber4TextView.setOnTouchListener(this);

        // target text views
        mMultipleOf2TextView = (TextView) findViewById(R.id.multiple_of_2_textview);
        mMultipleOf2TextView.setOnDragListener(this);
        mMultipleOf3TextView = (TextView) findViewById(R.id.multiple_of_3_textview);
        mMultipleOf3TextView.setOnDragListener(this);
        mMultipleOf5TextView = (TextView) findViewById(R.id.multiple_of_5_textview);
        mMultipleOf5TextView.setOnDragListener(this);
        mMultipleOf10TextView = (TextView) findViewById(R.id.multiple_of_10_textview);
        mMultipleOf10TextView.setOnDragListener(this);

        // retrieve an instance from ConnectivityManager for checking connectivity later
        mConnectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Checks internet connection
     * @return true if there is internet connectivity, false otherwise
     */
    private boolean hasInternetConnection() {
        NetworkInfo networkInfo = mConnectMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * Handler for @+id/retrieve_random_number_bttn
     * @param v
     */
    public void retrieveRandomNumbers(View v) {
        if (!hasInternetConnection()) {
            Toast.makeText(this,
                    "Please ensure you have an internet connection",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        GetRandomNumbersTask task = new GetRandomNumbersTask();
        // start asynchronous retrieval of random numbers
        task.execute(API_URL);
    }

    /**
     * Handler for @+id/clear_all_bttn
     * @param v
     */
    public void clearAll(View v) {
        setAllRandomNumberTextVisible();
        underlineAllRandomNumberTextView();
        clearMultiplesTextView();
    }

    private void setAllRandomNumberTextVisible() {
        mRandomNumber1TextView.setVisibility(View.VISIBLE);
        mRandomNumber2TextView.setVisibility(View.VISIBLE);
        mRandomNumber3TextView.setVisibility(View.VISIBLE);
        mRandomNumber4TextView.setVisibility(View.VISIBLE);
    }

    private void underlineAllRandomNumberTextView() {
        String underline = getResources().getString(R.string.underline);
        mRandomNumber1TextView.setText(underline);
        mRandomNumber2TextView.setText(underline);
        mRandomNumber3TextView.setText(underline);
        mRandomNumber4TextView.setText(underline);
    }

    private void clearMultiplesTextView() {
        mMultipleOf2TextView.setText("");
        mMultipleOf3TextView.setText("");
        mMultipleOf5TextView.setText("");
        mMultipleOf10TextView.setText("");
    }
}
