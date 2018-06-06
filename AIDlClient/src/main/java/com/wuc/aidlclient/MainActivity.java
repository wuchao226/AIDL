package com.wuc.aidlclient;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;

import com.wuc.aidltest.ICalculateInterface;
import com.wuc.aidltest.IOnNewPersonArrivedListener;
import com.wuc.aidltest.Person;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int MESSAGE_NEW_PERSON_ARRIVED = 1;

    private AppCompatEditText mEdt_num1;
    private AppCompatEditText mEdt_num2;
    private AppCompatButton mBtn_calculate;
    private AppCompatTextView mTxt_result;


    private ICalculateInterface mICalculateInterface;
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            if (mICalculateInterface == null) {
                return;
            }
            //移除之前绑定的代理并重新绑定远程服务
            mICalculateInterface.asBinder().unlinkToDeath(mDeathRecipient, 0);
            mICalculateInterface = null;
            bindService();
        }
    };

    ;
    private ClientHandler mHandler = new ClientHandler();
    /**
     * 当客户端发起远程请求时，由于当前线程会被挂起直至服务端进程返回数据，所以如果一个远程方法是很耗时的，
     * 则不能在UI 线程中发起此远程请求，为了避免阻塞UI 线程出现ANR
     * 由于服务端的Binder 方法运行在 Binder 的线程池中，所以 Binder 方法不管是否耗时都应该采用同步的方式去实现，
     * 因为它已经运行在一个线程中了
     */
    private IOnNewPersonArrivedListener mOnNewPersonArrivedListener = new IOnNewPersonArrivedListener.Stub() {
        @Override
        public void onNewPersonArrived(Person person) throws RemoteException {
            mHandler.obtainMessage(MESSAGE_NEW_PERSON_ARRIVED, person).sendToTarget();
        }
    };
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //判断Binder是否死忙
            //boolean binderAlive = service.isBinderAlive();
            //用于将服务端的Binder对象转换为客户端需要的AIDL接口类型的对象
            mICalculateInterface = ICalculateInterface.Stub.asInterface(service);
            try {
                mICalculateInterface.registerListener(mOnNewPersonArrivedListener);
                //给binder设置死忙代理，当Binder死忙时就可以收到通知
                service.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //连接断开，释放AIDL Binder对象
            mICalculateInterface = null;
            Log.d(TAG, "binder died");
        }
    };

    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEdt_num1 = findViewById(R.id.edt_num1);
        mEdt_num2 = findViewById(R.id.edt_num2);
        mTxt_result = findViewById(R.id.txt_result);
        mBtn_calculate = findViewById(R.id.btn_calculate);

        mBtn_calculate.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                int num1 = Integer.parseInt(mEdt_num1.getText().toString());
                int num2 = Integer.parseInt(mEdt_num2.getText().toString());
                try {
                    int num = mICalculateInterface.addNum(num1, num2);
                    mTxt_result.setText("结果：" + num);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    mTxt_result.setText("计算错误");
                }
                try {
                    List<Person> personList = mICalculateInterface.addPerson(new Person("wuc", 22));
                    Log.d("aidl", personList.toString());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        bindService();
    }

    private void bindService() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.wuc.aidltest",
                "com.wuc.aidltest.IRemoteService"));
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        //如果连接持续，并且Binder未死亡
        if (mICalculateInterface != null && mICalculateInterface.asBinder().isBinderAlive()) {
            try {
                Log.d(TAG, "unregister listener : " + mOnNewPersonArrivedListener);
                mICalculateInterface.unregisterListener(mOnNewPersonArrivedListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(conn);
        super.onDestroy();
    }

    /**
     * 防止Handler泄漏
     */
    private static class ClientHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_NEW_PERSON_ARRIVED:
                    Log.d(TAG, "receive new person : " + msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }
}
