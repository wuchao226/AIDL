// ICalculateInterface.aidl
package com.wuc.aidltest;

// Declare any non-default types here with import statements
import com.wuc.aidltest.Person;
import com.wuc.aidltest.IOnNewPersonArrivedListener;
interface ICalculateInterface {
     //计算两个数的和
     int addNum(int num1,int num2);
     //除了基本数据类型，其他类型的参数都需要标上方向类型：in(输入), out(输出), inout(输入输出)
     List<Person> addPerson(in Person person);
     void registerListener(IOnNewPersonArrivedListener listener);
     void unregisterListener(IOnNewPersonArrivedListener listener);
}
