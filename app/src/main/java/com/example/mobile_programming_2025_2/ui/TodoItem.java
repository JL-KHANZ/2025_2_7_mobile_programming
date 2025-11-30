package com.example.mobile_programming_2025_2.data;

public class TodoItem {
    private String text;
    private boolean isChecked;

    public TodoItem(String text, boolean isChecked) {
        this.text = text;
        this.isChecked = isChecked;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }
}