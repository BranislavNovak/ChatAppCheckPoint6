// INotificationBinder.aidl
package com.example.branislavnovak.chatapplication;

// Declare any non-default types here with import statements
import com.example.branislavnovak.chatapplication.INotificationCallback;

interface INotificationBinder {
    void setCallback(in INotificationCallback callback);
}
