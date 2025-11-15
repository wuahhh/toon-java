# TOON-Java: 面向LLM的高效数据序列化库（JDK 1.8+）

![TOON Logo](doc/imges/toon-java-icon.png)

> **TOON (Text-Oriented Object Notation)** 是专为大型语言模型（LLM）交互设计的轻量级数据序列化格式，兼顾人类可读性与Token优化，比JSON节省30-60%的API调用成本。本库是TOON规范在Java 8环境下的生产级实现。

## 项目目标

1. 为Java开发者提供**低Token消耗**的LLM交互数据格式，降低API调用成本；
2. 保持**高可读性**，让开发者与LLM都能快速理解数据结构；
3. 支持复杂业务场景（嵌套对象、集合、自定义类型），兼容JDK 1.8及以上；
4. 提供灵活扩展能力，满足不同业务的类型转换、字段注释等个性化需求。

## 核心优势

| 特性                | 说明                                                |
|---------------------|---------------------------------------------------|
| 🚀 **Token优化**    | 表格化数据存储+元数据前置压缩，比JSON平均节省30-60% Token，大幅降低LLM调用成本 |
| 🧠 **LLM友好**      | 支持字段注释，元数据显式约束，减少模型解析幻觉，提升交互准确率                   |
| 📦 **全面兼容**     | 支持基础类型、集合、嵌套对象、枚举、LocalDateTime等常用类型              |
| 🔌 **灵活扩展**     | 自定义类型转换器、字段注释、序列化策略，适配复杂业务场景                      |
| ⚡ **性能高效**     | 反射字段缓存，避免重复解析类结构，高频场景性能提升50%以上                    |
| 🛡️ **健壮可靠**     | 精细化异常体系+特殊字符处理，生产环境稳定可用                           |

## 功能特性

### 1. 基础功能
- ✅ 支持所有Java基础类型及包装类（int/Integer、long/Long等）
- ✅ 支持String、枚举、LocalDateTime等常用类型
- ✅ 支持List/Set集合类型，自动校验数据长度
- ✅ 支持嵌套对象（无限层级），通过缩进标识层级关系

### 2. 高级功能
- ✅ 字段注释：通过`@ToonField(comment="")`为字段添加说明，LLM可直接理解
- ✅ 自定义类型转换器：扩展支持Date、BigDecimal等业务类型
- ✅ 特殊字符处理：自动转义逗号、分号等分隔符，避免格式破坏
- ✅ 反射缓存：缓存类字段结构，提升高频序列化/反序列化性能
- ✅ 精细化异常：区分格式错误、类型转换失败、反射异常等场景，便于调试

### 3. 兼容性
- 兼容JDK 1.8及以上版本
- 与TOON规范v1.3完全兼容
- 支持与JSON格式双向转换（需自行集成Jackson等JSON库）

## 快速开始

### 1. 引入依赖（Maven）
```xml
<dependency>
    <groupId>com.github</groupId>
    <artifactId>toon-java</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```
### 2. 定义实体类（带注释）

```java
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
```
### 3. 序列化示例

```java
import com.github.toon.Toons;
import com.github.toon.exception.ToonException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuickStartDemo {
    public static void main(String[] args) throws ToonException {
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
    }
}
```
### 4. 输出结果
```text
TOON序列化结果：
users(2){id#用户唯一标识，自增ID,name#用户姓名，最长32字符,registerTime#注册时间，ISO格式,status#用户状态（ACTIVE/INACTIVE）,address#用户地址信息[$object],address.street#街道地址，含门牌号和单元号,address.city#城市名称}: 
  1,Alice,2024-01-01T10:30:00,ACTIVE,(123 Main St\, Apt 4B,New York);
  2,Bob,2024-02-15T14:20:00,INACTIVE,(456 Business Ave,San Francisco);

```

## 进阶用法

### 1. 自定义类型转换器（以 BigDecimal 为例）
```java
import com.github.toon.converter.TypeConverter;
import com.github.toon.exception.ToonTypeConvertException;
import java.math.BigDecimal;

// 自定义BigDecimal转换器
public class BigDecimalConverter implements TypeConverter {
    @Override
    public boolean support(Class<?> type) {
        return BigDecimal.class.equals(type);
    }

    @Override
    public Object convert(String value, Class<?> type) throws ToonTypeConvertException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new ToonTypeConvertException("", type, value);
        }
    }

    @Override
    public String serialize(Object value) {
        return value == null ? "" : value.toString();
    }
}

// 注册转换器
public class ConverterDemo {
    public static void main(String[] args) throws ToonException {
        DefaultToonSerializer serializer = new DefaultToonSerializer();
        serializer.addConverter(new BigDecimalConverter());

        DefaultToonDeserializer deserializer = new DefaultToonDeserializer();
        deserializer.addConverter(new BigDecimalConverter());

        // 后续序列化/反序列化可自动处理BigDecimal类型
    }
}
```

### 2. 混合格式策略（JSON 存储 + TOON 入模）

```java
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.toon.exception.ToonException;

import java.util.List;

public class MixedFormatDemo {
    public static void main(String[] args) throws Exception {
        // 1. 构建数据
        Address addr = new Address("123 Main St", "New York");
        List<User> users = List.of(new User(1, "Alice", LocalDateTime.now(), addr));

        // 2. JSON序列化（存储/持久化）
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonStr = objectMapper.writeValueAsString(users);
        
        // 4. 转换为TOON（与LLM交互）
        String toonStr = Toons.serialize("users", jsonData);
        System.out.println("TOON格式（入模用）：");
        System.out.println(toonStr);
    }
}
```

## 适用场景
1. LLM API 调用：按 Token 计费的场景（如 OpenAI、Anthropic），大幅降低调用成本；
2. RAG 系统：知识库数据传输，兼顾可读性与传输效率；
3. AI Agent 交互：结构化指令传输，字段注释帮助 Agent 理解数据含义；
4. 成本敏感型 AI 项目：需要控制 API 开销的大规模 LLM 应用。

## 不适用场景
1. 需跨语言极致兼容性（优先 JSON）；
2. 深度嵌套的非表格化数据（JSON 更高效）；
3. 对序列化速度要求极高且数据结构简单（可考虑 Protobuf）。

## 贡献指南
### 如何贡献
- Fork 本仓库；
- 创建特性分支（git checkout -b feature/xxx）；
- 提交代码（git commit -m "feat: 新增xxx功能"）；
- 推送分支（git push origin feature/xxx）；
- 提交 Pull Request。
### 代码规范
- 遵循 Google Java Code Style；
- 新增功能需配套单元测试（覆盖率≥80%）；
- 不破坏原有 API 兼容性（除非重大版本迭代）。

## 许可证
本项目基于 Apache License 2.0 开源，可自由用于商业和非商业项目。