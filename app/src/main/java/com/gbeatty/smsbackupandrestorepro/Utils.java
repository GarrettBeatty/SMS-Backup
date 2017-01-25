package com.gbeatty.smsbackupandrestorepro;


import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Utils {

    public static final int BACKUP_IDLE = 0;
    public static final int BACKUP_COMPLETE = 1;
    public static final int BACKUP_STARTING = 2;
    public static final int BACKUP_STOPPING = 3;
    public static final int BACKUP_RUNNING = 4;
    public static final int RESTORE_COMPLETE = 5;
    public static final int RESTORE_STARTING = 6;
    public static final int RESTORE_STOPPING = 7;
    public static final int RESTORE_RUNNING = 8;
    public static final String BACKUP_RESULT = "com.gbeatty.smsbackupandrestorepro.BackupService.REQUEST_PROCESSED";
    public static final String BACKUP_MESSAGE = "com.gbeatty.smsbackupandrestorepro.BackupService.BACKUP_MSG";
    public static final String RESTORE_RESULT = "com.gbeatty.smsbackupandrestorepro.BackupService.RESTORE_PROCESSED";
    public static final String RESTORE_MESSAGE = "com.gbeatty.smsbackupandrestorepro.BackupService.RESTORE_MSG";
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_MULTIPLE = 1004;
    static final int DEFAULT_SMS_REQUEST = 1005;
    static final String PREF_ACCOUNT_NAME = "accountName";
    static final String[] SCOPES = {GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_MODIFY};

    public static String createLabelIfNotExistAndGetLabelID(Gmail service, String user, String name) throws IOException {
        ListLabelsResponse response = service.users().labels().list(user).execute();
        List<Label> labels = response.getLabels();

        Label label = null;
        for (Label l : labels) {
            if (l.getName().equalsIgnoreCase(name)) label = l;
        }

        if (label == null) {
            label = new Label();
            label.setName(name).setLabelListVisibility("labelShow").setMessageListVisibility("show");
            label = service.users().labels().create(user, label).execute();
            return label.getId();
        }

        return label.getId();
    }

    public static List<Message> getMessagesMatchingQuery(Gmail service, String userId,
                                                         String... labelIds) throws IOException {
        ListMessagesResponse response = service.users().messages().list(userId).setLabelIds(Arrays.asList(labelIds)).execute();

        List<Message> messages = new ArrayList<>();
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = service.users().messages().list(userId).setLabelIds(Arrays.asList(labelIds))
                        .setPageToken(pageToken).execute();
            } else {
                break;
            }
        }

        for (Message message : messages) {
            System.out.println(message.toPrettyString());
        }

        return messages;
    }


    /**
     * List all Threads of the user's mailbox with labelIds applied.
     *
     * @param service  Authorized Gmail API instance.
     * @param userId   User's email address. The special value "me"
     *                 can be used to indicate the authenticated user.
     * @param labelIds String used to filter the Threads listed.
     * @throws IOException
     */
    public static List<Thread> getThreadsWithLabelsQuery(Gmail service, String userId,
                                                         String query, String... labelIds) throws IOException {

        ListThreadsResponse response = service.users().threads().list(userId).setLabelIds(Arrays.asList(labelIds)).setQ(query).execute();
        List<Thread> threads = new ArrayList<>();
        while (response.getThreads() != null) {
            threads.addAll(response.getThreads());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = service.users().threads().list(userId).setLabelIds(Arrays.asList(labelIds)).setQ(query)
                        .setPageToken(pageToken).execute();
            } else {
                break;
            }
        }

        return threads;
    }

    public static MimeMessage getMimeMessage(Gmail service, String userId, String messageId)
            throws IOException, MessagingException {
        Message message = service.users().messages().get(userId, messageId).setFormat("raw").execute();

        byte[] emailBytes = Base64.decodeBase64(message.getRaw());

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        return new MimeMessage(session, new ByteArrayInputStream(emailBytes));
    }


    /**
     * Insert an email message into the user's mailbox.
     *
     * @param service Authorized Gmail API instance.
     * @param userId  User's email address. The special value "me"
     *                can be used to indicate the authenticated user.
     * @param email   Email to be inserted.
     * @throws MessagingException
     * @throws IOException
     */
    public static Message insertMessage(Gmail service, String userId, MimeMessage email, String thread, String... labels)
            throws MessagingException, IOException {

        Message message = createMessageWithEmail(email);

        //set a
        if (labels.length > 0) {
            message.setLabelIds(Arrays.asList(labels));
        }

        if (thread != null) {
            message.setThreadId(thread);
        }

        message = service.users().messages().insert(userId, message).execute();

        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());

        return message;
    }


    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param to       email address of the receiver
     * @param from     email address of the sender, the mailbox account
     * @param subject  subject of the email
     * @param bodyText body text of the email
     * @return the MimeMessage to be used to send email
     * @throws MessagingException
     */
    public static MimeMessage createEmail(String to,
                                          String from,
                                          String personal,
                                          String subject,
                                          String bodyText,
                                          Date sentDate)
            throws MessagingException, UnsupportedEncodingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from, personal));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        email.setSentDate(sentDate);

        return email;
    }

    /**
     * Create a message from an email.
     *
     * @param emailContent Email to be set to raw of message
     * @return a message containing a base64url encoded email
     * @throws IOException
     * @throws MessagingException
     */
    public static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

}
