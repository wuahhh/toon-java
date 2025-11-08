package com.github.toon;

import com.github.toon.anno.ToonField;

import java.time.LocalDateTime;

public class Address {
    @ToonField(order = 1, comment = "街道地址，含门牌号")
    private String street;

    @ToonField(order = 2, comment = "城市名称")
    private String city;

    @Override
    public String toString() {
        return "Address{street='" + street + "', city='" + city + "'}";
    }

    public Address(String street, String city) {
        this.street = street;
        this.city = city;
    }
}

enum UserStatus {
    ACTIVE, INACTIVE
}

class User {
    @ToonField(order = 1, comment = "用户唯一标识，自增ID")
    private int id;

    @ToonField(order = 2, comment = "用户姓名，最长32字符")
    private String name;

    @ToonField(order = 3, comment = "注册时间，ISO格式")
    private LocalDateTime registerTime;

    @ToonField(order = 4, comment = "用户状态（ACTIVE/INACTIVE）")
    private UserStatus status;

    @ToonField(order = 5, comment = "用户地址信息")
    private Address address;
    public User(int id, String name, LocalDateTime registerTime, UserStatus status, Address address) {
        this.id = id;
        this.name = name;
        this.registerTime = registerTime;
        this.status = status;
        this.address = address;
    }
}
