package com.example.myapplication.keep;

import java.io.Serializable;

public class User implements Serializable {
    private String name;
    private int age;
    private String email;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }

    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setEmail(String email) { this.email = email; }

    private void updateProfile() {

    }
}