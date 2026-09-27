package com.bankflow.notification.channel;

import com.bankflow.common.enums.NotificationChannel;
import com.bankflow.notification.model.NotificationRecord;

public interface NotificationChannelSender {
    NotificationChannel getChannel();
    void send(NotificationRecord record);
}
