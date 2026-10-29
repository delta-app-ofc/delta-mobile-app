package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ChatConversation;
import com.aula.delta_mobile_app.view.ChatBubbleDrawable;
import com.aula.delta_mobile_app.viewmodel.GluhChatViewModel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GluhChatFragment extends AuthFlowFragment {
    private View root, menuSpace, typing, sendButton, retryButton, routineButton;
    private android.widget.EditText input;
    private TextView notice;
    private LinearLayout messageList;
    private androidx.core.widget.NestedScrollView scroll;
    private GluhChatViewModel model;
    private final Map<Long, TextView> bubbles = new LinkedHashMap<>();
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener;
    private TextWatcher watcher;
    private View menu;
    private boolean restoring;
    private long lastId;

    public GluhChatFragment() { super(R.layout.fragment_gluh_chat); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        root = view;
        input = view.findViewById(R.id.chat_input);
        notice = view.findViewById(R.id.chat_notice);
        messageList = view.findViewById(R.id.chat_messages);
        scroll = view.findViewById(R.id.chat_scroll);
        menuSpace = view.findViewById(R.id.chat_menu_space);
        typing = view.findViewById(R.id.chat_typing);
        sendButton = view.findViewById(R.id.chat_send);
        retryButton = view.findViewById(R.id.chat_retry);
        routineButton = view.findViewById(R.id.chat_routine);
        setScreenColor(R.color.chat_background);
        model = new ViewModelProvider(this).get(GluhChatViewModel.class);
        input.setText(model.draft());
        input.setSelection(input.length());
        watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!restoring) model.draft(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        input.addTextChangedListener(watcher);
        sendButton.setOnClickListener(v -> send());
        input.setOnEditorActionListener((label, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEND) return false;
            send();
            return true;
        });
        retryButton.setOnClickListener(v -> model.retry());
        routineButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.goalsFragment));
        menu = requireActivity().findViewById(R.id.main_menu);
        layoutListener = this::updateLayout;
        view.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        model.changes().observe(getViewLifecycleOwner(), value -> render());
        model.connect();
    }

    private void send() {
        if (!model.send()) return;
        input.clearFocus();
        WindowCompat.getInsetsController(requireActivity().getWindow(), root).hide(WindowInsetsCompat.Type.ime());
        scrollToEnd();
    }

    private void render() {
        if (root == null) return;
        boolean follow = nearEnd();
        List<ChatConversation.Message> messages = model.conversation.messages();
        if (messages.isEmpty() && !bubbles.isEmpty()) {
            messageList.removeAllViews();
            bubbles.clear();
            lastId = 0;
        }
        for (ChatConversation.Message message : messages) {
            TextView bubble = bubbles.get(message.id);
            if (bubble == null) {
                bubble = createBubble(message);
                bubbles.put(message.id, bubble);
            }
            String description = message.delivery == ChatConversation.Delivery.FAILED ? message.text + ". " + getString(R.string.chat_failed) : message.text;
            if (!description.contentEquals(bubble.getContentDescription() == null ? "" : bubble.getContentDescription())) bubble.setContentDescription(description);
        }
        typing.setVisibility(model.conversation.pending() ? View.VISIBLE : View.GONE);
        String noticeText = model.error();
        if (noticeText.isEmpty() && !model.available()) noticeText = getString(model.loadingUser() ? R.string.chat_connecting : R.string.chat_unavailable);
        if (noticeText.isEmpty() && model.draft().codePointCount(0, model.draft().length()) > 2000) noticeText = getString(R.string.chat_limit);
        if (!notice.getText().toString().equals(noticeText)) notice.setText(noticeText);
        notice.setVisibility(noticeText.isEmpty() ? View.GONE : View.VISIBLE);
        retryButton.setVisibility(!model.loadingUser() && (!model.available() || model.conversation.failed()) ? View.VISIBLE : View.GONE);
        routineButton.setVisibility(!model.available() && !model.loadingUser() ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(model.canSend());
        sendButton.setAlpha(model.canSend() ? 1f : .4f);
        if (!input.getText().toString().equals(model.draft())) {
            restoring = true;
            input.setText(model.draft());
            input.setSelection(input.length());
            restoring = false;
        }
        long newest = messages.isEmpty() ? 0 : messages.get(messages.size() - 1).id;
        if (newest != lastId && follow) scrollToEnd();
        lastId = newest;
    }

    private TextView createBubble(ChatConversation.Message message) {
        FrameLayout row = new FrameLayout(requireContext());
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(12);
        rowParams.bottomMargin = dp(6);
        row.setLayoutParams(rowParams);
        AppCompatTextView bubble = new AppCompatTextView(requireContext());
        bubble.setTextAppearance(R.style.Chat_Body);
        bubble.setText(message.text);
        bubble.setTextColor(ContextCompat.getColor(requireContext(), message.user ? R.color.white : R.color.delta_water));
        bubble.setPadding(dp(14), dp(11), dp(14), dp(19));
        bubble.setTextIsSelectable(true);
        bubble.setBackground(new ChatBubbleDrawable(message.user, getResources().getDisplayMetrics().density,
            ContextCompat.getColor(requireContext(), message.user ? R.color.delta_water : R.color.chat_reply_background),
            ContextCompat.getColor(requireContext(), message.user ? R.color.chat_gradient_end : R.color.chat_reply_background)));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2, message.user ? Gravity.RIGHT : Gravity.LEFT);
        row.addView(bubble, params);
        messageList.addView(row);
        return bubble;
    }

    private void updateLayout() {
        if (root == null) return;
        int reserve = dp(12);
        if (menu != null && menu.getVisibility() == View.VISIBLE) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) menu.getLayoutParams();
            reserve += menu.getHeight() + params.bottomMargin;
        }
        if (menuSpace.getLayoutParams().height != reserve) {
            menuSpace.getLayoutParams().height = reserve;
            menuSpace.requestLayout();
        }
        int width = messageList.getWidth();
        if (width <= 0) return;
        List<ChatConversation.Message> messages = model.conversation.messages();
        for (ChatConversation.Message message : messages) {
            TextView bubble = bubbles.get(message.id);
            int max = (int) (width * (message.user ? .85f : .95f));
            if (bubble != null && bubble.getMaxWidth() != max) bubble.setMaxWidth(max);
        }
    }

    private boolean nearEnd() {
        View child = scroll.getChildAt(0);
        return child == null || child.getHeight() - scroll.getScrollY() - scroll.getHeight() < dp(80);
    }

    private void scrollToEnd() {
        scroll.post(() -> {
            if (root != null) scroll.scrollTo(0, scroll.getChildAt(0).getHeight());
        });
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    public void onDestroyView() {
        root.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        input.removeTextChangedListener(watcher);
        root = null;
        input = null;
        notice = null;
        messageList = null;
        scroll = null;
        menuSpace = typing = sendButton = retryButton = routineButton = null;
        bubbles.clear();
        menu = null;
        super.onDestroyView();
    }
}
