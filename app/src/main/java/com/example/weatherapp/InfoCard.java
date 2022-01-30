package com.example.weatherapp;


public class InfoCard {
    private String title;
    private String valueTop, valueBottom;
    private int icon;
    // The background color applies to all cards to its a static variable
    private static int backgroundColor;


    public InfoCard(String title, String valueTop, String valueBottom, int icon) {
        this.title = title;
        this.valueTop = valueTop;
        this.valueBottom = valueBottom;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getValueTop() {
        return valueTop;
    }

    public void setValueTop(String valueTop) {
        this.valueTop = valueTop;
    }

    public String getValueBottom() {
        return valueBottom;
    }

    public void setValueBottom(String valueBottom) {
        this.valueBottom = valueBottom;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public static int getBackgroundColor() {
        return backgroundColor;
    }

    public static void setBackgroundColor(int backgroundColor) {
        InfoCard.backgroundColor = backgroundColor;
    }
}
