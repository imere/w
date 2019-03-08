package com.w.im;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

class ListAdapter extends RecyclerView.Adapter<ListAdapter.ListViewHolder> {
    private OnItemClickListener mOnItemClickListener;
    private LayoutInflater inflater;

    private List<Item> mList = new ArrayList<>(0);

    public ListAdapter(Context context, OnItemClickListener onItemClickListener) {
        inflater = LayoutInflater.from(context);
        mOnItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = inflater.inflate(R.layout.list_item, parent, false);
        return new ListViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull ListViewHolder holder, int position) {
        Item item = mList.get(position);
        holder.mTitle.setText(item.getTitle());
        holder.mContent.setText(item.getContent());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public List<Item> getAll() {
        return mList;
    }

    void clear() {
        mList.clear();
    }

    public void setAll(List<Item> list) {
        mList.clear();
        mList.addAll(list);
        notifyDataSetChanged();
    }

    public class ListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView mTitle;
        private TextView mContent;

        ListViewHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            mTitle = itemView.findViewById(R.id.title);
            mContent = itemView.findViewById(R.id.content);

        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onClick(v, getLayoutPosition());
            }
        }
    }

    interface OnItemClickListener {
        void onClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }
}