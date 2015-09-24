package com.example.lu.mdpgrp13;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class CommandActivity extends Activity {

    public static final String MDP_PREFERENCES = "mdpPrefKey" ;
    public static final String COMMAND_1 = "cmdOneKey";
    public static final String COMMAND_2 = "cmdTwoKey";
    private SharedPreferences sharedpreferences;

    private EditText editTxtCmd1;
    private EditText editTxtCmd2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command);

        editTxtCmd1 = (EditText)findViewById(R.id.editTxtCmd1);
        editTxtCmd2 = (EditText)findViewById(R.id.editTxtCmd2);

        sharedpreferences = getSharedPreferences(MDP_PREFERENCES, Context.MODE_PRIVATE);

        String cmd1 = sharedpreferences.getString(COMMAND_1, "");
        String cmd2 = sharedpreferences.getString(COMMAND_2, "");

        editTxtCmd1.setText(cmd1);
        editTxtCmd2.setText(cmd2);
    }

    public void saveCommand(View view) {


        String cmd1 = editTxtCmd1.getText().toString();
        String cmd2 = editTxtCmd2.getText().toString();

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(COMMAND_1, cmd1);
        editor.putString(COMMAND_2, cmd2);
        editor.commit();

        finish();
    }

    public void closeActivity(View view) {
        finish();
    }
}
