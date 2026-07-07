package com.authbar87.authenticator.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.authbar87.authenticator.R;
import com.authbar87.authenticator.model.Account;
import com.authbar87.authenticator.util.TotpGenerator;

import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.VH> {

    public interface Listener {
        void onCodeTapped(Account account, String code);
        void onLongPress(Account account, View anchor);
        void onHotpRefresh(Account account);
    }

    private final List<Account> accounts;
    private final Listener listener;

    public AccountAdapter(List<Account> accounts, Listener listener) {
        this.accounts = accounts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Account account = accounts.get(position);
        long now = System.currentTimeMillis() / 1000L;
        String code = TotpGenerator.generate(account, now);
        String formatted = formatCode(code);

        holder.issuerText.setText(account.displayName());
        holder.codeText.setText(formatted);

        try {
            holder.avatar.setColorFilter(Color.parseColor(account.colorHex));
        } catch (Exception ignored) {}

        if (account.type == Account.Type.TOTP) {
            int remaining = TotpGenerator.secondsRemaining(account, now);
            holder.progress.setMax(account.period);
            holder.progress.setProgress(remaining);
            holder.progress.setVisibility(View.VISIBLE);
            holder.refreshButton.setVisibility(View.GONE);
        } else {
            holder.progress.setVisibility(View.GONE);
            holder.refreshButton.setVisibility(View.VISIBLE);
            holder.refreshButton.setOnClickListener(v -> {
                account.counter++;
                listener.onHotpRefresh(account);
                notifyItemChanged(holder.getBindingAdapterPosition());
            });
        }

        holder.itemView.setOnClickListener(v -> listener.onCodeTapped(account, code));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onLongPress(account, v);
            return true;
        });
    }

    private String formatCode(String code) {
        if (code.length() <= 4) return code;
        int mid = code.length() / 2;
        return code.substring(0, mid) + " " + code.substring(mid);
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    /** Called every second by the host activity to refresh visible codes/progress. */
    public void tick() {
        notifyItemRangeChanged(0, accounts.size());
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView issuerText;
        TextView codeText;
        ProgressBar progress;
        ImageView avatar;
        ImageView refreshButton;

        VH(@NonNull View itemView) {
            super(itemView);
            issuerText = itemView.findViewById(R.id.text_issuer);
            codeText = itemView.findViewById(R.id.text_code);
            progress = itemView.findViewById(R.id.progress_timer);
            avatar = itemView.findViewById(R.id.image_avatar);
            refreshButton = itemView.findViewById(R.id.button_refresh_hotp);
        }
    }
}
