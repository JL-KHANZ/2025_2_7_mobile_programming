package com.example.mobile_programming_2025_2.ui.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.data.ChatMessage;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    // Constants for message types
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<ChatMessage> messagesList = new ArrayList<>();
    private final String currentUserId = "user_A"; // !! IMPORTANT: MUST match your Auth logic !!
    private final LayoutInflater inflater;

    public ChatAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * Updates the data list and notifies the RecyclerView.
     */
    public void setMessages(List<ChatMessage> newMessages) {
        // Simple update: In a production app, use DiffUtil for better performance
        this.messagesList = newMessages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    /**
     * Determines which layout XML to use based on who sent the message.
     */
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messagesList.get(position);

        // Check if the message sender ID matches the current user ID
        if (message.senderUid.equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_SENT) {
            // Inflate the sent message layout (item_message_sent.xml)
            view = inflater.inflate(R.layout.item_message_sent, parent, false);
        } else {
            // Inflate the received message layout (item_message_received.xml)
            view = inflater.inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messagesList.get(position);
        holder.bind(message);
    }

    // --- ViewHolder Class ---
    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        private TextView messageBody;
        private TextView senderName; // Only for received messages

        public MessageViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);

            if (viewType == VIEW_TYPE_SENT) {
                messageBody = itemView.findViewById(R.id.text_message_body_sent);
            } else {
                messageBody = itemView.findViewById(R.id.text_message_body_received);
                senderName = itemView.findViewById(R.id.text_message_name_received);
            }
        }

        public void bind(ChatMessage message) {
            messageBody.setText(message.text);

            // Set sender name only if the TextView exists (i.e., it's a received message)
            if (senderName != null) {
                // You might need a service to fetch the actual user display name
                senderName.setText(message.senderUid);
            }
            // Optional: Format and set the timestamp TextView here
        }
    }
}