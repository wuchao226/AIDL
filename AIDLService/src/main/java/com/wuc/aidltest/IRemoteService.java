package com.wuc.aidltest;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: wuchao
 * @date: 2018/5/18 11:31
 * @desciption:
 */
public class IRemoteService extends Service {
    private static final String TAG = "IRemoteService";
    //存储注册监听客户端集合
    private final RemoteCallbackList<IOnNewPersonArrivedListener> mListenerList = new RemoteCallbackList<>();
    /**
     * CopyOnWriteArrayList支持并发读写，AIDL方法是在服务端的Binder线程池中执行的，因此当多个客户端同时连接的时候，
     * 会存在多个线程同时访问的情形，所以我们要在AIDL方法中处理线程同步，这里使用CopyOnWriteArrayList来进行自动的线程同步
     * <p>
     * 因为AIDL中所支持的是抽象的List，二List只是一个接口，因此虽然服务端返回的是CopyOnWriteArrayList，但是在Binder中
     * 会按照List的规范去访问数据并最终形成一个新的ArrayList传递给客户端，所以采用CopyOnWriteArrayList是可以的，类似的
     * 还有ConcurrentHashMap
     */
    private CopyOnWriteArrayList<Person> mPersonList;
    private AtomicBoolean mIsServiceDestoryed = new AtomicBoolean(false);
    private IBinder mIBinder = new ICalculateInterface.Stub() {

        @Override
        public int addNum(int num1, int num2) throws RemoteException {
            return num1 + num2;
        }

        @Override
        public List<Person> addPerson(Person person) throws RemoteException {
            mPersonList.add(person);
            return mPersonList;
        }

        @SuppressLint("NewApi")
        @Override
        public void registerListener(IOnNewPersonArrivedListener listener) throws RemoteException {
            mListenerList.register(listener);
           /* if (!mListenerList.contains(listener)) {
                mListenerList.add(listener);
            } else {
                Log.d(TAG, "already exists.");
            }*/
            Log.d(TAG, "registerListener，size：" + mListenerList.getRegisteredCallbackCount());
        }

        @SuppressLint("NewApi")
        @Override
        public void unregisterListener(IOnNewPersonArrivedListener listener) throws RemoteException {
            mListenerList.unregister(listener);
           /* if (mListenerList.contains(listener)) {
                mListenerList.remove(listener);
                Log.d(TAG, "unregister listener succeed.");
            } else {
                Log.d(TAG, "not found,can not unregister.");
            }*/
            Log.d(TAG, "unregisterListener，current size：" + mListenerList.getRegisteredCallbackCount());
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new ServiceWorker()).start();
    }

    @Override
    public void onDestroy() {
        mIsServiceDestoryed.set(true);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mPersonList = new CopyOnWriteArrayList<>();
        int check = checkCallingOrSelfPermission("com.wuc.aidlservice.permission.ACCESS_SERVICE");
        if (check == PackageManager.PERMISSION_DENIED) {
            return null;
        }
        return mIBinder;
    }

    private void onNewPersonArrived(Person person) throws RemoteException {
        mPersonList.add(person);
        /*Log.d(TAG, "onNewPersonArrived, notify listener:" + mListenerList.size());
        for (int i = 0; i < mListenerList.size(); i++) {
            IOnNewPersonArrivedListener listener = mListenerList.get(i);
            Log.d(TAG, "onNewPersonArrived, notify listener:" + listener);
            listener.onNewPersonArrived(person);
        }*/
        synchronized (mListenerList) {
            int n = mListenerList.beginBroadcast();
            try {
                for (int i = 0; i < n; i++) {
                    IOnNewPersonArrivedListener listener = mListenerList.getBroadcastItem(i);
                    if (listener != null) {
                        listener.onNewPersonArrived(person);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mListenerList.finishBroadcast();
        }
    }

    /**
     * 每个5秒增加一个新人，并通知所有感兴趣的员工
     */
    private class ServiceWorker implements Runnable {

        @Override
        public void run() {
            while (!mIsServiceDestoryed.get()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Person person = new Person("new person name：" + 2, 22);
                try {
                    onNewPersonArrived(person);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
