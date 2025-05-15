package com.fashionai.captioning.fashion_captioner.model;

public enum CategoryStatus {
    DISPLAY(0),
    HIDDEN(1);

    final int value;
    public int value() {
        return this.value;
    }

    CategoryStatus(int value) {
        this.value = value;
    }
}