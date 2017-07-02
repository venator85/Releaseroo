package eu.alessiobianchi.releaseroo

import com.android.build.gradle.api.BaseVariant
import org.apache.commons.io.FileUtils
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.jtwig.JtwigModel
import org.jtwig.JtwigTemplate

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

class ReleaserooPlugin implements Plugin<Project> {

	private static final String DIST_DIR_NAME = "dist"
	private static final String DIST_STATIC_DIR_NAME = "static"
	private static final String INDEX_HTM = "index.htm"

	@Override
	void apply(Project project) {
		try {
			Class.forName("com.android.builder.Version")
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("releaseroo requires the Android plugin to be configured", e)
		}

		DomainObjectCollection<BaseVariant> variants
		if (project.plugins.hasPlugin('com.android.application')) {
			variants = project.android.applicationVariants
		} else {
			throw new IllegalArgumentException('releaseroo plugin can only be applied to Android application modules')
		}

		project.extensions.create("releaseroo", ReleaserooPluginExtension)

		def generateReleaserooPage = createGenerateReleaserooPage(project)

		variants.all { variant ->
			if (variant.hasProperty('releaseroo.profile')) {
				String profile = variant.ext['releaseroo.profile']

				def releaseBuildTask = createReleaseTask(project, variant, profile)
				releaseBuildTask.dependsOn variant.assemble
				releaseBuildTask.finalizedBy generateReleaserooPage
			}
		}
	}

	Task createGenerateReleaserooPage(Project project) {
		return project.tasks.create(name: "generateReleaserooPage") {
			group "releaseroo"
			description "Generates an HTML page with all the releases"
			doLast {
				ReleaseFactory releaseFactory = new ReleaseFactory()

				def releases = project.files { project.releaseroo.releasesDir.listFiles() }.findAll {
					it.name.endsWith(".apk")
				}.collect {
					releaseFactory.create(it)
				}.toSorted()

				File releasesDir = project.releaseroo.releasesDir

				// cleanup
				FileUtils.deleteQuietly(new File(releasesDir, INDEX_HTM))
				FileUtils.deleteQuietly(new File(releasesDir, DIST_STATIC_DIR_NAME))

				releasesDir.mkdirs()

				copyStaticAssetsTo(releasesDir)

				JtwigTemplate template = JtwigTemplate.classpathTemplate("${DIST_DIR_NAME}/${INDEX_HTM}")
				JtwigModel model = JtwigModel.newModel()
						.with("site_name", project.releaseroo.siteName)
						.with("project_name", project.rootDir.name)
						.with("releases", releases)
						.with("now", new Date().toString())

				FileOutputStream fos = new FileOutputStream(new File(releasesDir, INDEX_HTM))
				template.render(model, fos)
				fos.close()
			}
		}
	}

	def copyStaticAssetsTo(File destDirFile) {
		URI distUri = ReleaserooPlugin.class.getClassLoader().getResource(DIST_DIR_NAME).toURI()
		FileSystem fileSystem = null
		try {
			fileSystem = FileSystems.newFileSystem(distUri, Collections.emptyMap())

			// cannot use Paths.get()/File.toPath()/Path.toFile() because the obtained path would have an incorrect
			// provider, different than the one used by the path provided by walkFileTree()
			Path dist = fileSystem.getPath("/${DIST_DIR_NAME}")
			Path destDir = fileSystem.getPath(destDirFile.toPath().toString())

			Files.walkFileTree(Paths.get(distUri), new SimpleFileVisitor<Path>() {
				@Override
				FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
//	                /dist/static/stylesheet.css
//	                /dist/static/exclamation_mark.svg
//	                /dist/static/android.svg
//	                /dist/index.htm

					if (path.fileName.toString() != INDEX_HTM) {
						Path pathNoDist = dist.relativize(path) // removes /dist

						if (pathNoDist.nameCount == 1) {
							// this file will be copied in the root of destDir
							copy(path, destDirFile)

						} else {
							// this file will be copied in the corresponding subdirectory of destDir
							Path targetPath = destDir.resolve(pathNoDist)

							def parentTargetFile = new File(targetPath.parent.toString())
							parentTargetFile.mkdirs()

							copy(path, parentTargetFile)
						}
					}

					return FileVisitResult.CONTINUE
				}

				private void copy(Path src, File targetDir) {
					String resName = src.toString().substring(1)
					InputStream stream = ReleaserooPlugin.class.getClassLoader().getResourceAsStream(resName)
					def targetFile = new File(targetDir, src.fileName.toString())
					targetFile.bytes = stream.bytes
				}
			})
		} finally {
			if (fileSystem != null) {
				fileSystem.close()
			}
		}
	}

	private Task createReleaseTask(Project project, variant, String profile) {
		return project.tasks.create(name: "release" + profile.capitalize(), type: Copy) {
			group "releaseroo"
			description "Packages a release APK for the profile ${profile} and copies the Proguard mapping file into ${project.releaseroo.releasesDir}"

			String today = new Date().format(Release.DATE_FORMAT)
			def versionName = variant.versionName.replace("_", "-")
			def outBaseName = "${profile}_${versionName}_${variant.versionCode}_${today}"

			from(variant.outputs[0].outputFile.path) {
				rename '.*', "${outBaseName}.apk"
			}
			if (variant.mappingFile != null) {
				from(variant.mappingFile.path) {
					rename '.*', "${outBaseName}_mapping.txt"
				}
			}
			into project.releaseroo.releasesDir
		}
	}

}

class ReleaserooPluginExtension {
	String siteName = "Releaseroo"
	File releasesDir
}
