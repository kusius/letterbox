import java.util.Properties

// Generate iOS Secrets.xcconfig from secrets.properties
val generateIosSecrets by tasks.registering {
    val secretsFile = rootProject.file("secrets.properties")
    val outputFile = rootProject.file("iosApp/Configuration/Secrets.xcconfig")

    inputs.file(secretsFile)
    outputs.file(outputFile)

    doLast {
        val secrets =
            Properties().apply {
                if (secretsFile.exists()) {
                    secretsFile.inputStream().use { load(it) }
                }
            }

        val iosClientId = secrets.getProperty("ios_client_id", "").trim('"')
        val reversedClientId =
            if (iosClientId.isNotEmpty()) {
                "com.googleusercontent.apps." + iosClientId.substringBefore(".apps.googleusercontent.com")
            } else {
                ""
            }

        outputFile.writeText(
            """
            // This file is auto-generated from secrets.properties
            // DO NOT EDIT MANUALLY - your changes will be overwritten
            // Generated at build time by generateIosSecrets Gradle task
            
            IOS_CLIENT_ID = $iosClientId
            IOS_REVERSED_CLIENT_ID = $reversedClientId
            """.trimIndent(),
        )

        println("Generated iOS secrets to ${outputFile.absolutePath}")
    }
}

val updateIosVersion by tasks.registering {
    val configFile = rootProject.file("iosApp/Configuration/Config.xcconfig")
    val version = providers.gradleProperty("iosVersion")

    inputs.property("version", version)
    outputs.file(configFile)

    doLast {
        val versionStr = version.orNull
            ?: throw GradleException("Usage: ./gradlew updateIosVersion -PiosVersion=1.2.3")
        val versionCode = versionStr.toVersionCode()

        val content = configFile.readText()
        val updatedContent =
            content
                .replaceFirst(
                    Regex("MARKETING_VERSION\\s*=\\s*[^\\n]+"),
                    "MARKETING_VERSION=$versionStr",
                ).replaceFirst(
                    Regex("CURRENT_PROJECT_VERSION\\s*=\\s*[^\\n]+"),
                    "CURRENT_PROJECT_VERSION=$versionCode",
                )

        configFile.writeText(updatedContent)
        println("Updated iOS MARKETING_VERSION to $versionStr and CURRENT_PROJECT_VERSION to $versionCode")
    }
}

// Run before iOS builds
tasks.matching { it.name.startsWith("compile") && it.name.contains("Ios") }.configureEach {
    dependsOn(generateIosSecrets)
}
