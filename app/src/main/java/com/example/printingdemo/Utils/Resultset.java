package com.example.printingdemo.Utils;

public class Resultset {
    private String names;
    private String number;
    private String dateTrain;

    public Resultset(String names, String number, String dateTrain) {
        this.names = names;
        this.number = number;
        this.dateTrain = dateTrain;
    }

    public String getNames() {
        return names;
    }

    public String getNumber() {
        return number;
    }

    public String getDateTrain() {
        return dateTrain;
    }
}
