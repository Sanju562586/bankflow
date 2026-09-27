package com.bankflow.notification.service;

import com.bankflow.common.enums.NotificationChannel;
import com.bankflow.notification.channel.NotificationChannelSender;
import com.bankflow.notification.model.NotificationRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationChannelSender smsSender;

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        when(smsSender.getChannel()).thenReturn(NotificationChannel.SMS);
        dispatcher = new NotificationDispatcher(List.of(smsSender));
    }

    @Test
    @DisplayName("dispatch: successfully routes to matching channel sender")
    void testDispatchSuccess() {
        NotificationRecord record = new NotificationRecord("notif-1", "+919876543210", "cust-1", NotificationChannel.SMS, "Sub", "Body");

        dispatcher.dispatch(record);

        verify(smsSender, times(1)).send(record);
        assertThat(dispatcher.getHistory()).contains(record);
    }

    @Test
    @DisplayName("dispatch: handles unsupported channel gracefully without throwing unhandled exception")
    void testDispatchUnsupportedChannel() {
        NotificationRecord record = new NotificationRecord("notif-2", "email@example.com", "cust-1", NotificationChannel.EMAIL, "Sub", "Body");

        dispatcher.dispatch(record);

        assertThat(record.getStatus()).isEqualTo("UNSUPPORTED_CHANNEL");
        assertThat(dispatcher.getHistory()).contains(record);
    }
}
