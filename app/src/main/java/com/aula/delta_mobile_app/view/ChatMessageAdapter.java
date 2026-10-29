package com.aula.delta_mobile_app.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ChatMessage;
import com.aula.delta_mobile_app.model.ScreenState;

import java.util.Objects;

public final class ChatMessageAdapter extends ListAdapter<ChatMessage, ChatMessageAdapter.Holder> {
    private static final DiffUtil.ItemCallback<ChatMessage> DIFFERENCE = new DiffUtil.ItemCallback<ChatMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage before, @NonNull ChatMessage after) {
            return before.id == after.id && before.author.equals(after.author);
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage before, @NonNull ChatMessage after) {
            return before.content.equals(after.content) && before.status.equals(after.status) && Objects.equals(before.agentUsed, after.agentUsed);
        }
    };

    public ChatMessageAdapter() {
        super(DIFFERENCE);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).author == ChatMessage.Author.USER ? R.layout.item_chat_user : R.layout.item_chat_assistant;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ChatMessage message = getItem(position);
        holder.content.setText(message.content);
        int author = message.author == ChatMessage.Author.USER ? R.string.chat_user_author : R.string.chat_assistant_author;
        holder.content.setContentDescription(holder.itemView.getContext().getString(R.string.chat_message_accessibility,
            holder.itemView.getContext().getString(author), message.content));
        if (holder.status != null) {
            int status = message.status == ScreenState.Status.LOADING ? R.string.chat_message_sending : R.string.chat_message_failed;
            boolean visible = message.status == ScreenState.Status.LOADING || message.status == ScreenState.Status.ERROR;
            holder.status.setText(visible ? holder.itemView.getContext().getString(status) : "");
            holder.status.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public static final class Holder extends RecyclerView.ViewHolder {
        private final TextView content;
        private final TextView status;

        Holder(View view) {
            super(view);
            content = view.findViewById(R.id.chat_message_text);
            status = view.findViewById(R.id.chat_message_status);
        }
    }
}
