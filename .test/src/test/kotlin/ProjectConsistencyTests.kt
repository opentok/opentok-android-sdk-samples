import org.amshove.kluent.should
import org.amshove.kluent.shouldExist
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

class ProjectConsistencyTests {

    @ParameterizedTest(name = "{0} AndroidManifest file exists")
    @MethodSource("getProjectNames")
    fun `AndroidManifest file exists`(projectDirectoryName: String) {
        val filePath = "$repoRootDirectoryPath/$projectDirectoryName/app/src/main/AndroidManifest.xml"
        val file = File(filePath)

        // ./Basic-Audio-Driver/app/src/main/AndroidManifest.xml
        file.shouldExist()
    }

    @ParameterizedTest(name = "{0} AndroidManifest file contains correct package id")
    @MethodSource("getProjectNames")
    fun `AndroidManifest file contains correct package id`(projectDirectoryName: String) {
        val filePath = "$repoRootDirectoryPath/$projectDirectoryName/app/src/main/AndroidManifest.xml"
        val file = File(filePath)
        val projectPackage = getProjectPackage(projectDirectoryName)

        // format: com.tokbox.sample.projectname
        val desiredProjectPackageIdString = "package=\"$projectPackage\""

        file shouldContainLineContainingString desiredProjectPackageIdString
    }

    @ParameterizedTest(name = "{0} project contains correct package directory structure")
    @MethodSource("getProjectNames")
    fun `project contains correct package directory structure`(projectDirectoryName: String) {
        val projectPackagePath = getAbsoluteProjectPackagePath(projectDirectoryName)
        val directory = File(projectPackagePath)

        // format: com/tokbox/sample/projectname
        directory.shouldExist()
    }

    @ParameterizedTest(name = "{0} app build gradle file exists")
    @MethodSource("getProjectNames")
    fun `app build gradle file exists`(projectDirectoryName: String) {
        val filePath = "$repoRootDirectoryPath/$projectDirectoryName/app/build.gradle"
        val file = File(filePath)

        // ./Basic-Audio-Driver/app/build.gradle
        file.shouldExist()
    }

    @ParameterizedTest(name = "{0} app build gradle file contains correct application id")
    @MethodSource("getProjectNames")
    fun `app build gradle file contains correct application id`(projectDirectoryName: String) {
        val filePath = "$repoRootDirectoryPath/$projectDirectoryName/app/build.gradle"
        val file = File(filePath)

        // applicationId "com.tokbox.sample.basicaudiodriver"
        val desiredProjectApplicationId = "applicationId \"${getProjectPackage(projectDirectoryName)}\""
        file shouldContainLineContainingString desiredProjectApplicationId
    }

    @ParameterizedTest(name = "{0} strings xml file contains correct application name")
    @MethodSource("getProjectNames")
    fun `strings xml file contains correct application name`(projectDirectoryName: String) {
        val filePath = "$repoRootDirectoryPath/$projectDirectoryName/app/src/main/res/values/strings.xml"
        val file = File(filePath)

        file.shouldExist()

        // format: <string name="app_name">Project-Name</string>
        val desiredApplicationName = "<string name=\"app_name\">$projectDirectoryName</string>"
        file shouldContainLineContainingString desiredApplicationName
    }

    @ParameterizedTest(name = "{0} MainActivity class exists in project")
    @MethodSource("getProjectNames")
    fun `MainActivity class exists in project`(projectDirectoryName: String) {
        val filePath = getAbsoluteProjectPackagePath(projectDirectoryName) + "/MainActivity.java"

        // ./Basic-Audio-Driver/app/src/main/java/com/tokbox/sample/projectname/MainActivity.java
        val file = File(filePath)
        file.shouldExist()
    }

    @ParameterizedTest(name = "{0} repository top-level README md file contains application name")
    @MethodSource("getProjectNames")
    fun `repository top-level README md file contains application name`(projectDirectoryName: String) {
        val filePath = "$repoRootDirectoryPath/README.md"
        val file = File(filePath)

        // [Project-Name]/(./README.md)
        val desiredProjectLink = "[$projectDirectoryName](./$projectDirectoryName)"
        file shouldContainLineContainingString desiredProjectLink
    }

    @ParameterizedTest(name = "{0} project directory contains README md file")
    @MethodSource("getProjectNames")
    fun `project directory contains README md file`(projectDirectoryName: String) {
        val filePath = "${getAbsoluteProjectPath(projectDirectoryName)}/README.md"

        // Project-Name/README.md
        val file = File(filePath)
        file.shouldExist()
    }

    @ParameterizedTest(name = "{0} project dedicated GithubActions workflow file exists")
    @MethodSource("getProjectNames")
    fun `project dedicated GithubActions workflow file exists`(projectDirectoryName: String) {
        val workflowFileName = "build-${projectDirectoryName.toLowerCase()}.yml"
        val filePath = "$repoRootDirectoryPath/.github/workflows/$workflowFileName"

        // .github/workflows/build-basic-audio-driver.yml
        val file = File(filePath)
        file.shouldExist()
    }

    @ParameterizedTest(name = "{0} project GithubActions workflow file contains correct project name")
    @MethodSource("getProjectNames")
    fun `project GithubActions workflow file contains correct project name`(projectDirectoryName: String) {
        val workflowFileName = "build-${projectDirectoryName.toLowerCase()}.yml"

        val filePath = "$repoRootDirectoryPath/.github/workflows/$workflowFileName"
        val file = File(filePath)

        // Format: "name: Project-Name"
        val desiredProjectName = "name: $projectDirectoryName"
        file shouldContainLineContainingString desiredProjectName
    }

    @ParameterizedTest(name = "{0} project GithubActions workflow file contains correct build command")
    @MethodSource("getProjectNames")
    fun `project GithubActions workflow file contains correct project build command`(projectDirectoryName: String) {
        val workflowFileName = "build-${projectDirectoryName.toLowerCase()}.yml"

        val filePath = "$repoRootDirectoryPath/.github/workflows/$workflowFileName"
        val file = File(filePath)

        // "cd Project-Name && ./gradlew app:assembleRelease && cd .."
        val desiredBuildCommand = "cd $projectDirectoryName && ./gradlew app:assembleRelease && cd .."
        file shouldContainLineContainingString desiredBuildCommand
    }

    @ParameterizedTest(name = "{0} OpenTokConfig file has empty API_KEY")
    @MethodSource("getProjectNames")
    fun `OpenTokConfig file has empty API_KEY`(projectDirectoryName: String) {
        val filePath = getAbsoluteProjectPackagePath(projectDirectoryName) + "/OpenTokConfig.java"
        val file = File(filePath)

        val propertyName = "API_KEY"

        if (file.exists() && file.lineContains(propertyName)) {
            file shouldContainLineContainingString "public static final String $propertyName = \"\""
        }
    }

    @ParameterizedTest(name = "{0} OpenTokConfig file has empty SESSION_ID")
    @MethodSource("getProjectNames")
    fun `OpenTokConfig file has empty SESSION_ID`(projectDirectoryName: String) {
        val filePath = getAbsoluteProjectPackagePath(projectDirectoryName) + "/OpenTokConfig.java"
        val file = File(filePath)

        val propertyName = "SESSION_ID"

        if (file.exists() && file.lineContains(propertyName)) {
            file shouldContainLineContainingString "public static final String $propertyName = \"\""
        }
    }

    @ParameterizedTest(name = "{0} OpenTokConfig file has empty TOKEN")
    @MethodSource("getProjectNames")
    fun `OpenTokConfig file has empty TOKEN`(projectDirectoryName: String) {
        val filePath = getAbsoluteProjectPackagePath(projectDirectoryName) + "/OpenTokConfig.java"
        val file = File(filePath)

        val propertyName = "TOKEN"

        if (file.exists() && file.lineContains(propertyName)) {
            file shouldContainLineContainingString "public static final String $propertyName = \"\""
        }
    }

    @ParameterizedTest(name = "{0} Server file has empty CHAT_URL")
    @MethodSource("getProjectNames")
    fun `Server file has empty CHAT_URL`(projectDirectoryName: String) {
        val filePath = getAbsoluteProjectPackagePath(projectDirectoryName) + "/Server.java"
        val file = File(filePath)

        val propertyName = "CHAT_URL"

        if (file.exists() && file.lineContains(propertyName)) {
            file shouldContainLineContainingString "public static final String $propertyName = \"\""
        }
    }

    companion object {
        /**
         * Return repository root directory File
         */
        private val repoRootDirectoryFile by lazy {
            val scriptDirectory = File("")
            println(scriptDirectory.absolutePath)
            scriptDirectory.absoluteFile.parentFile
        }

        /**
         * Return repository root directory
         */
        private val repoRootDirectoryPath = repoRootDirectoryFile.path

        /**
         * Return list of project names
         */
        @JvmStatic
        private fun getProjectNames(): List<String> {
            return repoRootDirectoryFile.listFiles()
                .filter { it.isDirectory }
                .filter { !it.name.startsWith(".") }
                .map { it.name }
        }

        /**
         * Return project package e.g.
         * com.tokbox.sample.projectname
         */
        private fun getProjectPackage(projectDirectoryName: String) =
            "com.tokbox.sample.${getRawProjectName(projectDirectoryName)}"

        /**
         * Return project package path e.g.
         * ./Project-Name/
         */
        private fun getAbsoluteProjectPath(projectDirectoryName: String) = "$repoRootDirectoryPath/$projectDirectoryName"

        /**
         * Return project package path e.g.
         * ./Project-Name/app/src/main/java/com/tokbox/sample/projectname/
         */
        private fun getAbsoluteProjectPackagePath(projectDirectoryName: String): String {
            val projectPackage = getProjectPackage(projectDirectoryName);
            val packagePath = projectPackage.replace(".", "/")
            return "$repoRootDirectoryFile/$projectDirectoryName/app/src/main/java/$packagePath"
        }

        /**
         * Converts project name
         * e.g.
         * Input: Project-Name
         * Output: projectname
         */
        private fun getRawProjectName(projectDirectoryName: String) =
            projectDirectoryName.replace("-", "").toLowerCase()
    }

    private fun File.lineContains(string: String) = readLines().any { it.contains(string) }
}

/**
Check if the file contains a given string within any of its lines
 */
infix fun File.shouldContainLineContainingString(string: String) =
    this.should("The file '${this.absolutePath}' should have a line containing string \"$string\", but does not") {
        readLines().any { it.contains(string) }
    }

/**
Check if the file contains a given string within any of its lines
 */
infix fun File.shouldContainEmptyProperty(propertyName: String) =
    this.should("The file '${this.absolutePath}' should contain empty property \"$propertyName\", but does not") {
        val propertyLine = "public static final String $propertyName"
        readLines().any { it.contains(propertyLine) && !it.contains("\"\"") }
    }
