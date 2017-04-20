package org.projects.shoppinglist;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by cecil on 09-03-2017.
 */

public class Product implements Parcelable {
    private String name;
    private int quantity;
    private double price; //Prisen pr. styk
    private String description;

    public Product(){

    }

    public Product(Parcel in) {
        name = in.readString();
        quantity = in.readInt();
        price = in.readDouble();
        description = in.readString();
    }

    public Product(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
        this.price = -1;
        this.description = "None";
    }

    public Product(String name, int quantity,
                   double price, String description) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    @Override
    public String toString() {
        return name + " x" + quantity;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(quantity);
        dest.writeDouble(price);
        dest.writeString(description);
    }

    public static final Parcelable.Creator CREATOR
            = new Parcelable.Creator() {
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        public Product[] newArray(int size) {
            return new Product[size];
        }
    };
}
