plugins {
    val kotlinVersion = "1.5.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version "2.10.0"
}

group = "com.hamster.pray.genshin"
version = "1.2.0"

repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    mavenCentral()
}

dependencies {
    api("net.mamoe:mirai-console-terminal:2.9.2") // 自行替换版本
    api("net.mamoe:mirai-core:2.9.2")
    api("org.apache.httpcomponents:fluent-hc:4.5.13")
    api("com.squareup.okhttp3:okhttp:4.9.3")
    api("com.google.code.gson:gson:2.9.0")
}


