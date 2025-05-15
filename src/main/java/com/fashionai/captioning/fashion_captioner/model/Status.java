package com.fashionai.captioning.fashion_captioner.model;

public enum Status {
    ACTIVE(0),
    SUSPENDED(1);

    final int value;
    public int value() {
        return this.value;
    }

    Status(int value) {
        this.value = value;
    }
}
