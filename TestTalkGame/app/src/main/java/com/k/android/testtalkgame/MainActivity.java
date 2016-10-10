package com.k.android.testtalkgame;

import android.content.DialogInterface;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.k.GameCenter.Game.PlayerInfo;
import com.k.android.testtalkgame.AbstractGameActivity.AbstractGameActivity;

import org.w3c.dom.Text;

public class MainActivity extends AbstractGameActivity {

    TextView tv_left_player;
    TextView tv_right_player;
    TextView tv_left_message;
    TextView tv_right_message;

    EditText et_message;
    Button bt_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

        }

        initView();
        initListener();
        initGame();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        switch (id){
            case android.R.id.home:
                quitGame();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initListener() {
        bt_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(preIndex==ownIndex){
                    String str=et_message.getText().toString();
                    if(str.equals("")){
                        et_message.setError("Empty");
                        return ;
                    }

                    sendPlayerActionMsg(str);
                    if(ownIndex==1){
                        tv_left_message.append(str+"\n");
                    }else{
                        tv_right_message.append(str+"\n");
                    }
                    moveToNextPlayer();
                }else{
                    notYourTurn();
                }
            }
        });
    }

    private void initView() {
        tv_left_player=(TextView)findViewById(R.id.main_tv_left_player_info);
        tv_right_player=(TextView)findViewById(R.id.main_tv_right_player_info);

        tv_left_message=(TextView)findViewById(R.id.main_tv_left_message);
        tv_right_message=(TextView)findViewById(R.id.main_tv_right_message);

        et_message=(EditText)findViewById(R.id.main_et_message);

        bt_send=(Button)findViewById(R.id.main_bt_send);
    }

    /**
     *
     *
     *
     *
     *
     *
     *
     */

    @Override
    public void notYourTurn() {
        Toast.makeText(MainActivity.this,"Not your turn",Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void handlePlayerActionMsg(String message) {
        if(preIndex==1){
            tv_left_message.append(message+"\n");
        }else{
            tv_right_message.append(message+"\n");
        }
        moveToNextPlayer();
    }

    @Override
    public void moveToNextPlayer() {
        preIndex++;
        if (preIndex > 2) {
            preIndex=1;
        }
        if(preIndex==1){
            tv_left_player.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            tv_right_player.setBackgroundColor(getResources().getColor(android.R.color.white));
        }else{
            tv_left_player.setBackgroundColor(getResources().getColor(android.R.color.white));
            tv_right_player.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        }
    }

    @Override
    public void showGameResult() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Message")
                .setMessage("对方已经退出")
                .setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        backToHallActivity();
                    }
                })
                .show();
    }

    @Override
    public void gameStart() {
        PlayerInfo playerInfo_1=playerInfosInOrder.get(1);
        PlayerInfo playerInfo_2=playerInfosInOrder.get(2);

        String player_1="Id:"+playerInfo_1.getPlayerId()+"\n"+"Name:"+playerInfo_1.getName();
        tv_left_player.setText(player_1);

        String player_2="Id:"+playerInfo_2.getPlayerId()+"\n"+"Name:"+playerInfo_2.getName();
        tv_right_player.setText(player_2);

        tv_left_player.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        tv_right_player.setBackgroundColor(getResources().getColor(android.R.color.white));

        Toast.makeText(MainActivity.this,"Game Start",Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void handleGameQuit(int playerId) {
        gameOver();
    }


}
