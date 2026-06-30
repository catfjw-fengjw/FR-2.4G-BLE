plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.rfcontrol"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.rfcontrol"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}

afterEvaluate {
    tasks.withType<Test>().configureEach {
        if (name != "testDebugUnitTest") return@configureEach
        dependsOn("compileDebugUnitTestJavaWithJavac", "compileDebugUnitTestKotlin")
        val javaTestClasses = layout.buildDirectory.dir(
            "intermediates/javac/debugUnitTest/compileDebugUnitTestJavaWithJavac/classes"
        )
        val kotlinTestClasses = layout.buildDirectory.dir(
            "intermediates/built_in_kotlinc/debugUnitTest/compileDebugUnitTestKotlin/classes"
        )
        testClassesDirs = files(javaTestClasses.get().asFile, kotlinTestClasses.get().asFile)
        classpath = classpath.plus(files(javaTestClasses.get().asFile, kotlinTestClasses.get().asFile))
        doFirst {
            classpath = classpath.plus(testClassesDirs)
        }
    }
}
