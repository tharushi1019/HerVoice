package com.example.hervoice;

public class Contact {

    private String name;
    private String phoneNumber;
    private String relationship;
    private boolean smsAlert;

    // Default constructor
    public Contact() {}

    // Constructor with parameters
    public Contact(String name, String phoneNumber, String relationship, boolean smsAlert) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
        this.smsAlert = smsAlert;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public boolean isSmsAlert() {
        return smsAlert;
    }

    public void setSmsAlert(boolean smsAlert) {
        this.smsAlert = smsAlert;
    }
}
