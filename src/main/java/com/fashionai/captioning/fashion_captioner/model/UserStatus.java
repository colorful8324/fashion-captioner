package com.fashionai.captioning.fashion_captioner.model;

public enum UserStatus {
    ACTIVE(0),
    SUSPENDED(1);

    final int value;
    public int value() {
        return this.value;
    }

    UserStatus(int value) {
        this.value = value;
    }
}