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
import com.aula.delta_mobile_app.viewmodel.ChatFlow.SessionEntry;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;
import java.util.function.Consumer;

public final class ChatSessionAdapter extends ListAdapter<SessionEntry, ChatSessionAdapter.Holder> {
    private final Consumer<String> select;

    public ChatSessionAdapter(Consumer<String> select) {
        super(new DiffUtil.ItemCallback<SessionEntry>() {
            @Override
            public boolean areItemsTheSame(@NonNull SessionEntry before, @NonNull SessionEntry after) {
                return before.id.equals(after.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull SessionEntry before, @NonNull SessionEntry after) {
                return Objects.equals(before.createdAt, after.createdAt);
            }
        });
        this.select = Objects.requireNonNull(select);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_session, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SessionEntry entry = getItem(position);
        String title = entry.created == null ? holder.itemView.getContext().getString(R.string.chat_session_no_date)
            : holder.itemView.getContext().getString(R.string.chat_session_date, DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(holder.itemView.getResources().getConfiguration().getLocales().get(0)).withZone(ZoneId.systemDefault()).format(entry.created));
        holder.title.setText(title);
        holder.identity.setText(holder.itemView.getContext().getString(R.string.chat_session_identity, entry.id));
        holder.itemView.setContentDescription(holder.itemView.getContext().getString(R.string.chat_session_open, title, entry.id));
        holder.itemView.setOnClickListener(view -> select.accept(entry.id));
    }

    public static final class Holder extends RecyclerView.ViewHolder {
        private final TextView title, identity;

        Holder(View view) {
            super(view);
            title = view.findViewById(R.id.chat_session_title);
            identity = view.findViewById(R.id.chat_session_identity);
        }
    }
}
