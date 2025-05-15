package com.fashionai.captioning.fashion_captioner.model;

public enum ProductStatus {
    AVAILABLE(0),
    UNAVAILABLE(1);

    final int value;
    public int value() {
        return this.value;
    }

    ProductStatus(int value) {
        this.value = value;
    }
}