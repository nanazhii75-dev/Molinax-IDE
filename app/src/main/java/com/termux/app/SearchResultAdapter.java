package com.termux.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.molinax.medialibrary.YtSearchResult;

import java.util.List;
import java.util.Locale;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    public interface OnResultClick {
        void onResultClick(YtSearchResult result);
    }

    private final List<YtSearchResult> results;
    private final OnResultClick listener;

    public SearchResultAdapter(List<YtSearchResult> results, OnResultClick listener) {
        this.results = results;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YtSearchResult result = results.get(position);
        holder.title.setText(result.getTitle());

        if (result.getDurationSec() != null) {
            holder.durationBadge.setVisibility(View.VISIBLE);
            holder.durationBadge.setText(formatDuration(result.getDurationSec()));
        } else {
            holder.durationBadge.setVisibility(View.GONE);
        }

        Glide.with(holder.itemView.getContext())
            .load(result.getThumbnailUrl())
            .centerCrop()
            .into(holder.thumb);

        holder.itemView.setOnClickListener(v -> listener.onResultClick(result));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    private static String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView durationBadge;
        TextView title;

        ViewHolder(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);
            durationBadge = itemView.findViewById(R.id.duration_badge);
            title = itemView.findViewById(R.id.title);
        }
    }
}
