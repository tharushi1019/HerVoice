package com.example.hervoice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private final List<Contact> contactList;
    private final OnItemClickListener dialClickListener;
    private final OnItemClickListener smsClickListener;
    private final OnItemClickListener deleteClickListener;
    private final OnItemClickListener editClickListener;

    public interface OnItemClickListener {
        void onItemClick(Contact contact);  // Pass the contact object for all listeners
    }

    // Updated constructor: Pass the whole contact object for edit
    public ContactAdapter(List<Contact> contactList,
                          OnItemClickListener dialClickListener,
                          OnItemClickListener smsClickListener,
                          OnItemClickListener deleteClickListener,
                          OnItemClickListener editClickListener) {
        this.contactList = contactList;
        this.dialClickListener = dialClickListener;
        this.smsClickListener = smsClickListener;
        this.deleteClickListener = deleteClickListener;
        this.editClickListener = editClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        holder.contactName.setText(contact.getName());

        // Dial contact when phone button is clicked
        holder.contactPhone.setOnClickListener(v -> dialClickListener.onItemClick(contact));

        // Send SMS when message button is clicked
        holder.contactMessage.setOnClickListener(v -> smsClickListener.onItemClick(contact));

        // Set up the delete button for each contact
        holder.contactDelete.setOnClickListener(v -> deleteClickListener.onItemClick(contact));

        // Make contact name clickable to navigate to edit contact activity
        holder.contactName.setOnClickListener(v -> {
            if (editClickListener != null) {
                editClickListener.onItemClick(contact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView contactName;
        ImageButton contactPhone, contactMessage, contactDelete;

        ViewHolder(View itemView) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name_1);
            contactPhone = itemView.findViewById(R.id.contact_phone_1);
            contactMessage = itemView.findViewById(R.id.contact_message_1);
            contactDelete = itemView.findViewById(R.id.contact_delete_1);
        }
    }
}
