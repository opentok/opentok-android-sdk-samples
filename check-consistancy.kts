import java.io.File
import kotlin.system.exitProcess

println("check-consistancy Start")

val errors = mutableListOf<String>()

File(".")
    .listFiles()
    .filter { it.isDirectory() }
    .filter { !it.name.startsWith(".") }
    .map { it.name }
    .forEach { projectDirectoryName ->
        // Check project itself
        checkPackage(projectDirectoryName)
        checkApplicationId(projectDirectoryName)
        checkApplicationName(projectDirectoryName)
        checkMainActivity(projectDirectoryName)
        checkReadme(projectDirectoryName)
        checkOpenTokConfig(projectDirectoryName)
        checkServerConfig(projectDirectoryName)

        // Check project references
        checkTopLevelReadmeContainsProject(projectDirectoryName)
        checkGithubWorkflowContainsProject(projectDirectoryName)
    }

if (errors.isNotEmpty()) {
    println("check-consistancy Error:")
    errors.forEach { println(it) }
    exitProcess(1)
} else {
    println("check-consistancy Ok")
}

/**
 * Check project package e.g.
 * Desired format: com.tokbox.sample.projectname
 */
fun checkPackage(projectDirectoryName: String) {
    // Check package in AndroidManifest.xml
    val filePath = "./$projectDirectoryName/app/src/main/AndroidManifest.xml"
    val file = File(filePath)
    val projectPackage = getProjectPackage(projectDirectoryName)

    if (!file.exists()) {
        addError(projectDirectoryName, "$filePath file not found")
    } else {
        val desiredProjectPackageIdString = "package=\"$projectPackage\""

        if (!file.contains(desiredProjectPackageIdString)) {
            addError(projectDirectoryName, "$filePath file has incorrect package (should be $projectPackage)")
        }
    }

    //Check if package directory structure exists
    val diretory = File(getProjectPackagePath(projectDirectoryName))

    if (!diretory.exists()) {
        addError(projectDirectoryName, "Incorrect package directory structure (should be $projectPackage)")
    }
}

/**
 * Check project application id e.g.
 * Desired format: com.tokbox.sample.projectname
 */
fun checkApplicationId(projectDirectoryName: String) {
    val filePath = "./$projectDirectoryName/app/build.gradle"
    val file = File(filePath)

    if (!file.exists()) {
        addError(projectDirectoryName, "$filePath file not found")
    } else {
        val desiredProjectApplicationId = "applicationId \"${getProjectPackage(projectDirectoryName)}\""

        if (!file.contains(desiredProjectApplicationId)) {
            val message = "$filePath has incorrect application id (should be $desiredProjectApplicationId)"
            addError(projectDirectoryName, message)
        }
    }
}

/**
 * Desired format of project application name e.g.
 * com.tokbox.sample.projectname
 */
fun checkApplicationName(projectDirectoryName: String) {
    val filePath = "./$projectDirectoryName/app/src/main/res/values/strings.xml"
    val file = File(filePath)

    if (!file.exists()) {
        addError(projectDirectoryName, "$filePath file not found")
    } else {
        val desiredApplicationName = "<string name=\"app_name\">$projectDirectoryName</string>"

        if (!file.contains(desiredApplicationName)) {
            val message = "$filePath has incorrect application name (should be $desiredApplicationName)"
            addError(projectDirectoryName, message)
        }
    }
}

/**
 * Checks if MainActivity class exists
 */
fun checkMainActivity(projectDirectoryName: String) {
    val filePath = getProjectPackagePath(projectDirectoryName) + "/MainActivity.java"
    val file = File(filePath)

    if (!file.exists()) {
        addError(projectDirectoryName, "$filePath file not found")
    }
}

/**
 * Check if top-level readme file contains project
 */
fun checkTopLevelReadmeContainsProject(projectDirectoryName: String) {
    val filePath = "README.md"
    val file = File(filePath)
    val titleCaseProjectName = getTitleCaseProjectName(projectDirectoryName)

    if(!file.contains(titleCaseProjectName)) {
        addError(projectDirectoryName, "$filePath file do not contain project title")
    }

    if(!file.contains(projectDirectoryName)) {
        addError(projectDirectoryName, "$filePath file do not contain project link")
    }
}

/**
 * Check if top-level readme file contain project
 */
fun checkGithubWorkflowContainsProject(projectDirectoryName: String) {
    val filePath = "./.github/workflows/check-projects.yml"
    val file = File(filePath)

    if(!file.contains("name: $projectDirectoryName")) {
        addError(projectDirectoryName, "$filePath file do not contain project name")
    }

    if(!file.contains("cd $projectDirectoryName && ./gradlew app:assembleRelease && cd ..")) {
        addError(projectDirectoryName, "$filePath file do not contain project check")
    }
}

/**
 * Check if project contains readme file
 */
fun checkReadme(projectDirectoryName: String) {
    val filePath = "./$projectDirectoryName/README.md"
    val file = File(filePath)

    if (!file.exists()) {
        addError(projectDirectoryName, "$filePath file not found")
    }
}

/**
 * Check if congif files have empty credentials.
 * This task prevents from adding non-empty credentials.
 */
fun checkOpenTokConfig(projectDirectoryName: String) {
    val filePath = getProjectPackagePath(projectDirectoryName) + "/OpenTokConfig.java"
    val file = File(filePath)

    if(file.exists()) {
        checkProperty(projectDirectoryName, file, "API_KEY")
        checkProperty(projectDirectoryName, file, "SESSION_ID")
        checkProperty(projectDirectoryName, file, "TOKEN")
    }
}

/**
 * Check if congif files have empty erver url.
 * This task prevents from adding non-empty server url.
 */
fun checkServerConfig(projectDirectoryName: String) {
    val filePath = getProjectPackagePath(projectDirectoryName) + "/ServerConfig.java"
    val file = File(filePath)

    if(file.exists()) {
        checkProperty(projectDirectoryName, file, "CHAT_SERVER_URL")
    }
}

// ================================== HELPERS

/**
 * Looks for a property in the file and verifis if it is empty.
 */
fun checkProperty(projectDirectoryName: String, file: File, propertyName: String) {
    val propertyLine = "public static final String $propertyName"

    file.readLines().forEach {
        if(it.contains(propertyLine) && !it.contains("\"\"")) {
            addError(projectDirectoryName, "Opentok config property is not empty: $it")
        }
    }
}

/**
 * Adds error to the list
 */
fun addError(projectDirectoryName: String, message: String) {
    errors.add("$projectDirectoryName: $message")
}

/**
 * Return project package e.g.
 * com.tokbox.sample.projectname
 */
fun getProjectPackage(projectDirectoryName: String) = "com.tokbox.sample.${ getRawProjectName(projectDirectoryName) }"

/**
 * Return project package path e.g.
 * ./Project-Name/app/src/main/java/com/tokbox/sample/projectname/
 */
fun getProjectPackagePath(projectDirectoryName: String): String {
    val projectPackage = getProjectPackage(projectDirectoryName);
    val packagePath = projectPackage.replace(".", "/")
    return "./$projectDirectoryName/app/src/main/java/$packagePath"
}

/**
 * Converts project name
 * e.g.
 * Input: Project-Name
 * Output: projectname
 */
fun getRawProjectName(projectDirectoryName: String) = projectDirectoryName.replace("-", "").toLowerCase();

/**
 * Converts project name
 * e.g.
 * Input: Project-Name
 * Output: Project Name
 */
fun getTitleCaseProjectName(projectDirectoryName: String) = projectDirectoryName.replace("-", " ");

/**
Check if file contains given string
 */
fun File.contains(string: String) = readLines().any { it.contains(string) }