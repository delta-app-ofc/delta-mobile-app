package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ChatMessage;
import com.aula.delta_mobile_app.model.ChatState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.ChatMessageAdapter;
import com.aula.delta_mobile_app.view.ChatSessionAdapter;
import com.aula.delta_mobile_app.view.StatusNoticeView;
import com.aula.delta_mobile_app.viewmodel.ChatFlow.HistoryState;
import com.aula.delta_mobile_app.viewmodel.ChatViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

public final class ChatFragment extends Fragment {
    private LiveData<ChatState> conversation;
    private Consumer<String> updateDraft;
    private Runnable sendAction;
    private ChatState current = ChatState.empty();
    private RecyclerView messages;
    private ChatMessageAdapter adapter;
    private FeedbackAdapter feedback;
    private LandingAdapter landing;
    private ChatSessionAdapter sessions;
    private LiveData<HistoryState> historySource;
    private HistoryState history;
    private Runnable reloadSessions;
    private Consumer<String> selectSession;
    private boolean productionHistory;
    private boolean sendingEnabled = true;
    private Consumer<Boolean> newChatAction;
    private Runnable resumeAction, introductionAction;
    private NavController navigation;
    private int sectionDestination;
    private View newButton;
    private AlertDialog newChatDialog;
    private EditText input;
    private TextView counter;
    private View sendButton;
    private TextWatcher watcher;
    private boolean rendering;
    private final Runnable retryReady = () -> {
        render(current);
        if (landing != null) landing.notifyItemChanged(0);
    };

    public ChatFragment() {
        super(R.layout.fragment_chat);
    }

    public void bindConversation(LiveData<ChatState> state, Consumer<String> draft, Runnable send) {
        if (getView() != null) throw new IllegalStateException("Bind conversation before creating the view");
        conversation = Objects.requireNonNull(state);
        updateDraft = Objects.requireNonNull(draft);
        sendAction = Objects.requireNonNull(send);
    }

    public void bindHistory(LiveData<HistoryState> state, Runnable reload, Consumer<String> select) {
        if (getView() != null) throw new IllegalStateException("Bind history before creating the view");
        historySource = Objects.requireNonNull(state);
        reloadSessions = Objects.requireNonNull(reload);
        selectSession = Objects.requireNonNull(select);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        if (conversation == null) {
            ChatViewModel model = ChatViewModel.forActivity(requireActivity());
            conversation = model.state();
            updateDraft = model::setDraft;
            sendAction = model::send;
            sendingEnabled = model.sendingEnabled();
            newChatAction = model::newChat;
            resumeAction = model::resumeConversation;
            introductionAction = model::showIntroduction;
            historySource = model.history();
            reloadSessions = model::loadSessions;
            selectSession = model::openSession;
            productionHistory = true;
        }
        if (productionHistory) {
            try {
                navigation = NavHostFragment.findNavController(this);
                sectionDestination = navigation.getCurrentDestination() == null ? 0 : navigation.getCurrentDestination().getId();
            } catch (IllegalStateException unavailable) {
                navigation = null;
            }
        }
        view.setAccessibilityPaneTitle(getText(R.string.chat_title));
        messages = view.findViewById(R.id.chat_messages);
        input = view.findViewById(R.id.chat_input);
        counter = view.findViewById(R.id.chat_character_count);
        sendButton = view.findViewById(R.id.chat_send);
        newButton = view.findViewById(R.id.chat_new);
        newButton.setOnClickListener(button -> requestNewChat());
        view.findViewById(R.id.chat_back).setOnClickListener(button -> { if (introductionAction != null && !current.showIntroduction) introductionAction.run(); });
        adapter = new ChatMessageAdapter();
        feedback = new FeedbackAdapter();
        landing = new LandingAdapter();
        sessions = new ChatSessionAdapter(id -> { if (selectSession != null) selectSession.accept(id); });
        messages.setLayoutManager(new LinearLayoutManager(requireContext()));
        messages.setItemAnimator(null);
        messages.setAdapter(new ConcatAdapter(landing, sessions, adapter, feedback));
        watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                if (!rendering && updateDraft != null) updateDraft.accept(value.toString());
                updateAvailability();
            }

            @Override
            public void afterTextChanged(Editable value) {}
        };
        input.addTextChangedListener(watcher);
        input.setOnEditorActionListener((field, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEND) return false;
            attemptSend();
            return true;
        });
        sendButton.setOnClickListener(button -> attemptSend());
        ViewCompat.setOnApplyWindowInsetsListener(view, (root, insets) -> {
            boolean keyboard = insets.isVisible(WindowInsetsCompat.Type.ime());
            root.findViewById(R.id.chat_actions).setVisibility(keyboard || current.showIntroduction || introductionAction == null ? View.GONE : View.VISIBLE);
            View column = root.findViewById(R.id.chat_content);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) column.getLayoutParams();
            int bottom = keyboard ? 0 : getResources().getDimensionPixelSize(R.dimen.chat_menu_clearance);
            if (params.bottomMargin != bottom) {
                params.bottomMargin = bottom;
                column.setLayoutParams(params);
            }
            if (messages != null) messages.setPaddingRelative(0, getResources().getDimensionPixelSize(keyboard ? R.dimen.chat_section_spacing : R.dimen.chat_top_spacing), 0, 0);
            updateComposer(keyboard);
            return insets;
        });
        view.addOnLayoutChangeListener((root, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
            if (bottom - top != oldBottom - oldTop && insets != null) updateComposer(insets.isVisible(WindowInsetsCompat.Type.ime()));
        });
        render(conversation == null ? ChatState.empty() : conversation.getValue());
        if (conversation != null) conversation.observe(getViewLifecycleOwner(), this::render);
        if (historySource != null) historySource.observe(getViewLifecycleOwner(), this::renderHistory);
        if (productionHistory) reloadSessions.run();
        ViewCompat.requestApplyInsets(view);
    }

    private void render(ChatState state) {
        if (state != null && !Objects.equals(current.account.data, state.account.data)) dismissNewChatDialog();
        current = state == null ? ChatState.empty() : state;
        if (input == null || adapter == null) return;
        rendering = true;
        if (!input.getText().toString().equals(current.draft)) {
            input.setText(current.draft);
            input.setSelection(input.length());
        }
        rendering = false;
        if (history != null && sessions != null) sessions.submitList(current.showIntroduction && history.sessions.data != null ? history.sessions.data : Collections.emptyList());
        landing.notifyItemChanged(0);
        boolean follow = !messages.canScrollVertically(1);
        if (current.messages.size() > adapter.getCurrentList().size() && !current.messages.isEmpty()) {
            ChatMessage last = current.messages.get(current.messages.size() - 1);
            follow |= last.author == ChatMessage.Author.USER && last.status == ScreenState.Status.LOADING;
        }
        updateAvailability();
        feedback.update(current);
        ChatMessageAdapter submitted = adapter;
        boolean scroll = follow;
        adapter.submitList(current.showIntroduction ? Collections.emptyList() : current.messages, () -> {
            if (adapter == submitted && messages != null && scroll && messages.getAdapter() != null) {
                if (current.showIntroduction) ((LinearLayoutManager) messages.getLayoutManager()).scrollToPositionWithOffset(0, hasConversation() ? -messages.getPaddingTop() : 0);
                else messages.scrollToPosition(messages.getAdapter().getItemCount() - 1);
            }
        });
        View root = getView();
        if (root != null) {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
            root.findViewById(R.id.chat_actions).setVisibility(current.showIntroduction || introductionAction == null
                || insets != null && insets.isVisible(WindowInsetsCompat.Type.ime()) ? View.GONE : View.VISIBLE);
            root.removeCallbacks(retryReady);
            long delay = current.sending.retryAt - System.currentTimeMillis();
            if (history != null) {
                for (long deadline : new long[]{history.sessions.retryAt, history.history.retryAt, history.closing.retryAt}) {
                    long waiting = deadline - System.currentTimeMillis();
                    if (waiting > 0 && (delay <= 0 || waiting < delay)) delay = waiting;
                }
            }
            if (delay > 0) root.postDelayed(retryReady, Math.min(delay, 60000));
        }
    }

    private boolean ready() {
        return sendingEnabled && updateDraft != null && sendAction != null && current.account.status == ScreenState.Status.SUCCESS
            && !(current.showIntroduction && (current.sessionId != null || !current.messages.isEmpty()))
            && (history == null || (!history.closing.busy() && !history.history.busy() && history.history.status != ScreenState.Status.ERROR));
    }

    private void renderHistory(HistoryState state) {
        history = state;
        if (sessions == null) return;
        sessions.submitList(state == null || state.sessions.data == null || !current.showIntroduction ? Collections.emptyList() : state.sessions.data);
        landing.notifyItemChanged(0);
        render(current);
    }

    private boolean canSend() {
        if (!ready() || input == null || current.sending.busy() || System.currentTimeMillis() < current.sending.retryAt) return false;
        String value = input.getText().toString();
        return value.codePointCount(0, value.length()) <= 2000 && !value.codePoints().allMatch(character -> Character.isWhitespace(character) || Character.isSpaceChar(character));
    }

    private void updateAvailability() {
        if (input == null) return;
        input.setEnabled(ready());
        String value = input.getText().toString();
        int length = value.codePointCount(0, value.length());
        counter.setText(getString(R.string.chat_character_count, length));
        counter.setContentDescription(getString(R.string.chat_character_count_accessibility, length));
        counter.setTextColor(ContextCompat.getColor(requireContext(), length > 2000 ? R.color.auth_error : R.color.chat_placeholder_text));
        input.setError(length > 2000 ? getString(R.string.chat_error_message_length) : null);
        sendButton.setEnabled(canSend());
        sendButton.setAlpha(sendButton.isEnabled() ? 1f : .45f);
        newButton.setEnabled(canStartNewChat());
        newButton.setAlpha(newButton.isEnabled() ? 1f : .45f);
    }

    private boolean hasConversation() {
        return current.sessionId != null || !current.messages.isEmpty() || !current.draft.isEmpty() || current.sending.busy();
    }

    private boolean canStartNewChat() {
        return !current.showIntroduction && newChatAction != null && current.account.status == ScreenState.Status.SUCCESS && !current.sending.busy()
            && (sendingEnabled || current.sessionId == null)
            && (history == null || (!history.history.busy() && !history.closing.busy() && System.currentTimeMillis() >= history.closing.retryAt));
    }

    private void requestNewChat() {
        if (!canStartNewChat()) return;
        if (current.draft.isEmpty()) {
            newChatAction.accept(false);
            return;
        }
        if (newChatDialog != null && newChatDialog.isShowing()) return;
        ChatState confirmed = current;
        newChatDialog = new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.chat_new_confirm_title).setMessage(R.string.chat_new_confirm_message)
            .setNegativeButton(R.string.chat_new_cancel, null).setPositiveButton(R.string.chat_new, (dialog, choice) -> {
                if (canStartNewChat() && Objects.equals(confirmed.account.data, current.account.data) && Objects.equals(confirmed.sessionId, current.sessionId)
                    && confirmed.draft.equals(current.draft)) newChatAction.accept(true);
            }).create();
        newChatDialog.show();
    }

    private void dismissNewChatDialog() {
        if (newChatDialog != null) newChatDialog.dismiss();
        newChatDialog = null;
    }

    private void attemptSend() {
        if (sendAction != null && canSend()) sendAction.run();
    }

    private void updateComposer(boolean keyboard) {
        View root = getView();
        if (root == null || input == null) return;
        boolean compact = root.getHeight() > 0 && root.getHeight() < getResources().getDimensionPixelSize(R.dimen.chat_composer_min_height) * (keyboard ? 3 : 8);
        int spacing = getResources().getDimensionPixelSize(R.dimen.chat_composer_spacing);
        int innerSpacing = getResources().getDimensionPixelSize(R.dimen.chat_composer_inner_spacing);
        View composer = root.findViewById(R.id.chat_composer);
        View row = root.findViewById(R.id.chat_composer_row);
        composer.setPaddingRelative(0, compact ? 0 : spacing, 0, 0);
        row.setPaddingRelative(spacing, compact ? 0 : innerSpacing, spacing, compact ? 0 : innerSpacing);
        counter.setVisibility(compact && keyboard ? View.GONE : View.VISIBLE);
        if (input.getMaxLines() != (compact ? 1 : 4)) input.setMaxLines(compact ? 1 : 4);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (productionHistory && navigation != null && sectionDestination != 0 && getActivity() != null && !getActivity().isChangingConfigurations()
            && navigation.getCurrentDestination() != null && navigation.getCurrentDestination().getId() != sectionDestination && introductionAction != null) introductionAction.run();
    }

    @Override
    public void onDestroyView() {
        dismissNewChatDialog();
        View root = getView();
        if (root != null) {
            root.removeCallbacks(retryReady);
            ViewCompat.setOnApplyWindowInsetsListener(root, null);
        }
        if (input != null) {
            input.removeTextChangedListener(watcher);
            input.setOnEditorActionListener(null);
        }
        if (messages != null) messages.setAdapter(null);
        watcher = null;
        input = null;
        counter = null;
        sendButton = null;
        messages = null;
        adapter = null;
        feedback = null;
        landing = null;
        sessions = null;
        newButton = null;
        navigation = null;
        super.onDestroyView();
    }

    private static final class StaticHolder extends RecyclerView.ViewHolder {
        StaticHolder(View view) {
            super(view);
        }
    }

    private final class LandingAdapter extends RecyclerView.Adapter<StaticHolder> {
        @NonNull
        @Override
        public StaticHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.include_chat_landing, parent, false);
            view.findViewById(R.id.chat_composer_container).setVisibility(View.GONE);
            view.findViewById(R.id.chat_heading).setAccessibilityHeading(true);
            view.setPaddingRelative(0, 0, 0, view.getResources().getDimensionPixelSize(R.dimen.chat_section_spacing));
            return new StaticHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StaticHolder holder, int position) {
            View resume = holder.itemView.findViewById(R.id.chat_resume);
            resume.setVisibility(current.showIntroduction && hasConversation() && resumeAction != null ? View.VISIBLE : View.GONE);
            resume.setOnClickListener(view -> { if (resumeAction != null) resumeAction.run(); });
            holder.itemView.findViewById(R.id.chat_heading).setVisibility(current.showIntroduction ? View.VISIBLE : View.GONE);
            View container = holder.itemView.findViewById(R.id.chat_history_container);
            container.setVisibility(historySource == null || !current.showIntroduction ? View.GONE : View.VISIBLE);
            TextView status = holder.itemView.findViewById(R.id.chat_sessions_status);
            boolean busy = history != null && history.sessions.busy();
            boolean empty = history != null && history.sessions.status == ScreenState.Status.SUCCESS && history.sessions.data.isEmpty();
            status.setVisibility(busy || empty ? View.VISIBLE : View.GONE);
            status.setText(busy ? R.string.chat_sessions_loading : R.string.chat_sessions_empty);
            ScreenState<?> failure = history == null ? current.account : history.sessions;
            if (current.account.status == ScreenState.Status.ERROR) failure = current.account;
            ((StatusNoticeView) holder.itemView.findViewById(R.id.chat_sessions_notice)).setText(failure.status == ScreenState.Status.ERROR ? failure.message : "");
            View retry = holder.itemView.findViewById(R.id.chat_sessions_retry);
            retry.setVisibility(failure.status == ScreenState.Status.ERROR ? View.VISIBLE : View.GONE);
            retry.setEnabled(System.currentTimeMillis() >= failure.retryAt);
            retry.setOnClickListener(view -> { if (reloadSessions != null) reloadSessions.run(); });
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

    private final class FeedbackAdapter extends RecyclerView.Adapter<StaticHolder> {
        private ChatState state = ChatState.empty();

        private boolean visible() {
            return state.account.busy() || state.account.status == ScreenState.Status.ERROR
                || !state.showIntroduction && (state.sending.busy() || state.sending.status == ScreenState.Status.ERROR)
                || history != null && (!history.notice.isEmpty() || history.closing.busy() || history.closing.status == ScreenState.Status.ERROR
                    || !state.showIntroduction && (history.history.busy() || history.history.status == ScreenState.Status.ERROR
                        || history.history.status == ScreenState.Status.SUCCESS && history.history.data.isEmpty() && state.messages.isEmpty()))
                || !sendingEnabled && state.account.status == ScreenState.Status.SUCCESS;
        }

        void update(ChatState value) {
            int previous = count;
            state = value;
            count = visible() ? 1 : 0;
            if (previous == 1 && count == 1) notifyItemChanged(0);
            else if (previous == 1) notifyItemRemoved(0);
            else if (count == 1) notifyItemInserted(0);
        }

        private int count;

        @NonNull
        @Override
        public StaticHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new StaticHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.include_chat_feedback, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull StaticHolder holder, int position) {
            boolean loadingHistory = !state.showIntroduction && history != null && history.history.busy();
            boolean closing = history != null && history.closing.busy();
            boolean busy = state.account.busy() || !state.showIntroduction && state.sending.busy() || loadingHistory || closing;
            holder.itemView.findViewById(R.id.chat_waiting).setVisibility(busy ? View.VISIBLE : View.GONE);
            TextView text = holder.itemView.findViewById(R.id.chat_waiting_text);
            boolean empty = !state.showIntroduction && history != null && history.history.status == ScreenState.Status.SUCCESS && history.history.data.isEmpty() && state.messages.isEmpty();
            holder.itemView.findViewById(R.id.chat_waiting).setVisibility(busy || empty ? View.VISIBLE : View.GONE);
            ((ViewGroup) text.getParent()).getChildAt(0).setVisibility(busy ? View.VISIBLE : View.GONE);
            text.setText(closing ? R.string.chat_closing : loadingHistory ? R.string.chat_history_loading : empty ? R.string.chat_history_empty : state.account.busy() ? R.string.chat_preparing : R.string.chat_waiting);
            ScreenState<?> failure = state.account.status == ScreenState.Status.ERROR ? state.account : state.showIntroduction ? ScreenState.idle() : state.sending;
            String message = failure.status == ScreenState.Status.ERROR ? failure.message : "";
            if (message.isEmpty() && !sendingEnabled && state.account.status == ScreenState.Status.SUCCESS) message = getString(R.string.chat_sending_pending);
            boolean historyError = !state.showIntroduction && history != null && history.history.status == ScreenState.Status.ERROR;
            if (historyError) message = history.history.message;
            if (history != null && !history.notice.isEmpty()) message = history.notice;
            boolean closeError = history != null && history.closing.status == ScreenState.Status.ERROR;
            if (closeError) message = getString(R.string.chat_close_failed, getString(R.string.chat_error_timeout).equals(history.closing.message)
                ? getString(R.string.chat_close_timeout) : history.closing.message);
            if (getString(R.string.chat_error_timeout).equals(message)) message += "\n" + getString(R.string.chat_retry_uncertain);
            ((StatusNoticeView) holder.itemView.findViewById(R.id.chat_notice)).setText(message);
            TextView retry = holder.itemView.findViewById(R.id.chat_retry);
            retry.setVisibility(!state.showIntroduction && state.sending.status == ScreenState.Status.ERROR || historyError || closeError ? View.VISIBLE : View.GONE);
            retry.setEnabled(closeError ? canStartNewChat() : historyError ? System.currentTimeMillis() >= history.history.retryAt : canSend());
            retry.setAlpha(retry.isEnabled() ? 1f : .45f);
            retry.setText(System.currentTimeMillis() < (closeError ? history.closing.retryAt : historyError ? history.history.retryAt : state.sending.retryAt) ? R.string.chat_retry_wait : R.string.chat_retry);
            retry.setOnClickListener(button -> {
                if (closeError) requestNewChat();
                else if (historyError && selectSession != null) selectSession.accept(history.selectedId);
                else attemptSend();
            });
        }

        @Override
        public int getItemCount() {
            return count;
        }
    }
}
