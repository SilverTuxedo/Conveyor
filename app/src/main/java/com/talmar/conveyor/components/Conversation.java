package com.talmar.conveyor.components;

import android.app.Notification;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.talmar.conveyor.BuildConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Conversation {
    private String m_title;
    private String m_title_raw;
    private List<Message> m_messages;

    /**
     * Creates a conversation from a notification that uses {@link Notification.MessagingStyle}.
     *
     * @param messagingStyleNotification The notification to create the conversation from.
     */
    public Conversation(@NonNull Notification messagingStyleNotification) {
        String template = messagingStyleNotification.extras.getString(Notification.EXTRA_TEMPLATE);
        if (BuildConfig.DEBUG && !Notification.MessagingStyle.class.getName().equals(template)) {
            throw new AssertionError("Assertion failed: message is not MessagingStyle");
        }

        Object titleObject = messagingStyleNotification.extras.get(Notification.EXTRA_CONVERSATION_TITLE);
        if (null != titleObject) {
            m_title_raw = titleObject.toString();
            m_title = Conversation.removeMessagesCount(m_title_raw);
        }

        m_messages = new ArrayList<>();
        Parcelable[] messageBundles = messagingStyleNotification.extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        if (null != messageBundles) {
            for (Parcelable p : messageBundles) {
                Message message = new Message((Bundle) p);
                m_messages.add(message);
            }
        }
    }

    /**
     * @return true if the conversation has a title.
     */
    public boolean hasTitle() {
        return null != m_title;
    }

    /**
     * @return The title of the conversation. May be null.
     */
    public String getTitle() {
        return m_title;
    }

    /**
     * @return The messages in the conversation.
     */
    public List<Message> getMessages() {
        return new ArrayList<>(m_messages);
    }

    /**
     * Groups sequential messages by the same author to one message and returns the grouped results.
     */
    public List<Message> getGroupedMessages() {
        ArrayList<Message> groupedMessages = new ArrayList<>();

        String lastAuthor = null;
        for (Message message : m_messages) {
            if (message.authorName.equals(lastAuthor)) {
                Message lastGroupedMessage = groupedMessages.get(groupedMessages.size() - 1);
                lastGroupedMessage.text = String.join("\n", lastGroupedMessage.text, message.text);
            } else {
                lastAuthor = message.authorName;
                groupedMessages.add(new Message(message));
            }
        }

        return groupedMessages;
    }

    /**
     * @return All authors in the conversation.
     */
    public Set<String> getAuthors() {
        HashSet<String> authors = new HashSet<>();
        for (Message message : m_messages) {
            authors.add(message.authorName);
        }
        return authors;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (null == m_title) {
            builder.append("Untitled conversation");
        } else {
            builder.append("Conversation titled '").append(m_title).append("'");
        }
        builder.append(" with ").append(m_messages.size()).append(" messages");

        return builder.toString();
    }

    @NonNull
    public static String removeMessagesCount(@NonNull String title){
        return title.replaceAll("\\s\\(\\d+\\smessages\\)", "");
    }
}
