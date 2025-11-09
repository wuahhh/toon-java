package com.github.toon;

import com.github.toon.core.DefaultToonSerializer;
import com.github.toon.exception.ToonException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ToonSerializerTest {
public static void main(String[] args) throws ToonException {
    // 构建测试数据
    Address addr = new Address("123 Main St, Apt 4B", "New York"); // 含逗号
    User user1 = new User(1, "Alice", LocalDateTime.of(2024, 1, 1, 10, 30), UserStatus.ACTIVE, addr);
    User user2 = new User(2, "Bob", LocalDateTime.of(2024, 2, 15, 14, 20), UserStatus.INACTIVE, addr);
    List<User> users = new ArrayList<>();
    users.add(user1);
    users.add(user2);

    // 执行序列化
    String toonStr = Toons.serialize("users", users);
    System.out.println("TOON序列化结果：");
    System.out.println(toonStr);
}
}
