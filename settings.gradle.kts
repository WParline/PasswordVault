pluginManagement {
    repositories {
        // 阿里云镜像
        maven { url=uri( "https://maven.aliyun.com/repository/public") }
        maven { url=uri( "https://maven.aliyun.com/repository/google") }
        maven { url=uri( "https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url=uri( "https://maven.aliyun.com/repository/central") }

        // 华为云镜像
        maven { url=uri( "https://repo.huaweicloud.com/repository/maven/") }

        // 腾讯云镜像
        maven { url=uri( "https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 网易镜像
        maven { url=uri( "https://mirrors.163.com/maven/repository/maven-public/") }

        // 首都在线
        maven { url=uri( "https://maven.oscs.oschina.net/content/groups/public/") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url=uri( "https://maven.aliyun.com/repository/public") }
        maven { url=uri( "https://maven.aliyun.com/repository/google") }
        maven { url=uri( "https://maven.aliyun.com/repository/central") }

        // 华为云镜像
        maven { url=uri( "https://repo.huaweicloud.com/repository/maven/") }

        // 腾讯云镜像
        maven { url=uri( "https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 网易镜像
        maven { url=uri( "https://mirrors.163.com/maven/repository/maven-public/") }

        // 首都在线
        maven { url=uri( "https://maven.oscs.oschina.net/content/groups/public/") }
        google()
        mavenCentral()
    }
}

rootProject.name = "PasswordVault"
include(":app")
