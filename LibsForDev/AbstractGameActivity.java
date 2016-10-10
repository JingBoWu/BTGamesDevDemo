package com.k.android.testtalkgame.AbstractGameActivity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.CallSuper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.example.k.GameCenter.Game.Game;
import com.example.k.GameCenter.Game.PlayerInfo;
import com.example.k.GameCenter.Message.GameMessage;
import com.example.k.GameCenter.Util.Parsers;
import com.example.k.GameCenter.Util.StaticVar;

import java.util.HashMap;
import java.util.List;

/**
 * Created by k on 16-9-17.
 * 继承自support v7包 的Activity
 * 可根据需要随意更改
 */
public abstract  class AbstractGameActivity extends AppCompatActivity {
    public int numOfPlayers;//总人数
    public int ownIndex;//自己的位置
    public int preIndex;//当前位置

    public Game game;
    public PlayerInfo thisPlayer;//当前玩家

    public HashMap<Integer,PlayerInfo> playerInfosInOrder;//以顺序为关键词的玩家信息

    private boolean flag_ordered=false;//是否其他玩家已经就绪 并已经安排了顺序
    private Handler gameReadyThreadHandler=new Handler();

    @Override
    @CallSuper
    protected void onDestroy() {
        unregisterReceiver(mainActivityReceiver);//注销广播接收
        super.onDestroy();
    }

    /**
    等待其他玩家就绪
     */
    Runnable r_waitForReady=new Runnable() {
        @Override
        public void run() {
            int i=0;
            while(!flag_ordered){
                SystemClock.sleep(100);
                i++;
                if(i>50){
                    gameReadyThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            timeOut();
                        }
                    });
                    return;
                }
            }
            //已就绪
            gameReadyThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    gameStart();//玩家信息已就绪，游戏开始
                    stopDialog();
                }
            });
        }
    };


    abstract public void notYourTurn();                         //提示玩家不是他的回合
    abstract public void handlePlayerActionMsg(String message); //处理其他玩家发来的信息（内部调用，）
    abstract public void moveToNextPlayer();                    //移动到下一个玩家
    abstract public void showGameResult();                      //游戏结束 展现游戏结果（内部调用）
    abstract public void gameStart();                           //游戏开始，此时其他玩家已就绪，顺序也已安排（内部调用，r_waitForReady）
    abstract public void handleGameQuit(int playerId);          //处理玩家退出（内部调用）

    /**
     * 游戏结束
     */
    @CallSuper
    public void gameOver(){
        sendGameOverMsg();//向服务器发送游戏结束信息
        showGameResult();//展现游戏结果
    }
    /**
     *初始化游戏参数
     */
    public void initGame(){
        myRegisterReceiver();//开启广播接收

        Intent intent=getIntent();

        game=JSONObject.parseObject(intent.getStringExtra(StaticVar.TAG_GAME),Game.class);
        thisPlayer=JSONObject.parseObject(intent.getStringExtra(StaticVar.TAG_PREPLAYER),PlayerInfo.class);


        preIndex=1;
        numOfPlayers=game.getTotalPlayerNumber();

        startDialog();//打开等待对话框
        new Thread(r_waitForReady).start();//启动等待其他人就绪的线程
        sendGameReadyMsg();//向服务器发送游戏就绪消息
    }

    /**
     * 处理游戏消息
     * @param message
     */
    private void handlePlayerActionMsgInAbstract(String message){
        handlePlayerActionMsg(message);//处理消息
    }

    /**
     * 启动关闭 ProgressDialog
     *
     *
     */
    public ProgressDialog progressDialog;
    public void startDialog(){
        progressDialog=ProgressDialog.show(AbstractGameActivity.this,null, "");
    }
    public void stopDialog(){
        if(progressDialog.isShowing()){
            progressDialog.dismiss();
        }
    }
    public void changeDialogTitle(String title){
        if(progressDialog.isShowing()){
            progressDialog.setTitle(title);
        }
    }
    /**
     * activity 相关
     *
     *
     *
     *
     */
    private void timeOut(){
        quitGame();
        stopDialog();
        Toast.makeText(AbstractGameActivity.this,"超时",Toast.LENGTH_SHORT)
                .show();
        backToHallActivity();
    }

    /**
     * 回到大厅
     */
    public void backToHallActivity(){
        try {
            String pkgName = StaticVar.GAME_CENTER_MAIN_PKG;
            String className=pkgName+".GameCenterMain.Activities.HallActivity";

            ComponentName cn = new ComponentName(pkgName, className);
            // 跳转到该Activity
            Intent intent=new Intent();
            intent.setComponent(cn);
            intent.putExtra(StaticVar.TAG_GAME,game);
            startActivity(intent) ;
            finish();

        }catch (Exception e) {
            backToHallError();
            Log.d("", e.toString()) ;
        }
    }
    private void backToHallError(){
        Toast.makeText(AbstractGameActivity.this,"back error",Toast.LENGTH_SHORT)
                .show();
    }
    /**
     * 服务器相关
     *
     *
     *
     *
     */
    private void sendGameReadyMsg(){
        GameMessage gameMessage=new GameMessage("");
        gameMessage.setFrom(thisPlayer.getPlayerId());
        gameMessage.setTo(game.getRoomId());
        gameMessage.setType(StaticVar.GAME_READY);

        sendMessageToService(gameMessage.getStirng());
    }
    private void sendGameOverMsg(){
        GameMessage gameMessage=new GameMessage("");
        gameMessage.setFrom(thisPlayer.getPlayerId());
        gameMessage.setTo(game.getRoomId());
        gameMessage.setType(StaticVar.GAME_OVER);

        sendMessageToService(gameMessage.getStirng());
    }

    /**
     * 退出游戏
     */
    public void quitGame(){
        int playerId=thisPlayer.getPlayerId();

        GameMessage gameMessage=new GameMessage("");
        gameMessage.setType(StaticVar.ASK_QUIT_GAME);
        gameMessage.setFrom(playerId);
        gameMessage.setTo(game.getRoomId());

        sendMessageToService(gameMessage.getStirng());

        backToHallActivity();
    }
    public void sendPlayerActionMsg(String msg){
        sendNormalMessage(msg);
    }
    private void sendNormalMessage(String msg){
        int playerId=thisPlayer.getPlayerId();

        GameMessage gameMessage=new GameMessage(msg);
        gameMessage.setType(StaticVar.NORMAL_MESSAGE);
        gameMessage.setFrom(playerId);
        gameMessage.setTo(game.getRoomId());

        sendMessageToService(gameMessage.getStirng());
    }
    /**
     * 处理玩家位置消息
     * @param orderMsg
     */
    private void handlePlayerOrderMessage(String orderMsg) {
        Log.i("----Order----",orderMsg);
        String[] strings=orderMsg.split(StaticVar.SPLIT_SECOND);
        int num=strings.length;
        if(num!=game.getPlayerInfoList().size()){
            return;
        }
        for(int i=0;i<num;i++){
            String[] info=strings[i].split(StaticVar.SPLIT_THIRD);
            int playerId=Integer.parseInt(info[0]);
            int order=Integer.parseInt(info[1]);
            if(playerId==thisPlayer.getPlayerId()){
                ownIndex=order;
            }
            setPlayerOrder(playerId,order);
        }
        Log.i("----Order----","finish init player order");
        flag_ordered=true;

    }
    private void setPlayerOrder(int playerId,int order){
        if(playerInfosInOrder==null){
            playerInfosInOrder=new HashMap<>();
        }

        List<PlayerInfo> playerInfos=game.getPlayerInfoList();
        if(playerInfos==null){
            throw new NullPointerException();
        }
        int totalNum=playerInfos.size();
        for(int i=0;i<totalNum;i++){
            PlayerInfo playerInfo=playerInfos.get(i);
            if(playerInfo.getPlayerId()==playerId){
                playerInfosInOrder.put(order,playerInfo);
                return;
            }
        }
    }
    /***
     * 广播接收
     *
     *
     *
     */

    MainActivityReceiver mainActivityReceiver;
    public  void myRegisterReceiver() {
        mainActivityReceiver = new MainActivityReceiver();
        IntentFilter intentFilter = new IntentFilter(StaticVar.TAG_ACTION_SERVICE);
        registerReceiver(mainActivityReceiver, intentFilter);
    }


    class MainActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message=intent.getStringExtra(StaticVar.TAG_MSG);
            handleMessage(message);
        }
    }

    private void handleMessage(String message) {
        if(message.equals(StaticVar.TAG_WATCH_DOG)){
            sendMessageToService(StaticVar.TAG_WATCH_DOG);
            return ;
        }
        List<GameMessage> msgs= Parsers.praseGameMsg(message);
        if (msgs==null){
            return;
        }
        for(int i=0;i<msgs.size();i++){
            handleSingleMessage(msgs.get(i));
        }
    }
    private void handleSingleMessage(GameMessage message){
        switch (message.getType()){
            case StaticVar.NORMAL_MESSAGE:
                String msg=message.getUtf_8_str();
                handlePlayerActionMsgInAbstract(msg);
                break;
            case StaticVar.GAME_QUIT:
                int playerId=message.getFrom();
                handleGameQuit(playerId);
                break;
            case StaticVar.PLAYER_ORDER:
                String orderMsg=message.getUtf_8_str();
                handlePlayerOrderMessage(orderMsg);
                break;
        }
    }

    /**
     * 向service发送信息
     * @param msg 信息内容
     */
    private void sendMessageToService(String msg){
        Intent intent=new Intent(StaticVar.TAG_ACTION);
        intent.putExtra(StaticVar.TAG_MSG,msg);
        sendBroadcast(intent);
    }

    /***
     * 监听返回按钮事件
     *
     *
     *
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            // 创建退出对话框

            AlertDialog isExit = new AlertDialog.Builder(this).create();

            // 设置对话框标题

            isExit.setTitle("现在退出吗?");

            // 添加选择按钮并注册监听

            isExit.setButton(DialogInterface.BUTTON_POSITIVE,getResources().getString(android.R.string.ok),back_listener);
            isExit.setButton(DialogInterface.BUTTON_NEGATIVE,getResources().getString(android.R.string.cancel),back_listener);

            // 显示对话框

            isExit.show();
        }
        return super.onKeyDown(keyCode, event);
    }
    DialogInterface.OnClickListener back_listener = new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int which)
        {
            switch (which)
            {
                case AlertDialog.BUTTON_POSITIVE:
                    quitGame();
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    break;
                default:
                    break;
            }
        }
    };



}
