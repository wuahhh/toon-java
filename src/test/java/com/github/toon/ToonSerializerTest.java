package com.github.toon;

import com.github.toon.exception.ToonException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ToonSerializerTest {

    public static void main(String[] args) throws ToonException {
        testCollectionSerialization();
        testSingleObjectSerialization();
        testEmptyCollectionSerialization();
    }
    /**
     * 测试完整对象集合的序列化
     */
    public static void testCollectionSerialization() throws ToonException {
        // 1. 构建测试数据（含特殊字符、嵌套对象、枚举类型）
        Address homeAddr = new Address("123 Main St, Apt 4B", "New York"); // 包含逗号的字符串
        Address workAddr = new Address("456 Business Ave", "San Francisco");

        User user1 = new User(
                1,
                "Alice",
                LocalDateTime.of(2024, 1, 1, 10, 30),
                UserStatus.ACTIVE,
                homeAddr
        );

        User user2 = new User(
                2,
                "Bob",
                LocalDateTime.of(2024, 2, 15, 14, 20),
                UserStatus.INACTIVE,
                workAddr
        );

        List<User> userList = new ArrayList<>();
        userList.add(user1);
        userList.add(user2);

        // 2. 执行序列化（使用全局工具类）
        String toonStr = Toons.serialize("users", userList);
        System.out.println("集合序列化结果:\n" + toonStr);

        // 3. 验证序列化结果
        assertNotNull("序列化结果不应为null", toonStr);
        assertTrue("应包含集合元数据", toonStr.contains("users(2){"));
        assertTrue("应包含字段注释", toonStr.contains("id#用户唯一标识，自增ID"));
        assertTrue("应正确转义特殊字符", toonStr.contains("123 Main St\\, Apt 4B")); // 验证逗号转义
        assertTrue("应包含嵌套对象信息", toonStr.contains("address#用户地址信息"));
    }

    /**
     * 测试单个对象的序列化
     */
    public static void testSingleObjectSerialization() throws ToonException {
        // 构建单个嵌套对象
        Address address = new Address("789 Park Rd", "Chicago");
        User user = new User(3, "Charlie", LocalDateTime.now(), UserStatus.ACTIVE, address);
        // 如需添加自定义转换器可在此处注册
        // serializer.addConverter(new BigDecimalConverter());

        String toonStr = Toons.serialize("singleUser", user);
        System.out.println("单个对象序列化结果:\n" + toonStr);

        // 验证结果
        assertNotNull(toonStr);
        assertTrue(toonStr.contains("singleUser{"));
        assertTrue(toonStr.contains("Charlie"));
        assertTrue(toonStr.contains("Chicago"));
    }

    /**
     * 测试空集合的序列化
     */
    public static void testEmptyCollectionSerialization() throws ToonException {
        List<User> emptyList = new ArrayList<>();
        String toonStr = Toons.serialize("emptyUsers", emptyList);
        System.out.println("空集合序列化结果:\n" + toonStr);

        assertTrue("空集合应标识为(0)", toonStr.contains("emptyUsers(0){}:"));
    }

    static class Address {
        @com.github.toon.anno.ToonField(order = 1, comment = "街道地址，含门牌号和单元号")
        private String street;

        @com.github.toon.anno.ToonField(order = 2, comment = "城市名称")
        private String city;

        public Address() {} // 反序列化需要默认构造函数

        public Address(String street, String city) {
            this.street = street;
            this.city = city;
        }

        // getter和setter（序列化需要访问字段值）
        public String getStreet() { return street; }
        public String getCity() { return city; }
    }

    enum UserStatus {
        ACTIVE, INACTIVE
    }

    static class User {
        @com.github.toon.anno.ToonField(order = 1, comment = "用户唯一标识，自增ID")
        private int id;

        @com.github.toon.anno.ToonField(order = 2, comment = "用户姓名，最长32字符")
        private String name;

        @com.github.toon.anno.ToonField(order = 3, comment = "注册时间，ISO格式")
        private LocalDateTime registerTime;

        @com.github.toon.anno.ToonField(order = 4, comment = "用户状态（ACTIVE/INACTIVE）")
        private UserStatus status;

        @com.github.toon.anno.ToonField(order = 5, comment = "用户地址信息")
        private Address address;

        public User() {} // 反序列化需要默认构造函数

        public User(int id, String name, LocalDateTime registerTime, UserStatus status, Address address) {
            this.id = id;
            this.name = name;
            this.registerTime = registerTime;
            this.status = status;
            this.address = address;
        }

        // getter和setter
        public int getId() { return id; }
        public String getName() { return name; }
        public LocalDateTime getRegisterTime() { return registerTime; }
        public UserStatus getStatus() { return status; }
        public Address getAddress() { return address; }
    }
}
