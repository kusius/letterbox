import java.util.Properties

val generateDesktopCredentials by tasks.registering {
    val secretsFile = rootProject.file("secrets.properties")
    val outputFile = project.file("src/desktopMain/composeResources/files/credentials.json")

    inputs.file(secretsFile)
    outputs.file(outputFile)

    doLast {
        val secrets =
            Properties().apply {
                if (secretsFile.exists()) {
                    secretsFile.inputStream().use { load(it) }
                }
            }

        val clientId = secrets.getProperty("desktop_client_id", "").trim('"')
        val clientSecret = secrets.getProperty("desktop_client_secret", "").trim('"')
        val projectId = secrets.getProperty("desktop_project_id", "").trim('"')

        // Generate Google OAuth credentials JSON format
        val credentialsJson =
            """
            {
              "installed": {
                "client_id": "$clientId",
                "project_id": "$projectId",
                "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                "token_uri": "https://oauth2.googleapis.com/token",
                "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                "client_secret": "$clientSecret",
                "redirect_uris": ["http://localhost"]
              }
            }
            """.trimIndent()

        outputFile.parentFile.mkdirs()
        outputFile.writeText(credentialsJson)

        println("Generated desktop credentials to ${outputFile.absolutePath}")
    }
}

// Run before desktop resource processing
afterEvaluate {
    tasks.matching { it.name == "desktopProcessResources" }.configureEach {
        dependsOn(generateDesktopCredentials)
    }

    tasks.matching { it.name == "copyNonXmlValueResourcesForDesktopMain" }.configureEach {
        dependsOn(generateDesktopCredentials)
    }
}
