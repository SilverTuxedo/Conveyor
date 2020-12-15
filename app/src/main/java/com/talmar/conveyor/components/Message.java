package com.talmar.conveyor.components;

import android.app.Notification;
import android.app.Person;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

public class Message {
    private static final String MESSAGE_BUNDLE_SENDER_PERSON_KEY = "sender_person";
    private static final String MESSAGE_BUNDLE_DEPRECATED_SENDER_KEY = "sender"; // deprecated since API 28.
    private static final String MESSAGE_BUNDLE_TEXT_KEY = "text";

    public String authorName;
    public String text;

    public Message(String authorName, String text) {
        this.authorName = authorName;
        this.text = text;
    }

    public Message(Message other) {
        this.authorName = other.authorName;
        this.text = other.text;
    }

    /**
     * Creates a message from a {@link Notification.MessagingStyle.Message} bundle (provided by a
     * {@link Notification.MessagingStyle} notification).
     *
     * @param messagingStyleMessageBundle The bundle to create the message from.
     */
    public Message(@NonNull Bundle messagingStyleMessageBundle) {
        authorName = getAuthorName(messagingStyleMessageBundle);
        Object textObject = messagingStyleMessageBundle.get(MESSAGE_BUNDLE_TEXT_KEY);
        if (null != textObject) {
            text = textObject.toString();
        }
    }

    /**
     * Takes a message bundle and gets the name of the author using the most suitable API.
     *
     * @param messagingStyleMessageBundle A {@link Notification.MessagingStyle.Message} bundle.
     * @return The author of the message. If there is no author, null is returned.
     */
    private static String getAuthorName(@NonNull Bundle messagingStyleMessageBundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Person person = messagingStyleMessageBundle.getParcelable(MESSAGE_BUNDLE_SENDER_PERSON_KEY);
            if (null == person) {
                return null;
            }
            return (String) person.getName();
        }
        Object senderNameObject = messagingStyleMessageBundle.get(MESSAGE_BUNDLE_DEPRECATED_SENDER_KEY);
        if (null == senderNameObject) {
            return null;
        }
        return senderNameObject.toString();
    }
}
