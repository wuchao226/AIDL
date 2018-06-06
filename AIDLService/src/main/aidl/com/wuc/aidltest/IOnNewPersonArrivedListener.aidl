// IOnNewPersonArrivedListener.aidl
package com.wuc.aidltest;

// Declare any non-default types here with import statements
import com.wuc.aidltest.Person;
// 当服务端有新人加入时，就通知每一个已经申请提醒功能的用户，由于AIDL中无法使用普通接口，所以提供一个AIDL接口
interface IOnNewPersonArrivedListener {
    void onNewPersonArrived(in Person person);
}
