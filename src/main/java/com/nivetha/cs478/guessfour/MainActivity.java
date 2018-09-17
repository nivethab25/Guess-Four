package com.nivetha.cs478.guessfour;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // Values to be used by handleMessage()
    public static final int SET_PLAYER1_NO = 0;
    public static final int SET_PLAYER2_NO = 1;
    public static final int UPDATE_PLAYER1_GUESSnRESPONSE = 2;
    public static final int UPDATE_PLAYER2_GUESSnRESPONSE = 3;

    // Creating instances of Player1 & Player2 HandlerThread subclasses
    public Player1 player1 = new Player1("Player-1");
    public Player2 player2 = new Player2("Player-2");

    // ArrayAdapter for the guess and responses ListView of each player
    ArrayAdapter<String> mAdapter1;
    ArrayAdapter<String> mAdapter2;

    // To check the status of the game
    boolean shouldContinue = true;

    // Views
    private Button mStart;
    private TextView mPlayer1Number;
    private TextView mPlayer2Number;
    private ListView mPlayer1GuessnResponse;
    private ListView mPlayer2GuessnResponse;

    // ArrayLists to hold Player1 & Player2 guesses and responses
    private ArrayList<String> mPlayer1List = new ArrayList<String>();
    private ArrayList<String> mPlayer2List = new ArrayList<String>();

    // UI thread Message Handler
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                // Set Player1 Secret Number
                case SET_PLAYER1_NO:
                    String secretNo1 = msg.obj.toString();
                    mPlayer1Number.setText(secretNo1.substring(1, secretNo1.length() - 1).replaceAll(",", ""));
                    break;
                // Set Player2 Secret Number
                case SET_PLAYER2_NO:
                    String secretNo2 = msg.obj.toString();
                    mPlayer2Number.setText(secretNo2.substring(1, secretNo2.length() - 1).replaceAll(",", ""));
                    break;
                // Update Player1 Guess and Response
                case UPDATE_PLAYER1_GUESSnRESPONSE:
                    mPlayer1List.add((String) msg.obj);
                    mAdapter1.notifyDataSetChanged();
                    break;
                // Update Player1 Guess and Response
                case UPDATE_PLAYER2_GUESSnRESPONSE:
                    mPlayer2List.add((String) msg.obj);
                    mAdapter2.notifyDataSetChanged();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Binding to view item for start button
        mStart = (Button) findViewById(R.id.start);

        // Starting Player1 & Player2 threads
        player1.start();
        player2.start();

        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Setting the view items empty to handle game restart case
                mPlayer1Number = (TextView) findViewById(R.id.player1Number);
                mPlayer1Number.setText("");
                mPlayer2Number = (TextView) findViewById(R.id.player2Number);
                mPlayer2Number.setText("");
                mPlayer1GuessnResponse = (ListView) findViewById(R.id.player1GuessnResponse);
                mPlayer2GuessnResponse = (ListView) findViewById(R.id.player2GuessnResponse);

                mPlayer1List.clear();
                mAdapter1 = new ArrayAdapter<String>(getApplicationContext(), R.layout.guessresponse_item, mPlayer1List);
                mPlayer1GuessnResponse.setAdapter(mAdapter1);

                mPlayer2List.clear();
                mAdapter2 = new ArrayAdapter<String>(getApplicationContext(), R.layout.guessresponse_item, mPlayer2List);
                mPlayer2GuessnResponse.setAdapter(mAdapter2);

                shouldContinue = true;

                // To remove the messages that were earlier sent so that game can be restarted afresh
                player1.player1Handler.removeCallbacksAndMessages(null);
                player2.player2Handler.removeCallbacksAndMessages(null);

                // Send Message to Player1 to generate Secret Number
                Message msg1 = player1.player1Handler.obtainMessage(Player1.GENERATE_SECRET_NUMBER);
                player1.player1Handler.sendMessage(msg1);

                // Send Message to Player2 to generate Secret Number
                Message msg2 = player2.player2Handler.obtainMessage(Player2.GENERATE_SECRET_NUMBER);
                player2.player2Handler.sendMessage(msg2);

                // Send Message to Player1 to make it's First Guess
                Message msg3 = player1.player1Handler.obtainMessage(Player1.MAKE_FIRST_GUESS);
                player1.player1Handler.sendMessageDelayed(msg3, 2000);

                // Send Message to Player2 to make it's First Guess
                Message msg4 = player2.player2Handler.obtainMessage(Player2.MAKE_FIRST_GUESS);
                player2.player2Handler.sendMessageDelayed(msg4, 2000);

            }
        });
    }

    // To check if it's the end of the game i.e. each player has made 20 unsuccessful guesses or a player has won the game
    public boolean endOfGame() {

        if (player1.guessCount == 20 && player2.guessCount == 20 || shouldContinue == false) {
            shouldContinue = false;
            Toast.makeText(MainActivity.this, "Game Over!!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    public class Player1 extends HandlerThread {

        // Values to be used by Player1
        public static final int GENERATE_SECRET_NUMBER = 0;
        public static final int MAKE_FIRST_GUESS = 1;
        public static final int CHECK_N_REPORT_GUESS = 2;
        public static final int GUESS_FROM_RESPONSE = 3;

        // Storing previous guess to be used for the next guess
        public LinkedHashSet<Integer> prevGuess = new LinkedHashSet<>();

        private int guessCount = 0;
        private Set<Integer> player1no;

        // Player1 message handler
        public Handler player1Handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {

                int what = msg.what;
                switch (what) {

                    // Make First Guess
                    case MAKE_FIRST_GUESS:
                        guessCount = makeNextGuess(0,0,0);
                        break;

                    // Generate Secret Number
                    case GENERATE_SECRET_NUMBER:
                        player1no = new LinkedHashSet<>();
                        Random r = new Random();
                        while (player1no.size() < 4) {
                            if (player1no.isEmpty()) {
                                int r1 = r.nextInt(9);
                                if (r1 == 0)
                                    player1no.add(++r1);
                                else
                                    player1no.add(r1);
                            } else
                                player1no.add(r.nextInt(9));
                        }

                        Message msg1 = mHandler.obtainMessage(MainActivity.SET_PLAYER1_NO);
                        msg1.obj = player1no.toString();
                        mHandler.sendMessage(msg1);
                        break;

                    // Check and report Player2's Guess
                    case CHECK_N_REPORT_GUESS:
                        ArrayList<Integer> guessedNo = (ArrayList<Integer>) msg.obj;
                        ArrayList<Integer> actualNo = new ArrayList<>(player1no);

                        int correctPositions = 0;
                        int wrongPositions = 0;

                        if (actualNo.get(0) == guessedNo.get(0)) {
                            ++correctPositions;
                        } else if (actualNo.contains(guessedNo.get(0))) {
                            ++wrongPositions;
                        }

                        if (actualNo.get(1) == guessedNo.get(1)) {
                            ++correctPositions;
                        } else if (actualNo.contains(guessedNo.get(1))) {
                            ++wrongPositions;
                        }

                        if (actualNo.get(2) == guessedNo.get(2)) {
                            ++correctPositions;
                        } else if (actualNo.contains(guessedNo.get(2))) {
                            ++wrongPositions;
                        }

                        if (actualNo.get(3) == guessedNo.get(3)) {
                            ++correctPositions;
                        } else if (actualNo.contains(guessedNo.get(3))) {
                            ++wrongPositions;
                        }

                        if (correctPositions == 4) {

                            Handler handler2 = player2.player2Handler;
                            Message msg2 = handler2.obtainMessage(player2.GUESS_FROM_RESPONSE);
                            msg2.arg1 = correctPositions;
                            msg2.arg2 = -1;
                            handler2.sendMessage(msg2);

                            // Posting a runnable to make a toast to UI Thread when all the digits have been guessed right
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Player 2 Wins !!", Toast.LENGTH_SHORT).show();
                                }
                            });

                            shouldContinue = false;

                            // Posting a runnable to UI Thread to halt the game
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    shouldContinue = endOfGame();
                                }
                            }, 2000);

                        } else {
                            Handler handler2 = player2.player2Handler;
                            Message msg2 = handler2.obtainMessage(player2.GUESS_FROM_RESPONSE);
                            msg2.arg1 = correctPositions;
                            msg2.arg2 = wrongPositions;
                            handler2.sendMessage(msg2);
                        }

                        break;

                    // Make Next Guess
                    case GUESS_FROM_RESPONSE:
                        int correctGuesses = msg.arg1;
                        int wrongPosGuesses = msg.arg2;

                        String prevGuessString = prevGuess.toString();
                        String guess = "Guess : " + prevGuessString.substring(1, prevGuessString.length() - 1).replaceAll(",", "");

                        String response = "\nResponse : " + correctGuesses + " digit(s) guessed in correct position(s) \n"
                                + (wrongPosGuesses == -1 ? 0 : wrongPosGuesses) + " digit(s) guessed in wrong position(s)";

                        Message msg2 = mHandler.obtainMessage(MainActivity.UPDATE_PLAYER1_GUESSnRESPONSE);
                        msg2.obj = guess + response;
                        mHandler.sendMessage(msg2);

                        if (shouldContinue && wrongPosGuesses != -1)
                            guessCount = makeNextGuess(guessCount, correctGuesses, wrongPosGuesses);
                        break;

                }
            }


        };

        public Player1(String name) {
            super(name);
        }

        public void run() {

        }

        // Make Next Guess
        private int makeNextGuess(int guessCount, int correctGuesses, int wrongGuesses) {
            if (guessCount < 20) {

                Random rn = new Random();
                final Set<Integer> nextGuess = new LinkedHashSet<>();
                // Make next guess from previous guess
                if (correctGuesses == 3 || wrongGuesses == 3) {

                    // If 3 digits guessed in correct position or 3 digits guessed in wrong position , replace a random digit with a random digit
                        Integer[] newGuess = prevGuess.toArray(new Integer[prevGuess.size()]);
                        int randomIndex = rn.nextInt(prevGuess.size() - 1);
                        int replacementDigit = randomIndex == 0 ? rn.nextInt(9) + 1 : rn.nextInt(9);
                        if (prevGuess.contains(replacementDigit)) {
                            while (prevGuess.contains(replacementDigit))
                                replacementDigit = randomIndex == 0 ? rn.nextInt(9) + 1 : rn.nextInt(9);
                        }

                        newGuess[randomIndex] = replacementDigit;
                    nextGuess.addAll(Arrays.asList(newGuess));

                    if(nextGuess.size()<4)
                        nextGuess.add(rn.nextInt(9));

                }
                else if(guessCount > 0 && correctGuesses + wrongGuesses == 0) {
                    // if none of the digits are guessed, make a new guess excluding the digits int he previous guess
                    while (nextGuess.size() < 4) {
                        int r1= rn.nextInt(9);
                        while (prevGuess.contains(r1))
                            r1 = rn.nextInt(9);
                        if (nextGuess.isEmpty()) {
                            if (r1 == 0)
                                nextGuess.add(++r1);
                            else
                                nextGuess.add(r1);
                        } else
                            nextGuess.add(r1);
                    }

                }
                else if (wrongGuesses == 4){
                    // If all the digits are guessed at wrong positions , shuffle the digits of the previous guess
                    ArrayList<Integer> previousGuess = new ArrayList<>(prevGuess);
                    Collections.shuffle(previousGuess);
                    nextGuess.addAll(previousGuess);
                }
                else {
                    // else make a new random guess
                    while (nextGuess.size() < 4) {
                        if (nextGuess.isEmpty()) {
                            int r1 = rn.nextInt(9);
                            if (r1 == 0)
                                nextGuess.add(++r1);
                            else
                                nextGuess.add(r1);
                        } else
                            nextGuess.add(rn.nextInt(9));
                    }
                }

                    // Updating previous guess with the new guess made
                    prevGuess = new LinkedHashSet<>(nextGuess);

                    // Sending message to Player2 with the no. of digits guessed in correct positions and wrong positions
                    Handler handler2 = player2.player2Handler;
                    Message msg2 = handler2.obtainMessage(Player2.CHECK_N_REPORT_GUESS);
                    msg2.obj = new ArrayList<Integer>(nextGuess);
                    handler2.sendMessageDelayed(msg2, 2000);
            }

                // If guess count is 20 setting shouldContinue to false
                if (guessCount == 20)
                    shouldContinue = endOfGame();

            // Updating guess count
            return ++guessCount;
        }
    }

    public class Player2 extends HandlerThread {

        // Value to be used by handle message
        public static final int GENERATE_SECRET_NUMBER = 0;
        public static final int MAKE_FIRST_GUESS = 1;
        public static final int CHECK_N_REPORT_GUESS = 2;
        public static final int GUESS_FROM_RESPONSE = 3;

        // Storing previous guess to be used for next guess
        public ArrayList<Integer> prevGuess = new ArrayList<>();

        // Storing previous partially correct guesses
        public ArrayList<ArrayList<Integer>> partiallyCorrectGuesses = new ArrayList<>();

        private int guessCount = 0;
        private Set<Integer> player2no;
        public Handler player2Handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {

                int what = msg.what;

                switch (what) {

                    // Make First Guess
                    case MAKE_FIRST_GUESS:
                        guessCount = makeNextGuess(0, 0, 0);
                        break;

                    // Generate Secret Number
                    case GENERATE_SECRET_NUMBER:
                        player2no = new LinkedHashSet<>();
                        Random r = new Random();
                        while (player2no.size() < 4) {
                            if (player2no.isEmpty()) {
                                int r1 = r.nextInt(9);
                                if (r1 == 0)
                                    player2no.add(++r1);
                                else
                                    player2no.add(r1);
                            } else
                                player2no.add(r.nextInt(9));
                        }

                        Message msg1 = mHandler.obtainMessage(MainActivity.SET_PLAYER2_NO);
                        msg1.obj = player2no.toString();
                        mHandler.sendMessage(msg1);
                        break;

                    // Check & report the no. of digits guessed at correct positions and wrong positions to Player1
                    case CHECK_N_REPORT_GUESS:
                        ArrayList<Integer> guessedNo = (ArrayList<Integer>) msg.obj;
                        ArrayList<Integer> actualNo = new ArrayList<>(player2no);

                        int correctPositions = 0;
                        int wrongPositions = 0;

                        if (actualNo.get(0) == guessedNo.get(0)) {
                            ++correctPositions;
                        } else if (actualNo.contains(guessedNo.get(0))) {
                            ++wrongPositions;
                        }

                        if (actualNo.get(1) == guessedNo.get(1)) {
                            ++correctPositions;
                        } else if (actualNo.contains(guessedNo.get(1))) {
                            ++wrongPositions;
                        }

                        if (actualNo.get(2) == guessedNo.get(2)) {
                            ++correctPositions;
                        } else if (actualNo.contains(guessedNo.get(2))) {
                            ++wrongPositions;
                        }

                        if (actualNo.get(3) == guessedNo.get(3)) {
                            ++correctPositions;
                        } else if (actualNo.contains(guessedNo.get(3))) {
                            ++wrongPositions;
                        }

                        if (correctPositions == 4) {

                            Handler handler1 = player1.player1Handler;
                            Message msg2 = handler1.obtainMessage(Player1.GUESS_FROM_RESPONSE);
                            msg2.arg1 = correctPositions;
                            msg2.arg2 = -1;
                            handler1.sendMessage(msg2);

                            // Posting a runnable to UI Thread to make a toast when all the digits have been guessed right by Player1
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Player 1 Wins !!", Toast.LENGTH_SHORT).show();
                                }
                            }, 2000);

                            shouldContinue = false;

                            // Posting a runnable to UI Thread to stop the game
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    shouldContinue = endOfGame();
                                }
                            }, 2000);

                        } else {
                            Handler handler1 = player1.player1Handler;
                            Message msg2 = handler1.obtainMessage(Player1.GUESS_FROM_RESPONSE);
                            msg2.arg1 = correctPositions;
                            msg2.arg2 = wrongPositions;
                            handler1.sendMessage(msg2);
                        }

                        break;

                    // Make next guess from previous guess
                    case GUESS_FROM_RESPONSE:
                        int correctGuesses = msg.arg1;
                        int wrongPosGuesses = msg.arg2;

                        String prevGuessString = prevGuess.toString();
                        String guess = "Guess : " + prevGuessString.substring(1, prevGuessString.length() - 1).replaceAll(",", "");

                        String response = "\nResponse : " + correctGuesses + " digit(s) guessed in correct position(s) \n"
                                + (wrongPosGuesses == -1 ? 0 : wrongPosGuesses) + " digit(s) guessed in wrong position(s)";

                        Message msg3 = mHandler.obtainMessage(MainActivity.UPDATE_PLAYER2_GUESSnRESPONSE);
                        msg3.obj = guess + response;
                        mHandler.sendMessage(msg3);

                        if (shouldContinue && wrongPosGuesses != -1)
                            guessCount = makeNextGuess(guessCount, correctGuesses, wrongPosGuesses);
                        break;


                }
            }

        };

        public Player2(String name) {
            super(name);
        }

        public void run() {

        }

        private int makeNextGuess(int guessCount, int correctGuesses, int wrongGuesses) {

            if (guessCount < 20) {

                Random rn = new Random();
                final Set<Integer> nextGuess = new LinkedHashSet<>();
                if (correctGuesses + wrongGuesses == 4 || wrongGuesses > 2) {
                    // Making a guess not made so far when atleast 3 digits have been guessed (at correct/ wrong positions)
                    partiallyCorrectGuesses.add(prevGuess);
                    ArrayList<Integer> previousGuess = new ArrayList<>(prevGuess);
                    Collections.shuffle(previousGuess);
                    if(!partiallyCorrectGuesses.contains(previousGuess) && previousGuess != prevGuess)
                        nextGuess.addAll(previousGuess);
                    else{
                        while (!partiallyCorrectGuesses.contains(previousGuess) && previousGuess != prevGuess)
                            Collections.shuffle(previousGuess);
                        nextGuess.addAll(previousGuess);
                    }
                } else {
                    // Else making a random guess
                    while (nextGuess.size() < 4) {
                        if (nextGuess.isEmpty()) {
                            int r1 = rn.nextInt(9);
                            if (r1 == 0)
                                nextGuess.add(++r1);
                            else
                                nextGuess.add(r1);
                        } else
                            nextGuess.add(rn.nextInt(9));
                    }
                }

                // Updating previous guess with current guess
                prevGuess = new ArrayList<>(nextGuess);

                // Sending message to Player1 to check and report the current guess
                Handler handler1 = player1.player1Handler;
                Message msg2 = handler1.obtainMessage(Player1.CHECK_N_REPORT_GUESS);
                msg2.obj = new ArrayList<Integer>(nextGuess);

                // Waiting for 2 secs before reporting to opponent
                handler1.sendMessageDelayed(msg2, 2000);

            }

            // When guess count is 20 set shouldContinue to false
            if (guessCount == 20)
                shouldContinue = endOfGame();

            // Updating guess count
            return ++guessCount;
        }
    }
}
