package com.fashionai.captioning.fashion_captioner.model;

public enum Role {
    ADMIN(0),
    USER(1);

    final int value;

    public int value() {
        return this.value;
    }

    Role(int value) {
        this.value = value;
    }
}