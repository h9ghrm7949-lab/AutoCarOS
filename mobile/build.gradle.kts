plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.dagger.hilt.android)
}

android {
    namespace = "com.autocar.launcher"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {

        applicationId = "com.autocar.launcher"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = properties["VERSION_CODE"].toString().toInt()
        versionName = properties["VERSION_NAME"].toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures{
        dataBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {

    // test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.swiperefreshlayout)
    implementation(libs.recyclerview)
    implementation(libs.constraintlayout)

    implementation(libs.base.util)
    implementation(libs.base.adapter)

    implementation(libs.android.ktx)

    implementation(libs.flowlayout)

    implementation(libs.logx)

    //leakCanary
    debugImplementation(libs.leakcanary)

    ksp(libs.room.compiler) {
        exclude(group = "com.intellij", module = "annotations") // 精准排除冲突依赖
    }

    implementation(libs.annotation)

    // glide
    implementation(libs.glide.core)
    ksp(libs.glide.compiler)

    // hilt
    api(libs.dagger.hilt.android)
    kapt(libs.dagger.hilt.compiler)

    implementation(libs.gson)


    implementation(libs.preference)
    implementation(libs.timber)

}


kapt {
    correctErrorTypes =  true
}