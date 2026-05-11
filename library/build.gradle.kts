plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "io.github.morozione.rollingtext"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "io.github.morozione"
            artifactId = "compose-rolling-text"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Compose Rolling Text")
                description.set("A Jetpack Compose library for animated rolling/odometer-style text transitions")
                url.set("https://github.com/morozione/compose-rolling-text")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("morozione")
                        name.set("Ivan Moroz")
                    }
                }

                scm {
                    url.set("https://github.com/morozione/compose-rolling-text")
                    connection.set("scm:git:git://github.com/morozione/compose-rolling-text.git")
                    developerConnection.set("scm:git:ssh://git@github.com/morozione/compose-rolling-text.git")
                }
            }
        }
    }
}
