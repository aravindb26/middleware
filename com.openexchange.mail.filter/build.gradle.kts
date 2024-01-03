val mainSrcDir = "src"
val jSievePrefix = "org/apache"
val javaCCDir = "javacc"
val libDir = "lib"

install {
    installBundleAsDirectory.set(false)
}

tasks {
    val preprocess = register<JavaExec>("preprocess") {
        description = "generate parse tree building actions"
        group = "JavaCC"
        doFirst {
            mkdir("$mainSrcDir/$jSievePrefix/jsieve/parser/generated")
        }
        classpath = files("$libDir/javacc.jar")
        mainClass.set("jjtree")
        args("-output_directory=$mainSrcDir/$jSievePrefix/jsieve/parser/generated", "$javaCCDir/sieve.jjt")
    }

    val replaceToken = register("replaceToken") {
        description = "fix the SimpleNode"
        group = "JavaCC"
        dependsOn(preprocess)
        val input = File(this.project.projectDir, "$mainSrcDir/$jSievePrefix/jsieve/parser/generated/SimpleNode.java")
        doLast {
            val content = input.readText(Charsets.US_ASCII)
            if (content.contains("public class SimpleNode extends org.apache.jsieve.parser.SieveNode")) { return@doLast }
            val filtered = content.replace(oldValue = "public class SimpleNode", newValue = "public class SimpleNode extends org.apache.jsieve.parser.SieveNode")
            input.writeText(filtered, Charsets.US_ASCII)
        }
    }

    val compileJJ = register<JavaExec>("compileJJ") {
        description = "Create the generated sources"
        group = "JavaCC"
        dependsOn(replaceToken)
        classpath = files("$libDir/javacc.jar")
        mainClass.set("javacc")
        args("-output_directory=$mainSrcDir/$jSievePrefix/jsieve/parser/generated",  "$mainSrcDir/$jSievePrefix/jsieve/parser/generated/sieve.jj")
        doLast {
            delete("$mainSrcDir/$jSievePrefix/jsieve/parser/generated/sieve.jj")
        }
    }
    named<JavaCompile>("compileJava") {
        dependsOn(compileJJ)
    }
    named<Delete>("clean") {
        println(this.name + " " + this.javaClass.name)
        doLast {
            delete(File("$mainSrcDir/$jSievePrefix/jsieve/parser/generated"))
        }
    }
}
