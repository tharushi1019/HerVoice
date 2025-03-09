package com.example.hervoice;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Parcelable {
    private String contactId;
    private String name;
    private String phone;
    private String relationship;
    private boolean smsAlert;

    // Default constructor required for Firebase
    public Contact() {
    }

    public Contact(String contactId, String name, String phone, String relationship, boolean smsAlert) {
        this.contactId = contactId;
        this.name = name;
        this.phone = phone;
        this.relationship = relationship;
        this.smsAlert = smsAlert;
    }

    // Parcelable Constructor
    protected Contact(Parcel in) {
        contactId = in.readString();
        name = in.readString();
        phone = in.readString();
        relationship = in.readString();
        smsAlert = in.readByte() != 0;
    }

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) { // Added setter
        this.contactId = contactId;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getRelationship() {
        return relationship;
    }

    public boolean isSmsAlert() {
        return smsAlert;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public void setSmsAlert(boolean smsAlert) {
        this.smsAlert = smsAlert;
    }

    // Validation method for phone numbers
    public boolean isValidPhoneNumber() {
        return phone != null && phone.matches("\\d{10}"); // Example: 10-digit phone number
    }

    @Override
    public String toString() {
        return "Contact{" +
                "contactId='" + contactId + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", relationship='" + relationship + '\'' +
                ", smsAlert=" + smsAlert +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(contactId);
        dest.writeString(name);
        dest.writeString(phone);
        dest.writeString(relationship);
        dest.writeByte((byte) (smsAlert ? 1 : 0));
    }
}
