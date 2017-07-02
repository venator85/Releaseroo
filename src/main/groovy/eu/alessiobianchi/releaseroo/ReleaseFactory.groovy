package eu.alessiobianchi.releaseroo

import java.text.SimpleDateFormat

class ReleaseFactory {

	private List<Factory> factories

	ReleaseFactory() {
		factories = new ArrayList<>()
		factories.add(new LegacyFactory())
		factories.add(new ReleaserooFactory())
	}

	Release create(File file) {
		for (Factory factory : factories) {
			def name = file.name.substring(0, file.name.lastIndexOf('.')) // remove extension
			try {
				Release release = factory.create(file, name)
				if (release != null) {
					return release
				}
			} catch (Exception ignore) {
			}
		}
		throw new IllegalArgumentException("Unable to create a release for ${file}")
	}

	interface Factory {
		Release create(File file, String apkName)
	}

	/**
	 * ${profile}_${versionName}_${versionCode}_${buildDate}*/
	static class ReleaserooFactory implements Factory {
		@Override
		Release create(File file, String apkName) {
			def split = apkName.split('_')
			if (split.length == 4) {
				def profile = split[0]
				def versionName = split[1]
				def versionCode = split[2]
				def buildDate = new SimpleDateFormat(Release.DATE_FORMAT).parse(split[3])
				return new Release(file, profile, versionName, versionCode, buildDate)
			} else {
				return null
			}
		}
	}

	static class LegacyFactory implements Factory {
		@Override
		Release create(File file, String apkName) {
			def split = apkName.split('_')
			if (split.length == 7) { // SharigoBusiness_1.0_20161122_8_real_20161122_223000
				def profile = split[4]
				def versionName = "${split[1]}_${split[2]}"
				def versionCode = split[3]
				def buildDate = new SimpleDateFormat(Release.DATE_FORMAT).parse("${split[5]}_${split[6]}")
				return new Release(file, profile, versionName, versionCode, buildDate)
			} else if (split.length == 6) { // SharigoBusiness_1.04_8_real_20161122_223000
				def profile = split[3]
				def versionName = split[1]
				def versionCode = split[2]
				def buildDate = new SimpleDateFormat(Release.DATE_FORMAT).parse("${split[3]}_${split[4]}")
				return new Release(file, profile, versionName, versionCode, buildDate)
			} else if (split.length == 5) { // cagliari_1.0.1_2_20160521_150456
				def profile = split[0]
				def versionName = split[1]
				def versionCode = split[2]
				def buildDate = new SimpleDateFormat(Release.DATE_FORMAT).parse("${split[3]}_${split[4]}")
				return new Release(file, profile, versionName, versionCode, buildDate)
			}
			return null
		}
	}

}
